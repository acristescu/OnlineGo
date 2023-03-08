package io.zenandroid.onlinego.ui.composables

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.zenandroid.onlinego.utils.repeatingClickable

@Composable
fun BottomBar(
  buttons: List<BottomBarButton>,
  bottomText: String?,
  onButtonPressed: (BottomBarButton) -> Unit,
) {
  Row(modifier = Modifier.height(56.dp)) {
    buttons.forEach {
      key(it.javaClass) {
        Box(modifier = Modifier
          .fillMaxHeight()
          .weight(1f)) {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .alpha(if (it.enabled) 1f else .4f)
              .background(
                if (it.highlighted) MaterialTheme.colors.primaryVariant else MaterialTheme.colors.surface
              )
              .clickable(enabled = it.enabled) {
                if (!it.repeatable) onButtonPressed(it)
              }
              .repeatingClickable(
                remember { MutableInteractionSource() },
                it.repeatable && it.enabled
              ) { onButtonPressed(it) },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
          ) {
            Icon(
              it.icon,
              null,
              modifier = Modifier.size(24.dp),
              tint = MaterialTheme.colors.onSurface,
            )
            Text(
              text = it.label,
              style = MaterialTheme.typography.h5,
              color = MaterialTheme.colors.onSurface,
            )
          }
          androidx.compose.animation.AnimatedVisibility(
            visible = it.bubbleText != null,
            enter = fadeIn(TweenSpec( 500)) + scaleIn(SpringSpec(Spring.DampingRatioHighBouncy)),
            exit = fadeOut() + shrinkOut(),
            modifier = Modifier
              .align(Alignment.TopCenter)
          ) {
            it.bubbleText?.let { bubble ->
              Text(
                text = bubble,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.surface,
                modifier = Modifier
                  .padding(start = 19.dp, top = 5.dp)
                  .background(MaterialTheme.colors.primary, CircleShape)
                  .size(16.dp)
                  .wrapContentHeight(),

                )
            }
          }
        }
      }
    }
    bottomText?.let { text ->
      Spacer(modifier = Modifier.weight(.5f))
      Text(
        text = text,
        style = MaterialTheme.typography.h2,
        color = MaterialTheme.colors.onSurface,
        modifier = Modifier.align(Alignment.CenterVertically)
      )
      DotsFlashing(
        dotSize = 4.dp,
        color = MaterialTheme.colors.onBackground,
        modifier = Modifier
          .align(Alignment.CenterVertically)
          .padding(top = 10.dp, start = 4.dp)
      )
      Spacer(modifier = Modifier.weight(.5f))
    }
  }
}

interface BottomBarButton {
  val icon: ImageVector
  val label: String
  val repeatable: Boolean
  val enabled: Boolean
  val bubbleText: String?
  val highlighted: Boolean
}
