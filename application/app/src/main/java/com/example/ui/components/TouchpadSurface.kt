package com.example.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import kotlin.math.hypot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class TouchPoint(val id: Long, val position: Offset)

/**
 * Full-surface touch area occupying the whole screen:
 * - Immediate pointer delta dispatch (<1ms latency)
 * - 1 finger: Drag to move cursor, quick tap for left-click, rapid double-tap for double-click
 * - 2 fingers: Vertical drag to scroll smoothly, 2-finger tap for right-click
 * - 3 fingers: Hold & drag for Windows drag-and-drop, tap for middle mouse click
 * - 4 fingers: Swipe left/right for browser & Explorer Forward/Backward (with direction swap option)
 * - Tactile haptic feedback on taps
 * - Dynamic touch ripple visual indicators
 */
@Composable
fun TouchpadSurface(
  modifier: Modifier = Modifier,
  isConnected: Boolean,
  hapticsEnabled: Boolean,
  isDragging: Boolean = false,
  isDragLocked: Boolean = false,
  onUnlockDrag: () -> Unit = {},
  swapNavDirection: Boolean = false,
  onPointerDrag: (dx: Float, dy: Float, isDragMode: Boolean) -> Unit,
  onScrollDelta: (dx: Float, dy: Float) -> Unit,
  onFlingScroll: (velX: Float, velY: Float) -> Unit = { _, _ -> },
  onCancelInfinityScroll: () -> Unit = {},
  onSingleTap: () -> Unit,
  onDoubleTap: () -> Unit,
  onTwoFingerTap: () -> Unit,
  onMiddleClick: () -> Unit,
  onDragStart: () -> Unit,
  onDragEnd: () -> Unit,
  onFourFingerSwipe: (isForward: Boolean) -> Unit = {},
  onResetPointerFilter: () -> Unit = {}
) {
  val view = LocalView.current
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val activePointers = remember { mutableStateListOf<TouchPoint>() }
  var isTouched by remember { mutableStateOf(false) }
  var lastTapTime by remember { mutableStateOf(0L) }
  var consecutiveTaps by remember { mutableStateOf(0) }
  var navBadgeText by remember { mutableStateOf<String?>(null) }
  var navBadgeTimerJob by remember { mutableStateOf<Job?>(null) }

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

  fun triggerMiddleClickFeedback() {
    if (hapticsEnabled) {
      view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
      VibrationHelper.vibrateMiddleClick(context)
    }
  }

  fun triggerDragStartFeedback() {
    if (hapticsEnabled) {
      view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
      VibrationHelper.vibrateDragState(context, isStarting = true)
    }
  }

  fun triggerDragEndFeedback() {
    if (hapticsEnabled) {
      view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
      VibrationHelper.vibrateDragState(context, isStarting = false)
    }
  }

  fun triggerNavigationFeedback() {
    if (hapticsEnabled) {
      view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
      VibrationHelper.vibrateNavigation(context)
    }
  }

  val surfaceBorderColor = if (isConnected) {
    if (isDragging || isDragLocked) {
      Color(0xFFFFB300) // Glowing amber border when drag is active or locked
    } else if (isTouched) {
      CyanPrimary
    } else {
      DarkSurfaceBorder
    }
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
        width = if (isDragging) 2.dp else 1.5.dp,
        color = surfaceBorderColor,
        shape = RoundedCornerShape(20.dp)
      )
      .testTag("touchpad_surface")
      .pointerInput(isConnected) {
        if (!isConnected) return@pointerInput

        awaitEachGesture {
          val downEvent = awaitFirstDown(requireUnconsumed = false)
          val downTime = System.currentTimeMillis()
          val initialDownPos = downEvent.position
          isTouched = true

          onCancelInfinityScroll()
          onResetPointerFilter()

          var maxPointers = 1
          var hasMovedSignificant = false
          val touchSlopPx = 14f // Slop deadzone suppresses tremor jumping during taps

          var lastPos1 = downEvent.position
          var lastPos2: Offset? = null

          // Real-time velocity tracking for two-finger infinity momentum fling scroll
          var scrollVelX = 0f
          var scrollVelY = 0f
          var lastScrollEventTime = downTime

          // Detect single-finger tap & hold drag gesture
          val timeSinceLastTap = downTime - lastTapTime
          val isPotentialSingleTapDrag = (consecutiveTaps >= 1 && timeSinceLastTap < 420)
          var isDraggingActive = false

          // Three-finger hold-to-drag / tap-and-hold drag state
          var threeFingerInitialPos1: Offset? = null
          var threeFingerInitialPos2: Offset? = null
          var threeFingerInitialPos3: Offset? = null
          var threeFingerDownTime = 0L

          // Four-finger swipe state for browser/explorer forward & backward
          var fourFingerDownTime = 0L
          var fourFingerStartX = 0f
          var fourFingerStartY = 0f
          var fourFingerSwipeTriggered = false

          // Track previous positions for multi-finger relative tracking
          var lastPos3: Offset? = null

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
              if (maxPointers == 1 || isDraggingActive || isDragLocked) {
                val currentPos = pressedPointers[0].position
                val totalDisplacement = hypot(
                  currentPos.x - initialDownPos.x,
                  currentPos.y - initialDownPos.y
                )

                if (!hasMovedSignificant) {
                  if (totalDisplacement > touchSlopPx) {
                    hasMovedSignificant = true
                    // Single finger double-tap & slide drag OR stationary hold drag (held stationary > 280ms)
                    val isStationaryHoldDrag = (System.currentTimeMillis() - downTime > 280L)
                    if ((isPotentialSingleTapDrag || isStationaryHoldDrag || isDragLocked) && !isDraggingActive) {
                      isDraggingActive = true
                      triggerDragStartFeedback()
                      if (!isDragLocked) {
                        onDragStart()
                      }
                    }
                    val initialDx = currentPos.x - lastPos1.x
                    val initialDy = currentPos.y - lastPos1.y
                    onPointerDrag(initialDx, initialDy, isDraggingActive || isDragLocked)
                    lastPos1 = currentPos
                  }
                } else {
                  val dx = currentPos.x - lastPos1.x
                  val dy = currentPos.y - lastPos1.y
                  onPointerDrag(dx, dy, isDraggingActive || isDragLocked)
                  lastPos1 = currentPos
                }
              }
              lastPos2 = null
              lastPos3 = null
            } else if (pointerCount == 2) {
              // PURE TWO-FINGER SCROLLING (Vertical + Horizontal)
              val p1 = pressedPointers[0].position
              val p2 = pressedPointers[1].position

              if (lastPos2 != null) {
                val dx1 = p1.x - lastPos1.x
                val dx2 = p2.x - lastPos2.x
                val avgDx = (dx1 + dx2) / 2f

                val dy1 = p1.y - lastPos1.y
                val dy2 = p2.y - lastPos2.y
                val avgDy = (dy1 + dy2) / 2f

                val now = System.currentTimeMillis()
                val dt = (now - lastScrollEventTime).coerceIn(1L, 100L)
                val instVx = avgDx / dt.toFloat()
                val instVy = avgDy / dt.toFloat()

                // Smooth exponential velocity tracking
                scrollVelX = 0.7f * instVx + 0.3f * scrollVelX
                scrollVelY = 0.7f * instVy + 0.3f * scrollVelY
                lastScrollEventTime = now

                if (abs(avgDx) > 1.8f || abs(avgDy) > 1.8f) {
                  hasMovedSignificant = true
                  onScrollDelta(avgDx, avgDy)
                }
              } else {
                lastScrollEventTime = System.currentTimeMillis()
              }

              lastPos1 = p1
              lastPos2 = p2
              lastPos3 = null
            } else if (pointerCount == 3) {
              // THREE-FINGER GESTURE: Tap-Hold or Hold-then-Drag for Drag and Drop
              val p1 = pressedPointers[0].position
              val p2 = pressedPointers[1].position
              val p3 = pressedPointers[2].position

              if (threeFingerDownTime == 0L) {
                threeFingerDownTime = System.currentTimeMillis()
                threeFingerInitialPos1 = p1
                threeFingerInitialPos2 = p2
                threeFingerInitialPos3 = p3
              }

              val now = System.currentTimeMillis()
              val threeFingerHoldDuration = now - threeFingerDownTime

              if (lastPos3 != null && lastPos2 != null) {
                val dx1 = p1.x - lastPos1.x
                val dx2 = p2.x - lastPos2.x
                val dx3 = p3.x - lastPos3!!.x
                val avgDx = (dx1 + dx2 + dx3) / 3f

                val dy1 = p1.y - lastPos1.y
                val dy2 = p2.y - lastPos2.y
                val dy3 = p3.y - lastPos3!!.y
                val avgDy = (dy1 + dy2 + dy3) / 3f

                val disp1 = hypot(p1.x - (threeFingerInitialPos1?.x ?: p1.x), p1.y - (threeFingerInitialPos1?.y ?: p1.y))
                val disp2 = hypot(p2.x - (threeFingerInitialPos2?.x ?: p2.x), p2.y - (threeFingerInitialPos2?.y ?: p2.y))
                val disp3 = hypot(p3.x - (threeFingerInitialPos3?.x ?: p3.x), p3.y - (threeFingerInitialPos3?.y ?: p3.y))
                val maxDisp = maxOf(disp1, disp2, disp3)

                if (!isDraggingActive) {
                  // Enter Drag mode if 3 fingers are held or start moving together
                  if (threeFingerHoldDuration > 180L || maxDisp > touchSlopPx) {
                    isDraggingActive = true
                    hasMovedSignificant = true
                    triggerDragStartFeedback()
                    onDragStart()
                  }
                }

                if (isDraggingActive) {
                  // Move cursor while holding left mouse button down (Windows Drag and Drop)
                  onPointerDrag(avgDx, avgDy, true)
                }
              }

              lastPos1 = p1
              lastPos2 = p2
              lastPos3 = p3
            } else if (pointerCount >= 4) {
              // FOUR-FINGER GESTURE: Swipe Left/Right for Windows Explorer & Browser Forward/Backward
              val p1 = pressedPointers[0].position
              val p2 = pressedPointers[1].position
              val p3 = pressedPointers[2].position
              val p4 = pressedPointers[3].position

              val curAvgX = (p1.x + p2.x + p3.x + p4.x) / 4f
              val curAvgY = (p1.y + p2.y + p3.y + p4.y) / 4f

              if (fourFingerDownTime == 0L) {
                fourFingerDownTime = System.currentTimeMillis()
                fourFingerStartX = curAvgX
                fourFingerStartY = curAvgY
                fourFingerSwipeTriggered = false
              }

              if (!fourFingerSwipeTriggered) {
                val totalDx = curAvgX - fourFingerStartX
                val totalDy = curAvgY - fourFingerStartY

                // Confident horizontal swipe detection
                if (abs(totalDx) > 65f && abs(totalDx) > 1.2f * abs(totalDy)) {
                  fourFingerSwipeTriggered = true
                  hasMovedSignificant = true

                  val isSwipeLeft = totalDx < 0f
                  // Default (swapNavDirection == false):
                  // Swipe Left -> Forward (Alt + Right Arrow)
                  // Swipe Right -> Backward (Alt + Left Arrow)
                  // Swapped (swapNavDirection == true):
                  // Swipe Left -> Backward (Alt + Left Arrow)
                  // Swipe Right -> Forward (Alt + Right Arrow)
                  val isForward = if (!swapNavDirection) isSwipeLeft else !isSwipeLeft

                  triggerNavigationFeedback()
                  val badge = if (isForward) "FORWARD ➔ (Alt+Right)" else "⬅ BACKWARD (Alt+Left)"
                  navBadgeText = badge
                  navBadgeTimerJob?.cancel()
                  navBadgeTimerJob = coroutineScope.launch {
                    delay(1200)
                    navBadgeText = null
                  }
                  onFourFingerSwipe(isForward)
                }
              }

              lastPos1 = p1
              lastPos2 = p2
              lastPos3 = p3
            }

            // Consume touch event so parent doesn't intercept
            event.changes.forEach { it.consume() }
          }

          isTouched = false
          activePointers.clear()

          val gestureDuration = System.currentTimeMillis() - downTime
          val timeSinceLastScroll = System.currentTimeMillis() - lastScrollEventTime

          // If the fingers paused before letting go, cancel kinetic release velocity
          if (timeSinceLastScroll > 90L) {
            scrollVelX = 0f
            scrollVelY = 0f
          }

          if (isDraggingActive) {
            // Finished dragging: release mouse button unless Drag Lock is active
            if (!isDragLocked) {
              triggerDragEndFeedback()
              onDragEnd()
            }
            consecutiveTaps = 0
            lastTapTime = 0L
          } else if (fourFingerSwipeTriggered) {
            // 4-finger swipe was handled: clear taps and momentum
            consecutiveTaps = 0
            lastTapTime = 0L
          } else if (hasMovedSignificant && maxPointers >= 2) {
            // Finger released after scrolling!
            // Check if user let go while moving fast -> Trigger Infinity Scroll
            val flingSpeed = hypot(scrollVelX, scrollVelY)
            if (flingSpeed > 0.28f && maxPointers == 2) {
              onFlingScroll(scrollVelX, scrollVelY)
            }
            consecutiveTaps = 0
            lastTapTime = 0L
          } else if (!hasMovedSignificant && gestureDuration < 300) {
            // Clean stationary tap (Zero cursor jump!)
            val now = System.currentTimeMillis()
            if (maxPointers == 1) {
              if (now - lastTapTime < 340) {
                consecutiveTaps++
              } else {
                consecutiveTaps = 1
              }
              lastTapTime = now

              when (consecutiveTaps) {
                1 -> {
                  triggerClickFeedback()
                  onSingleTap()
                }
                2 -> {
                  triggerDoubleClickFeedback()
                  onDoubleTap()
                }
                3 -> {
                  // 3 consecutive taps -> Middle Mouse Click
                  triggerMiddleClickFeedback()
                  onMiddleClick()
                  consecutiveTaps = 0
                  lastTapTime = 0L
                }
                else -> {
                  triggerClickFeedback()
                  onSingleTap()
                  consecutiveTaps = 1
                }
              }
            } else if (maxPointers == 2) {
              // 2-finger tap -> Right Click
              triggerRightClickFeedback()
              onTwoFingerTap()
              consecutiveTaps = 0
              lastTapTime = 0L
            } else if (maxPointers == 3) {
              // 3-finger tap -> Middle Mouse Click
              triggerMiddleClickFeedback()
              onMiddleClick()
              consecutiveTaps = 0
              lastTapTime = 0L
            } else if (maxPointers >= 4) {
              consecutiveTaps = 0
              lastTapTime = 0L
            }
          } else {
            if (System.currentTimeMillis() - lastTapTime > 340) {
              consecutiveTaps = 0
            }
          }
        }
      }
      .drawBehind {
        // Draw subtle tactile dot matrix in authentic PS1 textured grip style
        val dotSpacing = 36.dp.toPx()
        val dotRadius = 1.3.dp.toPx()
        val dotColor = Color(0x28454854)

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
    // Top Dragging / Drag Lock HUD banner
    if (isDragLocked) {
      Surface(
        onClick = onUnlockDrag,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFD97706),
        shadowElevation = 6.dp,
        modifier = Modifier
          .align(Alignment.TopCenter)
          .padding(top = 16.dp)
          .testTag("banner_drag_lock")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "🔒 DRAG LOCKED (Tap to Drop)",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
          )
        }
      }
    } else if (isDragging) {
      Box(
        modifier = Modifier
          .align(Alignment.TopCenter)
          .padding(top = 16.dp)
          .background(Color(0xFFD97706), RoundedCornerShape(16.dp))
          .padding(horizontal = 14.dp, vertical = 6.dp)
      ) {
        Text(
          text = "● DRAGGING (Button Held)",
          color = Color.White,
          fontWeight = FontWeight.Bold,
          fontSize = 11.sp
        )
      }
    }

    // 4-Finger Navigation Feedback HUD Badge
    if (navBadgeText != null) {
      Box(
        modifier = Modifier
          .align(Alignment.TopCenter)
          .padding(top = if (isDragging || isDragLocked) 52.dp else 16.dp)
          .background(CyanPrimary, RoundedCornerShape(16.dp))
          .padding(horizontal = 16.dp, vertical = 7.dp)
      ) {
        Text(
          text = "● 4-FINGER: $navBadgeText",
          color = Color.Black,
          fontWeight = FontWeight.Bold,
          fontSize = 11.sp
        )
      }
    }

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
          text = "Full Touch Surface\n\n1 Finger: Move • Tap: Left Click • Tap-Hold / Double-Tap: Drag\n2 Fingers: Pure Scroll (Swipe Left: H-Scroll) • Tap: Right Click\n3 Fingers: Hold & Drag (Windows Drag & Drop) • Tap: Middle Mouse\n4 Fingers: Swipe Left: Forward • Swipe Right: Backward\nDrag Lock: Continuous drag for Unity, Unreal & Notifications",
          style = MaterialTheme.typography.bodyMedium,
          color = Color(0x88202430),
          fontSize = 12.sp,
          lineHeight = 20.sp,
          textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
      }
    }

    // Dynamic glowing touch rings for low-latency visual confirmation
    Canvas(modifier = Modifier.fillMaxSize()) {
      val ringColor = if (isDragging || isDragLocked) Color(0xFFD97706) else CyanPrimary
      for (pointer in activePointers) {
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(
              ringColor.copy(alpha = 0.35f),
              ringColor.copy(alpha = 0.05f),
              Color.Transparent
            ),
            center = pointer.position,
            radius = 54.dp.toPx()
          ),
          radius = 54.dp.toPx(),
          center = pointer.position
        )

        drawCircle(
          color = ringColor.copy(alpha = 0.9f),
          radius = 12.dp.toPx(),
          center = pointer.position
        )
      }
    }
  }
}
