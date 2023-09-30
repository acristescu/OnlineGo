package io.zenandroid.onlinego.ui.screens.puzzle

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Browser
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.DrawableRes
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnLifecycleDestroyed
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.google.accompanist.pager.*
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.movement.MovementMethodPlugin
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.utils.showIf
import io.zenandroid.onlinego.ui.composables.Board
import io.zenandroid.onlinego.ui.composables.RatingBar
import io.zenandroid.onlinego.ui.screens.main.MainActivity
import io.zenandroid.onlinego.ui.screens.puzzle.TsumegoAction.*
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.local.Puzzle
import io.zenandroid.onlinego.data.model.local.PuzzleCollection
import io.zenandroid.onlinego.mvi.MviView
import io.zenandroid.onlinego.data.repositories.SettingsRepository
import io.zenandroid.onlinego.utils.PersistenceManager
import io.zenandroid.onlinego.utils.analyticsReportScreen
import io.zenandroid.onlinego.utils.convertCountryCodeToEmojiFlag
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.collect
import org.commonmark.node.*
import org.koin.android.ext.android.inject
import org.koin.android.ext.android.get
import java.time.Instant.now
import java.time.temporal.ChronoUnit.*
import org.koin.core.context.GlobalContext
import org.koin.core.parameter.parametersOf
import androidx.compose.runtime.getValue
import io.zenandroid.onlinego.utils.rememberStateWithLifecycle

private const val TAG = "TsumegoUI"

@ExperimentalAnimationApi
@ExperimentalMaterialApi
@ExperimentalFoundationApi
@ExperimentalComposeUiApi
object TsumegoUI {
    private val settingsRepository: SettingsRepository by lazy { GlobalContext.get().get() }

    @Composable
    fun MainUI(
        state: TsumegoState,
        hasPreviousPuzzle: Boolean,
        hasNextPuzzle: Boolean,
        onMove: (Cell) -> Unit,
        onHint: () -> Unit,
        onResetPuzzle: () -> Unit,
        onRate: (Int) -> Unit,
        onSolved: () -> Unit,
        onPreviousPuzzle: () -> Unit,
        onNextPuzzle: () -> Unit,
        onBack: () -> Unit,
    ) {
        val solved = state.continueButtonVisible
        Column (
            modifier = Modifier.fillMaxHeight()
        ) {
            val titleState = remember {
                val base = "Tsumego"
                derivedStateOf {
                    state.puzzle?.name?.let {
                        "${base}: ${it}"
                    } ?: base
                }
            }
            TopAppBar(
                title = {
                    Text(
                        text = titleState.value,
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
                  //val listener = { action: TsumegoAction ->
                  //    Toast.makeText(requireContext(), action.toString(), Toast.LENGTH_SHORT).show()
                  //    internalActions.onNext(action)
                  //}
                    Column(modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 10.dp)) {
                        it.puzzle.let {
                            Board(
                                boardWidth = it.width,
                                boardHeight = it.height,
                                position = state.boardPosition,
                                boardTheme = settingsRepository.boardTheme,
                                drawCoordinates = settingsRepository.showCoordinates,
                                interactive = state.boardInteractive,
                                drawShadow = false,
                                fadeInLastMove = false,
                                fadeOutRemovedStones = false,
                                removedStones = state.removedStones?.map { it.toPair() },
                                candidateMove = state.hoveredCell,
                                candidateMoveType = StoneType.BLACK,
                              //onTapMove = { if (state.boardInteractive) listener(BoardCellHovered(it)) },
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
        LaunchedEffect(solved) {
            snapshotFlow { solved }
            .distinctUntilChanged()
            .filter { it }
            .collect {
                onSolved()
            }
        }
    }
}
