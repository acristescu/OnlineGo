package io.zenandroid.onlinego.ui.screens.puzzle

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnLifecycleDestroyed
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.accompanist.pager.*
import io.zenandroid.onlinego.ui.screens.puzzle.TsumegoAction.*
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme
import io.zenandroid.onlinego.utils.analyticsReportScreen
import io.zenandroid.onlinego.utils.rememberStateWithLifecycle
import org.commonmark.node.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import java.time.temporal.ChronoUnit.*

const val COLLECTION_ID = "COLLECTION_ID"
const val PUZZLE_ID = "PUZZLE_ID"

private const val TAG = "TsumegoFragment"

class TsumegoFragment : Fragment() {
    private val viewModel: TsumegoViewModel by viewModel {
        parametersOf(
            arguments!!.getLong(COLLECTION_ID),
            arguments!!.getLong(PUZZLE_ID))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                OnlineGoTheme {
                    val state by rememberStateWithLifecycle(viewModel.state)

                    TsumegoScreen(
                        state = state,
                        hasPreviousPuzzle = viewModel.hasPreviousPuzzle,
                        hasNextPuzzle = viewModel.hasNextPuzzle,
                        collection = viewModel.collectionContents,
                        positions = viewModel.collectionPositions,
                        ratings = viewModel.collectionRatings,
                        renderCollectionPuzzle = viewModel::renderCollectionPuzzle,
                        onMove = { viewModel.makeMove(it) },
                        onHint = { viewModel.addBoardHints() },
                        onResetPuzzle = { viewModel.resetPuzzle() },
                        onRate = { viewModel.rate(it) },
                        onPreviousPuzzle = { viewModel.previousPuzzle() },
                        onSelectPuzzle = { viewModel.selectPuzzle(it) },
                        onNextPuzzle = { viewModel.nextPuzzle() },
                        onBack = { findNavController().navigateUp() },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        analyticsReportScreen("Tsumego")
    }
}
