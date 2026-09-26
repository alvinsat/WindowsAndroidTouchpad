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
    assertFalse(settings.invertHScroll)
    assertFalse(settings.swapFourFingerNavDirection)
    assertTrue(settings.infinityScroll)
    assertTrue(settings.hapticFeedback)
    assertFalse(settings.binaryByteMode)
    assertTrue(settings.hideNotificationBar)
    assertTrue(settings.hideNavigationBar)
  }

  @Test
  fun testBinaryOpCodes() {
    assertEquals(0x01.toByte(), TouchpadNetworkManager.OP_MOVE)
    assertEquals(0x02.toByte(), TouchpadNetworkManager.OP_CLICK)
    assertEquals(0x03.toByte(), TouchpadNetworkManager.OP_SCROLL)
    assertEquals(0x04.toByte(), TouchpadNetworkManager.OP_HSCROLL)
    assertEquals(0x05.toByte(), TouchpadNetworkManager.OP_NAV)
  }

  @Test
  fun testVibrationHelperTriggersSafely() {
    val context = RuntimeEnvironment.getApplication()
    // Verify none of the vibration triggers crash on Robolectric or Android
    VibrationHelper.vibrateClick(context)
    VibrationHelper.vibrateDoubleClick(context)
    VibrationHelper.vibrateRightClick(context)
    VibrationHelper.vibrateMiddleClick(context)
    VibrationHelper.vibrateNavigation(context)
    VibrationHelper.vibrateDragState(context, isStarting = true)
    VibrationHelper.vibrateDragState(context, isStarting = false)
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
    net.sendMiddleClick()
    net.sendDragStart()
    net.sendDragEnd()
    net.sendScroll(2)
    net.sendHScroll(-2)
    net.sendNavigation("forward")
    net.sendNavigation("back")
    net.sendNavigation(true)
    net.sendNavigation(false)

    // Test byte mode
    net.setBinaryMode(true)
    net.sendMove(15, -8)
    net.sendClick("left")
    net.sendClick("double")
    net.sendClick("right")
    net.sendMiddleClick()
    net.sendDragStart()
    net.sendDragEnd()
    net.sendScroll(-3)
    net.sendHScroll(3)
    net.sendNavigation("forward")
    net.sendNavigation("back")
    net.sendNavigation(true)
    net.sendNavigation(false)

    net.cleanup()
  }

  @Test
  fun testUsbModeSwitching() {
    val context = RuntimeEnvironment.getApplication()
    val net = TouchpadNetworkManager(context)

    assertFalse(net.isUsbMode())
    net.setUsbMode(true)
    assertTrue(net.isUsbMode())

    // Sending events in USB mode
    net.sendMove(5, 5)
    net.sendClick("left")

    net.setUsbMode(false)
    assertFalse(net.isUsbMode())

    net.cleanup()
  }

  @Test
  fun testConnectionPriorityHierarchy() {
    val context = RuntimeEnvironment.getApplication()
    val net = TouchpadNetworkManager(context)

    // Manual 127.0.0.1 sets ADB_REVERSE priority
    net.setManualIp("127.0.0.1")
    val state = net.connectionState.value
    assertTrue(state is com.example.network.ConnectionState.Connected)
    val connected = state as com.example.network.ConnectionState.Connected
    assertEquals(com.example.network.ConnectionType.ADB_REVERSE, connected.connectionType)
    assertEquals("127.0.0.1", connected.serverIp)
    assertTrue(connected.isUsb)

    // Manual custom IP sets MANUAL
    net.setManualIp("192.168.1.150")
    val manualState = net.connectionState.value as com.example.network.ConnectionState.Connected
    assertEquals(com.example.network.ConnectionType.MANUAL, manualState.connectionType)
    assertEquals("192.168.1.150", manualState.serverIp)
    assertFalse(manualState.isUsb)

    net.cleanup()
  }

  @Test
  fun testDragLockStateAndToggle() {
    val context = RuntimeEnvironment.getApplication()
    val vm = com.example.ui.TouchpadViewModel(context)

    assertFalse(vm.isDragLocked.value)
    assertFalse(vm.isDragging.value)

    vm.toggleDragLock()
    assertTrue(vm.isDragLocked.value)
    assertTrue(vm.isDragging.value)

    // While drag locked, normal release does not drop drag
    vm.sendDragEnd()
    assertTrue(vm.isDragLocked.value)
    assertTrue(vm.isDragging.value)

    // Unlocking drops the drag
    vm.unlockDrag()
    assertFalse(vm.isDragLocked.value)
    assertFalse(vm.isDragging.value)
  }
}
