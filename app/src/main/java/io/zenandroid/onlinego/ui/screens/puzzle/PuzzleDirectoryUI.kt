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
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.safeCast

@Composable
fun PuzzleDirectoryScreen(
    state: PuzzleDirectoryState,
    onCollection: (PuzzleCollection) -> Unit,
    onBack: () -> Unit,
) {
    val listState = rememberLazyListState()
    var filterText by remember { mutableStateOf(TextFieldValue()) }
    var sortField by remember { mutableStateOf<PuzzleDirectorySort>(RatingSort(false)) }
    val resultCollections = filterText.text.lowercase().let { query ->
        state.collections.map { it.value }
            .filter { it.name.lowercase().contains(query)
                    || it.owner?.username?.lowercase()?.contains(query) == true }
            .sortedWith(sortField.compare)
    }

    LazyColumn (
        state = listState,
        modifier = Modifier.fillMaxHeight()
    ) {
        stickyHeader {
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
                backgroundColor = MaterialTheme.colors.surface
            )
            Box(modifier = Modifier.background(MaterialTheme.colors.background)) {
                Column(modifier = Modifier.padding(all = 16.dp).background(MaterialTheme.colors.surface)) {
                    TextField(
                        value = filterText,
                        onValueChange = { filterText = it },
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
                            IconButton(onClick = { filterText = TextFieldValue() }) {
                                Icon(imageVector = Icons.Filled.Cancel,
                                     tint = MaterialTheme.colors.primary,
                                     contentDescription = null)
                            }
                        },
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    ) {
                        @Composable
                        fun <T : PuzzleDirectorySort> ratingButton(type: KClass<T>, name: String) {
                            val sorting = type.safeCast(sortField)
                            Spacer(modifier = Modifier.weight(1f))
                            TextButton(onClick = {
                                sortField = sorting?.reversed ?: type.createInstance()
                            }) {
                                Text(
                                    text = name,
                                    color = if(sorting == null) MaterialTheme.colors.secondary
                                            else MaterialTheme.colors.primary,
                                    fontSize = 12.sp)
                                Icon(
                                    imageVector = if(sorting?.asc == false) Icons.Filled.ExpandMore
                                                  else Icons.Filled.ExpandLess,
                                    tint = if(sorting == null) MaterialTheme.colors.secondary
                                           else MaterialTheme.colors.primary,
                                    modifier = Modifier
                                       .align(Alignment.CenterVertically),
                                    contentDescription = null)
                            }
                        }
                        Text(
                            text = "Sort:",
                            color = MaterialTheme.colors.onBackground,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                        )
                        ratingButton(name = "Name", type = NameSort::class)
                        ratingButton(name = "Rating", type = RatingSort::class)
                        ratingButton(name = "Count", type = CountSort::class)
                        ratingButton(name = "Views", type = ViewsSort::class)
                        ratingButton(name = "Rank", type = RankSort::class)
                    }

                    if (filterText.text.isEmpty()) {
                        LazyRow(modifier = Modifier.fillMaxWidth()) {
                            state.recents.map { it.value }.distinctBy { it.collectionId }.chunked(3).ifEmpty { null }?.let { chunk ->
                                items(items = chunk) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        (it.filterIsInstance<VisitedPuzzleCollection?>()
                                              .plus(listOf(null, null))).take(3).forEach {
                                            val ts = it?.timestamp
                                            state.collections.get(it?.collectionId)?.let {
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
                                                                        style = TextStyle.Default.copy(
                                                                            fontSize = 8.sp,
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

        resultCollections.ifEmpty { null }?.let { collections ->
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
                            Spacer(modifier = Modifier.height(2.dp))
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
                                    fontSize = 14.sp
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
                                            style = TextStyle.Default.copy(
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Light
                                            )
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                if (it.id in state.recents.values.map { it.collectionId }) {
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
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${it.puzzle_count} puzzle(s)",
                                        fontSize = 12.sp,
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
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text =
                                            if(it.min_rank == it.max_rank)
                                                "${rankToString(it.min_rank)}"
                                            else
                                                "${rankToString(it.min_rank)} to ${rankToString(it.max_rank)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    val solutions = state.solutions.get(it.id) ?: 0
                                    val percentage = (solutions * 100f) / it.puzzle_count
                                    Text(
                                        text = "Solved",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row {
                                        Text(
                                            text = "${solutions} (${
                                                "%.1f".format(
                                                    percentage
                                                )
                                            }%)",
                                            fontSize = 12.sp,
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
                                    fontSize = 12.sp,
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
