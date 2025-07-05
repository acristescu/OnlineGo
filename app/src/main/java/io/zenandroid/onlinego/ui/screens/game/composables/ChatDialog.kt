package io.zenandroid.onlinego.ui.screens.game.composables

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.zenandroid.onlinego.data.model.local.Message
import io.zenandroid.onlinego.ui.screens.game.ChatMessage
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme

@Composable
fun ChatDialog(
  messages: Map<Long, List<ChatMessage>>,
  onDialogDismiss: (() -> Unit),
  onSendMessage: ((String) -> Unit),
) {
  BackHandler { onDialogDismiss() }
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0x88000000))
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null
      ) { onDialogDismiss() }
      .imePadding()
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null
        ) { }
        .fillMaxWidth(.9f)
        .fillMaxHeight(.9f)
        .align(Alignment.Center)
        .shadow(4.dp)
        .background(
          color = MaterialTheme.colorScheme.surface,
          shape = RoundedCornerShape(10.dp)
        )
        .padding(16.dp)
    ) {
      val listState = rememberLazyListState()
      LaunchedEffect(messages) {
        val index =
          messages.values.fold(messages.keys.size) { count, list -> count + list.size } - 1
        if (index >= 0) {
          listState.animateScrollToItem(index)
        }
      }
      LazyColumn(
        state = listState,
        modifier = Modifier.weight(1f)
      ) {
        messages.keys.sortedBy { it }.forEach { moveNo ->
          stickyHeader {
            Text(
              text = "Move $moveNo",
              style = MaterialTheme.typography.titleSmall,
              color = MaterialTheme.colorScheme.onBackground,
              modifier = Modifier
                .fillMaxWidth(1f)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                .padding(vertical = 8.dp),
              textAlign = TextAlign.Center,
            )
          }
          items(messages[moveNo] ?: emptyList()) {
            if (it.fromUser) {
              Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                  .fillParentMaxWidth()
                  .padding(bottom = 8.dp)
              ) {
                Text(
                  text = it.message.text,
                  color = MaterialTheme.colorScheme.onSurface,
                  style = MaterialTheme.typography.bodyMedium,
                  modifier = Modifier
                    .border(
                      width = 1.dp,
                      color = MaterialTheme.colorScheme.outline,
                      shape = RoundedCornerShape(24.dp, 0.dp, 24.dp, 24.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                )
              }
            } else {
              Row(
                modifier = Modifier
                  .fillParentMaxWidth()
                  .padding(bottom = 8.dp)
              ) {
                Text(
                  text = it.message.text,
                  color = MaterialTheme.colorScheme.surface,
                  style = MaterialTheme.typography.bodyMedium,
                  modifier = Modifier
                    .background(
                      color = MaterialTheme.colorScheme.surfaceTint,
                      shape = RoundedCornerShape(0.dp, 24.dp, 24.dp, 24.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                )
              }
            }
          }
        }
      }
      Row(
        verticalAlignment = Alignment.CenterVertically
      ) {
        var message by rememberSaveable { mutableStateOf("") }
        OutlinedTextField(
          value = message,
          onValueChange = { message = it },
          modifier = Modifier.weight(1f),
          placeholder = { Text("Type a message...") }
        )
        IconButton(
          onClick = {
            onSendMessage(message)
            message = ""
          },
          enabled = message.isNotBlank()
        ) {
          Icon(
            painter = rememberVectorPainter(image = Icons.AutoMirrored.Rounded.Send),
            contentDescription = "send",
          )
        }
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
fun Preview() {
    OnlineGoTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            ChatDialog(
                messages = mapOf(
                    0L to listOf(
                        ChatMessage(true, Message(Message.Type.MAIN, null, "User", 2134L, 0L, 0, "aaaa", "This is a message from the user at move 0")),
                        ChatMessage(false, Message(Message.Type.MAIN, null, "User", 2134L, 0L, 0, "aaaa", "This is a reply from the opponent at move 0")),
                    ),
                    1L to listOf(
                        ChatMessage(true, Message(Message.Type.MAIN, null, "User", 2134L, 0L, 0, "aaaa", "This is a message from the user at move 1")),
                        ChatMessage(false, Message(Message.Type.MAIN, null, "User", 2134L, 0L, 0, "aaaa", "This is a reply from the opponent at move 1")),
                    )
                ),
                onDialogDismiss = {},
                onSendMessage = {}
            )
        }
    }
}
