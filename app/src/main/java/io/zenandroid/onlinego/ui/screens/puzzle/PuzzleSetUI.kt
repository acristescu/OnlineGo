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
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnLifecycleDestroyed
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
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
import org.koin.core.context.GlobalContext
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.gamelogic.RulesManager
import io.zenandroid.onlinego.utils.showIf
import io.zenandroid.onlinego.ui.composables.Board
import io.zenandroid.onlinego.ui.composables.RatingBar
import io.zenandroid.onlinego.ui.screens.main.MainActivity
import io.zenandroid.onlinego.ui.screens.puzzle.PuzzleSetAction.*
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.data.model.local.Puzzle
import io.zenandroid.onlinego.data.model.local.PuzzleCollection
import io.zenandroid.onlinego.data.repositories.SettingsRepository
import io.zenandroid.onlinego.mvi.MviView
import io.zenandroid.onlinego.utils.PersistenceManager
import io.zenandroid.onlinego.utils.convertCountryCodeToEmojiFlag
import org.commonmark.node.*
import org.koin.android.ext.android.inject
import org.koin.android.ext.android.get
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.time.Instant.now
import java.time.temporal.ChronoUnit.*
import org.koin.core.parameter.parametersOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import io.zenandroid.onlinego.data.model.BoardTheme
import io.zenandroid.onlinego.gamelogic.Util.toCoordinateSet
import io.zenandroid.onlinego.utils.analyticsReportScreen
import io.zenandroid.onlinego.utils.rememberStateWithLifecycle

private const val TAG = "PuzzleSetUI"

@ExperimentalAnimationApi
@ExperimentalMaterialApi
@ExperimentalFoundationApi
@ExperimentalComposeUiApi
object PuzzleSetUI {
    private val settingsRepository: SettingsRepository by lazy { GlobalContext.get().get() }

    @Composable
    fun MainUI(
        state: PuzzleSetState,
        fetchSolutions: (puzzleId: Long) -> Unit,
        onPuzzle: (puzzle: Puzzle) -> Unit,
        onBack: () -> Unit,
    ) {
        Column (
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
                        val solved = !state.solutions.get(it.id).isNullOrEmpty()
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
                                        val pos = RulesManager.buildPos(moves = emptyList(),
                                            boardWidth = it.width, boardHeight = it.height,
                                            whiteInitialState = it.initial_state.white.toCoordinateSet(),
                                            blackInitialState = it.initial_state.black.toCoordinateSet()
                                        )
                                        Board(
                                            boardWidth = it.width,
                                            boardHeight = it.height,
                                            position = pos,
                                            boardTheme = settingsRepository.boardTheme,
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
                                    Image(painter = painterResource(R.drawable.ic_check_circle),
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
}
