package io.zenandroid.onlinego.ui.screens.learn

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.ui.screens.tutorial.TUTORIAL_NAME
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme
import io.zenandroid.onlinego.utils.analyticsReportScreen
import io.zenandroid.onlinego.utils.rememberStateWithLifecycle
import org.koin.androidx.viewmodel.ext.android.viewModel


/**
 * Created by alex on 05/11/2017.
 */
@ExperimentalAnimationApi
@ExperimentalMaterialApi
class LearnFragment : Fragment() {


    private val viewModel: LearnViewModel by viewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val state by rememberStateWithLifecycle(viewModel.state)

                OnlineGoTheme {
                    Screen(state, ::onAction)
                }
            }
        }
    }

    private fun onAction(action: LearnAction) {
        when(action) {
            LearnAction.JosekiExplorerClicked -> view?.findNavController()?.navigate(R.id.action_learnFragment_to_josekiExplorerFragment)
            LearnAction.PuzzlesClicked -> view?.findNavController()?.navigate(R.id.action_learnFragment_to_puzzleDirectoryFragment)
            is LearnAction.TutorialClicked -> view?.findNavController()?.navigate(R.id.action_learnFragment_to_tutorialFragment, bundleOf(TUTORIAL_NAME to action.tutorial.name))
            else -> viewModel.onAction(action)
        }
    }

    override fun onResume() {
        super.onResume()
        analyticsReportScreen("Learn")
    }
}
