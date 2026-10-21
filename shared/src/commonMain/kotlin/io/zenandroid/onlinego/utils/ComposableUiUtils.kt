package io.zenandroid.onlinego.utils

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun Modifier.repeatingClickable(
  interactionSource: InteractionSource,
  enabled: Boolean,
  maxDelayMillis: Long = 300,
  minDelayMillis: Long = 20,
  delayDecayFactor: Float = .15f,
  onClick: () -> Unit
): Modifier = composed {

  val currentClickListener by rememberUpdatedState(onClick)

  pointerInput(interactionSource, enabled) {
    forEachGesture {
      coroutineScope {
        awaitPointerEventScope {
          val down = awaitFirstDown(requireUnconsumed = false)
          val heldButtonJob = launch {
            var currentDelayMillis = maxDelayMillis
            while (enabled && down.pressed) {
              currentClickListener()
              delay(currentDelayMillis)
              val nextMillis = currentDelayMillis - (currentDelayMillis * delayDecayFactor)
              currentDelayMillis = nextMillis.toLong().coerceAtLeast(minDelayMillis)
            }
          }
          waitForUpOrCancellation()
          heldButtonJob.cancel()
        }
      }
    }
  }
}

fun Modifier.shimmer(
  visible: Boolean,
  shape: Shape = RoundedCornerShape(4.dp),
  shimmerColor: Color = Color.LightGray.copy(alpha = 0.6f),
  backgroundColor: Color = Color.LightGray.copy(alpha = 0.2f),
  durationMillis: Int = 1000
): Modifier = composed {
  if (!visible) return@composed this

  val transition = rememberInfiniteTransition(label = "shimmerTransition")
  val translateAnim by transition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = durationMillis, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "shimmerTranslateAnim"
  )

  this.background(
    brush = Brush.linearGradient(
      colors = listOf(
        backgroundColor,
        shimmerColor,
        backgroundColor,
      ),
      start = Offset.Zero,
      end = Offset(x = 100f + 400f * translateAnim, y = 100f + 400f * translateAnim)
    ),
    shape = shape
  )
}


@Composable
fun PreviewBackground(content: @Composable () -> Unit) {
  OnlineGoTheme {
    Surface(color = MaterialTheme.colorScheme.background) {
      content()
    }
  }
}
