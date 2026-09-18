package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.network.ConnectionState
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

  val (statusColor, statusText) = when (connectionState) {
    is ConnectionState.Connected -> Pair(
      StatusGreen,
      if (connectionState.isManual) "${connectionState.serverIp} ${if (isBinaryMode) "(Byte Mode)" else ""}" else "${connectionState.serverIp} ${if (isBinaryMode) "(Byte Mode)" else ""}"
    )
    is ConnectionState.Searching -> Pair(
      StatusAmber,
      "Searching UDP :8080..."
    )
    is ConnectionState.Error -> Pair(
      StatusRed,
      connectionState.message
    )
    is ConnectionState.Disconnected -> Pair(
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
            text = if (connectionState is ConnectionState.Connected) "TOUCHPAD ACTIVE" else if (connectionState is ConnectionState.Searching) "SEARCHING" else "STATUS",
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

      // Quick Actions
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
  onOpenSettings: () -> Unit
) {
  Surface(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp)),
    color = DarkSurfaceElevated
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.width(92.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Speed,
          contentDescription = "Speed",
          tint = CyanPrimary,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "Speed ${(sensitivity * 10).roundToInt() / 10f}x",
          style = MaterialTheme.typography.labelSmall,
          color = TextPrimary,
          fontSize = 11.sp,
          fontWeight = FontWeight.Medium
        )
      }

      Slider(
        value = sensitivity,
        onValueChange = onSensitivityChange,
        valueRange = 0.5f..3.5f,
        steps = 29,
        colors = SliderDefaults.colors(
          thumbColor = CyanPrimary,
          activeTrackColor = CyanPrimary,
          inactiveTrackColor = DarkSurfaceBorder
        ),
        modifier = Modifier
          .weight(1f)
          .padding(horizontal = 8.dp)
          .testTag("inline_speed_slider")
      )
    }
  }
}
