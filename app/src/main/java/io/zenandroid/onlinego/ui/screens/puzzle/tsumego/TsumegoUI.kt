@file:OptIn(ExperimentalMaterial3Api::class)

package io.zenandroid.onlinego.ui.screens.puzzle.tsumego

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.data.model.local.InitialState
import io.zenandroid.onlinego.data.model.local.Puzzle
import io.zenandroid.onlinego.data.model.ogs.MoveTree
import io.zenandroid.onlinego.data.model.ogs.OGSPuzzle
import io.zenandroid.onlinego.data.model.ogs.PuzzleSolution
import io.zenandroid.onlinego.ui.composables.Board
import io.zenandroid.onlinego.ui.composables.ExposedLazyDropdownMenu
import io.zenandroid.onlinego.ui.composables.RatingBar
import kotlinx.collections.immutable.toImmutableList
import org.koin.androidx.compose.koinViewModel
import java.util.Stack

@Composable
fun TsumegoScreen(
  viewModel: TsumegoViewModel = koinViewModel(),
  onNavigateBack: () -> Unit,
) {
  val state by viewModel.state.collectAsStateWithLifecycle()

  TsumegoContent(
    state = state,
    hasPreviousPuzzle = viewModel.hasPreviousPuzzle,
    hasNextPuzzle = viewModel.hasNextPuzzle,
    collection = viewModel.collectionContents,
    positions = viewModel.collectionPositions,
    ratings = viewModel.collectionRatings,
    renderCollectionPuzzle = viewModel::renderCollectionPuzzle,
    onMove = viewModel::makeMove,
    onHint = viewModel::addBoardHints,
    onResetPuzzle = viewModel::resetPuzzle,
    onPreviousPuzzle = viewModel::previousPuzzle,
    onSelectPuzzle = viewModel::selectPuzzle,
    onNextPuzzle = viewModel::nextPuzzle,
    onBack = onNavigateBack,
  )

}

@ExperimentalMaterial3Api
@Composable
private fun TsumegoContent(
  state: TsumegoState,
  hasPreviousPuzzle: Boolean,
  hasNextPuzzle: Boolean,
  collection: List<Puzzle>,
  positions: Map<Long, Position>,
  ratings: Map<Long, Float?>,
  renderCollectionPuzzle: (Int) -> Unit,
  onMove: (Cell) -> Unit,
  onHint: () -> Unit,
  onResetPuzzle: () -> Unit,
  onPreviousPuzzle: () -> Unit,
  onSelectPuzzle: (Int) -> Unit,
  onNextPuzzle: () -> Unit,
  onBack: () -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxHeight()
  ) {
    TopAppBar(
      title = {
        var expanded by remember { mutableStateOf(false) }
        val selectedPuzzleIndex = collection.indexOfFirst {
          it.id == state.puzzle?.id
        }.takeUnless { it == -1 }
        val listState = rememberLazyListState(
          initialFirstVisibleItemIndex = selectedPuzzleIndex ?: 0
        )
        ExposedDropdownMenuBox(
          expanded = expanded,
          onExpandedChange = { expanded = it },
          modifier = Modifier.weight(1f)
        ) {
          TextField(
            readOnly = true,
            value = "Tsumego".let { base ->
              state.puzzle?.name?.let {
                "$base: $it"
              } ?: base
            },
            onValueChange = { },
            trailingIcon = {
              ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            textStyle = LocalTextStyle.current.copy(fontSize = 18.sp),
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true)
          )
          LaunchedEffect(expanded) {
            if (expanded) {
              selectedPuzzleIndex?.let { listState.animateScrollToItem(it) }
            }
          }
          val infiniteTransition = rememberInfiniteTransition()
          ExposedLazyDropdownMenu<Puzzle>(
            expanded = expanded,
            items = collection,
            onDismissRequest = {
              expanded = false
            },
            scrollState = listState,
            verticalArrangement = Arrangement.spacedBy(2.dp),
          ) { i, puzzle ->
            val position = positions[puzzle.id]
            val rating = ratings[puzzle.id]
            LaunchedEffect(position) {
              if (position == null) {
                renderCollectionPuzzle(i)
              }
            }

            DropdownMenuItem(
              onClick = {
                onSelectPuzzle(i)
                expanded = false
              },
              text =
                {
                  val selected = puzzle.id == state.puzzle?.id

                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                  ) {
                    val tint by infiniteTransition.animateColor(
                      initialValue = LocalContentColor.current.copy(
                        alpha = 0.1f
                      ),
                      targetValue = LocalContentColor.current.copy(
                        alpha = 0.5f
                      ),
                      animationSpec = infiniteRepeatable(
                        animation = tween(
                          durationMillis = 1000,
                          easing = LinearEasing,
                        ),
                        repeatMode = RepeatMode.Reverse
                      ),
                      label = "animateColor"
                    )
                    if (position == null)
                      Icon(
                        painter = painterResource(R.drawable.ic_go_board),
                        contentDescription = "Board",
                        tint = tint,
                        modifier = Modifier
                          .height(64.dp)
                          .width(64.dp)
                          .padding(end = 20.dp)
                          .clip(MaterialTheme.shapes.small),
                      )
                    else
                      Board(
                        boardWidth = puzzle.puzzle.width,
                        boardHeight = puzzle.puzzle.height,
                        position = position,
                        drawCoordinates = false,
                        interactive = false,
                        drawShadow = false,
                        fadeInLastMove = false,
                        fadeOutRemovedStones = false,
                        modifier = Modifier
                          .height(64.dp)
                          .width(64.dp)
                          .padding(end = 10.dp)
                          .clip(MaterialTheme.shapes.small)
                      )

                    Column(
                      modifier = Modifier
                        .fillMaxHeight()
                    ) {
                      Text(
                        text = puzzle.name,
                        fontSize = 18.sp,
                        fontWeight = if (selected) FontWeight.Bold
                        else FontWeight.Normal,
                        maxLines = 1,
                        modifier = Modifier
                          .padding(top = 10.dp),
                      )
                      rating?.let {
                        RatingBar(
                          rating = it,
                          modifier = Modifier
                            .height(16.dp)
                        )
                      } ?: Spacer(modifier = Modifier.height(16.dp))
                    }
                  }
                }
            )
          }
          if (state.solutions.isNotEmpty()) {
            Image(
              painter = painterResource(R.drawable.ic_check_circle),
              modifier = Modifier
                .padding(horizontal = 8.dp),
              contentDescription = "Solved"
            )
          }
        }
      },
      navigationIcon = {
        IconButton(onClick = { onBack() }) {
          Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
        }
      },
    )

    state.puzzle?.let {
      Surface(
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxSize()
      ) {
        Column(
          modifier = Modifier
            .padding(horizontal = 10.dp, vertical = 10.dp)
        ) {
          it.puzzle.let {
            Board(
              boardWidth = it.width,
              boardHeight = it.height,
              position = state.boardPosition,
              interactive = state.boardInteractive,
              drawShadow = true,
              fadeInLastMove = false,
              fadeOutRemovedStones = false,
              removedStones = state.removedStones?.map { it.toPair() }?.toImmutableList(),
              candidateMove = state.hoveredCell,
              candidateMoveType = StoneType.BLACK,
              onTapUp = { if (state.boardInteractive) onMove(it) },
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .shadow(6.dp, MaterialTheme.shapes.large)
                .clip(MaterialTheme.shapes.small)
            )
            Row {
              Row(modifier = Modifier.weight(1f)) {
                if (hasPreviousPuzzle) {
                  TextButton(
                    onClick = { onPreviousPuzzle() },
                    modifier = Modifier
                      .align(Alignment.CenterVertically)
                      .padding(all = 4.dp)
                      .weight(1f)
                  ) {
                    Image(
                      painter = painterResource(R.drawable.ic_navigate_previous),
                      modifier = Modifier
                        .align(Alignment.CenterVertically),
                      contentDescription = null
                    )
                    Text(
                      "PREVIOUS",
                      color = MaterialTheme.colorScheme.secondary,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.padding(start = 8.dp)
                    )
                  }
                }
              }

              TextButton(
                onClick = { onHint() },
                modifier = Modifier
                  .align(Alignment.CenterVertically)
                  .padding(all = 4.dp)
              ) {
                Text(
                  "HINT",
                  color = state.nodeStack.lastOrNull()
                    ?.let { MaterialTheme.colorScheme.secondary }
                    ?: MaterialTheme.colorScheme.onBackground,
                  fontWeight = FontWeight.Bold)
              }

              Row(modifier = Modifier.weight(1f)) {
                if (hasNextPuzzle) {
                  TextButton(
                    onClick = { onNextPuzzle() },
                    modifier = Modifier
                      .align(Alignment.CenterVertically)
                      .padding(all = 4.dp)
                      .weight(1f)
                  ) {
                    Text(
                      "NEXT",
                      color = MaterialTheme.colorScheme.secondary,
                      fontWeight = FontWeight.Bold
                    )
                    Image(
                      painter = painterResource(R.drawable.ic_navigate_next),
                      modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(start = 8.dp),
                      contentDescription = null
                    )
                  }
                }
              }
            }
            val boxState = rememberScrollState()
            Box(
              modifier = Modifier
                .verticalScroll(state = boxState)
                .weight(1f)
            ) {
              Text(
                text = state.nodeStack.let { stack ->
                  stack.lastOrNull()?.text
                    ?: stack.dropLast(1).lastOrNull()?.text
                } ?: state.description,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
              )
            }

            Column {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 12.dp, vertical = 16.dp)
              ) {
                if (state.retryButtonVisible) {
                  OutlinedButton(
                    onClick = { onResetPuzzle() },
                    modifier = Modifier.weight(1f)
                  ) {
                    Icon(
                      imageVector = Icons.Filled.Refresh,
                      tint = MaterialTheme.colorScheme.onSurface,
                      modifier = Modifier.size(16.dp),
                      contentDescription = null
                    )
                    Text(
                      text = "RETRY",
                      color = MaterialTheme.colorScheme.onSurface,
                      modifier = Modifier.padding(start = 8.dp)
                    )
                  }
                }
                if (state.retryButtonVisible && state.continueButtonVisible) {
                  Spacer(modifier = Modifier.width(16.dp))
                }
                if (state.continueButtonVisible) {
                  if (hasNextPuzzle) {
                    Button(
                      onClick = { onNextPuzzle() },
                      modifier = Modifier.weight(1f)
                    ) {
                      Text(text = "CONTINUE")
                    }
                  } else {
                    Button(
                      onClick = { onBack() },
                      modifier = Modifier.weight(1f)
                    ) {
                      Text(text = "DONE")
                    }
                  }
                }
              }
            }
          }
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
          color = MaterialTheme.colorScheme.onBackground,
        )
      }
    }
  }
}

@Composable
@Preview
fun TsumegoScreenPreview() {
  TsumegoContent(
    state = TsumegoState(
      puzzle = Puzzle(
        id = 1,
        name = "Test",
        puzzle = OGSPuzzle.PuzzleData(
          puzzle_rank = "",
          name = "",
          move_tree = MoveTree(),
          initial_player = "",
          height = 9,
          width = 9,
          mode = "",
          puzzle_collection = "",
          puzzle_type = "",
          initial_state = InitialState(),
          puzzle_description = "Lorem ipsum dolor sit amet consectetur adipiscing elit. ",
        ),
        order = 1,
        owner = null,
        created = null,
        modified = null,
        private = false,
        width = 9,
        height = 9,
        type = null,
        has_solution = false,
        rating = 0f,
        rating_count = 0,
        rank = 0,
        collection = null,
        view_count = 0,
        solved_count = 0,
        attempt_count = 0,
      ),
      boardPosition = Position(
        boardWidth = 9,
        boardHeight = 9,
        whiteStones = setOf(
          Cell(1, 1),
          Cell(3, 3),
          Cell(5, 5),
          Cell(7, 7),
        ),
        blackStones = setOf(
          Cell(0, 0),
          Cell(2, 2),
          Cell(4, 4),
          Cell(6, 6),
          Cell(8, 8),
        ),
      ),
      boardInteractive = true,
      removedStones = null,
      hoveredCell = null,
      nodeStack = Stack(),
      description = "Test description",
      retryButtonVisible = true,
      continueButtonVisible = true,
      rating = null,
      solutions = listOf(PuzzleSolution(1, 1, 1, 1, 1, true, true)),
    ),
    hasPreviousPuzzle = true,
    hasNextPuzzle = true,
    collection = emptyList(),
    positions = emptyMap(),
    ratings = emptyMap(),
    renderCollectionPuzzle = {},
    onMove = {},
    onHint = {},
    onResetPuzzle = {},
    onPreviousPuzzle = {},
    onSelectPuzzle = {},
    onNextPuzzle = {},
    onBack = {},
  )
}
