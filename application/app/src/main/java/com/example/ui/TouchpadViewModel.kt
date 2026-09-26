package com.example.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.network.ConnectionState
import com.example.network.TouchpadNetworkManager
import com.example.util.VibrationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sign

data class TouchpadSettings(
  val pointerSensitivity: Float = 1.3f,
  val pointerAcceleration: Boolean = true,
  val scrollSensitivity: Float = 1.0f,
  val invertScroll: Boolean = false,
  val invertHScroll: Boolean = false,
  val swapFourFingerNavDirection: Boolean = false,
  val binaryByteMode: Boolean = false,
  val hapticFeedback: Boolean = true,
  val infinityScroll: Boolean = true,
  val hideNotificationBar: Boolean = true,
  val hideNavigationBar: Boolean = true
)

class TouchpadViewModel(application: Application) : AndroidViewModel(application) {
  private val prefs: SharedPreferences = application.getSharedPreferences("touchpad_prefs", Context.MODE_PRIVATE)

  val networkManager = TouchpadNetworkManager(application)
  val connectionState: StateFlow<ConnectionState> = networkManager.connectionState

  private val _settings = MutableStateFlow(
    TouchpadSettings(
      pointerSensitivity = prefs.getFloat("pointer_sens", 1.3f),
      pointerAcceleration = prefs.getBoolean("pointer_accel", true),
      scrollSensitivity = prefs.getFloat("scroll_sens", 1.0f),
      invertScroll = prefs.getBoolean("invert_scroll", false),
      invertHScroll = prefs.getBoolean("invert_hscroll", false),
      swapFourFingerNavDirection = prefs.getBoolean("swap_four_finger_nav", false),
      binaryByteMode = prefs.getBoolean("binary_mode", false),
      hapticFeedback = prefs.getBoolean("haptic_feedback", true),
      infinityScroll = prefs.getBoolean("infinity_scroll", true),
      hideNotificationBar = prefs.getBoolean("hide_notification_bar", true),
      hideNavigationBar = prefs.getBoolean("hide_navigation_bar", true)
    )
  )
  val settings: StateFlow<TouchpadSettings> = _settings.asStateFlow()

  private val _showManualIpDialog = MutableStateFlow(false)
  val showManualIpDialog: StateFlow<Boolean> = _showManualIpDialog.asStateFlow()

  private val _showHelpDialog = MutableStateFlow(false)
  val showHelpDialog: StateFlow<Boolean> = _showHelpDialog.asStateFlow()

  private val _showSettingsDialog = MutableStateFlow(false)
  val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

  val isUsbMode: Boolean
    get() = networkManager.isUsbMode()

  // Dragging state (Left-mouse button down during drag gesture)
  private val _isDragging = MutableStateFlow(false)
  val isDragging: StateFlow<Boolean> = _isDragging.asStateFlow()

  // Drag Lock state (Locks left mouse button down for effortless dragging in Unity, Unreal, and Windows notifications)
  private val _isDragLocked = MutableStateFlow(false)
  val isDragLocked: StateFlow<Boolean> = _isDragLocked.asStateFlow()

  // High-precision sub-pixel delta accumulators
  private var accumulatedDx = 0f
  private var accumulatedDy = 0f

  // Adaptive low-pass / EMA smoothing filters for silky smooth motion
  private var smoothFilterDx = 0f
  private var smoothFilterDy = 0f

  // Scroll accumulators (Vertical and Horizontal)
  private var accumulatedScrollDy = 0f
  private var accumulatedScrollDx = 0f

  // Kinetic / Infinity scroll momentum coroutine job
  private var infinityScrollJob: Job? = null

  init {
    networkManager.setBinaryMode(_settings.value.binaryByteMode)
    startDiscovery()
  }

  fun startDiscovery() {
    networkManager.startDiscovery()
  }

  fun disconnect() {
    networkManager.disconnect()
  }

  fun setManualIp(ip: String) {
    val clean = ip.trim()
    networkManager.setManualIp(clean)
    _showManualIpDialog.value = false
  }

  fun setShowManualIpDialog(show: Boolean) {
    _showManualIpDialog.value = show
  }

  fun setShowHelpDialog(show: Boolean) {
    _showHelpDialog.value = show
  }

  fun setShowSettingsDialog(show: Boolean) {
    _showSettingsDialog.value = show
  }

  /**
   * Fast inline sensitivity slider update
   */
  fun setPointerSensitivity(sensitivity: Float) {
    val current = _settings.value
    _settings.value = current.copy(pointerSensitivity = sensitivity)
    prefs.edit().putFloat("pointer_sens", sensitivity).apply()
  }

  fun updateSettings(
    pointer: Float,
    accel: Boolean,
    scroll: Float,
    invert: Boolean,
    invertH: Boolean,
    swapFourFingerNav: Boolean,
    binaryByte: Boolean,
    haptics: Boolean,
    infinity: Boolean = true,
    hideNotification: Boolean = true,
    hideNavigation: Boolean = true
  ) {
    _settings.value = TouchpadSettings(
      pointerSensitivity = pointer,
      pointerAcceleration = accel,
      scrollSensitivity = scroll,
      invertScroll = invert,
      invertHScroll = invertH,
      swapFourFingerNavDirection = swapFourFingerNav,
      binaryByteMode = binaryByte,
      hapticFeedback = haptics,
      infinityScroll = infinity,
      hideNotificationBar = hideNotification,
      hideNavigationBar = hideNavigation
    )
    networkManager.setBinaryMode(binaryByte)

    prefs.edit()
      .putFloat("pointer_sens", pointer)
      .putBoolean("pointer_accel", accel)
      .putFloat("scroll_sens", scroll)
      .putBoolean("invert_scroll", invert)
      .putBoolean("invert_hscroll", invertH)
      .putBoolean("swap_four_finger_nav", swapFourFingerNav)
      .putBoolean("binary_mode", binaryByte)
      .putBoolean("haptic_feedback", haptics)
      .putBoolean("infinity_scroll", infinity)
      .putBoolean("hide_notification_bar", hideNotification)
      .putBoolean("hide_navigation_bar", hideNavigation)
      .apply()
  }

  /**
   * 4-Finger swipe navigation event (Forward or Backward)
   */
  fun onFourFingerSwipe(isForward: Boolean) {
    val action = if (isForward) "forward" else "back"
    if (_settings.value.hapticFeedback) {
      VibrationHelper.vibrateNavigation(getApplication())
    }
    networkManager.sendNavigation(action)
  }

  fun setInfinityScroll(enabled: Boolean) {
    val current = _settings.value
    _settings.value = current.copy(infinityScroll = enabled)
    prefs.edit().putBoolean("infinity_scroll", enabled).apply()
    if (!enabled) {
      cancelInfinityScroll()
    }
  }

  /**
   * Reset motion smoothing filter on new touch contact and cancel kinetic momentum
   */
  fun resetPointerFilter() {
    cancelInfinityScroll()
    smoothFilterDx = 0f
    smoothFilterDy = 0f
    accumulatedDx = 0f
    accumulatedDy = 0f
  }

  /**
   * Ultra-smooth cursor drag processor with adaptive EMA low-pass filtering.
   * Eliminates digitizer jitter on slow moves while preserving 1:1 instantaneous response on fast swipes.
   */
  fun onPointerDrag(rawDx: Float, rawDy: Float, isDragMode: Boolean = false) {
    val current = _settings.value
    val speed = kotlin.math.sqrt(rawDx * rawDx + rawDy * rawDy)

    // Dynamic smoothing factor: higher smoothing for slow fine-grained moves, direct pass-through for fast moves
    val alpha = (speed / 18f).coerceIn(0.65f, 1.0f)
    smoothFilterDx = alpha * rawDx + (1f - alpha) * smoothFilterDx
    smoothFilterDy = alpha * rawDy + (1f - alpha) * smoothFilterDy

    var factor = current.pointerSensitivity

    if (isDragMode || _isDragging.value || _isDragLocked.value) {
      // Steady, controlled tracking while dragging objects or selecting text
      factor *= 0.96f
    } else if (current.pointerAcceleration) {
      // Natural ballistics curve for normal navigation
      val accelFactor = if (speed > 24f) 1.55f else if (speed > 10f) 1.25f else 1.0f
      factor *= accelFactor
    }

    accumulatedDx += smoothFilterDx * factor
    accumulatedDy += smoothFilterDy * factor

    val intDx = accumulatedDx.toInt()
    val intDy = accumulatedDy.toInt()

    if (intDx != 0 || intDy != 0) {
      accumulatedDx -= intDx
      accumulatedDy -= intDy
      networkManager.sendMove(intDx, intDy)
    }
  }

  /**
   * Start dragging (Mouse left-button held down)
   */
  fun sendDragStart() {
    _isDragging.value = true
    sendClick("down")
  }

  /**
   * End dragging (Mouse left-button released)
   */
  fun sendDragEnd() {
    if (_isDragLocked.value) {
      // Keep dragging active if Drag Lock is engaged
      return
    }
    _isDragging.value = false
    sendClick("up")
  }

  /**
   * Toggle Drag Lock mode: holds left mouse button down continuously.
   * Perfect for dragging windows, dock tabs, sliders in Unity/Unreal Engine,
   * or moving Windows notifications without having to continuously hold down fingers.
   */
  fun toggleDragLock() {
    val newState = !_isDragLocked.value
    _isDragLocked.value = newState
    _isDragging.value = newState
    if (_settings.value.hapticFeedback) {
      VibrationHelper.vibrateDragState(getApplication(), newState)
    }
    if (newState) {
      sendClick("down")
    } else {
      sendClick("up")
    }
  }

  fun unlockDrag() {
    if (_isDragLocked.value) {
      _isDragLocked.value = false
      _isDragging.value = false
      if (_settings.value.hapticFeedback) {
        VibrationHelper.vibrateDragState(getApplication(), false)
      }
      sendClick("up")
    }
  }

  /**
   * Backward-compatible vertical scroll
   */
  fun onScrollDelta(rawDy: Float) {
    onScrollDelta(0f, rawDy)
  }

  /**
   * Two-finger scrolling processor: handles both vertical and horizontal scroll.
   * Swiping left produces horizontal scroll (dx < 0); swiping up/down produces vertical scroll (dy).
   * Directional locking prevents jittery diagonal drift.
   */
  fun onScrollDelta(rawDx: Float, rawDy: Float) {
    val current = _settings.value
    val factor = current.scrollSensitivity
    val invertVMultiplier = if (current.invertScroll) -1f else 1f
    val invertHMultiplier = if (current.invertHScroll) -1f else 1f

    val absX = abs(rawDx)
    val absY = abs(rawDy)

    // Directional bias:
    // When swiping predominantly horizontally (e.g. swipe left): suppress minor vertical wobble
    // When swiping predominantly vertically: suppress minor horizontal wobble
    val processX = absX > 1.8f * absY || absX > 4f
    val processY = absY > 1.8f * absX || absY > 4f
    val isDiagonal = !processX && !processY

    if (processX || isDiagonal) {
      accumulatedScrollDx += rawDx * factor * invertHMultiplier
      val scrollThreshold = 14f
      val stepsX = (accumulatedScrollDx / scrollThreshold).toInt()
      if (stepsX != 0) {
        accumulatedScrollDx -= stepsX * scrollThreshold
        networkManager.sendHScroll(stepsX)
      }
    }

    if (processY || isDiagonal) {
      accumulatedScrollDy += rawDy * factor * invertVMultiplier
      val scrollThreshold = 14f
      val stepsY = (accumulatedScrollDy / scrollThreshold).toInt()
      if (stepsY != 0) {
        accumulatedScrollDy -= stepsY * scrollThreshold
        networkManager.sendScroll(stepsY)
      }
    }
  }

  /**
   * Start kinetic / infinity scroll when releasing fingers at high velocity ("let go while moving fast").
   * Continues smoothly decaying with natural exponential friction until stopped or touched again.
   */
  fun startInfinityScroll(velX: Float, velY: Float) {
    if (!_settings.value.infinityScroll) return
    infinityScrollJob?.cancel()

    val speed = hypot(velX, velY)
    if (speed < 0.28f) return

    infinityScrollJob = viewModelScope.launch {
      var curVx = velX * 16f
      var curVy = velY * 16f
      val friction = 0.952f // Smooth exponential coasting decay

      while (isActive && (abs(curVx) > 0.25f || abs(curVy) > 0.25f)) {
        onScrollDelta(curVx, curVy)
        curVx *= friction
        curVy *= friction
        delay(16) // ~60 FPS cadence
      }
      infinityScrollJob = null
    }
  }

  /**
   * Instantly cancel running infinity scroll momentum (e.g. when user touches touchpad again)
   */
  fun cancelInfinityScroll() {
    infinityScrollJob?.cancel()
    infinityScrollJob = null
  }

  /**
   * Mouse click event (left, right, double, middle, down, up) with tactile vibration
   */
  fun sendClick(action: String) {
    if (_settings.value.hapticFeedback) {
      val app = getApplication<Application>()
      when (action) {
        "double" -> VibrationHelper.vibrateDoubleClick(app)
        "right" -> VibrationHelper.vibrateRightClick(app)
        "middle" -> VibrationHelper.vibrateMiddleClick(app)
        "down" -> VibrationHelper.vibrateDragState(app, true)
        "up" -> VibrationHelper.vibrateDragState(app, false)
        else -> VibrationHelper.vibrateClick(app)
      }
    }
    networkManager.sendClick(action)
  }

  override fun onCleared() {
    super.onCleared()
    cancelInfinityScroll()
    networkManager.cleanup()
  }
}
