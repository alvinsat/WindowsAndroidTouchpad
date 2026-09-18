package com.example.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.util.VibrationHelper
import kotlin.math.abs

data class TouchPoint(val id: Long, val position: Offset)

/**
 * Full-surface touch area occupying the whole screen:
 * - Immediate pointer delta dispatch (<1ms latency)
 * - 1 finger: Drag to move cursor, quick tap for left-click, rapid double-tap for double-click
 * - 2 fingers: Vertical drag to scroll smoothly, 2-finger tap for right-click
 * - Tactile haptic feedback on taps
 * - Dynamic touch ripple visual indicators
 */
@Composable
fun TouchpadSurface(
  modifier: Modifier = Modifier,
  isConnected: Boolean,
  hapticsEnabled: Boolean,
  onPointerDrag: (dx: Float, dy: Float) -> Unit,
  onScrollDelta: (dy: Float) -> Unit,
  onSingleTap: () -> Unit,
  onDoubleTap: () -> Unit,
  onTwoFingerTap: () -> Unit
) {
  val view = LocalView.current
  val context = LocalContext.current
  val activePointers = remember { mutableStateListOf<TouchPoint>() }
  var isTouched by remember { mutableStateOf(false) }
  var lastTapTime by remember { mutableStateOf(0L) }

  fun triggerClickFeedback() {
    if (hapticsEnabled) {
      view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
      VibrationHelper.vibrateClick(context)
    }
  }

  fun triggerDoubleClickFeedback() {
    if (hapticsEnabled) {
      view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
      VibrationHelper.vibrateDoubleClick(context)
    }
  }

  fun triggerRightClickFeedback() {
    if (hapticsEnabled) {
      view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
      VibrationHelper.vibrateRightClick(context)
    }
  }

  val surfaceBorderColor = if (isConnected) {
    if (isTouched) CyanPrimary else DarkSurfaceBorder
  } else {
    DarkSurfaceBorder
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .clip(RoundedCornerShape(20.dp))
      .background(
        Brush.verticalGradient(
          colors = listOf(
            DarkSurface,
            DarkSurfaceElevated
          )
        )
      )
      .border(
        width = 1.5.dp,
        color = surfaceBorderColor,
        shape = RoundedCornerShape(20.dp)
      )
      .testTag("touchpad_surface")
      .pointerInput(isConnected) {
        if (!isConnected) return@pointerInput

        awaitEachGesture {
          val downEvent = awaitFirstDown(requireUnconsumed = false)
          val downTime = System.currentTimeMillis()
          isTouched = true

          var maxPointers = 1
          var hasMovedSignificant = false
          var totalDx = 0f
          var totalDy = 0f

          var lastPos1 = downEvent.position
          var lastPos2: Offset? = null

          activePointers.clear()
          activePointers.add(TouchPoint(downEvent.id.value, downEvent.position))

          while (true) {
            val event = awaitPointerEvent()
            val pressedPointers = event.changes.filter { it.pressed }

            if (pressedPointers.isEmpty()) {
              break
            }

            val pointerCount = pressedPointers.size
            if (pointerCount > maxPointers) {
              maxPointers = pointerCount
            }

            // Real-time touch point indicators for visual feedback
            activePointers.clear()
            for (p in pressedPointers) {
              activePointers.add(TouchPoint(p.id.value, p.position))
            }

            if (pointerCount == 1) {
              val currentPos = pressedPointers[0].position
              val dx = currentPos.x - lastPos1.x
              val dy = currentPos.y - lastPos1.y

              totalDx += abs(dx)
              totalDy += abs(dy)
              // Low threshold (3px) for immediate responsiveness without jitter
              if (totalDx > 3f || totalDy > 3f) {
                hasMovedSignificant = true
              }

              if (hasMovedSignificant) {
                onPointerDrag(dx, dy)
              }

              lastPos1 = currentPos
              lastPos2 = null
            } else if (pointerCount >= 2) {
              val p1 = pressedPointers[0].position
              val p2 = pressedPointers[1].position

              if (lastPos2 != null) {
                val dy1 = p1.y - lastPos1.y
                val dy2 = p2.y - lastPos2.y
                val avgDy = (dy1 + dy2) / 2f

                totalDy += abs(avgDy)
                if (totalDy > 3f) {
                  hasMovedSignificant = true
                }

                if (hasMovedSignificant) {
                  onScrollDelta(avgDy)
                }
              }

              lastPos1 = p1
              lastPos2 = p2
            }

            // Consume touch event so parent doesn't intercept
            event.changes.forEach { it.consume() }
          }

          isTouched = false
          activePointers.clear()

          // Check if gesture was a quick tap
          val gestureDuration = System.currentTimeMillis() - downTime
          if (!hasMovedSignificant && gestureDuration < 280) {
            if (maxPointers == 1) {
              val now = System.currentTimeMillis()
              if (now - lastTapTime < 280) {
                triggerDoubleClickFeedback()
                onDoubleTap()
                lastTapTime = 0L
              } else {
                lastTapTime = now
                triggerClickFeedback()
                onSingleTap()
              }
            } else if (maxPointers >= 2) {
              triggerRightClickFeedback()
              onTwoFingerTap()
              lastTapTime = 0L
            }
          }
        }
      }
      .drawBehind {
        // Draw subtle tactile dot matrix
        val dotSpacing = 36.dp.toPx()
        val dotRadius = 1.2.dp.toPx()
        val dotColor = Color(0x15FFFFFF)

        var x = dotSpacing / 2
        while (x < size.width) {
          var y = dotSpacing / 2
          while (y < size.height) {
            drawCircle(
              color = dotColor,
              radius = dotRadius,
              center = Offset(x, y)
            )
            y += dotSpacing
          }
          x += dotSpacing
        }
      }
  ) {
    // Watermark instructions centered cleanly
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(24.dp),
      contentAlignment = Alignment.Center
    ) {
      if (!isConnected) {
        Text(
          text = "Waiting for connection...",
          style = MaterialTheme.typography.bodyMedium,
          color = TextMuted,
          fontSize = 14.sp
        )
      } else {
        Text(
          text = "Full Touch Surface\n\n1 Finger: Move & Tap to Click\n2 Fingers: Drag to Scroll & Tap for Right Click",
          style = MaterialTheme.typography.bodyMedium,
          color = Color(0x35FFFFFF),
          fontSize = 13.sp,
          lineHeight = 22.sp,
          textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
      }
    }

    // Dynamic glowing touch rings for low-latency visual confirmation
    Canvas(modifier = Modifier.fillMaxSize()) {
      for (pointer in activePointers) {
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(
              CyanPrimary.copy(alpha = 0.35f),
              CyanPrimary.copy(alpha = 0.05f),
              Color.Transparent
            ),
            center = pointer.position,
            radius = 54.dp.toPx()
          ),
          radius = 54.dp.toPx(),
          center = pointer.position
        )

        drawCircle(
          color = CyanPrimary.copy(alpha = 0.9f),
          radius = 12.dp.toPx(),
          center = pointer.position
        )
      }
    }
  }
}
