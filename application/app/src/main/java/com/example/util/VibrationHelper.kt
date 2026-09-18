package com.example.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/**
 * High-performance, tactile vibration helper for Touchpad click actions.
 * Provides distinct haptic feedback:
 * - Single Click: Crisp, snappy 20ms pulse
 * - Double Click: Rapid double-tap pulse pattern
 * - Right Click (2 fingers): Solid, heavier tactile click
 */
object VibrationHelper {
  private const val TAG = "VibrationHelper"

  @Suppress("DEPRECATION")
  fun vibrateClick(context: Context) {
    try {
      val vibrator = getVibrator(context) ?: return
      if (!vibrator.hasVibrator()) return

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
      } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(22, VibrationEffect.DEFAULT_AMPLITUDE))
      } else {
        vibrator.vibrate(22)
      }
    } catch (e: Exception) {
      Log.w(TAG, "Failed to vibrate for click: ${e.message}")
    }
  }

  @Suppress("DEPRECATION")
  fun vibrateDoubleClick(context: Context) {
    try {
      val vibrator = getVibrator(context) ?: return
      if (!vibrator.hasVibrator()) return

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
      } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val timings = longArrayOf(0, 18, 55, 22)
        val amplitudes = intArrayOf(0, 220, 0, 255)
        vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
      } else {
        val pattern = longArrayOf(0, 18, 55, 22)
        vibrator.vibrate(pattern, -1)
      }
    } catch (e: Exception) {
      Log.w(TAG, "Failed to vibrate for double click: ${e.message}")
    }
  }

  @Suppress("DEPRECATION")
  fun vibrateRightClick(context: Context) {
    try {
      val vibrator = getVibrator(context) ?: return
      if (!vibrator.hasVibrator()) return

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
      } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
      } else {
        vibrator.vibrate(35)
      }
    } catch (e: Exception) {
      Log.w(TAG, "Failed to vibrate for right click: ${e.message}")
    }
  }

  private fun getVibrator(context: Context): Vibrator? {
    return try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator ?: (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
      } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
      }
    } catch (e: Exception) {
      null
    }
  }
}
