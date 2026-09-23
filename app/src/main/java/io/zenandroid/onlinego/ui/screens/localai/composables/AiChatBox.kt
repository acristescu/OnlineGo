package io.zenandroid.onlinego.ui.screens.localai.composables

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme
import kotlinx.coroutines.delay

@Composable
fun AiChatBox(
  text: String?,
  modifier: Modifier = Modifier,
) {
  ElevatedCard(
    modifier = modifier,
    colors = CardDefaults.elevatedCardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ),
    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = Icons.AutoMirrored.Filled.Chat,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
          .padding(end = 12.dp)
          .size(22.dp),
      )
      AnimatedContent(
        targetState = text,
        transitionSpec = {
          fadeIn(tween(200))
            .togetherWith(fadeOut(tween(150)))
        },
        label = "aiChatBox",
      ) { targetText ->
        if (targetText != null) {
          TypewriterText(
            text = targetText,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Start,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}

@Composable
private fun TypewriterText(
  text: String,
  color: Color,
  style: TextStyle,
  textAlign: TextAlign,
  maxLines: Int,
  overflow: TextOverflow,
) {
  val words = remember(text) { text.split(" ") }
  val isPreview = LocalInspectionMode.current
  var visibleWordCount by remember(text) { mutableIntStateOf(if (isPreview) words.size else 0) }

  if (!isPreview) {
    LaunchedEffect(text) {
      words.indices.forEach { index ->
        visibleWordCount = index + 1
        if (index < words.lastIndex) delay(WordRevealDelayMillis)
      }
    }
  }

  Text(
    text = words.take(visibleWordCount).joinToString(" "),
    color = color,
    style = style,
    textAlign = textAlign,
    maxLines = maxLines,
    overflow = overflow,
  )
}

private const val WordRevealDelayMillis = 45L

@Composable
@Preview
private fun AiChatBoxShortPreview() {
  OnlineGoTheme {
    AiChatBox(text = "Your turn", modifier = Modifier.height(72.dp))
  }
}

@Composable
@Preview
private fun AiChatBoxLongPreview() {
  OnlineGoTheme {
    AiChatBox(
      text = "Game ended because of two passes. Final score is black 61.5 to white 58. Looks like I win this time.",
      modifier = Modifier.height(72.dp),
    )
  }
}
