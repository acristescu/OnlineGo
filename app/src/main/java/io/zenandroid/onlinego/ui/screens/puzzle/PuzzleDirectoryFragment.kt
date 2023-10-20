package io.zenandroid.onlinego.ui.screens.puzzle

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnLifecycleDestroyed
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.model.local.PuzzleCollection
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme
import io.zenandroid.onlinego.utils.PersistenceManager
import io.zenandroid.onlinego.utils.analyticsReportScreen
import io.zenandroid.onlinego.utils.rememberStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.viewmodel.ext.android.viewModel

private const val TAG = "PuzzleDirectoryFragment"

class PuzzleDirectoryFragment : Fragment() {
    private val viewModel: PuzzleDirectoryViewModel by viewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                OnlineGoTheme {
                    val state by rememberStateWithLifecycle(viewModel.state)

                    PuzzleDirectoryScreen(
                        state = state,
                        onCollection = ::navigateToPuzzleScreen,
                        onBack = { findNavController().navigateUp() },
                        onSortChanged = { viewModel.onSortChanged(it) },
                        onFilterChanged = { viewModel.onFilterChanged(it) },
                        onToggleOnlyOpened = { viewModel.onToggleOnlyOpened() },
                    )
                }
            }
        }
    }

    private fun navigateToPuzzleScreen(collection: PuzzleCollection) {
        lifecycleScope.launch(context = Dispatchers.IO) {
            val puzzle_id = viewModel.getFirstUnsolvedForCollection(collection)

            withContext(Dispatchers.Main) {
                findNavController().navigate(
                    R.id.tsumegoFragment,
                    bundleOf(
                        COLLECTION_ID to collection.id,
                        PUZZLE_ID to puzzle_id,
                    ),
                    NavOptions.Builder()
                        .setLaunchSingleTop(true)
                        .build()
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        analyticsReportScreen("PuzzleDirectory")
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        PersistenceManager.visitedPuzzleDirectory = true
    }

}
