package io.zenandroid.onlinego.ui.screens.puzzle

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.pager.*
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.ui.composables.Board
import io.zenandroid.onlinego.ui.composables.RatingBar
import io.zenandroid.onlinego.ui.screens.puzzle.TsumegoAction.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import org.commonmark.node.*
import org.koin.core.context.GlobalContext
import java.time.temporal.ChronoUnit.*

private const val TAG = "TsumegoUI"

@Composable
fun TsumegoScreen(
    state: TsumegoState,
    hasPreviousPuzzle: Boolean,
    hasNextPuzzle: Boolean,
    onMove: (Cell) -> Unit,
    onHint: () -> Unit,
    onResetPuzzle: () -> Unit,
    onRate: (Int) -> Unit,
    onPreviousPuzzle: () -> Unit,
    onNextPuzzle: () -> Unit,
    onBack: () -> Unit,
) {
    Column (
        modifier = Modifier.fillMaxHeight()
    ) {
        val title = "Tsumego".let { base ->
            state.puzzle?.name?.let {
                "${base}: ${it}"
            } ?: base
        }
        TopAppBar(
            title = {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f)
                )
                if(state.solutions.size > 0) {
                    Image(painter = painterResource(R.drawable.ic_check_circle),
                        modifier = Modifier
                            .padding(start = 18.dp),
                        contentDescription = "Solved"
                    )
                }
            },
            elevation = 1.dp,
            navigationIcon = {
                IconButton(onClick = { onBack() }) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null)
                }
            },
            backgroundColor = MaterialTheme.colors.surface
        )

        state.puzzle?.let {
            Surface(
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxSize()
            ) {
                Column(modifier = Modifier
                        .padding(horizontal = 10.dp, vertical = 10.dp)) {
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
                                if(hasPreviousPuzzle) {
                                    Image(painter = painterResource(R.drawable.ic_navigate_previous),
                                        modifier = Modifier
                                            .align(Alignment.CenterVertically)
                                            .padding(start = 18.dp),
                                        contentDescription = null
                                    )
                                    TextButton(onClick = { onPreviousPuzzle() },
                                            modifier = Modifier
                                                .align(Alignment.CenterVertically)
                                                .padding(all = 4.dp)) {
                                        Text("PREVIOUS", color = MaterialTheme.colors.secondary, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }

                            TextButton(onClick = { onHint() },
                                    modifier = Modifier
                                        .align(Alignment.CenterVertically)
                                        .padding(all = 4.dp)) {
                                Text("HINT", color = state.nodeStack.lastOrNull()?.let { MaterialTheme.colors.secondary } ?: MaterialTheme.colors.onBackground, fontWeight = FontWeight.Bold)
                            }

                            Row(modifier = Modifier.weight(1f)) {
                                if(hasNextPuzzle) {
                                    Spacer(modifier = Modifier.weight(1f))
                                    TextButton(onClick = { onNextPuzzle() },
                                            modifier = Modifier
                                                .align(Alignment.CenterVertically)
                                                .padding(all = 4.dp)) {
                                        Text("NEXT", color = MaterialTheme.colors.secondary, fontWeight = FontWeight.Bold)
                                    }
                                    Image(painter = painterResource(R.drawable.ic_navigate_next),
                                        modifier = Modifier
                                            .align(Alignment.CenterVertically)
                                            .padding(start = 18.dp),
                                        contentDescription = null
                                    )
                                }
                            }
                        }
                        var boxState = rememberScrollState()
                        Box(modifier = Modifier.verticalScroll(state = boxState)
                                .weight(1f)) {
                            Text(
                                text = state.nodeStack.let { stack ->
                                    stack.lastOrNull()?.text
                                        ?: stack.dropLast(1).lastOrNull()?.text
                                } ?: it.puzzle_description,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.body2,
                                fontSize = 16.sp,
                                color = MaterialTheme.colors.onSurface,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Column {
                            Row(horizontalArrangement = Arrangement.SpaceAround,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 16.dp)) {
                                if (state.retryButtonVisible) {
                                    OutlinedButton(
                                            onClick = { onResetPuzzle() },
                                            modifier = Modifier.weight(1f)) {
                                        Icon(imageVector = Icons.Filled.Refresh,
                                            tint = MaterialTheme.colors.onSurface,
                                            modifier = Modifier.size(16.dp),
                                            contentDescription = null)
                                        Text(text = "RETRY",
                                            color = MaterialTheme.colors.onSurface,
                                            modifier = Modifier.padding(start = 8.dp))
                                    }
                                }
                                if (state.continueButtonVisible) {
                                    if(hasNextPuzzle) {
                                        Button(onClick = { onNextPuzzle() },
                                            modifier = Modifier.weight(1f)) {
                                            Text(text = "CONTINUE")
                                        }
                                    } else {
                                        Button(onClick = { onBack() },
                                            modifier = Modifier.weight(1f)) {
                                            Text(text = "DONE")
                                        }
                                    }
                                }
                            }
                            if (state.continueButtonVisible || (state.rating?.rating ?: 0) > 0) {
                                Row(modifier = Modifier.height(32.dp)
                                            .align(Alignment.CenterHorizontally)
                                        ) {
                                    RatingBar(
                                        rating = state.rating?.rating?.toFloat() ?: 0f,
                                        onTap = { onRate(it) },
                                        modifier = Modifier
                                            .align(Alignment.CenterVertically)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } ?: run {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
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
