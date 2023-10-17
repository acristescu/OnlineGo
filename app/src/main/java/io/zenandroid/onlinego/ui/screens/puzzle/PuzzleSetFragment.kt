package io.zenandroid.onlinego.ui.screens.puzzle

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnLifecycleDestroyed
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.google.accompanist.pager.*
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.model.local.Puzzle
import io.zenandroid.onlinego.ui.screens.puzzle.PuzzleSetAction.*
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme
import io.zenandroid.onlinego.utils.analyticsReportScreen
import io.zenandroid.onlinego.utils.rememberStateWithLifecycle
import org.commonmark.node.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import java.time.temporal.ChronoUnit.*

const val COLLECTION_ID = "COLLECTION_ID"

private const val TAG = "PuzzleSetFragment"

@ExperimentalAnimationApi
@ExperimentalMaterialApi
@ExperimentalFoundationApi
@ExperimentalComposeUiApi
class PuzzleSetFragment : Fragment() {
    private val viewModel: PuzzleSetViewModel by viewModel {
        parametersOf(requireArguments().getLong(COLLECTION_ID))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                OnlineGoTheme {
                    val state by rememberStateWithLifecycle(viewModel.state)

                    PuzzleSetScreen(
                        state = state,
                        fetchSolutions = viewModel::fetchSolutions,
                        onPuzzle = ::navigateToTsumegoScreen,
                        onBack = { findNavController().navigateUp() },
                    )
                }
            }
        }
    }

    private fun navigateToTsumegoScreen(puzzle: Puzzle) {
        findNavController().navigate(
            R.id.tsumegoFragment,
            bundleOf(
                PUZZLE_ID to puzzle.id,
            ),
            NavOptions.Builder()
                .setLaunchSingleTop(true)
                .build()
        )
    }

    override fun onResume() {
        super.onResume()
        analyticsReportScreen("Puzzle")
    }
}
