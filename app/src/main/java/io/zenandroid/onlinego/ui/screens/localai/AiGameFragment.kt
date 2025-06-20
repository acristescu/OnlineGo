package io.zenandroid.onlinego.ui.screens.localai

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import io.zenandroid.onlinego.data.repositories.SettingsRepository
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme
import io.zenandroid.onlinego.utils.analyticsReportScreen
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class AiGameFragment : Fragment() {
  private val viewModel: AiGameViewModel by viewModel()
  private val settingsRepository: SettingsRepository by inject()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    return ComposeView(requireContext()).apply {
      setContent {
        OnlineGoTheme {
          val state by viewModel.state.collectAsState()
          val userIcon = get<UserSessionRepository>().uiConfig?.user?.icon

          AiGameUI(
            state = state,
            userIcon = userIcon,
            boardTheme = settingsRepository.boardTheme,
            showCoordinates = settingsRepository.showCoordinates,
            onUserTappedCoordinate = viewModel::onUserTappedCoordinate,
            onUserHotTrackedCoordinate = viewModel::onUserHotTrackedCoordinate,
            onUserPressedPass = viewModel::onUserPressedPass,
            onUserPressedPrevious = viewModel::onUserPressedPrevious,
            onUserPressedNext = viewModel::onUserPressedNext,
            onShowNewGameDialog = viewModel::onShowNewGameDialog,
            onUserAskedForHint = viewModel::onUserAskedForHint,
            onUserAskedForOwnership = viewModel::onUserAskedForOwnership,
            onNewGame = viewModel::onNewGame,
            onDismissNewGameDialog = viewModel::onDismissNewGameDialog,
            onNavigateBack = { findNavController().popBackStack() }
          )
        }
      }
    }
  }

  override fun onPause() {
    viewModel.onViewPaused()
    super.onPause()
  }

  override fun onResume() {
    super.onResume()
    analyticsReportScreen("AiGame")
  }
}