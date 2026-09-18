package com.example

import com.example.network.TouchpadNetworkManager
import com.example.ui.TouchpadSettings
import com.example.util.VibrationHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class TouchpadUnitTest {
  @Test
  fun testTouchpadSettingsDefaults() {
    val settings = TouchpadSettings()
    assertEquals(1.3f, settings.pointerSensitivity, 0.01f)
    assertTrue(settings.pointerAcceleration)
    assertFalse(settings.invertScroll)
    assertTrue(settings.hapticFeedback)
    assertFalse(settings.binaryByteMode)
  }

  @Test
  fun testBinaryOpCodes() {
    assertEquals(0x01.toByte(), TouchpadNetworkManager.OP_MOVE)
    assertEquals(0x02.toByte(), TouchpadNetworkManager.OP_CLICK)
    assertEquals(0x03.toByte(), TouchpadNetworkManager.OP_SCROLL)
  }

  @Test
  fun testVibrationHelperTriggersSafely() {
    val context = RuntimeEnvironment.getApplication()
    // Verify none of the vibration triggers crash on Robolectric or Android
    VibrationHelper.vibrateClick(context)
    VibrationHelper.vibrateDoubleClick(context)
    VibrationHelper.vibrateRightClick(context)
  }

  @Test
  fun testNetworkManagerSendNoCrash() {
    val context = RuntimeEnvironment.getApplication()
    val net = TouchpadNetworkManager(context)
    net.setManualIp("127.0.0.1")

    // Test text mode
    net.setBinaryMode(false)
    net.sendMove(10, -5)
    net.sendClick("left")
    net.sendClick("double")
    net.sendClick("right")
    net.sendScroll(2)

    // Test byte mode
    net.setBinaryMode(true)
    net.sendMove(15, -8)
    net.sendClick("left")
    net.sendClick("double")
    net.sendClick("right")
    net.sendScroll(-3)

    net.cleanup()
  }
}
