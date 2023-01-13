package io.zenandroid.onlinego.ui.screens.mygames.composables

import android.content.res.Configuration
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
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
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme
import io.zenandroid.onlinego.utils.calculateTimer

@ExperimentalComposeUiApi
@Composable
fun LargeGameItem(game: Game, boardTheme: BoardTheme, userId: Long, onAction: (Action) -> Unit, modifier: Modifier = Modifier) {
    val opponent =
        when (userId) {
            game.blackPlayer.id -> game.whitePlayer
            game.whitePlayer.id -> game.blackPlayer
            else -> null
        }

    Surface(
        shape = MaterialTheme.shapes.large,
        modifier = modifier
            .padding(horizontal = 24.dp)
    ) {
        Column(modifier = Modifier
            .clickable {
                onAction(Action.GameSelected(game))
            }
            .padding(
                vertical = 16.dp,
                horizontal = 24.dp
            )) {
            Board(
                boardWidth = game.width,
                boardHeight = game.height,
                position = game.position,
                boardTheme = boardTheme,
                drawCoordinates = false,
                interactive = false,
                fadeInLastMove = false,
                fadeOutRemovedStones = false,
                modifier = Modifier.clip(MaterialTheme.shapes.large)
                    .run {
                        if(LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                            height((LocalConfiguration.current.screenHeightDp * .4f).dp)
                        } else {
                            this
                        }
                    }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Column {
                    Row {
                        Text(
                            text = opponent?.username ?: "Unknown",
                            style = TextStyle.Default.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        val indicatorColor =
                            if (opponent?.id == game.blackPlayer.id) StoneType.BLACK else StoneType.WHITE
                        PlayerColorIndicator(
                            color = indicatorColor,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                    Row {
                        Text(
                            text = calculateTimer(game),
                            fontSize = 12.sp,
                        )
                        if (game.pauseControl.isPaused()) {
                            Text(
                                text = "  ·  paused",
                                style = TextStyle.Default.copy(
                                    fontSize = 12.sp,
                                ),
                            )
                        }
                    }
                }
                if(game.messagesCount != null && game.messagesCount != 0) {
                    Spacer(modifier = Modifier.weight(1f))
                    ChatIndicator(
                        chatCount = game.messagesCount,
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .align(Alignment.CenterVertically)
                    )
                }
            }
        }
    }
}

@ExperimentalComposeUiApi
@Preview
@Composable
private fun Preview() {
    OnlineGoTheme {
        LargeGameItem(
            game = Game.sampleData(),
            boardTheme = BoardTheme.WOOD,
            userId = 100L,
            onAction = {}
        )
    }
}