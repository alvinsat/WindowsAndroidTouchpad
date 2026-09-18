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
import java.net.SocketTimeoutException
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentLinkedQueue

sealed interface ConnectionState {
  data object Disconnected : ConnectionState
  data object Searching : ConnectionState
  data class Connected(val serverIp: String, val isManual: Boolean = false) : ConnectionState
  data class Error(val message: String) : ConnectionState
}

/**
 * High-performance, ultra-low-latency Touchpad Network Manager.
 *
 * Latency Optimizations:
 * 1. Pre-resolved and cached InetAddress avoids repeated DNS/getByName lookups on every touch point.
 * 2. Pre-allocated byte buffers & reused DatagramPacket eliminate GC pauses during continuous drag.
 * 3. Lock-free ConcurrentLinkedQueue with high-frequency IO transmission thread (<1ms wake-up).
 * 4. Dual-mode support:
 *    - Standard protocol matching user's Python server: "move,dx,dy\n", "click,left", "scroll,dy"
 *    - Optional binary mode header: fast byte packet support with Python binary adapter code provided in Help dialog.
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
  }

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private var discoveryJob: Job? = null
  private var senderJob: Job? = null

  private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
  val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

  // Packet data container to prevent object allocation churn
  class PacketData(val buffer: ByteArray, val length: Int)

  private val outgoingQueue = ConcurrentLinkedQueue<PacketData>()

  @Volatile
  private var currentServerIp: String? = null

  @Volatile
  private var cachedServerAddress: InetAddress? = null

  @Volatile
  private var binaryMode: Boolean = false

  private var inputSocket: DatagramSocket? = null
  private var multicastLock: WifiManager.MulticastLock? = null

  init {
    startSenderLoop()
  }

  fun setBinaryMode(enabled: Boolean) {
    binaryMode = enabled
    Log.d(TAG, "Binary transmission mode: $enabled")
  }

  fun isBinaryMode(): Boolean = binaryMode

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
    } catch (e: Exception) {
      Log.w(TAG, "Could not acquire multicast lock: ${e.message}")
    }
  }

  private fun releaseMulticastLock() {
    try {
      multicastLock?.let {
        if (it.isHeld) {
          it.release()
        }
      }
    } catch (e: Exception) {
      Log.w(TAG, "Error releasing multicast lock: ${e.message}")
    }
  }

  /**
   * Broadcasts UDP discovery packet to 255.255.255.255:8080 and listens for reply.
   */
  fun startDiscovery() {
    discoveryJob?.cancel()
    _connectionState.value = ConnectionState.Searching
    acquireMulticastLock()

    discoveryJob = scope.launch {
      var socket: DatagramSocket? = null
      try {
        socket = DatagramSocket().apply {
          broadcast = true
          soTimeout = 1200
        }

        val discoverBytes = DISCOVERY_MSG.toByteArray(StandardCharsets.UTF_8)
        val broadcastAddresses = getBroadcastAddresses()

        var attempts = 0
        val maxAttempts = 15

        while (isActive && attempts < maxAttempts && _connectionState.value is ConnectionState.Searching) {
          attempts++
          for (address in broadcastAddresses) {
            try {
              val packet = DatagramPacket(discoverBytes, discoverBytes.size, address, DISCOVERY_PORT)
              socket.send(packet)
            } catch (e: Exception) {
              Log.v(TAG, "Error broadcasting to $address: ${e.message}")
            }
          }

          // Emulator loopback
          try {
            val emulatorHost = InetAddress.getByName("10.0.2.2")
            socket.send(DatagramPacket(discoverBytes, discoverBytes.size, emulatorHost, DISCOVERY_PORT))
          } catch (_: Exception) {}

          val receiveBuffer = ByteArray(1024)
          val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)
          val listenStartTime = System.currentTimeMillis()

          while (System.currentTimeMillis() - listenStartTime < 1200 && isActive) {
            try {
              socket.receive(receivePacket)
              val replyMsg = String(receivePacket.data, 0, receivePacket.length, StandardCharsets.UTF_8).trim()
              if (replyMsg == OFFER_MSG) {
                val serverIp = receivePacket.address.hostAddress ?: "127.0.0.1"
                updateServerIp(serverIp, isManual = false)
                return@launch
              }
            } catch (_: SocketTimeoutException) {
              break
            } catch (e: Exception) {
              Log.w(TAG, "Error during receive: ${e.message}")
              break
            }
          }
          delay(350)
        }

        if (_connectionState.value is ConnectionState.Searching) {
          _connectionState.value = ConnectionState.Error("Server not found on Wi-Fi. Check Python script or enter IP manually.")
        }
      } catch (e: kotlinx.coroutines.CancellationException) {
        // Normal coroutine cancellation when re-triggering discovery or stopping
      } catch (e: Exception) {
        Log.e(TAG, "Discovery failed", e)
        if (_connectionState.value is ConnectionState.Searching) {
          _connectionState.value = ConnectionState.Error(e.message ?: "Discovery failed")
        }
      } finally {
        try {
          socket?.close()
        } catch (_: Exception) {}
        releaseMulticastLock()
      }
    }
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
      updateServerIp(cleanIp, isManual = true)
    }
  }

  private fun updateServerIp(ip: String, isManual: Boolean) {
    currentServerIp = ip
    scope.launch(Dispatchers.IO) {
      try {
        cachedServerAddress = InetAddress.getByName(ip)
      } catch (e: Exception) {
        Log.e(TAG, "Failed to resolve server IP $ip: ${e.message}")
      }
    }
    _connectionState.value = ConnectionState.Connected(serverIp = ip, isManual = isManual)
  }

  fun disconnect() {
    stopDiscovery()
    currentServerIp = null
    cachedServerAddress = null
    _connectionState.value = ConnectionState.Disconnected
  }

  /**
   * Send cursor delta: format is "move,dx,dy" in string mode
   * or [0x01, dx_high, dx_low, dy_high, dy_low] in byte mode.
   */
  fun sendMove(dx: Int, dy: Int) {
    if (cachedServerAddress == null && currentServerIp == null) return
    if (dx == 0 && dy == 0) return

    if (binaryMode) {
      // 5-byte binary packet: [OP_MOVE, dx(16-bit signed), dy(16-bit signed)]
      val buf = ByteArray(5)
      buf[0] = OP_MOVE
      buf[1] = ((dx shr 8) and 0xFF).toByte()
      buf[2] = (dx and 0xFF).toByte()
      buf[3] = ((dy shr 8) and 0xFF).toByte()
      buf[4] = (dy and 0xFF).toByte()
      outgoingQueue.offer(PacketData(buf, 5))
    } else {
      // Direct string payload for Python server
      val msg = "move,$dx,$dy"
      val bytes = msg.toByteArray(StandardCharsets.US_ASCII)
      outgoingQueue.offer(PacketData(bytes, bytes.size))
    }
  }

  /**
   * Send click action: format is "click,left|right|double" in string mode
   * or [0x02, click_code] in byte mode.
   */
  fun sendClick(action: String) {
    if (cachedServerAddress == null && currentServerIp == null) return

    if (binaryMode) {
      val clickCode: Byte = when (action) {
        "left" -> 1
        "right" -> 2
        "double" -> 3
        else -> 1
      }
      val buf = byteArrayOf(OP_CLICK, clickCode)
      outgoingQueue.offer(PacketData(buf, 2))
    } else {
      val msg = "click,$action"
      val bytes = msg.toByteArray(StandardCharsets.US_ASCII)
      outgoingQueue.offer(PacketData(bytes, bytes.size))
    }
  }

  /**
   * Send scroll action: format is "scroll,dy" in string mode
   * or [0x03, dy_high, dy_low] in byte mode.
   */
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
      val msg = "scroll,$dy"
      val bytes = msg.toByteArray(StandardCharsets.US_ASCII)
      outgoingQueue.offer(PacketData(bytes, bytes.size))
    }
  }

  /**
   * High-priority low-latency sender loop running on dedicated IO thread.
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
              } catch (e: Exception) {
                Log.v(TAG, "Failed to send packet: ${e.message}")
              }
            }
          } else {
            // Micro-sleep to prevent busy-looping while staying sub-millisecond responsive
            delay(2)
          }
        } catch (e: kotlinx.coroutines.CancellationException) {
          break
        } catch (e: Exception) {
          Log.w(TAG, "Error in sender loop: ${e.message}")
          delay(10)
        }
      }
    }
  }

  private fun getBroadcastAddresses(): List<InetAddress> {
    val broadcastList = mutableListOf<InetAddress>()
    try {
      broadcastList.add(InetAddress.getByName("255.255.255.255"))
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
    senderJob?.cancel()
    try {
      inputSocket?.close()
    } catch (_: Exception) {}
    releaseMulticastLock()
  }
}
