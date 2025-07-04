package io.zenandroid.onlinego.ui.screens.puzzle.directory

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Beenhere
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.model.local.PuzzleCollection
import io.zenandroid.onlinego.gamelogic.RulesManager
import io.zenandroid.onlinego.gamelogic.Util.toCoordinateSet
import io.zenandroid.onlinego.ui.composables.Board
import io.zenandroid.onlinego.ui.composables.FilterSortPanel
import io.zenandroid.onlinego.ui.composables.RatingBar
import io.zenandroid.onlinego.ui.composables.SearchTextField
import io.zenandroid.onlinego.ui.composables.SortChip
import io.zenandroid.onlinego.ui.screens.mygames.composables.SenteCard
import io.zenandroid.onlinego.utils.convertCountryCodeToEmojiFlag
import io.zenandroid.onlinego.utils.rememberStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@Composable
fun PuzzleDirectoryScreen(
  viewModel: PuzzleDirectoryViewModel = koinViewModel(),
  onNavigateBack: () -> Unit,
  onNavigateToPuzzle: (Long, Long) -> Unit,
) {
  val state by rememberStateWithLifecycle(viewModel.state)

  state.navigateToPuzzle?.let {
    onNavigateToPuzzle(it.first, it.second)
    viewModel.onPuzzleNavigated()
  }

  PuzzleDirectoryContent(
    state = state,
    onCollection = viewModel::onPuzzleCollectionClick,
    onBack = onNavigateBack,
    onSortChanged = viewModel::onSortChanged,
    onFilterChanged = viewModel::onFilterChanged,
    onToggleOnlyOpened = viewModel::onToggleOnlyOpened,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PuzzleDirectoryContent(
  state: PuzzleDirectoryState,
  onCollection: (PuzzleCollection) -> Unit,
  onBack: () -> Unit,
  onSortChanged: (PuzzleDirectorySort) -> Unit,
  onFilterChanged: (String) -> Unit,
  onToggleOnlyOpened: () -> Unit,
) {
  val listState = rememberLazyListState()

  Column {
    TopAppBar(
      title = {
        Text(text = "Puzzles", fontSize = 18.sp)
      },
      navigationIcon = {
        IconButton(onClick = { onBack() }) {
          Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
        }
      },
    )

    LazyColumn(state = listState, modifier = Modifier.fillMaxHeight()) {
      item("header") {
        var filterExpanded by remember { mutableStateOf(false) }

        FilterSortPanel(
          filterIcon = {
            IconButton(onClick = { filterExpanded = true }) {
              Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
              )
            }
          },
          filterTextField = {
            Surface {
              SearchTextField(
                value = state.filterString ?: "",
                onValueChange = { onFilterChanged(it) },
                hint = "Search",
                modifier = Modifier
                  .padding(horizontal = 8.dp)
                  .fillMaxWidth(),
                onCleared = {
                  onFilterChanged("")
                  filterExpanded = false
                },
              )
            }
          },
          filterExpanded = filterExpanded,
          modifier = Modifier.padding(vertical = 8.dp),
        ) {
          FilterChip(
            selected = state.onlyOpenend,
            leadingIcon = {
              AnimatedVisibility(visible = state.onlyOpenend) {
                Icon(
                  imageVector = Icons.Default.Done,
                  contentDescription = null,
                )
              }
            },
            onClick = onToggleOnlyOpened,
            label = {
              Text(text = "Recently opened")
            },
          )

          Spacer(modifier = Modifier.weight(1f))

          SortChip(
            sortOptions = state.availableSorts,
            currentSortOption = state.currentSort,
            onSortSelected = { onSortChanged(it as PuzzleDirectorySort) },
          )
        }
      }

      items(items = state.collections, key = { it.id }) {
        SenteCard (
          modifier = Modifier
            .height(150.dp)
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Row(modifier = Modifier.clickable { onCollection(it) }) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
              it.starting_puzzle.let {
                val pos = remember {
                  RulesManager.buildPos(
                    moves = emptyList(),
                    boardWidth = it.width,
                    boardHeight = it.height,
                    whiteInitialState = it.initial_state.white.toCoordinateSet(),
                    blackInitialState = it.initial_state.black.toCoordinateSet()
                  )
                }
                Board(
                  boardWidth = it.width,
                  boardHeight = it.height,
                  position = pos,
                  drawCoordinates = false,
                  interactive = false,
                  drawShadow = false,
                  fadeInLastMove = false,
                  fadeOutRemovedStones = false,
                  modifier = Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.small)
                )
              }
              Spacer(modifier = Modifier.height(10.dp))
              Row(
                modifier = Modifier
                  .height(16.dp)
                  .align(Alignment.CenterHorizontally)
              ) {
                RatingBar(rating = it.rating, modifier = Modifier.align(Alignment.CenterVertically))
                Spacer(modifier = Modifier.width(2.dp))
                val ratingCount = when {
                  it.rating_count < 1000 -> "${it.rating_count}"
                  else -> "${it.rating_count / 1000}k"
                }
                Text(
                  text = "($ratingCount)",
                  color = MaterialTheme.colorScheme.onBackground,
                  fontSize = 12.sp,
                  modifier = Modifier.align(Alignment.CenterVertically)
                )
              }
            }
            Column(
              modifier = Modifier
                .weight(1f)
                .padding(bottom = 8.dp, end = 4.dp)
            ) {
              Row {
                Column(modifier = Modifier.padding(top = 8.dp, end = 8.dp)) {
                  Text(
                    text = it.name,
                    style = TextStyle.Default.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold)
                  )
                  it.owner?.let {
                    val flag = convertCountryCodeToEmojiFlag(it.country)
                    Text(
                      text = "by ${it.username} $flag",
                      maxLines = 1,
                      style = TextStyle.Default.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Light
                      )
                    )
                  }
                }
                Spacer(modifier = Modifier.weight(1f))
                if (it.id in state.recents) {
                  Box(modifier = Modifier.width(8.dp)) {
                    Icon(imageVector = Icons.Filled.Beenhere, contentDescription = null)
                  }
                }
              }
              Spacer(modifier = Modifier.weight(1f))
              Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                  .padding(top = 8.dp, end = 8.dp)
                  .fillMaxWidth(),
              ) {
                Column(modifier = Modifier) {
                  Text(text = "Count", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                  Text(
                    text = "${it.puzzle_count} puzzle(s)",
                    maxLines = 1,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                  )
                }
                Column(modifier = Modifier) {
                  fun rankToString(rank: Int) = when {
                    rank < 30 -> "${30 - rank}k"
                    else -> "${rank - 29}d"
                  }
                  Text(text = "Rank", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                  Text(
                    text = if (it.min_rank == it.max_rank) {
                      rankToString(it.min_rank)
                    } else {
                      "${rankToString(it.min_rank)} to ${rankToString(it.max_rank)}"
                    }, maxLines = 1, fontSize = 11.sp, fontWeight = FontWeight.Medium
                  )
                }
                Column(modifier = Modifier) {
                  val solutions = state.solutions[it.id] ?: 0
                  val percentage = (solutions * 100f) / it.puzzle_count
                  Text(text = "Solved", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                  Row {
                    Text(
                      text = "$solutions (${
                        "%.1f".format(percentage)
                      }%)", maxLines = 1, fontSize = 11.sp, fontWeight = FontWeight.Medium
                    )
                    if (percentage >= 100f) {
                      Image(
                        painter = painterResource(R.drawable.ic_check_circle),
                        contentDescription = "Completed"
                      )
                    }
                  }
                }
              }
              Spacer(modifier = Modifier.weight(1f))
              Row(modifier = Modifier.align(Alignment.End)) {
                val solveRate = "%.2f".format((it.solved_count * 100f) / it.attempt_count)
                val viewCount = when {
                  it.view_count < 1_000 -> it.view_count.toString()
                  it.view_count < 1_000_000 -> (it.view_count / 1000).toString() + "k"
                  else -> (it.view_count / 1_000_000).toString() + "M"
                }
                val solvedCount = when {
                  it.solved_count < 1_000 -> it.solved_count.toString()
                  it.solved_count < 1_000_000 -> (it.solved_count / 1000).toString() + "k"
                  else -> (it.solved_count / 1_000_000).toString() + "M"
                }
                val attemptCount = when {
                  it.attempt_count < 1_000 -> it.attempt_count.toString()
                  it.attempt_count < 1_000_000 -> (it.attempt_count / 1000).toString() + "k"
                  else -> (it.attempt_count / 1_000_000).toString() + "M"
                }
                Text(
                  text = "$viewCount views, solved $solvedCount times of $attemptCount ($solveRate %)",
                  maxLines = 1,
                  fontSize = 11.sp,
                  fontStyle = FontStyle.Italic,
                  fontWeight = FontWeight.Light,
                  color = MaterialTheme.colorScheme.onBackground,
                )
              }
            }
          }
        }
      }
      if (state.collections.isEmpty()) {
        item("placeholder") {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .fillParentMaxSize()
              .padding(horizontal = 8.dp, vertical = 4.dp),
          ) {
            Text(
              text = "Loading...",
              color = MaterialTheme.colorScheme.onBackground,
            )
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(8.dp))
      }
    }
  }
}
