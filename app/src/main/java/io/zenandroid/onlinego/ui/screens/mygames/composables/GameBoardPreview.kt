package io.zenandroid.onlinego.ui.screens.mygames.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.gamelogic.RulesManager
import io.zenandroid.onlinego.ui.composables.Board
import io.zenandroid.onlinego.utils.CrashReporter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun GameBoardPreview(
  game: Game,
  modifier: Modifier = Modifier,
  drawShadow: Boolean = false,
) {
  var position by remember(game) { mutableStateOf<Position?>(null) }
  var loading by remember(game) { mutableStateOf(true) }
  LaunchedEffect(game) {
    try {
      position = withContext(Dispatchers.Default) {
        RulesManager.replay(game, computeTerritory = false)
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      CrashReporter.recordException(e)
    } finally {
      loading = false
    }
  }
  Box(modifier, contentAlignment = Alignment.Center) {
    Board(
      boardWidth = game.width,
      boardHeight = game.height,
      position = position,
      drawCoordinates = false,
      interactive = false,
      drawShadow = drawShadow,
      fadeInLastMove = false,
      fadeOutRemovedStones = false,
    )
    if (loading) CircularProgressIndicator(modifier = Modifier.size(24.dp))
  }
}
