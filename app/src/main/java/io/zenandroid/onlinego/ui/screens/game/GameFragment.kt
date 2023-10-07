package io.zenandroid.onlinego.ui.screens.game

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme
import io.zenandroid.onlinego.utils.analyticsReportScreen
import io.zenandroid.onlinego.utils.rememberStateWithLifecycle
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

const val GAME_ID = "GAME_ID"
const val GAME_WIDTH = "GAME_WIDTH"
const val GAME_HEIGHT = "GAME_HEIGHT"

class GameFragment : Fragment() {

    private val viewModel: GameViewModel by viewModel()
    private val stoneSoundMediaPlayer = MediaPlayer.create(OnlineGoApplication.instance, R.raw.stone)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel.initialize(
            gameId = requireArguments().getLong(GAME_ID),
            gameWidth = requireArguments().getInt(GAME_WIDTH),
            gameHeight = requireArguments().getInt(GAME_HEIGHT),
        )

        lifecycleScope.launch {
            viewModel.events.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED).collect {
                when(it) {
                    Event.PlayStoneSound -> stoneSoundMediaPlayer.start()
                    null -> {}
                }
            }
        }

        return ComposeView(requireContext()).apply {
            setContent {
                viewModel.pendingNavigation ?.let { nav ->
                    when(nav) {
                        is PendingNavigation.NavigateToGame -> navigateToGameScreen(nav.game)
                        is PendingNavigation.OpenURL -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(nav.url)))
                    }
                }

                val state by rememberStateWithLifecycle(viewModel.state)

                OnlineGoTheme {
                    GameScreen(
                        state = state,
                        analysisMode = viewModel.analyzeMode,
                        onBack = ::onBackPressed,
                        onUserAction = viewModel::onUserAction
                    )
                }
            }
        }
    }

    private fun navigateToGameScreen(game: Game) {
        view?.findNavController()
            ?.navigate(
                R.id.gameFragment,
                bundleOf(GAME_ID to game.id, GAME_WIDTH to game.width, GAME_HEIGHT to game.height),
                NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setPopUpTo(R.id.gameFragment, true)
                    .build()
            )
    }

    private fun onBackPressed() {
        requireActivity().onBackPressedDispatcher.onBackPressed()
    }

    override fun onResume() {
        super.onResume()
        analyticsReportScreen("Game")
    }
}
