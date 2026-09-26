package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.TouchpadSettings
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun ManualIpDialog(
  initialIp: String,
  onDismiss: () -> Unit,
  onConfirm: (ip: String) -> Unit
) {
  var ipText by remember { mutableStateOf(initialIp) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = DarkSurface,
    tonalElevation = 6.dp,
    shape = RoundedCornerShape(20.dp),
    icon = {
      Icon(
        imageVector = Icons.Default.Edit,
        contentDescription = null,
        tint = CyanPrimary,
        modifier = Modifier.size(28.dp)
      )
    },
    title = {
      Text(
        text = "Connect to Windows PC",
        style = MaterialTheme.typography.titleMedium,
        color = TextPrimary,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = "Enter your Windows PC IPv4 address (found via 'ipconfig' in Command Prompt):",
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary,
          fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
          value = ipText,
          onValueChange = { ipText = it },
          label = { Text("PC IP Address", color = TextSecondary) },
          placeholder = { Text("192.168.1.100", color = TextMuted) },
          singleLine = true,
          keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Done
          ),
          keyboardActions = KeyboardActions(
            onDone = {
              if (ipText.isNotBlank()) onConfirm(ipText.trim())
            }
          ),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CyanPrimary,
            unfocusedBorderColor = DarkSurfaceBorder,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            cursorColor = CyanPrimary
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_manual_ip")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Quick Preset Chips
        Text(
          text = "Quick Presets:",
          style = MaterialTheme.typography.labelSmall,
          color = TextMuted
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          FilterChip(
            selected = ipText == "127.0.0.1",
            onClick = { ipText = "127.0.0.1" },
            label = { Text("127.0.0.1 (USB Cable)", fontSize = 11.sp) },
            colors = FilterChipDefaults.filterChipColors(
              containerColor = DarkSurfaceElevated,
              labelColor = TextSecondary
            )
          )
          FilterChip(
            selected = ipText == "10.0.2.2",
            onClick = { ipText = "10.0.2.2" },
            label = { Text("10.0.2.2", fontSize = 11.sp) },
            colors = FilterChipDefaults.filterChipColors(
              containerColor = DarkSurfaceElevated,
              labelColor = TextSecondary
            )
          )
          FilterChip(
            selected = ipText.startsWith("192.168."),
            onClick = { ipText = "192.168.1." },
            label = { Text("192.168.*", fontSize = 11.sp) },
            colors = FilterChipDefaults.filterChipColors(
              containerColor = DarkSurfaceElevated,
              labelColor = TextSecondary
            )
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { if (ipText.isNotBlank()) onConfirm(ipText.trim()) },
        colors = ButtonDefaults.buttonColors(
          containerColor = CyanPrimary,
          contentColor = androidx.compose.ui.graphics.Color.White
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.testTag("button_confirm_ip")
      ) {
        Text("Connect", fontWeight = FontWeight.SemiBold)
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
      ) {
        Text("Cancel")
      }
    }
  )
}

@Composable
fun TouchpadSettingsDialog(
  settings: TouchpadSettings,
  onDismiss: () -> Unit,
  onSave: (pointer: Float, accel: Boolean, scroll: Float, invert: Boolean, invertH: Boolean, swapFourFingerNav: Boolean, binary: Boolean, haptics: Boolean, infinity: Boolean, hideNotification: Boolean, hideNavigation: Boolean) -> Unit
) {
  var pointerSens by remember { mutableFloatStateOf(settings.pointerSensitivity) }
  var pointerAccel by remember { mutableStateOf(settings.pointerAcceleration) }
  var scrollSens by remember { mutableFloatStateOf(settings.scrollSensitivity) }
  var invertScroll by remember { mutableStateOf(settings.invertScroll) }
  var invertHScroll by remember { mutableStateOf(settings.invertHScroll) }
  var swapFourFingerNav by remember { mutableStateOf(settings.swapFourFingerNavDirection) }
  var binaryMode by remember { mutableStateOf(settings.binaryByteMode) }
  var hapticEnabled by remember { mutableStateOf(settings.hapticFeedback) }
  var infinityScroll by remember { mutableStateOf(settings.infinityScroll) }
  var hideNotificationBar by remember { mutableStateOf(settings.hideNotificationBar) }
  var hideNavigationBar by remember { mutableStateOf(settings.hideNavigationBar) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = DarkSurface,
    tonalElevation = 6.dp,
    shape = RoundedCornerShape(20.dp),
    icon = {
      Icon(
        imageVector = Icons.Default.Tune,
        contentDescription = null,
        tint = CyanPrimary,
        modifier = Modifier.size(28.dp)
      )
    },
    title = {
      Text(
        text = "Speed & Performance",
        style = MaterialTheme.typography.titleMedium,
        color = TextPrimary,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
      ) {
        // 1. Cursor Speed / Sensitivity
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Speed, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Cursor Speed",
              style = MaterialTheme.typography.bodyMedium,
              color = TextPrimary,
              fontWeight = FontWeight.Medium
            )
          }
          Text(
            text = "${(pointerSens * 10).roundToInt() / 10f}x",
            style = MaterialTheme.typography.labelMedium,
            color = CyanPrimary,
            fontWeight = FontWeight.Bold
          )
        }
        Slider(
          value = pointerSens,
          onValueChange = { pointerSens = it },
          valueRange = 0.5f..3.5f,
          steps = 29,
          colors = SliderDefaults.colors(
            thumbColor = CyanPrimary,
            activeTrackColor = CyanPrimary,
            inactiveTrackColor = DarkSurfaceBorder
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("slider_cursor_sensitivity")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Cursor Acceleration Switch
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Pointer Acceleration",
              style = MaterialTheme.typography.bodyMedium,
              color = TextPrimary,
              fontWeight = FontWeight.Medium
            )
            Text(
              text = "Quick swipes jump across monitors, slow moves stay precise",
              style = MaterialTheme.typography.bodySmall,
              color = TextMuted,
              fontSize = 11.sp
            )
          }
          Switch(
            checked = pointerAccel,
            onCheckedChange = { pointerAccel = it },
            colors = SwitchDefaults.colors(
              checkedThumbColor = CyanPrimary,
              checkedTrackColor = CyanPrimary.copy(alpha = 0.3f),
              uncheckedThumbColor = TextMuted,
              uncheckedTrackColor = DarkSurfaceElevated
            )
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Scroll Sensitivity
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "2-Finger Scroll Speed",
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Medium
          )
          Text(
            text = "${(scrollSens * 10).roundToInt() / 10f}x",
            style = MaterialTheme.typography.labelMedium,
            color = CyanPrimary,
            fontWeight = FontWeight.Bold
          )
        }
        Slider(
          value = scrollSens,
          onValueChange = { scrollSens = it },
          valueRange = 0.5f..2.5f,
          steps = 19,
          colors = SliderDefaults.colors(
            thumbColor = CyanPrimary,
            activeTrackColor = CyanPrimary,
            inactiveTrackColor = DarkSurfaceBorder
          )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 4a. Vertical Natural Scrolling
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Natural Scrolling (Vertical)",
              style = MaterialTheme.typography.bodyMedium,
              color = TextPrimary,
              fontWeight = FontWeight.Medium
            )
            Text(
              text = "Reverse vertical scroll direction",
              style = MaterialTheme.typography.bodySmall,
              color = TextMuted,
              fontSize = 11.sp
            )
          }
          Switch(
            checked = invertScroll,
            onCheckedChange = { invertScroll = it },
            colors = SwitchDefaults.colors(
              checkedThumbColor = CyanPrimary,
              checkedTrackColor = CyanPrimary.copy(alpha = 0.3f),
              uncheckedThumbColor = TextMuted,
              uncheckedTrackColor = DarkSurfaceElevated
            ),
            modifier = Modifier.testTag("switch_natural_scroll_v")
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4b. Horizontal Natural Scrolling (Inverse or standard)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Natural Scrolling (Horizontal)",
              style = MaterialTheme.typography.bodyMedium,
              color = TextPrimary,
              fontWeight = FontWeight.Medium
            )
            Text(
              text = if (invertHScroll) "Natural: Inverted horizontal swipe direction" else "Standard: Direct horizontal swipe direction",
              style = MaterialTheme.typography.bodySmall,
              color = TextMuted,
              fontSize = 11.sp
            )
          }
          Switch(
            checked = invertHScroll,
            onCheckedChange = { invertHScroll = it },
            colors = SwitchDefaults.colors(
              checkedThumbColor = CyanPrimary,
              checkedTrackColor = CyanPrimary.copy(alpha = 0.3f),
              uncheckedThumbColor = TextMuted,
              uncheckedTrackColor = DarkSurfaceElevated
            ),
            modifier = Modifier.testTag("switch_natural_scroll_h")
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4c. 4-Finger Navigation Swipe Direction (Swap Option)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Swap 4-Finger Swipe Direction",
              style = MaterialTheme.typography.bodyMedium,
              color = TextPrimary,
              fontWeight = FontWeight.Medium
            )
            Text(
              text = if (swapFourFingerNav)
                "Swapped: Swipe Left = Backward, Swipe Right = Forward"
              else
                "Default: Swipe Left = Forward, Swipe Right = Backward",
              style = MaterialTheme.typography.bodySmall,
              color = TextMuted,
              fontSize = 11.sp
            )
          }
          Switch(
            checked = swapFourFingerNav,
            onCheckedChange = { swapFourFingerNav = it },
            colors = SwitchDefaults.colors(
              checkedThumbColor = CyanPrimary,
              checkedTrackColor = CyanPrimary.copy(alpha = 0.3f),
              uncheckedThumbColor = TextMuted,
              uncheckedTrackColor = DarkSurfaceElevated
            ),
            modifier = Modifier.testTag("switch_swap_four_finger_nav")
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 5. Binary Byte Transmission Mode (Latency boost)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.ElectricBolt, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Binary Byte Mode",
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
              )
            }
            Text(
              text = "Sends compact 5-byte UDP datagrams instead of ASCII strings for lowest latency and zero GC overhead",
              style = MaterialTheme.typography.bodySmall,
              color = TextMuted,
              fontSize = 11.sp
            )
          }
          Switch(
            checked = binaryMode,
            onCheckedChange = { binaryMode = it },
            colors = SwitchDefaults.colors(
              checkedThumbColor = CyanPrimary,
              checkedTrackColor = CyanPrimary.copy(alpha = 0.3f),
              uncheckedThumbColor = TextMuted,
              uncheckedTrackColor = DarkSurfaceElevated
            ),
            modifier = Modifier.testTag("switch_binary_mode")
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 6. Haptic Feedback
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Haptic Vibration",
              style = MaterialTheme.typography.bodyMedium,
              color = TextPrimary,
              fontWeight = FontWeight.Medium
            )
            Text(
              text = "Subtle click feedback on tap gestures",
              style = MaterialTheme.typography.bodySmall,
              color = TextMuted,
              fontSize = 11.sp
            )
          }
          Switch(
            checked = hapticEnabled,
            onCheckedChange = { hapticEnabled = it },
            colors = SwitchDefaults.colors(
              checkedThumbColor = CyanPrimary,
              checkedTrackColor = CyanPrimary.copy(alpha = 0.3f),
              uncheckedThumbColor = TextMuted,
              uncheckedTrackColor = DarkSurfaceElevated
            )
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 7. Infinity Momentum Scroll
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Speed, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Infinity Momentum Scroll",
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
              )
            }
            Text(
              text = "Scrolling glides smoothly when releasing fast (Infinity scroll). Touch anywhere to stop instantly.",
              style = MaterialTheme.typography.bodySmall,
              color = TextMuted,
              fontSize = 11.sp
            )
          }
          Switch(
            checked = infinityScroll,
            onCheckedChange = { infinityScroll = it },
            colors = SwitchDefaults.colors(
              checkedThumbColor = CyanPrimary,
              checkedTrackColor = CyanPrimary.copy(alpha = 0.3f),
              uncheckedThumbColor = TextMuted,
              uncheckedTrackColor = DarkSurfaceElevated
            ),
            modifier = Modifier.testTag("switch_infinity_scroll")
          )
        }
        Spacer(modifier = Modifier.height(14.dp))

        // 8. Immersive Mode - Hide Notification Bar (Status Bar)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Hide Notification Bar (Full Screen)",
              style = MaterialTheme.typography.bodyMedium,
              color = TextPrimary,
              fontWeight = FontWeight.SemiBold
            )
            Text(
              text = "Hides top notification/status bar across all orientations (portrait and landscape) so touchpad fills screen completely",
              style = MaterialTheme.typography.bodySmall,
              color = TextMuted,
              fontSize = 11.sp
            )
          }
          Switch(
            checked = hideNotificationBar,
            onCheckedChange = { hideNotificationBar = it },
            colors = SwitchDefaults.colors(
              checkedThumbColor = CyanPrimary,
              checkedTrackColor = CyanPrimary.copy(alpha = 0.3f),
              uncheckedThumbColor = TextMuted,
              uncheckedTrackColor = DarkSurfaceElevated
            ),
            modifier = Modifier.testTag("switch_hide_notification_bar")
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 9. Hide Navigation Bar (Gesture / Pill bar)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Hide Navigation Bar (Immersive)",
              style = MaterialTheme.typography.bodyMedium,
              color = TextPrimary,
              fontWeight = FontWeight.Medium
            )
            Text(
              text = "Hides bottom system navigation bar for true uninterrupted edge-to-edge touch space",
              style = MaterialTheme.typography.bodySmall,
              color = TextMuted,
              fontSize = 11.sp
            )
          }
          Switch(
            checked = hideNavigationBar,
            onCheckedChange = { hideNavigationBar = it },
            colors = SwitchDefaults.colors(
              checkedThumbColor = CyanPrimary,
              checkedTrackColor = CyanPrimary.copy(alpha = 0.3f),
              uncheckedThumbColor = TextMuted,
              uncheckedTrackColor = DarkSurfaceElevated
            ),
            modifier = Modifier.testTag("switch_hide_navigation_bar")
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSave(
            pointerSens,
            pointerAccel,
            scrollSens,
            invertScroll,
            invertHScroll,
            swapFourFingerNav,
            binaryMode,
            hapticEnabled,
            infinityScroll,
            hideNotificationBar,
            hideNavigationBar
          )
          onDismiss()
        },
        colors = ButtonDefaults.buttonColors(
          containerColor = CyanPrimary,
          contentColor = androidx.compose.ui.graphics.Color.White
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.testTag("button_save_settings")
      ) {
        Text("Save & Apply", fontWeight = FontWeight.SemiBold)
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
      ) {
        Text("Cancel")
      }
    }
  )
}

@Composable
fun PythonServerHelpDialog(
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val pythonCode = """
# Python Ultra-Responsive UDP & USB Touchpad Server
# Native Win32 hardware injection: fixes dragging on Windows Notifications,
# Unity Editor dock tabs & sliders, Unreal Engine Slate & viewports, and 3rd-party windows.
import sys
import os
import socket
import threading
import struct

PORT = 8080
DISCOVERY_MSG = "TOUCHPAD_DISCOVER"
OFFER_MSG = "TOUCHPAD_OFFER"

IS_WINDOWS = (os.name == 'nt')

if IS_WINDOWS:
    import ctypes
    user32 = ctypes.windll.user32

    # Enable Per-Monitor DPI awareness so coordinates and deltas are never scaled
    try:
        ctypes.windll.shcore.SetProcessDpiAwareness(2)
    except Exception:
        try:
            user32.SetProcessDPIAware()
        except Exception:
            pass

    # Win32 hardware mouse_event flags
    MOUSEEVENTF_MOVE = 0x0001
    MOUSEEVENTF_LEFTDOWN = 0x0002
    MOUSEEVENTF_LEFTUP = 0x0004
    MOUSEEVENTF_RIGHTDOWN = 0x0008
    MOUSEEVENTF_RIGHTUP = 0x0010
    MOUSEEVENTF_MIDDLEDOWN = 0x0020
    MOUSEEVENTF_MIDDLEUP = 0x0040
    MOUSEEVENTF_WHEEL = 0x0800
    MOUSEEVENTF_HWHEEL = 0x01000

    VK_MENU = 0x12       # Alt
    VK_LEFT = 0x25       # Left Arrow
    VK_RIGHT = 0x27      # Right Arrow
    KEYEVENTF_KEYUP = 0x0002

    def mouse_move(dx, dy):
        # MOUSEEVENTF_MOVE injects hardware packets recognized by Unity, Unreal Engine,
        # and Windows DirectManipulation (Action Center & Notification banners)
        user32.mouse_event(MOUSEEVENTF_MOVE, int(dx), int(dy), 0, 0)

    def mouse_click(btn):
        if btn == "left":
            user32.mouse_event(MOUSEEVENTF_LEFTDOWN, 0, 0, 0, 0)
            user32.mouse_event(MOUSEEVENTF_LEFTUP, 0, 0, 0, 0)
        elif btn == "right":
            user32.mouse_event(MOUSEEVENTF_RIGHTDOWN, 0, 0, 0, 0)
            user32.mouse_event(MOUSEEVENTF_RIGHTUP, 0, 0, 0, 0)
        elif btn == "double":
            user32.mouse_event(MOUSEEVENTF_LEFTDOWN, 0, 0, 0, 0)
            user32.mouse_event(MOUSEEVENTF_LEFTUP, 0, 0, 0, 0)
            user32.mouse_event(MOUSEEVENTF_LEFTDOWN, 0, 0, 0, 0)
            user32.mouse_event(MOUSEEVENTF_LEFTUP, 0, 0, 0, 0)
        elif btn == "middle":
            user32.mouse_event(MOUSEEVENTF_MIDDLEDOWN, 0, 0, 0, 0)
            user32.mouse_event(MOUSEEVENTF_MIDDLEUP, 0, 0, 0, 0)
        elif btn == "down":
            user32.mouse_event(MOUSEEVENTF_LEFTDOWN, 0, 0, 0, 0)
        elif btn == "up":
            user32.mouse_event(MOUSEEVENTF_LEFTUP, 0, 0, 0, 0)

    def mouse_scroll_v(dy):
        user32.mouse_event(MOUSEEVENTF_WHEEL, 0, 0, int(dy * 120), 0)

    def mouse_scroll_h(dx):
        user32.mouse_event(MOUSEEVENTF_HWHEEL, 0, 0, int(dx * 120), 0)

    def nav_back():
        user32.keybd_event(VK_MENU, 0, 0, 0)
        user32.keybd_event(VK_LEFT, 0, 0, 0)
        user32.keybd_event(VK_LEFT, 0, KEYEVENTF_KEYUP, 0)
        user32.keybd_event(VK_MENU, 0, KEYEVENTF_KEYUP, 0)
        print("[Event] 4-Finger Navigation: Backward (Alt+Left)")

    def nav_forward():
        user32.keybd_event(VK_MENU, 0, 0, 0)
        user32.keybd_event(VK_RIGHT, 0, 0, 0)
        user32.keybd_event(VK_RIGHT, 0, KEYEVENTF_KEYUP, 0)
        user32.keybd_event(VK_MENU, 0, KEYEVENTF_KEYUP, 0)
        print("[Event] 4-Finger Navigation: Forward (Alt+Right)")

else:
    # Cross-platform fallback for Linux / macOS using pynput
    try:
        from pynput.mouse import Controller as MouseController, Button
        from pynput.keyboard import Controller as KeyboardController, Key
        pynput_mouse = MouseController()
        pynput_keyboard = KeyboardController()

        def mouse_move(dx, dy):
            pynput_mouse.move(dx, dy)

        def mouse_click(btn):
            if btn == "left": pynput_mouse.click(Button.left, 1)
            elif btn == "right": pynput_mouse.click(Button.right, 1)
            elif btn == "double": pynput_mouse.click(Button.left, 2)
            elif btn == "middle": pynput_mouse.click(Button.middle, 1)
            elif btn == "down": pynput_mouse.press(Button.left)
            elif btn == "up": pynput_mouse.release(Button.left)

        def mouse_scroll_v(dy):
            pynput_mouse.scroll(0, dy)

        def mouse_scroll_h(dx):
            pynput_mouse.scroll(dx, 0)

        def nav_back():
            with pynput_keyboard.pressed(Key.alt):
                pynput_keyboard.press(Key.left)
                pynput_keyboard.release(Key.left)
            print("[Event] 4-Finger Navigation: Backward (Alt+Left)")

        def nav_forward():
            with pynput_keyboard.pressed(Key.alt):
                pynput_keyboard.press(Key.right)
                pynput_keyboard.release(Key.right)
            print("[Event] 4-Finger Navigation: Forward (Alt+Right)")
    except ImportError:
        print("[Warning] Install pynput with: pip install pynput (for non-Windows OS)")

def handle_packet(data):
    if not data:
        return
    op = data[0]
    if op == 0x01 and len(data) >= 5:
        dx, dy = struct.unpack('>hh', data[1:5])
        mouse_move(dx, dy)
    elif op == 0x02 and len(data) >= 2:
        code = data[1]
        if code == 1: mouse_click("left")
        elif code == 2: mouse_click("right")
        elif code == 3: mouse_click("double")
        elif code == 4: mouse_click("down")
        elif code == 5: mouse_click("up")
        elif code == 6: mouse_click("middle")
    elif op == 0x03 and len(data) >= 3:
        dy, = struct.unpack('>h', data[1:3])
        mouse_scroll_v(dy)
    elif op == 0x04 and len(data) >= 3:
        dx, = struct.unpack('>h', data[1:3])
        mouse_scroll_h(dx)
    elif op == 0x05 and len(data) >= 2:
        code = data[1]
        if code == 1: nav_back()
        elif code == 2: nav_forward()
    else:
        # String format (newline separated for TCP/UDP)
        msg = data.decode('utf-8', errors='ignore').strip()
        for line in msg.split('\n'):
            line = line.strip()
            if not line: continue
            parts = line.split(',')
            t = parts[0]
            if t == "ping":
                continue
            elif t == "move" and len(parts) >= 3:
                mouse_move(int(parts[1]), int(parts[2]))
            elif t == "click" and len(parts) >= 2:
                mouse_click(parts[1])
            elif t == "scroll" and len(parts) >= 2:
                mouse_scroll_v(int(parts[1]))
            elif t == "hscroll" and len(parts) >= 2:
                mouse_scroll_h(int(parts[1]))
            elif t == "nav" and len(parts) >= 2:
                act = parts[1]
                if act in ("back", "backward"):
                    nav_back()
                elif act == "forward":
                    nav_forward()

# Wi-Fi Auto-Discovery responder on port 8080
last_connected_ip = None

def start_discovery_responder():
    global last_connected_ip
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    sock.bind(('0.0.0.0', PORT))
    while True:
        try:
            data, addr = sock.recvfrom(1024)
            if data.decode('utf-8', errors='ignore').strip() == DISCOVERY_MSG:
                sock.sendto(OFFER_MSG.encode('utf-8'), addr)
                if last_connected_ip != addr[0]:
                    last_connected_ip = addr[0]
                    print(f"[Event] Device paired via Wi-Fi/Network: {addr[0]}")
        except Exception:
            pass

# Wi-Fi & USB Tethering UDP Input on port 8081
def start_udp_input():
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    sock.bind(('0.0.0.0', PORT + 1))
    while True:
        try:
            data, _ = sock.recvfrom(1024)
            handle_packet(data)
        except (KeyboardInterrupt, SystemExit):
            raise
        except Exception:
            pass

# USB Cable (Wired ADB reverse) TCP Input on port 8081
def start_tcp_input():
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    sock.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
    sock.bind(('0.0.0.0', PORT + 1))
    sock.listen(5)
    while True:
        try:
            client, addr = sock.accept()
            client.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
            print("[Event] USB Cable mode connected (127.0.0.1)")
            threading.Thread(target=handle_tcp_client, args=(client,), daemon=True).start()
        except Exception:
            pass

def handle_tcp_client(client):
    while True:
        try:
            data = client.recv(1024)
            if not data: break
            handle_packet(data)
        except Exception:
            break
    client.close()
    print("[Event] USB Cable disconnected")

if __name__ == "__main__":
    print(f"[Event] Touchpad Server ready (UDP/TCP {PORT}-{PORT + 1})")
    threading.Thread(target=start_discovery_responder, daemon=True).start()
    threading.Thread(target=start_tcp_input, daemon=True).start()
    try:
        start_udp_input()
    except KeyboardInterrupt:
        print("\n[Event] Server stopped.")
""".trimIndent()

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = DarkSurface,
    tonalElevation = 6.dp,
    shape = RoundedCornerShape(20.dp),
    icon = {
      Icon(
        imageVector = Icons.Default.Info,
        contentDescription = null,
        tint = CyanPrimary,
        modifier = Modifier.size(28.dp)
      )
    },
    title = {
      Text(
        text = "Windows Server Guide",
        style = MaterialTheme.typography.titleMedium,
        color = TextPrimary,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
      ) {
        Text(
          text = "1. Install Requirement:",
          style = MaterialTheme.typography.bodyMedium,
          color = TextPrimary,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "On Windows: 100% built-in! Zero pip packages required (uses Windows native ctypes driver).",
          style = MaterialTheme.typography.bodySmall,
          color = StatusGreen,
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.padding(top = 2.dp)
        )
        Text(
          text = "On Linux / macOS (optional cross-platform fallback):",
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary,
          fontSize = 11.sp,
          modifier = Modifier.padding(top = 4.dp)
        )
        Surface(
          color = DarkSurfaceElevated,
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
        ) {
          Text(
            text = "pip install pynput",
            fontFamily = FontFamily.Monospace,
            color = CyanPrimary,
            fontSize = 12.sp,
            modifier = Modifier.padding(10.dp)
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "2. Run Python Server:",
          style = MaterialTheme.typography.bodyMedium,
          color = TextPrimary,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "Supports both standard ASCII and ultra-fast 5-byte binary datagrams seamlessly:",
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary,
          fontSize = 12.sp,
          modifier = Modifier.padding(vertical = 4.dp)
        )

        Surface(
          color = androidx.compose.ui.graphics.Color(0xFF22242D),
          shape = RoundedCornerShape(8.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
        ) {
          Text(
            text = pythonCode,
            fontFamily = FontFamily.Monospace,
            color = androidx.compose.ui.graphics.Color(0xFFE2E4EC),
            fontSize = 10.sp,
            lineHeight = 15.sp,
            modifier = Modifier.padding(10.dp)
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          OutlinedButton(
            onClick = {
              val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              val clip = ClipData.newPlainText("Touchpad Python Server", pythonCode)
              clipboard.setPrimaryClip(clip)
              Toast.makeText(context, "Copied Python server code to clipboard!", Toast.LENGTH_SHORT).show()
            },
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanPrimary),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary)
          ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Copy Script", fontSize = 12.sp)
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = "3. USB Cable Mode (0ms Lag, No Wi-Fi Needed):",
          style = MaterialTheme.typography.bodyMedium,
          color = TextPrimary,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "Connect phone to PC with a USB cable (USB Debugging enabled in Developer Options), then run this command on your PC:",
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary,
          fontSize = 12.sp,
          modifier = Modifier.padding(vertical = 4.dp)
        )

        Surface(
          color = DarkSurfaceElevated,
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = "adb reverse tcp:8081 tcp:8081",
              fontFamily = FontFamily.Monospace,
              color = CyanPrimary,
              fontSize = 11.sp,
              modifier = Modifier.weight(1f)
            )
            IconButton(
              onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("ADB Command", "adb reverse tcp:8081 tcp:8081")
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Copied ADB reverse command!", Toast.LENGTH_SHORT).show()
              },
              modifier = Modifier.size(32.dp)
            ) {
              Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy ADB command",
                tint = CyanPrimary,
                modifier = Modifier.size(16.dp)
              )
            }
          }
        }

        Text(
          text = "Then tap the USB icon in the app status bar — instantaneous zero-latency control!",
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary,
          fontSize = 12.sp,
          modifier = Modifier.padding(bottom = 6.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = "Multi-Touch Gestures & Dragging:",
          style = MaterialTheme.typography.bodyMedium,
          color = TextPrimary,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "• 1 Finger: Smooth cursor movement, tap for left-click, double-tap or tap-and-hold stationary (>280ms) to drag.\n• 2 Fingers: Vertical & horizontal scroll (swipe left for H-scroll), tap for right-click.\n• 3 Fingers: Hold & drag for Windows drag-and-drop, tap for middle mouse click.\n• 4 Fingers: Swipe Left for Forward (Alt+Right), Swipe Right for Backward (Alt+Left) in Explorer, Chrome, Edge, Firefox, etc. (direction can be swapped in Settings).\n• Drag Lock (DRAG button): Keeps left mouse button held down on PC so you can drag long distances across Unity, Unreal Engine, and Windows Notifications without holding down fingers. Tap 'LOCKED' or tap the top banner to release/drop.\n• Fling Release: Let go during fast scroll to trigger infinity momentum scroll.",
          style = MaterialTheme.typography.bodySmall,
          color = TextMuted,
          fontSize = 12.sp,
          lineHeight = 18.sp,
          modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = "Fix for Unity, Unreal Engine & Windows Notifications:",
          style = MaterialTheme.typography.bodyMedium,
          color = TextPrimary,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "• Native Win32 Injection: Uses mouse_event directly on Windows so modal drag loops, dock tabs, inspector sliders, and Action Center notification swipe-to-dismiss work 100% reliably without losing mouse capture.\n• Admin Elevation (UIPI): If Unity Editor or Unreal Engine is running as Administrator, please launch Command Prompt/PowerShell as Administrator ('Run as administrator') before starting the server so Windows UIPI permits mouse injection into elevated windows.\n• Burst UDP Reliability: Drag down/up states are sent with duplicate redundancy to guarantee zero packet loss over Wi-Fi.",
          style = MaterialTheme.typography.bodySmall,
          color = TextMuted,
          fontSize = 12.sp,
          lineHeight = 18.sp,
          modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = "Tips for Minimal Latency:",
          style = MaterialTheme.typography.bodyMedium,
          color = TextPrimary,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "• Use USB Cable mode when gaming or needing absolute zero jitter.\n• For Wi-Fi: Keep Android and PC on 5GHz band for <2ms transmission.\n• Enable 'Binary Byte Mode' in Settings to send 5-byte datagrams instead of strings.\n• Tap the speed icon anytime to adjust cursor velocity on the fly.",
          style = MaterialTheme.typography.bodySmall,
          color = TextMuted,
          fontSize = 12.sp,
          lineHeight = 18.sp,
          modifier = Modifier.padding(top = 4.dp)
        )
      }
    },
    confirmButton = {
      Button(
        onClick = onDismiss,
        colors = ButtonDefaults.buttonColors(
          containerColor = CyanPrimary,
          contentColor = androidx.compose.ui.graphics.Color.White
        ),
        shape = RoundedCornerShape(10.dp)
      ) {
        Text("Got It", fontWeight = FontWeight.SemiBold)
      }
    }
  )
}
