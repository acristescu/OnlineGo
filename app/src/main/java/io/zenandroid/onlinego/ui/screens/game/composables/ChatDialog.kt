package io.zenandroid.onlinego.ui.screens.game.composables

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Send
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import io.zenandroid.onlinego.ui.screens.game.UserAction
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme

@Composable
fun ChatDialog(
    messages: Map<Long, List<ChatMessage>>,
    onDialogDismiss: (() -> Unit),
    onSendMessage: ((String) -> Unit),
) {
    BackHandler { onDialogDismiss() }
    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color(0x88000000))
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) { onDialogDismiss() }
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
                    color = MaterialTheme.colors.surface,
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(16.dp)
        ) {
            val listState = rememberLazyListState()
            LaunchedEffect(messages) {
                val index =
                    messages.values.fold(messages.keys.size) { count, list -> count + list.size } - 1
                listState.animateScrollToItem(index.coerceAtLeast(0))
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f)
            ) {
                messages.keys.sortedBy { it }.forEach { moveNo ->
                    stickyHeader {
                        Text(
                            text = "Move $moveNo",
                            style = MaterialTheme.typography.h5,
                            color = MaterialTheme.colors.onBackground,
                            modifier = Modifier
                                .fillMaxWidth(1f)
                                .padding(bottom = 8.dp),
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
                                    color = MaterialTheme.colors.onSurface,
                                    style = MaterialTheme.typography.body2,
                                    modifier = Modifier
                                        .border(
                                            width = 1.dp,
                                            color = MaterialTheme.colors.onSurface,
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
                                    color = MaterialTheme.colors.surface,
                                    style = MaterialTheme.typography.body2,
                                    modifier = Modifier
                                        .background(
                                            color = MaterialTheme.colors.onSurface,
                                            shape = RoundedCornerShape(0.dp, 24.dp, 24.dp, 24.dp)
                                        )
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
            }
            Row {
                var message by rememberSaveable { mutableStateOf("") }
                TextField(
                    value = message,
                    onValueChange = { message = it },
                    colors = TextFieldDefaults.textFieldColors(
                        textColor = MaterialTheme.colors.onSurface
                    ),
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = {
                        onSendMessage(message)
                        message = ""
                    },
                    enabled = message.isNotBlank()
                ) {
                    Icon(
                        painter = rememberVectorPainter(image = Icons.Rounded.Send),
                        tint = MaterialTheme.colors.onSurface,
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