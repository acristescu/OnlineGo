package io.zenandroid.onlinego.ui.screens.mygames.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.zenandroid.onlinego.data.model.BoardTheme
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.ui.composables.Board
import io.zenandroid.onlinego.ui.screens.mygames.Action


@ExperimentalComposeUiApi
@Composable
fun HistoricGameLazyRow(
    games: List<Game>,
    boardTheme: BoardTheme,
    userId: Long,
    loadedAllHistoricGames: Boolean,
    onAction: (Action) -> Unit
) {
    LazyRow {
        items(games) { game ->
            Surface(
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .size(width = 125.dp, height = 140.dp)
                    .padding(horizontal = 8.dp)
            ) {
                Column(
                    modifier = Modifier.clickable {
                        onAction(Action.GameSelected(game))
                    }
                ) {
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
                            .padding(horizontal = 15.dp, vertical = 10.dp)
                            .clip(MaterialTheme.shapes.small)
                    )
                    val opponent =
                        when (userId) {
                            game.blackPlayer.id -> game.whitePlayer
                            game.whitePlayer.id -> game.blackPlayer
                            else -> null
                        }

                    Text(
                        text = opponent?.username ?: "Unknown",
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        style = TextStyle.Default.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 15.dp)
                    )
                    val outcome = when {
                        game.outcome == "Cancellation" -> "Cancelled"
                        userId == game.blackPlayer.id ->
                            if (game.blackLost == true) "Lost"
                            else "Won"
                        userId == game.whitePlayer.id ->
                            if (game.whiteLost == true) "Lost"
                            else "Won"
                        game.whiteLost == true ->
                            "Black won"
                        else ->
                            "White won"
                    }
                    Text(
                        text = outcome,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 15.dp)
                    )
                }
            }
        }
        if (!loadedAllHistoricGames) {
            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .size(width = 125.dp, height = 140.dp)
                        .padding(horizontal = 8.dp)
                ) {
                    LaunchedEffect(games) {
                        onAction(Action.LoadMoreHistoricGames(games.lastOrNull()))
                    }
                    CircularProgressIndicator(
                        modifier = Modifier.padding(horizontal = 15.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}
