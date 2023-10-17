package io.zenandroid.onlinego.ui.screens.puzzle

import android.text.format.DateUtils.getRelativeTimeSpanString
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.pager.*
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.model.local.PuzzleCollection
import io.zenandroid.onlinego.data.model.local.VisitedPuzzleCollection
import io.zenandroid.onlinego.data.repositories.SettingsRepository
import io.zenandroid.onlinego.gamelogic.RulesManager
import io.zenandroid.onlinego.gamelogic.Util.toCoordinateSet
import io.zenandroid.onlinego.ui.composables.Board
import io.zenandroid.onlinego.ui.composables.RatingBar
import io.zenandroid.onlinego.ui.screens.puzzle.PuzzleDirectoryAction.*
import io.zenandroid.onlinego.ui.screens.puzzle.PuzzleDirectorySort.*
import io.zenandroid.onlinego.utils.convertCountryCodeToEmojiFlag
import org.commonmark.node.*
import org.koin.core.context.GlobalContext
import java.time.Instant.now
import java.time.temporal.ChronoUnit.*

@Composable
fun PuzzleDirectoryScreen(
    state: PuzzleDirectoryState,
    filterText: String,
    sortField: PuzzleDirectorySort,
    onCollection: (PuzzleCollection) -> Unit,
    onBack: () -> Unit,
    onSortChanged: (PuzzleDirectorySort) -> Unit,
    onFilterChanged: (String) -> Unit,
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
            elevation = 1.dp,
            navigationIcon = {
                IconButton(onClick = { onBack() }) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null)
                }
            },
            backgroundColor = MaterialTheme.colors.surface,
        )

        LazyColumn (
            state = listState,
            modifier = Modifier.fillMaxHeight()
        ) {
            stickyHeader {
                Box(modifier = Modifier.background(MaterialTheme.colors.background)) {
                    Column(modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 0.dp).background(MaterialTheme.colors.surface)) {
                        TextField(
                            value = filterText,
                            onValueChange = { onFilterChanged(it) },
                            placeholder = { Text(text = "Search") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.None,
                                autoCorrect = true,
                                keyboardType = KeyboardType.Text,
                            ),
                            textStyle = TextStyle(color = MaterialTheme.colors.onSurface,
                                fontSize = 15.sp,
                                fontFamily = FontFamily.SansSerif),
                            maxLines = 2,
                            singleLine = true,
                            leadingIcon = {
                                Icon(imageVector = Icons.Filled.Search,
                                     tint = MaterialTheme.colors.primary,
                                     contentDescription = null)
                            },
                            trailingIcon = {
                                IconButton(onClick = { onFilterChanged("") }) {
                                    Icon(imageVector = Icons.Filled.Cancel,
                                         tint = MaterialTheme.colors.primary,
                                         contentDescription = null)
                                }
                            },
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        ) {
                            Text(
                                text = "Sort:",
                                color = MaterialTheme.colors.onBackground,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .align(Alignment.CenterVertically)
                            )
                            mapOf(
                                "Name" to NameSort(!sortField.asc),
                                "Rating" to RatingSort(!sortField.asc),
                                "Count" to CountSort(!sortField.asc),
                                "Views" to ViewsSort(!sortField.asc),
                                "Rank" to RankSort(!sortField.asc),
                            ).forEach {
                                val name = it.key
                                Spacer(modifier = Modifier.weight(1f))
                                TextButton(onClick = {
                                    onSortChanged(it.value)
                                }) {
                                    val currentSort = it.value::class == sortField::class
                                    Text(
                                        text = name,
                                        color = if(currentSort) MaterialTheme.colors.secondary
                                                else MaterialTheme.colors.primary,
                                        fontSize = 10.sp)
                                    Icon(
                                        imageVector = if(currentSort && sortField.desc) Icons.Filled.ExpandMore
                                                      else Icons.Filled.ExpandLess,
                                        tint = if(currentSort) MaterialTheme.colors.secondary
                                               else MaterialTheme.colors.primary,
                                        modifier = Modifier
                                            .align(Alignment.CenterVertically),
                                        contentDescription = null)
                                }
                            }
                        }
                    }
                }
            }
            item {
                Box(modifier = Modifier.background(MaterialTheme.colors.background)) {
                    Column(modifier = Modifier.padding(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 16.dp).background(MaterialTheme.colors.surface)) {
                        if (filterText.isEmpty()) {
                            LazyRow(modifier = Modifier.fillMaxWidth()) {
                                state.recentsPages.ifEmpty { null }?.let { chunk ->
                                    items(items = chunk) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            it.forEach {
                                                val ts = it.timestamp
                                                state.collections.get(it.collectionId)?.let {
                                                    Surface(
                                                        shape = MaterialTheme.shapes.medium,
                                                        modifier = Modifier
                                                            .height(50.dp)
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 12.dp, vertical = 4.dp)
                                                            .clickable { onCollection(it) }
                                                    ) {
                                                        Row(modifier = Modifier.fillMaxWidth()) {
                                                            Column(modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .padding(horizontal = 10.dp, vertical = 10.dp)) {
                                                                it.starting_puzzle.let {
                                                                    val pos = RulesManager.buildPos(moves = emptyList(),
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
                                                            }
                                                            Column {
                                                                Column(modifier = Modifier.padding(8.dp)) {
                                                                    Text(
                                                                        text = it.name,
                                                                        style = TextStyle.Default.copy(
                                                                            fontSize = 12.sp,
                                                                            fontWeight = FontWeight.Bold
                                                                        )
                                                                    )
                                                                    it.owner?.let {
                                                                        val flag = convertCountryCodeToEmojiFlag(it.country)
                                                                        val ago = getRelativeTimeSpanString((ts ?: now()).toEpochMilli())
                                                                        Text(
                                                                            text = "by ${it.username} $flag - visited $ago",
                                                                            maxLines = 1,
                                                                            style = TextStyle.Default.copy(
                                                                                fontSize = 9.sp,
                                                                                fontWeight = FontWeight.Light
                                                                            )
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                } ?: Surface(
                                                    shape = MaterialTheme.shapes.medium,
                                                    modifier = Modifier.height(50.dp)
                                                ) {}
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            state.collections.map { it.value }.ifEmpty { null }?.let { collections ->
                item {
                    var compositionCount by remember { mutableStateOf(0) }
                    SideEffect { compositionCount++ }
                    android.util.Log.d("PDUI", "Composition: $compositionCount")
                }
                items(items = collections) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .height(150.dp)
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(modifier = Modifier
                                .clickable { onCollection(it) }) {
                            Column(modifier = Modifier
                                    .padding(horizontal = 10.dp, vertical = 10.dp)) {
                                it.starting_puzzle.let {
                                    val pos = RulesManager.buildPos(moves = emptyList(),
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
                                Row(modifier = Modifier.height(16.dp)
                                        .align(Alignment.CenterHorizontally)) {
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
                                        color = MaterialTheme.colors.onBackground,
                                        fontSize = 12.sp,
                                        modifier = Modifier
                                            .align(Alignment.CenterVertically)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)
                                    .padding(bottom = 8.dp, end = 4.dp)) {
                                Row {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = it.name,
                                            style = TextStyle.Default.copy(
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        val private = if(it.private) "(private)" else ""
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
                                                if(it.min_rank == it.max_rank)
                                                    "${rankToString(it.min_rank)}"
                                                else
                                                    "${rankToString(it.min_rank)} to ${rankToString(it.max_rank)}",
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
                                                text = "${solutions} (${
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
                                Row(modifier = Modifier
                                        .align(Alignment.End)
                                    ) {
                                    val solveRate = (it.solved_count*100f) / it.attempt_count
                                    Text(
                                        text = "${it.view_count} views, solved ${it.solved_count} times of ${it.attempt_count} (${"%.2f".format(solveRate)}%)",
                                        maxLines = 1,
                                        fontSize = 11.sp,
                                        fontStyle = FontStyle.Italic,
                                        fontWeight = FontWeight.Light,
                                        color = MaterialTheme.colors.onBackground,
                                    )
                                }
                            }
                        }
                    }
                }
            } ?: run {
                item {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillParentMaxSize()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "Loading...",
                            color = MaterialTheme.colors.onBackground,
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
