package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.network.ConnectionState
import com.example.network.ConnectionType
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun ConnectionStatusBar(
  modifier: Modifier = Modifier,
  connectionState: ConnectionState,
  isBinaryMode: Boolean,
  onRefreshClick: () -> Unit,
  onManualIpClick: () -> Unit,
  onSettingsClick: () -> Unit,
  onHelpClick: () -> Unit
) {
  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.35f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulseAlpha"
  )

  val rotationAngle by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "rotate"
  )

  val (statusCategory, statusColor, statusText) = when (connectionState) {
    is ConnectionState.Connected -> {
      val modeName = when (connectionState.connectionType) {
        ConnectionType.ADB_REVERSE -> "USB CABLE (ADB)"
        ConnectionType.LOCAL_WIFI -> "LOCAL WI-FI"
        ConnectionType.USB_TETHERING -> "USB TETHERING"
        ConnectionType.MANUAL -> "MANUAL IP"
      }
      val subText = when (connectionState.connectionType) {
        ConnectionType.ADB_REVERSE -> "127.0.0.1 (Priority 1 • 0ms)"
        ConnectionType.LOCAL_WIFI -> "${connectionState.serverIp} (Priority 2)"
        ConnectionType.USB_TETHERING -> "${connectionState.serverIp} (Priority 3)"
        ConnectionType.MANUAL -> connectionState.serverIp
      } + if (isBinaryMode) " [Byte]" else ""
      Triple(modeName, StatusGreen, subText)
    }
    is ConnectionState.Searching -> Triple(
      "AUTO-DISCOVERY",
      StatusAmber,
      "Checking Cable -> Wi-Fi -> Tether..."
    )
    is ConnectionState.Error -> Triple(
      "NOT CONNECTED",
      StatusRed,
      connectionState.message
    )
    is ConnectionState.Disconnected -> Triple(
      "OFFLINE",
      TextMuted,
      "Disconnected"
    )
  }

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(16.dp)),
    color = DarkSurface,
    tonalElevation = 2.dp
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      // Status Indicator & Text
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(
              if (connectionState is ConnectionState.Searching) {
                statusColor.copy(alpha = pulseAlpha)
              } else {
                statusColor
              }
            )
            .testTag("status_indicator_dot")
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column {
          Text(
            text = statusCategory,
            style = MaterialTheme.typography.labelSmall,
            color = statusColor,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            letterSpacing = 1.sp
          )
          Text(
            text = statusText,
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimary,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      // Quick Actions (No manual toggle button needed; priority auto-discovery handles everything)
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = onRefreshClick,
          modifier = Modifier
            .size(36.dp)
            .testTag("action_refresh_discovery")
        ) {
          Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "Re-discover PC",
            tint = if (connectionState is ConnectionState.Searching) CyanPrimary else TextSecondary,
            modifier = if (connectionState is ConnectionState.Searching) Modifier.rotate(rotationAngle) else Modifier.size(19.dp)
          )
        }

        IconButton(
          onClick = onManualIpClick,
          modifier = Modifier
            .size(36.dp)
            .testTag("action_manual_ip")
        ) {
          Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = "Enter Manual IP",
            tint = TextSecondary,
            modifier = Modifier.size(18.dp)
          )
        }

        IconButton(
          onClick = onSettingsClick,
          modifier = Modifier
            .size(36.dp)
            .testTag("action_settings")
        ) {
          Icon(
            imageVector = Icons.Default.Tune,
            contentDescription = "Sensitivity & Settings",
            tint = CyanPrimary,
            modifier = Modifier.size(19.dp)
          )
        }

        IconButton(
          onClick = onHelpClick,
          modifier = Modifier
            .size(36.dp)
            .testTag("action_help_python")
        ) {
          Icon(
            imageVector = Icons.Default.Code,
            contentDescription = "Python Server Setup",
            tint = TextSecondary,
            modifier = Modifier.size(19.dp)
          )
        }
      }
    }
  }
}

/**
 * Compact inline sensitivity / speed bar right above full-screen touchpad
 */
@Composable
fun QuickSensitivityBar(
  modifier: Modifier = Modifier,
  sensitivity: Float,
  onSensitivityChange: (Float) -> Unit,
  onOpenSettings: () -> Unit,
  isDragLocked: Boolean = false,
  onToggleDragLock: () -> Unit = {}
) {
  Surface(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp)),
    color = DarkSurfaceElevated,
    tonalElevation = 1.dp
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(
        onClick = onOpenSettings,
        modifier = Modifier.size(28.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Speed,
          contentDescription = "Touchpad Speed",
          tint = CyanPrimary,
          modifier = Modifier.size(16.dp)
        )
      }

      Text(
        text = "Speed",
        style = MaterialTheme.typography.labelSmall,
        color = TextSecondary,
        fontSize = 11.sp
      )

      Spacer(modifier = Modifier.width(6.dp))

      Slider(
        value = sensitivity,
        onValueChange = onSensitivityChange,
        valueRange = 0.5f..3.0f,
        steps = 25,
        colors = SliderDefaults.colors(
          thumbColor = CyanPrimary,
          activeTrackColor = CyanPrimary,
          inactiveTrackColor = DarkSurfaceBorder
        ),
        modifier = Modifier
          .weight(1f)
          .height(28.dp)
          .testTag("quick_sensitivity_slider")
      )

      Spacer(modifier = Modifier.width(6.dp))

      Text(
        text = "${(sensitivity * 10).roundToInt() / 10f}x",
        style = MaterialTheme.typography.labelSmall,
        color = TextPrimary,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        modifier = Modifier.width(30.dp)
      )

      Spacer(modifier = Modifier.width(6.dp))

      // Drag Lock quick toggle (Instant drag for Unity, Unreal, Windows Notifications)
      Surface(
        onClick = onToggleDragLock,
        shape = RoundedCornerShape(8.dp),
        color = if (isDragLocked) Color(0xFFD97706) else DarkSurface,
        border = BorderStroke(1.dp, if (isDragLocked) Color(0xFFFFB300) else DarkSurfaceBorder),
        modifier = Modifier.testTag("button_drag_lock_toggle")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = if (isDragLocked) Icons.Default.Lock else Icons.Default.LockOpen,
            contentDescription = "Toggle Drag Lock",
            tint = if (isDragLocked) Color.White else TextSecondary,
            modifier = Modifier.size(12.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = if (isDragLocked) "LOCKED" else "DRAG",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDragLocked) Color.White else TextSecondary
          )
        }
      }
    }
  }
}
