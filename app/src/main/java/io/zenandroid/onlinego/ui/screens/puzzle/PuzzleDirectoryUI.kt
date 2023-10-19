package io.zenandroid.onlinego.ui.screens.puzzle

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.material3.TopAppBarDefaults
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
import io.zenandroid.onlinego.utils.convertCountryCodeToEmojiFlag
import java.time.Instant.now
import java.time.temporal.ChronoUnit.DAYS


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PuzzleDirectoryScreen(
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
        Text(
          text = "Puzzles",
          fontSize = 18.sp
        )
      },
      navigationIcon = {
        IconButton(onClick = { onBack() }) {
          Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null)
        }
      },
      colors = TopAppBarDefaults.topAppBarColors(
        containerColor = androidx.compose.material.MaterialTheme.colors.surface,
        titleContentColor = androidx.compose.material.MaterialTheme.colors.onSurface,
        actionIconContentColor = androidx.compose.material.MaterialTheme.colors.onSurface,
        navigationIconContentColor = androidx.compose.material.MaterialTheme.colors.onSurface,
      ),
    )

    LazyColumn(
      state = listState,
      modifier = Modifier.fillMaxHeight()
    ) {
      item("header") {
        var filterExpanded by remember { mutableStateOf(false) }

        FilterSortPanel(
          filterIcon = {
            IconButton(onClick = { filterExpanded = true }) {
              Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = androidx.compose.material.MaterialTheme.colors.onSurface,
              )
            }
          },
          filterTextField = {
            SearchTextField(
              value = state.filterString ?: "",
              onValueChange = { onFilterChanged(it) },
              hint = "Search",
              modifier = Modifier.padding(horizontal = 8.dp).fillMaxWidth(),
              onCleared = {
                onFilterChanged("")
                filterExpanded = false
              },
            )
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

      items(
        items = state.collections,
        key = { it.id }
      ) {
        Surface(
          shape = MaterialTheme.shapes.medium,
          color = androidx.compose.material.MaterialTheme.colors.surface,
          contentColor = androidx.compose.material.MaterialTheme.colors.onSurface,
          modifier = Modifier
            .height(150.dp)
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Row(modifier = Modifier
            .clickable { onCollection(it) }) {
            Column(
              modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 10.dp)
            ) {
              it.starting_puzzle.let {
                val pos = RulesManager.buildPos(
                  moves = emptyList(),
                  boardWidth = it.width, boardHeight = it.height,
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
                    .clip(MaterialTheme.shapes.small)
                )
              }
              Spacer(modifier = Modifier.height(10.dp))
              Row(
                modifier = Modifier
                  .height(16.dp)
                  .align(Alignment.CenterHorizontally)
              ) {
                RatingBar(
                  rating = it.rating,
                  modifier = Modifier
                    .align(Alignment.CenterVertically)
                )
                Spacer(modifier = Modifier.width(2.dp))
                val rating_count = when {
                  it.rating_count < 1000 -> "${it.rating_count}"
                  else -> "${it.rating_count / 1000}k"
                }
                Text(
                  text = "($rating_count)",
                  color = MaterialTheme.colorScheme.onBackground,
                  fontSize = 12.sp,
                  modifier = Modifier
                    .align(Alignment.CenterVertically)
                )
              }
            }
            Column(
              modifier = Modifier
                .weight(1f)
                .padding(bottom = 8.dp, end = 4.dp)
            ) {
              Row {
                Column(modifier = Modifier.padding(8.dp)) {
                  Text(
                    text = it.name,
                    style = TextStyle.Default.copy(
                      fontSize = 16.sp,
                      fontWeight = FontWeight.Bold
                    )
                  )
                  val private = if (it.private) "(private)" else ""
                  val ago = "created ${DAYS.between(it.created, now())} days ago"
                  it.owner?.let {
                    val flag = convertCountryCodeToEmojiFlag(it.country)
                    Text(
                      text = "by ${it.username} $flag $private - $ago",
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
              Row {
                Spacer(modifier = Modifier.width(24.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "Count",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                  )
                  Text(
                    text = "${it.puzzle_count} puzzle(s)",
                    maxLines = 1,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                  )
                }
                Column(modifier = Modifier.weight(1f)) {
                  fun rankToString(rank: Int) = when {
                    rank < 30 -> "${30 - rank}k"
                    else -> "${rank - 29}d"
                  }
                  Text(
                    text = "Rank",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                  )
                  Text(
                    text =
                    if (it.min_rank == it.max_rank) {
                      rankToString(it.min_rank)
                    } else {
                      "${rankToString(it.min_rank)} to ${rankToString(it.max_rank)}"
                    },
                    maxLines = 1,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                  )
                }
                Column(modifier = Modifier.weight(1f)) {
                  val solutions = state.solutions.get(it.id) ?: 0
                  val percentage = (solutions * 100f) / it.puzzle_count
                  Text(
                    text = "Solved",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                  )
                  Row {
                    Text(
                      text = "$solutions (${
                        "%.1f".format(
                          percentage
                        )
                      }%)",
                      maxLines = 1,
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Medium
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
              Row(
                modifier = Modifier
                  .align(Alignment.End)
              ) {
                val solveRate = (it.solved_count * 100f) / it.attempt_count
                Text(
                  text = "${it.view_count} views, solved ${it.solved_count} times of ${it.attempt_count} (${
                    "%.2f".format(
                      solveRate
                    )
                  }%)",
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
        item ("placeholder") {
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
