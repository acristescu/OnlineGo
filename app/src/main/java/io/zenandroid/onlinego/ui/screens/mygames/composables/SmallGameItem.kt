package io.zenandroid.onlinego.ui.screens.mygames.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.zenandroid.onlinego.data.model.BoardTheme
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.data.model.local.isPaused
import io.zenandroid.onlinego.ui.composables.Board
import io.zenandroid.onlinego.ui.composables.ChatIndicator
import io.zenandroid.onlinego.ui.composables.PlayerColorIndicator
import io.zenandroid.onlinego.ui.screens.mygames.Action
import io.zenandroid.onlinego.utils.calculateTimer

@ExperimentalComposeUiApi
@Composable
fun SmallGameItem(game: Game, boardTheme: BoardTheme, userId: Long, onAction: (Action) -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .height(110.dp)
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        val opponent =
            when (userId) {
                game.blackPlayer.id -> game.whitePlayer
                game.whitePlayer.id -> game.blackPlayer
                else -> null
            }
        Row(modifier = Modifier.clickable { onAction(Action.GameSelected(game)) }) {
            Board(
                boardWidth = game.width,
                boardHeight = game.height,
                position = game.position,
                boardTheme = boardTheme,
                drawCoordinates = false,
                interactive = false,
                drawShadow = false,
                fadeInLastMove = false,
                fadeOutRemovedStones = false,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(horizontal = 10.dp, vertical = 10.dp)
                    .clip(MaterialTheme.shapes.small)
            )
            Column {
                Row (modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = opponent?.username ?: "Unknown",
                        color = MaterialTheme.colors.onSurface,
                        style = TextStyle.Default.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    val indicatorColor = if (opponent?.id == game.blackPlayer.id) StoneType.BLACK else StoneType.WHITE
                    PlayerColorIndicator(
                        color = indicatorColor,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
                if(game.blackLost != true && game.whiteLost != true) {
                    Row(modifier = Modifier.padding(top = 4.dp)) {
                        Text(
                            text = calculateTimer(game),
                            color = MaterialTheme.colors.onSurface,
                            style = TextStyle.Default.copy(
                                fontSize = 12.sp,
                            ),
                        )
                        if (game.pauseControl.isPaused()) {
                            Text(
                                text = "  ·  paused",
                                color = MaterialTheme.colors.onSurface,
                                style = TextStyle.Default.copy(
                                    fontSize = 12.sp,
                                ),
                            )
                        }
                    }
                } else {
                    val outcome = when {
                        game.outcome == "Cancellation" -> "Cancelled"
                        userId == game.blackPlayer.id ->
                            if (game.blackLost == true) "Lost by ${game.outcome}"
                            else "Won by ${game.outcome}"
                        userId == game.whitePlayer.id ->
                            if (game.whiteLost == true) "Lost by ${game.outcome}"
                            else "Won by ${game.outcome}"
                        game.whiteLost == true ->
                            "Black won by ${game.outcome}"
                        else ->
                            "White won by ${game.outcome}"
                    }
                    Text(
                        text = outcome,
                        color = MaterialTheme.colors.onSurface,
                        fontSize = 12.sp,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                if(game.messagesCount != null && game.messagesCount != 0) {
                    ChatIndicator(
                        chatCount = game.messagesCount,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
        }
    }
}
