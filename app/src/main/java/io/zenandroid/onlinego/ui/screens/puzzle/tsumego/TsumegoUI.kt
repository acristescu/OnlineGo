package io.zenandroid.onlinego.ui.screens.puzzle.tsumego

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
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
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.model.BoardTheme
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
import java.util.Stack

private const val TAG = "TsumegoUI"

@Composable
fun TsumegoScreen(
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
                    onExpandedChange = {
                        expanded = !expanded
                    },
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
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = expanded
                            )
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                            textColor = MaterialTheme.colors.onSurface,
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 18.sp),
                        modifier = Modifier.weight(1f)
                    )
                    LaunchedEffect(expanded) {
                        if (expanded) {
                            selectedPuzzleIndex?.let { listState.animateScrollToItem(it) }
                        }
                    }
                    val infiniteTransition = rememberInfiniteTransition()
                    ExposedLazyDropdownMenu(
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
                            }
                        ) {
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
                                        boardTheme = state.boardTheme,
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
                    }
                }
                if (state.solutions.isNotEmpty()) {
                    Image(
                        painter = painterResource(R.drawable.ic_check_circle),
                        modifier = Modifier
                            .padding(horizontal = 8.dp),
                        contentDescription = "Solved"
                    )
                }
            },
            elevation = 1.dp,
            navigationIcon = {
                IconButton(onClick = { onBack() }) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                }
            },
            backgroundColor = MaterialTheme.colors.surface
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
                            boardTheme = state.boardTheme,
                            drawCoordinates = state.drawCoordinates,
                            interactive = state.boardInteractive,
                            drawShadow = true,
                            fadeInLastMove = false,
                            fadeOutRemovedStones = false,
                            removedStones = state.removedStones?.map { it.toPair() },
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
                                            color = MaterialTheme.colors.secondary,
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
                                        ?.let { MaterialTheme.colors.secondary }
                                        ?: MaterialTheme.colors.onBackground,
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
                                            color = MaterialTheme.colors.secondary,
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
                                style = MaterialTheme.typography.body2,
                                fontSize = 16.sp,
                                color = MaterialTheme.colors.onSurface,
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
                                            tint = MaterialTheme.colors.onSurface,
                                            modifier = Modifier.size(16.dp),
                                            contentDescription = null
                                        )
                                        Text(
                                            text = "RETRY",
                                            color = MaterialTheme.colors.onSurface,
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                }
                                if(state.retryButtonVisible && state.continueButtonVisible) {
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
                    color = MaterialTheme.colors.onBackground,
                )
            }
        }
    }
}

@Composable
@Preview
fun TsumegoScreenPreview() {
    TsumegoScreen(
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
            boardTheme = BoardTheme.WOOD,
            drawCoordinates = true,
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
