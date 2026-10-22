package io.zenandroid.onlinego.ui.screens.mygames.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.data.model.local.isPaused
import io.zenandroid.onlinego.resources.Res
import io.zenandroid.onlinego.resources.mygames_paused
import io.zenandroid.onlinego.resources.unknown
import io.zenandroid.onlinego.ui.composables.Board
import io.zenandroid.onlinego.ui.composables.ChatIndicator
import io.zenandroid.onlinego.ui.composables.PlayerColorIndicator
import io.zenandroid.onlinego.ui.composables.clockText
import io.zenandroid.onlinego.ui.composables.isLandscape
import io.zenandroid.onlinego.ui.composables.screenSize
import io.zenandroid.onlinego.ui.screens.mygames.Action
import io.zenandroid.onlinego.ui.theme.OnlineGoPreviewTheme
import io.zenandroid.onlinego.utils.currentClockMillis
import org.jetbrains.compose.resources.stringResource

@ExperimentalComposeUiApi
@Composable
fun LargeGameItem(game: Game, userId: Long?, onAction: (Action) -> Unit, modifier: Modifier = Modifier) {
    val opponent =
        when (userId) {
            game.blackPlayer.id -> game.whitePlayer
            game.whitePlayer.id -> game.blackPlayer
            else -> null
        }

    SenteCard(
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
                drawCoordinates = false,
                interactive = false,
                fadeInLastMove = false,
                fadeOutRemovedStones = false,
                modifier = Modifier.clip(MaterialTheme.shapes.large)
                    .run {
                      if (isLandscape()) {
                        height(screenSize().height * .4f)
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
                          text = opponent?.username ?: stringResource(Res.string.unknown),
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
                          text = currentClockMillis(game)?.let { clockText(it) } ?: "",
                            fontSize = 12.sp,
                        )
                        if (game.pauseControl.isPaused()) {
                            Text(
                              text = "  ·  " + stringResource(Res.string.mygames_paused),
                                style = TextStyle.Default.copy(
                                    fontSize = 12.sp,
                                ),
                            )
                        }
                    }
                }
              val messagesCount = game.messagesCount
              if (messagesCount != null && messagesCount != 0) {
                    Spacer(modifier = Modifier.weight(1f))
                    ChatIndicator(
                      chatCount = messagesCount,
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
  OnlineGoPreviewTheme {
        LargeGameItem(
            game = Game.sampleData(),
            userId = 100L,
            onAction = {}
        )
    }
}