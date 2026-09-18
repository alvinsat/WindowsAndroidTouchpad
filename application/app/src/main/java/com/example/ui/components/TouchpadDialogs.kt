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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
            selected = false,
            onClick = { ipText = "10.0.2.2" },
            label = { Text("10.0.2.2 (Emulator)", fontSize = 11.sp) },
            colors = FilterChipDefaults.filterChipColors(
              containerColor = DarkSurfaceElevated,
              labelColor = TextSecondary
            )
          )
          FilterChip(
            selected = false,
            onClick = { ipText = "192.168.1." },
            label = { Text("192.168.1.*", fontSize = 11.sp) },
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
          contentColor = DarkBackground
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
  onSave: (pointer: Float, accel: Boolean, scroll: Float, invert: Boolean, binary: Boolean, haptics: Boolean) -> Unit
) {
  var pointerSens by remember { mutableFloatStateOf(settings.pointerSensitivity) }
  var pointerAccel by remember { mutableStateOf(settings.pointerAcceleration) }
  var scrollSens by remember { mutableFloatStateOf(settings.scrollSensitivity) }
  var invertScroll by remember { mutableStateOf(settings.invertScroll) }
  var binaryMode by remember { mutableStateOf(settings.binaryByteMode) }
  var hapticEnabled by remember { mutableStateOf(settings.hapticFeedback) }

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

        // 4. Invert Scroll
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Natural Scrolling",
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
            )
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
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSave(pointerSens, pointerAccel, scrollSens, invertScroll, binaryMode, hapticEnabled)
          onDismiss()
        },
        colors = ButtonDefaults.buttonColors(
          containerColor = CyanPrimary,
          contentColor = DarkBackground
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
# Python Ultra-Responsive UDP Touchpad Server
# Requirement: pip install pynput
import socket
import threading
import struct
from pynput.mouse import Controller, Button

mouse = Controller()

PORT = 8080
DISCOVERY_MSG = "TOUCHPAD_DISCOVER"
OFFER_MSG = "TOUCHPAD_OFFER"

# Auto-Discovery responder on port 8080
def start_discovery_responder():
    disc_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    disc_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    disc_socket.bind(('0.0.0.0', PORT))
    print(f"[Auto-Sync] Server discovery active on UDP port {PORT}...")
    while True:
        try:
            data, addr = disc_socket.recvfrom(1024)
            if data.decode('utf-8', errors='ignore').strip() == DISCOVERY_MSG:
                disc_socket.sendto(OFFER_MSG.encode('utf-8'), addr)
                print(f"[Auto-Sync] Handshake paired with Android at {addr[0]}")
        except Exception:
            pass

# High-frequency gesture receiver on port 8081
# Automatically supports BOTH string messages ("move,dx,dy") AND fast 5-byte binary datagrams!
def start_input_server():
    input_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    input_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    input_socket.bind(('0.0.0.0', PORT + 1))
    print(f"[Input Engine] Ready for high-speed touch input on UDP port {PORT + 1}...")
    
    while True:
        try:
            data, _ = input_socket.recvfrom(1024)
            if not data:
                continue

            # Check if binary packet opcode (0x01 = Move, 0x02 = Click, 0x03 = Scroll)
            op = data[0]
            if op == 0x01 and len(data) >= 5:
                # 5-byte binary move: [0x01, dx (short), dy (short)]
                dx, dy = struct.unpack('>hh', data[1:5])
                mouse.move(dx, dy)
            elif op == 0x02 and len(data) >= 2:
                # Binary click: [0x02, click_code]
                code = data[1]
                if code == 1:
                    mouse.click(Button.left, 1)
                elif code == 2:
                    mouse.click(Button.right, 1)
                elif code == 3:
                    mouse.click(Button.left, 2)
            elif op == 0x03 and len(data) >= 3:
                # Binary scroll: [0x03, dy (short)]
                dy, = struct.unpack('>h', data[1:3])
                mouse.scroll(0, dy)
            else:
                # Fallback ASCII string format ("move,dx,dy", "click,left", "scroll,dy")
                message = data.decode('utf-8', errors='ignore').strip()
                parts = message.split(',')
                event_type = parts[0]
                if event_type == "move" and len(parts) >= 3:
                    mouse.move(int(parts[1]), int(parts[2]))
                elif event_type == "click" and len(parts) >= 2:
                    action = parts[1]
                    if action == "left":
                        mouse.click(Button.left, 1)
                    elif action == "right":
                        mouse.click(Button.right, 1)
                    elif action == "double":
                        mouse.click(Button.left, 2)
                elif event_type == "scroll" and len(parts) >= 2:
                    mouse.scroll(0, int(parts[1]))
        except Exception:
            pass

if __name__ == "__main__":
    threading.Thread(target=start_discovery_responder, daemon=True).start()
    start_input_server()
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
        Surface(
          color = DarkSurfaceElevated,
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
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
          color = DarkBackground,
          shape = RoundedCornerShape(8.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
        ) {
          Text(
            text = pythonCode,
            fontFamily = FontFamily.Monospace,
            color = TextSecondary,
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

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = "Tips for Minimal Latency:",
          style = MaterialTheme.typography.bodyMedium,
          color = TextPrimary,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "• Keep Android and Windows on the same 5GHz Wi-Fi band for <2ms transmission.\n• Enable 'Binary Byte Mode' in Settings to send 5-byte datagrams instead of strings.\n• Tap the speed icon anytime to adjust cursor velocity on the fly.",
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
          contentColor = DarkBackground
        ),
        shape = RoundedCornerShape(10.dp)
      ) {
        Text("Got It", fontWeight = FontWeight.SemiBold)
      }
    }
  )
}
