package io.zenandroid.onlinego.ui.screens.localai.composables

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme

/**
 * The AI's chat message. Sized entirely by [modifier] (the caller gives it a fixed share of
 * the layout, e.g. via `Modifier.weight(...)`) rather than by its own text, so the message
 * length never changes this box's footprint and never shifts the board.
 */
@Composable
fun AiChatBox(
  text: String?,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.medium)
      .border(0.1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
      .padding(horizontal = 16.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      imageVector = Icons.AutoMirrored.Filled.Chat,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.tertiary,
      modifier = Modifier
        .padding(end = 8.dp)
        .size(20.dp),
    )
    AnimatedContent(
      targetState = text,
      transitionSpec = {
        (fadeIn(tween(200)) + slideInVertically(tween(200)) { it / 4 })
          .togetherWith(fadeOut(tween(150)))
      },
      label = "aiChatBox",
    ) { targetText ->
      if (targetText != null) {
        Text(
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
