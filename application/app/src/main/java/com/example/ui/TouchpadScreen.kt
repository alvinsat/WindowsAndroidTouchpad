package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.network.ConnectionState
import com.example.network.ConnectionType
import com.example.ui.components.ConnectionStatusBar
import com.example.ui.components.ManualIpDialog
import com.example.ui.components.PythonServerHelpDialog
import com.example.ui.components.QuickSensitivityBar
import com.example.ui.components.TouchpadSettingsDialog
import com.example.ui.components.TouchpadSurface
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.PS1Blue
import com.example.ui.theme.PS1Surface
import com.example.ui.theme.PS1SurfaceBorder
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun TouchpadScreen(
  viewModel: TouchpadViewModel
) {
  val connectionState by viewModel.connectionState.collectAsState()
  val settings by viewModel.settings.collectAsState()
  val isDragging by viewModel.isDragging.collectAsState()
  val isDragLocked by viewModel.isDragLocked.collectAsState()

  val showManualIpDialog by viewModel.showManualIpDialog.collectAsState()
  val showHelpDialog by viewModel.showHelpDialog.collectAsState()
  val showSettingsDialog by viewModel.showSettingsDialog.collectAsState()

  // Controls visibility state (Default to hidden so touchpad fills all screen space)
  var showControls by remember { mutableStateOf(false) }

  val snackbarHostState = remember { SnackbarHostState() }

  val isConnected = connectionState is ConnectionState.Connected
  val serverIp = (connectionState as? ConnectionState.Connected)?.serverIp ?: "127.0.0.1"

  val connectionIndicatorColor = when (connectionState) {
    is ConnectionState.Connected -> StatusGreen
    is ConnectionState.Searching -> StatusAmber
    is ConnectionState.Error -> StatusRed
    is ConnectionState.Disconnected -> TextMuted
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    containerColor = DarkBackground,
    contentWindowInsets = WindowInsets.safeDrawing,
    snackbarHost = { SnackbarHost(snackbarHostState) }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
      Column(
        modifier = Modifier.fillMaxSize()
      ) {
        // Collapsible Controls (Connection Status & Speed Multiplier)
        AnimatedVisibility(
          visible = showControls,
          enter = expandVertically() + fadeIn(),
          exit = shrinkVertically() + fadeOut()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 6.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(modifier = Modifier.weight(1f)) {
                ConnectionStatusBar(
                  connectionState = connectionState,
                  isBinaryMode = settings.binaryByteMode,
                  onRefreshClick = { viewModel.startDiscovery() },
                  onManualIpClick = { viewModel.setShowManualIpDialog(true) },
                  onSettingsClick = { viewModel.setShowSettingsDialog(true) },
                  onHelpClick = { viewModel.setShowHelpDialog(true) }
                )
              }
              Spacer(modifier = Modifier.width(6.dp))
              IconButton(
                onClick = { showControls = false },
                modifier = Modifier
                  .size(40.dp)
                  .background(PS1Surface, RoundedCornerShape(12.dp))
                  .border(1.dp, PS1SurfaceBorder, RoundedCornerShape(12.dp))
                  .testTag("hide_controls_button")
              ) {
                Icon(
                  imageVector = Icons.Default.KeyboardArrowUp,
                  contentDescription = "Hide status and speed",
                  tint = TextSecondary,
                  modifier = Modifier.size(22.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Inline Speed Multiplier Control & Drag Lock Toggle
            QuickSensitivityBar(
              sensitivity = settings.pointerSensitivity,
              onSensitivityChange = { viewModel.setPointerSensitivity(it) },
              onOpenSettings = { viewModel.setShowSettingsDialog(true) },
              isDragLocked = isDragLocked,
              onToggleDragLock = { viewModel.toggleDragLock() }
            )
          }
        }

        // FULL TOUCH SURFACE (Fills all available screen space within safe area)
        TouchpadSurface(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          isConnected = isConnected,
          hapticsEnabled = settings.hapticFeedback,
          isDragging = isDragging,
          isDragLocked = isDragLocked,
          onUnlockDrag = { viewModel.unlockDrag() },
          swapNavDirection = settings.swapFourFingerNavDirection,
          onPointerDrag = { dx, dy, isDragMode -> viewModel.onPointerDrag(dx, dy, isDragMode) },
          onScrollDelta = { dx, dy -> viewModel.onScrollDelta(dx, dy) },
          onFlingScroll = { vx, vy -> viewModel.startInfinityScroll(vx, vy) },
          onCancelInfinityScroll = { viewModel.cancelInfinityScroll() },
          onSingleTap = { viewModel.sendClick("left") },
          onDoubleTap = { viewModel.sendClick("double") },
          onTwoFingerTap = { viewModel.sendClick("right") },
          onMiddleClick = { viewModel.sendClick("middle") },
          onDragStart = { viewModel.sendDragStart() },
          onDragEnd = { viewModel.sendDragEnd() },
          onFourFingerSwipe = { isForward -> viewModel.onFourFingerSwipe(isForward) },
          onResetPointerFilter = { viewModel.resetPointerFilter() }
        )
      }

      // Compact floating button to reveal status & speed multiplier when hidden
      if (!showControls) {
        Surface(
          onClick = { showControls = true },
          shape = RoundedCornerShape(20.dp),
          color = PS1Surface.copy(alpha = 0.92f),
          border = BorderStroke(1.dp, PS1SurfaceBorder),
          shadowElevation = 4.dp,
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = 8.dp, end = 8.dp)
            .testTag("toggle_controls_button")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(connectionIndicatorColor)
            )
            Spacer(modifier = Modifier.width(6.dp))

            // Icon showing active connection type
            val pillIcon = when (val state = connectionState) {
              is ConnectionState.Connected -> when (state.connectionType) {
                ConnectionType.ADB_REVERSE -> Icons.Default.Usb
                ConnectionType.LOCAL_WIFI -> Icons.Default.Wifi
                ConnectionType.USB_TETHERING -> Icons.Default.Usb
                ConnectionType.MANUAL -> Icons.Default.Tune
              }
              else -> null
            }

            if (pillIcon != null) {
              Icon(
                imageVector = pillIcon,
                contentDescription = null,
                tint = PS1Blue,
                modifier = Modifier.size(13.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
            }

            val pillLabel = when (val state = connectionState) {
              is ConnectionState.Connected -> when (state.connectionType) {
                ConnectionType.ADB_REVERSE -> "USB • ${(settings.pointerSensitivity * 10).roundToInt() / 10f}x"
                ConnectionType.LOCAL_WIFI -> "Wi-Fi • ${(settings.pointerSensitivity * 10).roundToInt() / 10f}x"
                ConnectionType.USB_TETHERING -> "Tether • ${(settings.pointerSensitivity * 10).roundToInt() / 10f}x"
                ConnectionType.MANUAL -> "Speed ${(settings.pointerSensitivity * 10).roundToInt() / 10f}x"
              }
              is ConnectionState.Searching -> "Searching..."
              is ConnectionState.Error -> "Offline"
              is ConnectionState.Disconnected -> "Disconnected"
            }

            Text(
              text = pillLabel,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = TextPrimary,
              fontSize = 11.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
              imageVector = Icons.Default.Tune,
              contentDescription = "Show status and speed",
              tint = PS1Blue,
              modifier = Modifier.size(15.dp)
            )
          }
        }
      }
    }
  }

  // Dialogs
  if (showManualIpDialog) {
    ManualIpDialog(
      initialIp = serverIp,
      onDismiss = { viewModel.setShowManualIpDialog(false) },
      onConfirm = { ip -> viewModel.setManualIp(ip) }
    )
  }

  if (showSettingsDialog) {
    TouchpadSettingsDialog(
      settings = settings,
      onDismiss = { viewModel.setShowSettingsDialog(false) },
      onSave = { pointer, accel, scroll, invert, invertH, swapFourFingerNav, binary, haptics, infinity, hideNotification, hideNavigation ->
        viewModel.updateSettings(pointer, accel, scroll, invert, invertH, swapFourFingerNav, binary, haptics, infinity, hideNotification, hideNavigation)
      }
    )
  }

  if (showHelpDialog) {
    PythonServerHelpDialog(
      onDismiss = { viewModel.setShowHelpDialog(false) }
    )
  }
}
