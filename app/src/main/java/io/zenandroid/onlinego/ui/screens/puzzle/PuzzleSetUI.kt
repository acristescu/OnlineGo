package io.zenandroid.onlinego.ui.screens.puzzle

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.model.local.Puzzle
import io.zenandroid.onlinego.gamelogic.RulesManager
import io.zenandroid.onlinego.gamelogic.Util.toCoordinateSet
import io.zenandroid.onlinego.ui.composables.Board

private const val TAG = "PuzzleSetUI"

@Composable
fun PuzzleSetScreen(
  state: PuzzleSetState,
  fetchSolutions: (puzzleId: Long) -> Unit,
  onPuzzle: (puzzle: Puzzle) -> Unit,
  onBack: () -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxHeight()
  ) {
    TopAppBar(
      title = {
        val base = "Puzzles"
        val text = state.collection?.name?.let {
          "${base}: ${it}"
        } ?: base
        Text(
          text = text,
          fontSize = 18.sp
        )
      },
      elevation = 1.dp,
      navigationIcon = {
        IconButton(onClick = { onBack() }) {
          Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null)
        }
      },
      backgroundColor = MaterialTheme.colors.surface
    )

    state.puzzles?.let { puzzles ->
      val gridState = rememberLazyGridState()
      LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Adaptive(minSize = 136.dp),
        modifier = Modifier.fillMaxHeight()
      ) {
        items(items = puzzles) {
          val solved = !state.solutions[it.id].isNullOrEmpty()
          Surface(
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Column(modifier = Modifier
              .clickable { onPuzzle(it) }
              .padding(horizontal = 10.dp, vertical = 10.dp)) {
              Row {
                it.puzzle.let {
                  val pos = RulesManager.buildPos(
                    moves = emptyList(),
                    boardWidth = it.width,
                    boardHeight = it.height,
                    whiteInitialState = it.initial_state.white.toCoordinateSet(),
                    blackInitialState = it.initial_state.black.toCoordinateSet()
                  )
                  Board(
                    boardWidth = it.width,
                    boardHeight = it.height,
                    position = pos,
                    boardTheme = state.boardTheme,
                    drawCoordinates = false,
                    interactive = false,
                    drawShadow = false,
                    fadeInLastMove = false,
                    fadeOutRemovedStones = false,
                    modifier = Modifier
                      .weight(1f)
                      .fillMaxWidth()
                      .clip(MaterialTheme.shapes.small)
                  )
                }
              }
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = it.name,
                style = TextStyle.Default.copy(
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Bold
                )
              )
              if (solved) {
                Image(
                  painter = painterResource(R.drawable.ic_check_circle),
                  modifier = Modifier
                    .padding(start = 18.dp)
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally),
                  contentDescription = "Solved"
                )
              }
            }
          }
          LaunchedEffect(true) {
            fetchSolutions(it.id)
          }
        }

        item {
          Spacer(modifier = Modifier.height(8.dp))
        }
      }
    } ?: run {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 8.dp, vertical = 4.dp),
      ) {
        Text(
          text = "Loading...",
          color = MaterialTheme.colors.onBackground,
        )
      }
    }
  }
}
