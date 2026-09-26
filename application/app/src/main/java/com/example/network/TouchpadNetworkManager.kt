package com.example.network

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.Socket
import java.net.SocketTimeoutException
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Transport connection priority:
 * 1. ADB_REVERSE: Wired USB Cable mode via ADB reverse (127.0.0.1:8081 via TCP stream, 0ms lag)
 * 2. LOCAL_WIFI: Local wireless Wi-Fi UDP datagram connection
 * 3. USB_TETHERING: USB Tethering UDP datagram connection (rndis / usb interfaces / 192.168.42.x)
 * 4. MANUAL: User explicitly specified IP address
 */
enum class ConnectionType {
  ADB_REVERSE,
  LOCAL_WIFI,
  USB_TETHERING,
  MANUAL
}

sealed interface ConnectionState {
  data object Disconnected : ConnectionState
  data object Searching : ConnectionState
  data class Connected(
    val serverIp: String,
    val connectionType: ConnectionType = ConnectionType.LOCAL_WIFI,
    val isManual: Boolean = false,
    val isUsb: Boolean = (connectionType == ConnectionType.ADB_REVERSE || connectionType == ConnectionType.USB_TETHERING)
  ) : ConnectionState
  data class Error(val message: String) : ConnectionState
}

/**
 * High-performance, ultra-low-latency Touchpad Network Manager.
 *
 * Automatic Connection Priority Hierarchy:
 * Priority 1: ADB Reverse (USB Cable Mode 127.0.0.1:8081 TCP)
 * Priority 2: Local Wi-Fi (UDP auto-discovery on port 8080/8081)
 * Priority 3: USB Tethering (UDP on rndis/usb tethering interfaces)
 */
class TouchpadNetworkManager(private val context: Context) {
  companion object {
    private const val TAG = "TouchpadNet"
    const val DISCOVERY_PORT = 8080
    const val INPUT_PORT = 8081
    const val DISCOVERY_MSG = "TOUCHPAD_DISCOVER"
    const val OFFER_MSG = "TOUCHPAD_OFFER"

    // Binary packet opcodes for ultra-low overhead
    const val OP_MOVE: Byte = 0x01
    const val OP_CLICK: Byte = 0x02
    const val OP_SCROLL: Byte = 0x03
    const val OP_HSCROLL: Byte = 0x04
    const val OP_NAV: Byte = 0x05
  }

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private var discoveryJob: Job? = null
  private var adbProbeJob: Job? = null
  private var senderJob: Job? = null

  private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
  val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

  // Packet data container to prevent object allocation churn
  class PacketData(val buffer: ByteArray, val length: Int)

  private val outgoingQueue = ConcurrentLinkedQueue<PacketData>()

  @Volatile
  private var manualServerIp: String? = null

  @Volatile
  private var discoveredAdbReverse: Boolean = false

  @Volatile
  private var discoveredWifiIp: String? = null

  @Volatile
  private var discoveredUsbTetheringIp: String? = null

  @Volatile
  private var activeConnectionType: ConnectionType? = null

  @Volatile
  private var currentServerIp: String? = null

  @Volatile
  private var cachedServerAddress: InetAddress? = null

  @Volatile
  private var binaryMode: Boolean = false

  @Volatile
  private var tcpSocket: Socket? = null
  private var tcpOutputStream: java.io.OutputStream? = null

  private var inputSocket: DatagramSocket? = null
  private var multicastLock: WifiManager.MulticastLock? = null

  init {
    startSenderLoop()
  }

  fun setBinaryMode(enabled: Boolean) {
    binaryMode = enabled
  }

  fun isBinaryMode(): Boolean = binaryMode

  fun isUsbMode(): Boolean {
    val state = _connectionState.value
    return state is ConnectionState.Connected && state.isUsb
  }

  fun setUsbMode(enabled: Boolean) {
    if (enabled) {
      setManualIp("127.0.0.1")
    } else {
      manualServerIp = null
      startDiscovery()
    }
  }

  private fun acquireMulticastLock() {
    try {
      if (multicastLock == null) {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        multicastLock = wifiManager?.createMulticastLock("TouchpadMulticast")?.apply {
          setReferenceCounted(true)
        }
      }
      multicastLock?.let {
        if (!it.isHeld) {
          it.acquire()
        }
      }
    } catch (_: Exception) {}
  }

  private fun releaseMulticastLock() {
    try {
      multicastLock?.let {
        if (it.isHeld) {
          it.release()
        }
      }
    } catch (_: Exception) {}
  }

  /**
   * Starts automatic priority discovery:
   * Probes ADB Reverse (Priority 1), Wi-Fi (Priority 2), and USB Tethering (Priority 3).
   */
  fun startDiscovery() {
    manualServerIp = null
    discoveryJob?.cancel()
    adbProbeJob?.cancel()
    _connectionState.value = ConnectionState.Searching
    acquireMulticastLock()

    Log.i(TAG, "[Event] Auto-discovery started")
    startAdbReverseProbe()
    startUdpDiscovery()
  }

  /**
   * Continuous Priority 1 probe for ADB Reverse (127.0.0.1:8081).
   * Automatically preempts Wi-Fi or USB Tethering whenever cable is plugged in.
   */
  private fun startAdbReverseProbe() {
    adbProbeJob?.cancel()
    adbProbeJob = scope.launch(Dispatchers.IO) {
      while (isActive && manualServerIp == null) {
        if (tcpSocket == null || tcpSocket?.isClosed == true || tcpSocket?.isConnected != true) {
          try {
            val socket = Socket()
            socket.tcpNoDelay = true
            socket.trafficClass = 0x10 // IPTOS_LOWDELAY
            socket.connect(java.net.InetSocketAddress("127.0.0.1", INPUT_PORT), 1000)
            tcpSocket = socket
            tcpOutputStream = socket.getOutputStream()
            discoveredAdbReverse = true
            evaluateAndApplyPriority()

            // Initial handshake ping
            val pingBytes = "ping\n".toByteArray(StandardCharsets.US_ASCII)
            socket.getOutputStream().write(pingBytes)
            socket.getOutputStream().flush()

            // Maintain persistent connection and monitor liveness via heartbeat
            while (isActive && manualServerIp == null && tcpSocket == socket && !socket.isClosed) {
              delay(2500)
              try {
                socket.getOutputStream().write(pingBytes)
                socket.getOutputStream().flush()
              } catch (_: Exception) {
                break
              }
            }
          } catch (_: Exception) {
            // Socket not available or connection refused
          } finally {
            discoveredAdbReverse = false
            try { tcpOutputStream?.close() } catch (_: Exception) {}
            try { tcpSocket?.close() } catch (_: Exception) {}
            tcpOutputStream = null
            tcpSocket = null
            evaluateAndApplyPriority()
          }
        }
        delay(1500)
      }
    }
  }

  /**
   * UDP discovery broadcaster scanning Wi-Fi and USB Tethering interfaces.
   */
  private fun startUdpDiscovery() {
    discoveryJob?.cancel()
    discoveryJob = scope.launch(Dispatchers.IO) {
      var socket: DatagramSocket? = null
      try {
        socket = DatagramSocket().apply {
          broadcast = true
          soTimeout = 1000
        }

        val discoverBytes = DISCOVERY_MSG.toByteArray(StandardCharsets.UTF_8)
        val broadcastAddresses = getBroadcastAddresses()

        while (isActive && manualServerIp == null) {
          // If Priority 1 (ADB reverse) is already connected, wait before next broad search
          if (discoveredAdbReverse) {
            delay(2000)
            continue
          }

          // Broadcast to all detected interfaces & subnets
          for (address in broadcastAddresses) {
            try {
              val packet = DatagramPacket(discoverBytes, discoverBytes.size, address, DISCOVERY_PORT)
              socket.send(packet)
            } catch (_: Exception) {}
          }

          // Emulator loopback
          try {
            val emulatorHost = InetAddress.getByName("10.0.2.2")
            socket.send(DatagramPacket(discoverBytes, discoverBytes.size, emulatorHost, DISCOVERY_PORT))
          } catch (_: Exception) {}

          // Listen for responses
          val receiveBuffer = ByteArray(1024)
          val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)
          val listenStartTime = System.currentTimeMillis()

          while (System.currentTimeMillis() - listenStartTime < 1000 && isActive) {
            try {
              socket.receive(receivePacket)
              val replyMsg = String(receivePacket.data, 0, receivePacket.length, StandardCharsets.UTF_8).trim()
              if (replyMsg == OFFER_MSG) {
                val serverIp = receivePacket.address.hostAddress ?: "127.0.0.1"
                val isTether = isUsbTetheringAddress(receivePacket.address)
                if (isTether) {
                  discoveredUsbTetheringIp = serverIp
                } else {
                  discoveredWifiIp = serverIp
                }
                evaluateAndApplyPriority()
              }
            } catch (_: SocketTimeoutException) {
              break
            } catch (_: Exception) {
              break
            }
          }
          delay(1200)
        }
      } catch (_: kotlinx.coroutines.CancellationException) {
      } catch (_: Exception) {
      } finally {
        try { socket?.close() } catch (_: Exception) {}
        releaseMulticastLock()
      }
    }
  }

  /**
   * Evaluates current candidates according to the strict priority rules:
   * Priority 1: ADB Reverse (127.0.0.1 TCP stream)
   * Priority 2: Local Wi-Fi (UDP)
   * Priority 3: USB Tethering (UDP)
   */
  @Synchronized
  private fun evaluateAndApplyPriority() {
    if (manualServerIp != null) {
      val ip = manualServerIp!!
      val type = if (ip == "127.0.0.1" || ip.equals("localhost", ignoreCase = true)) {
        ConnectionType.ADB_REVERSE
      } else {
        ConnectionType.MANUAL
      }
      applyActiveConnection(ip, type, isManual = true)
      return
    }

    // Priority 1: ADB Reverse Cable Mode
    if (discoveredAdbReverse) {
      applyActiveConnection("127.0.0.1", ConnectionType.ADB_REVERSE, isManual = false)
      return
    }

    // Priority 2: Local Wi-Fi
    val wifi = discoveredWifiIp
    if (wifi != null) {
      applyActiveConnection(wifi, ConnectionType.LOCAL_WIFI, isManual = false)
      return
    }

    // Priority 3: USB Tethering
    val tether = discoveredUsbTetheringIp
    if (tether != null) {
      applyActiveConnection(tether, ConnectionType.USB_TETHERING, isManual = false)
      return
    }

    // Still searching
    if (_connectionState.value !is ConnectionState.Searching) {
      activeConnectionType = null
      currentServerIp = null
      cachedServerAddress = null
      _connectionState.value = ConnectionState.Searching
      Log.i(TAG, "[Event] Connection lost, searching...")
    }
  }

  private fun applyActiveConnection(ip: String, type: ConnectionType, isManual: Boolean) {
    if (currentServerIp == ip && activeConnectionType == type && _connectionState.value is ConnectionState.Connected) {
      return
    }
    currentServerIp = ip
    activeConnectionType = type
    scope.launch(Dispatchers.IO) {
      try {
        cachedServerAddress = InetAddress.getByName(ip)
      } catch (_: Exception) {}
    }
    _connectionState.value = ConnectionState.Connected(
      serverIp = ip,
      connectionType = type,
      isManual = isManual
    )
    Log.i(TAG, "[Event] Connected: $type -> $ip")
  }

  private fun isUsbTetheringAddress(remoteAddress: InetAddress): Boolean {
    val host = remoteAddress.hostAddress ?: ""
    if (host.startsWith("192.168.42.") || host.startsWith("192.168.44.") || host.startsWith("192.168.137.")) {
      return true
    }
    try {
      val interfaces = NetworkInterface.getNetworkInterfaces()
      while (interfaces != null && interfaces.hasMoreElements()) {
        val iface = interfaces.nextElement()
        if (!iface.isUp || iface.isLoopback) continue
        val name = iface.name.lowercase()
        if (name.startsWith("rndis") || name.startsWith("usb") || name.startsWith("ncm")) {
          for (addr in iface.interfaceAddresses) {
            val localHost = addr.address.hostAddress ?: ""
            val prefix = localHost.substringBeforeLast(".")
            if (prefix.isNotEmpty() && host.startsWith("$prefix.")) {
              return true
            }
          }
        }
      }
    } catch (_: Exception) {}
    return false
  }

  fun stopDiscovery() {
    discoveryJob?.cancel()
    discoveryJob = null
    releaseMulticastLock()
    if (_connectionState.value is ConnectionState.Searching) {
      _connectionState.value = ConnectionState.Disconnected
    }
  }

  fun setManualIp(ip: String) {
    val cleanIp = ip.trim()
    if (cleanIp.isNotEmpty()) {
      manualServerIp = cleanIp
      evaluateAndApplyPriority()
    }
  }

  fun disconnect() {
    manualServerIp = null
    discoveryJob?.cancel()
    adbProbeJob?.cancel()
    discoveredAdbReverse = false
    discoveredWifiIp = null
    discoveredUsbTetheringIp = null
    activeConnectionType = null
    currentServerIp = null
    cachedServerAddress = null
    try { tcpOutputStream?.close() } catch (_: Exception) {}
    try { tcpSocket?.close() } catch (_: Exception) {}
    tcpOutputStream = null
    tcpSocket = null
    _connectionState.value = ConnectionState.Disconnected
  }

  /**
   * Send cursor delta: format is "move,dx,dy\n" in string mode
   * or [0x01, dx_high, dx_low, dy_high, dy_low] in byte mode.
   */
  fun sendMove(dx: Int, dy: Int) {
    if (cachedServerAddress == null && currentServerIp == null) return
    if (dx == 0 && dy == 0) return

    if (binaryMode) {
      val buf = ByteArray(5)
      buf[0] = OP_MOVE
      buf[1] = ((dx shr 8) and 0xFF).toByte()
      buf[2] = (dx and 0xFF).toByte()
      buf[3] = ((dy shr 8) and 0xFF).toByte()
      buf[4] = (dy and 0xFF).toByte()
      outgoingQueue.offer(PacketData(buf, 5))
    } else {
      val msg = "move,$dx,$dy\n"
      val bytes = msg.toByteArray(StandardCharsets.US_ASCII)
      outgoingQueue.offer(PacketData(bytes, bytes.size))
    }
  }

  /**
   * Send click or drag action.
   * Drag down/up states are sent with burst redundancy (2 packets) to prevent
   * packet drop over UDP when dragging third-party windows or Windows notifications.
   */
  fun sendClick(action: String) {
    if (cachedServerAddress == null && currentServerIp == null) return

    val burstCount = if (action == "down" || action == "up") 2 else 1
    for (i in 0 until burstCount) {
      if (binaryMode) {
        val clickCode: Byte = when (action) {
          "left" -> 1
          "right" -> 2
          "double" -> 3
          "down" -> 4
          "up" -> 5
          "middle" -> 6
          else -> 1
        }
        val buf = byteArrayOf(OP_CLICK, clickCode)
        outgoingQueue.offer(PacketData(buf, 2))
      } else {
        val msg = "click,$action\n"
        val bytes = msg.toByteArray(StandardCharsets.US_ASCII)
        outgoingQueue.offer(PacketData(bytes, bytes.size))
      }
    }
  }

  fun sendDragStart() {
    sendClick("down")
  }

  fun sendDragEnd() {
    sendClick("up")
  }

  fun sendMiddleClick() {
    sendClick("middle")
  }

  fun sendScroll(dy: Int) {
    if (cachedServerAddress == null && currentServerIp == null) return
    if (dy == 0) return

    if (binaryMode) {
      val buf = ByteArray(3)
      buf[0] = OP_SCROLL
      buf[1] = ((dy shr 8) and 0xFF).toByte()
      buf[2] = (dy and 0xFF).toByte()
      outgoingQueue.offer(PacketData(buf, 3))
    } else {
      val msg = "scroll,$dy\n"
      val bytes = msg.toByteArray(StandardCharsets.US_ASCII)
      outgoingQueue.offer(PacketData(bytes, bytes.size))
    }
  }

  fun sendHScroll(dx: Int) {
    if (cachedServerAddress == null && currentServerIp == null) return
    if (dx == 0) return

    if (binaryMode) {
      val buf = ByteArray(3)
      buf[0] = OP_HSCROLL
      buf[1] = ((dx shr 8) and 0xFF).toByte()
      buf[2] = (dx and 0xFF).toByte()
      outgoingQueue.offer(PacketData(buf, 3))
    } else {
      val msg = "hscroll,$dx\n"
      val bytes = msg.toByteArray(StandardCharsets.US_ASCII)
      outgoingQueue.offer(PacketData(bytes, bytes.size))
    }
  }

  /**
   * Send 4-finger navigation event: "forward" or "back"
   * Binary format: [0x05, 0x01] for back, [0x05, 0x02] for forward
   * String format: "nav,forward\n" or "nav,back\n"
   */
  fun sendNavigation(action: String) {
    if (cachedServerAddress == null && currentServerIp == null) return

    if (binaryMode) {
      val navCode: Byte = if (action == "forward") 2 else 1
      val buf = byteArrayOf(OP_NAV, navCode)
      outgoingQueue.offer(PacketData(buf, 2))
    } else {
      val msg = "nav,$action\n"
      val bytes = msg.toByteArray(StandardCharsets.US_ASCII)
      outgoingQueue.offer(PacketData(bytes, bytes.size))
    }
  }

  fun sendNavigation(isForward: Boolean) {
    sendNavigation(if (isForward) "forward" else "back")
  }

  /**
   * Ultra-low-latency sender loop running on dedicated IO thread.
   */
  private fun startSenderLoop() {
    senderJob?.cancel()
    senderJob = scope.launch(Dispatchers.IO) {
      try {
        inputSocket = DatagramSocket().apply {
          trafficClass = 0x10 // IPTOS_LOWDELAY for minimal packet latency
          sendBufferSize = 64 * 1024
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to initialize DatagramSocket", e)
      }

      while (isActive) {
        try {
          val item = outgoingQueue.poll()
          if (item != null) {
            // 1. If currently connected via Priority 1 (ADB Reverse) TCP stream
            if (activeConnectionType == ConnectionType.ADB_REVERSE && tcpOutputStream != null) {
              try {
                tcpOutputStream?.write(item.buffer, 0, item.length)
                tcpOutputStream?.flush()
              } catch (_: Exception) {
                discoveredAdbReverse = false
                try { tcpSocket?.close() } catch (_: Exception) {}
                tcpOutputStream = null
                tcpSocket = null
                evaluateAndApplyPriority()
              }
            } else {
              // 2. Otherwise send over UDP datagram (Priority 2 Local Wi-Fi or Priority 3 USB Tethering)
              var target = cachedServerAddress
              if (target == null && currentServerIp != null) {
                try {
                  target = InetAddress.getByName(currentServerIp)
                  cachedServerAddress = target
                } catch (_: Exception) {}
              }

              if (target != null && inputSocket != null) {
                try {
                  val packet = DatagramPacket(item.buffer, item.length, target, INPUT_PORT)
                  inputSocket?.send(packet)
                } catch (_: Exception) {}
              }
            }
          } else {
            delay(2)
          }
        } catch (_: kotlinx.coroutines.CancellationException) {
          break
        } catch (_: Exception) {
          delay(10)
        }
      }
    }
  }

  private fun getBroadcastAddresses(): List<InetAddress> {
    val broadcastList = mutableListOf<InetAddress>()
    try {
      broadcastList.add(InetAddress.getByName("255.255.255.255"))
      broadcastList.add(InetAddress.getByName("192.168.42.255"))
      broadcastList.add(InetAddress.getByName("192.168.44.255"))
      broadcastList.add(InetAddress.getByName("192.168.137.255"))

      val interfaces = NetworkInterface.getNetworkInterfaces()
      while (interfaces != null && interfaces.hasMoreElements()) {
        val networkInterface = interfaces.nextElement()
        if (networkInterface.isLoopback || !networkInterface.isUp) continue
        for (interfaceAddress in networkInterface.interfaceAddresses) {
          val broadcast = interfaceAddress.broadcast
          if (broadcast != null && !broadcastList.contains(broadcast)) {
            broadcastList.add(broadcast)
          }
        }
      }
    } catch (e: Exception) {
      Log.w(TAG, "Error getting broadcast addresses: ${e.message}")
    }
    return broadcastList
  }

  fun cleanup() {
    stopDiscovery()
    adbProbeJob?.cancel()
    senderJob?.cancel()
    try { tcpOutputStream?.close() } catch (_: Exception) {}
    try { tcpSocket?.close() } catch (_: Exception) {}
    try { inputSocket?.close() } catch (_: Exception) {}
    releaseMulticastLock()
  }
}
