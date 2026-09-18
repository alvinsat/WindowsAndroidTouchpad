package com.example.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import com.example.network.ConnectionState
import com.example.network.TouchpadNetworkManager
import com.example.util.VibrationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.pow
import kotlin.math.sign

data class TouchpadSettings(
  val pointerSensitivity: Float = 1.3f,
  val pointerAcceleration: Boolean = true,
  val scrollSensitivity: Float = 1.0f,
  val invertScroll: Boolean = false,
  val binaryByteMode: Boolean = false,
  val hapticFeedback: Boolean = true
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
      binaryByteMode = prefs.getBoolean("binary_mode", false),
      hapticFeedback = prefs.getBoolean("haptic_feedback", true)
    )
  )
  val settings: StateFlow<TouchpadSettings> = _settings.asStateFlow()

  private val _showManualIpDialog = MutableStateFlow(false)
  val showManualIpDialog: StateFlow<Boolean> = _showManualIpDialog.asStateFlow()

  private val _showHelpDialog = MutableStateFlow(false)
  val showHelpDialog: StateFlow<Boolean> = _showHelpDialog.asStateFlow()

  private val _showSettingsDialog = MutableStateFlow(false)
  val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

  // High-precision sub-pixel delta accumulators
  private var accumulatedDx = 0f
  private var accumulatedDy = 0f

  // Scroll accumulator
  private var accumulatedScrollDy = 0f

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
    networkManager.setManualIp(ip)
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
    binaryByte: Boolean,
    haptics: Boolean
  ) {
    _settings.value = TouchpadSettings(
      pointerSensitivity = pointer,
      pointerAcceleration = accel,
      scrollSensitivity = scroll,
      invertScroll = invert,
      binaryByteMode = binaryByte,
      hapticFeedback = haptics
    )
    networkManager.setBinaryMode(binaryByte)

    prefs.edit()
      .putFloat("pointer_sens", pointer)
      .putBoolean("pointer_accel", accel)
      .putFloat("scroll_sens", scroll)
      .putBoolean("invert_scroll", invert)
      .putBoolean("binary_mode", binaryByte)
      .putBoolean("haptic_feedback", haptics)
      .apply()
  }

  /**
   * Ultra-fast cursor drag processor with optional ballistics / acceleration
   */
  fun onPointerDrag(rawDx: Float, rawDy: Float) {
    val current = _settings.value
    var factor = current.pointerSensitivity

    // Pointer acceleration curve: fast swipes cover large distance, slow movements stay pixel-precise
    if (current.pointerAcceleration) {
      val distance = kotlin.math.sqrt(rawDx * rawDx + rawDy * rawDy)
      val accelFactor = if (distance > 20f) 1.5f else if (distance > 10f) 1.25f else 1.0f
      factor *= accelFactor
    }

    accumulatedDx += rawDx * factor
    accumulatedDy += rawDy * factor

    val intDx = accumulatedDx.toInt()
    val intDy = accumulatedDy.toInt()

    if (intDx != 0 || intDy != 0) {
      accumulatedDx -= intDx
      accumulatedDy -= intDy
      networkManager.sendMove(intDx, intDy)
    }
  }

  /**
   * Two-finger scrolling processor
   */
  fun onScrollDelta(rawDy: Float) {
    val current = _settings.value
    val factor = current.scrollSensitivity
    val invertMultiplier = if (current.invertScroll) -1f else 1f
    accumulatedScrollDy += rawDy * factor * invertMultiplier

    // Smooth scroll discretization
    val scrollThreshold = 14f
    val steps = (accumulatedScrollDy / scrollThreshold).toInt()

    if (steps != 0) {
      accumulatedScrollDy -= steps * scrollThreshold
      networkManager.sendScroll(steps)
    }
  }

  /**
   * Mouse click event (left, right, double) with tactile vibration
   */
  fun sendClick(action: String) {
    if (_settings.value.hapticFeedback) {
      val app = getApplication<Application>()
      when (action) {
        "double" -> VibrationHelper.vibrateDoubleClick(app)
        "right" -> VibrationHelper.vibrateRightClick(app)
        else -> VibrationHelper.vibrateClick(app)
      }
    }
    networkManager.sendClick(action)
  }

  override fun onCleared() {
    super.onCleared()
    networkManager.cleanup()
  }
}
