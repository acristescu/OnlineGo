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
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.time.Instant.now
import java.time.temporal.ChronoUnit.*
import org.koin.core.parameter.parametersOf
import androidx.compose.runtime.getValue
import io.zenandroid.onlinego.utils.rememberStateWithLifecycle

const val PUZZLE_ID = "PUZZLE_ID"

private const val TAG = "TsumegoFragment"

class TsumegoFragment : Fragment(), MviView<TsumegoState, TsumegoAction> {
    private val settingsRepository: SettingsRepository by inject()
    private val viewModel: TsumegoViewModel by viewModel {
        parametersOf(arguments!!.getLong(PUZZLE_ID))
    }

    private val internalActions = PublishSubject.create<TsumegoAction>()
    private var currentState: TsumegoState? = null

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            internalActions.onNext(UserPressedBack)
            findNavController().navigateUp()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                OnlineGoTheme {
                    val state by rememberStateWithLifecycle(viewModel.state)

                    TsumegoUI.MainUI(
                        state = state,
                        hasPreviousPuzzle = viewModel.hasPreviousPuzzle,
                        hasNextPuzzle = viewModel.hasNextPuzzle,
                        onMove = { viewModel.makeMove(it) },
                        onHint = { viewModel.addBoardHints() },
                        onResetPuzzle = { viewModel.resetPuzzle() },
                        onRate = { viewModel.rate(it) },
                        onSolved = { viewModel.markSolved() },
                        onPreviousPuzzle = { viewModel.previousPuzzle() },
                        onNextPuzzle = { viewModel.nextPuzzle() },
                        onBack = { findNavController().navigateUp() },
                    )
                }
            }
        }
    }

    override val actions: Observable<TsumegoAction>
        get() =
            Observable.merge(
                listOf(
                        internalActions
                )
            ).startWith(ViewReady)

    override fun render(state: TsumegoState) {
        currentState = state
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        analyticsReportScreen("Tsumego")
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        requireActivity().onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }
}
