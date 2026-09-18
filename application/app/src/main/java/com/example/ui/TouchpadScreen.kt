package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.network.ConnectionState
import com.example.ui.components.ConnectionStatusBar
import com.example.ui.components.ManualIpDialog
import com.example.ui.components.PythonServerHelpDialog
import com.example.ui.components.QuickSensitivityBar
import com.example.ui.components.TouchpadSettingsDialog
import com.example.ui.components.TouchpadSurface
import com.example.ui.theme.DarkBackground

@Composable
fun TouchpadScreen(
  viewModel: TouchpadViewModel
) {
  val connectionState by viewModel.connectionState.collectAsState()
  val settings by viewModel.settings.collectAsState()

  val showManualIpDialog by viewModel.showManualIpDialog.collectAsState()
  val showHelpDialog by viewModel.showHelpDialog.collectAsState()
  val showSettingsDialog by viewModel.showSettingsDialog.collectAsState()

  val snackbarHostState = remember { SnackbarHostState() }

  val isConnected = connectionState is ConnectionState.Connected
  val serverIp = (connectionState as? ConnectionState.Connected)?.serverIp ?: "192.168.1."

  Scaffold(
    modifier = Modifier
      .fillMaxSize()
      .background(DarkBackground),
    containerColor = DarkBackground,
    snackbarHost = { SnackbarHost(snackbarHostState) }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .statusBarsPadding()
        .navigationBarsPadding()
        .padding(horizontal = 12.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.Top
    ) {
      // 1. Connection Status Bar
      ConnectionStatusBar(
        connectionState = connectionState,
        isBinaryMode = settings.binaryByteMode,
        onRefreshClick = { viewModel.startDiscovery() },
        onManualIpClick = { viewModel.setShowManualIpDialog(true) },
        onSettingsClick = { viewModel.setShowSettingsDialog(true) },
        onHelpClick = { viewModel.setShowHelpDialog(true) }
      )

      Spacer(modifier = Modifier.height(8.dp))

      // 2. Inline Sensitivity & Speed Control
      QuickSensitivityBar(
        sensitivity = settings.pointerSensitivity,
        onSensitivityChange = { viewModel.setPointerSensitivity(it) },
        onOpenSettings = { viewModel.setShowSettingsDialog(true) }
      )

      Spacer(modifier = Modifier.height(8.dp))

      // 3. FULL TOUCH SURFACE (Occupies the entire screen, no side scroll, no bottom buttons)
      TouchpadSurface(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        isConnected = isConnected,
        hapticsEnabled = settings.hapticFeedback,
        onPointerDrag = { dx, dy -> viewModel.onPointerDrag(dx, dy) },
        onScrollDelta = { dy -> viewModel.onScrollDelta(dy) },
        onSingleTap = { viewModel.sendClick("left") },
        onDoubleTap = { viewModel.sendClick("double") },
        onTwoFingerTap = { viewModel.sendClick("right") }
      )
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
      onSave = { pointer, accel, scroll, invert, binary, haptics ->
        viewModel.updateSettings(pointer, accel, scroll, invert, binary, haptics)
      }
    )
  }

  if (showHelpDialog) {
    PythonServerHelpDialog(
      onDismiss = { viewModel.setShowHelpDialog(false) }
    )
  }
}
