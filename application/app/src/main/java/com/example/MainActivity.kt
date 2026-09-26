package com.example

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.ui.TouchpadScreen
import com.example.ui.TouchpadViewModel
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  private val viewModel: TouchpadViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setupWindowDisplayCutout()
    applyImmersiveMode()

    // Dynamically observe settings changes for notification & navigation bar preferences
    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.settings.collect { settings ->
          applyImmersiveMode(
            hideStatusBar = settings.hideNotificationBar,
            hideNavBar = settings.hideNavigationBar
          )
        }
      }
    }

    setContent {
      MyApplicationTheme {
        TouchpadScreen(viewModel = viewModel)
      }
    }
  }

  override fun onWindowFocusChanged(hasFocus: Boolean) {
    super.onWindowFocusChanged(hasFocus)
    if (hasFocus) {
      val settings = viewModel.settings.value
      applyImmersiveMode(
        hideStatusBar = settings.hideNotificationBar,
        hideNavBar = settings.hideNavigationBar
      )
    }
  }

  override fun onResume() {
    super.onResume()
    val settings = viewModel.settings.value
    applyImmersiveMode(
      hideStatusBar = settings.hideNotificationBar,
      hideNavBar = settings.hideNavigationBar
    )
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    // Maintain immersive full-screen across portrait/landscape rotation
    val settings = viewModel.settings.value
    applyImmersiveMode(
      hideStatusBar = settings.hideNotificationBar,
      hideNavBar = settings.hideNavigationBar
    )
  }

  /**
   * Extends layout into display cutouts / camera punch-holes so touchpad utilizes 100% of device screen.
   */
  private fun setupWindowDisplayCutout() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      window.attributes.layoutInDisplayCutoutMode =
        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
    }
  }

  /**
   * Applies immersive sticky mode hiding the notification (status) bar and navigation bars.
   * Uses BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE so momentary edge swipes show system bars as
   * transparent overlays without resizing the layout, then smoothly auto-hides them.
   */
  private fun applyImmersiveMode(
    hideStatusBar: Boolean = true,
    hideNavBar: Boolean = true
  ) {
    val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
    windowInsetsController.systemBarsBehavior =
      WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

    var typesToHide = 0
    var typesToShow = 0

    if (hideStatusBar) {
      typesToHide = typesToHide or WindowInsetsCompat.Type.statusBars()
    } else {
      typesToShow = typesToShow or WindowInsetsCompat.Type.statusBars()
    }

    if (hideNavBar) {
      typesToHide = typesToHide or WindowInsetsCompat.Type.navigationBars()
    } else {
      typesToShow = typesToShow or WindowInsetsCompat.Type.navigationBars()
    }

    if (typesToHide != 0) {
      windowInsetsController.hide(typesToHide)
    }
    if (typesToShow != 0) {
      windowInsetsController.show(typesToShow)
    }
  }
}

@androidx.compose.runtime.Composable
fun Greeting(name: String, modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier) {
  androidx.compose.material3.Text(text = "Hello $name!", modifier = modifier)
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@androidx.compose.runtime.Composable
fun GreetingPreview() {
  com.example.ui.theme.MyApplicationTheme { Greeting("Android") }
}
