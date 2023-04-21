package io.zenandroid.onlinego.ui.screens.settings

import androidx.lifecycle.ViewModel
import io.zenandroid.onlinego.data.model.BoardTheme
import io.zenandroid.onlinego.data.repositories.SettingsRepository
import io.zenandroid.onlinego.ui.screens.settings.SettingsAction.BoardThemeClicked
import io.zenandroid.onlinego.ui.screens.settings.SettingsAction.CoordinatesClicked
import io.zenandroid.onlinego.ui.screens.settings.SettingsAction.DeleteAccountClicked
import io.zenandroid.onlinego.ui.screens.settings.SettingsAction.LogoutClicked
import io.zenandroid.onlinego.ui.screens.settings.SettingsAction.NotificationsClicked
import io.zenandroid.onlinego.ui.screens.settings.SettingsAction.PrivacyClicked
import io.zenandroid.onlinego.ui.screens.settings.SettingsAction.RanksClicked
import io.zenandroid.onlinego.ui.screens.settings.SettingsAction.SoundsClicked
import io.zenandroid.onlinego.ui.screens.settings.SettingsAction.SupportClicked
import io.zenandroid.onlinego.ui.screens.settings.SettingsAction.ThemeClicked
import io.zenandroid.onlinego.ui.screens.settings.SettingsAction.VibrateClicked
import kotlinx.coroutines.flow.MutableStateFlow

class SettingsViewModel(
  private val settingsRepository: SettingsRepository
) : ViewModel() {
  val state = MutableStateFlow(SettingsState(
    theme = settingsRepository.appTheme ?: "Default",
    vibrate = settingsRepository.vibrate,
    sounds = settingsRepository.sound,
    ranks = settingsRepository.showRanks,
    coordinates = settingsRepository.showCoordinates,
  ))

  fun onAction(action: SettingsAction) {
    val state = state.value
    when(action) {
      CoordinatesClicked -> {
        settingsRepository.showCoordinates = !state.coordinates
        this.state.value = state.copy(coordinates = !state.coordinates)
      }
      VibrateClicked -> {
        settingsRepository.vibrate = !state.vibrate
        this.state.value = state.copy(vibrate = !state.vibrate)
      }
      RanksClicked -> {
        settingsRepository.showRanks = !state.ranks
        this.state.value = state.copy(ranks = !state.ranks)
      }
      SoundsClicked -> {
        settingsRepository.sound = !state.sounds
        this.state.value = state.copy(sounds = !state.sounds)
      }
      is ThemeClicked -> {
        settingsRepository.appTheme = action.theme
        this.state.value = state.copy(theme = action.theme)
      }
      is BoardThemeClicked -> {
        settingsRepository.boardTheme = BoardTheme.values().find { it.displayName == action.boardDisplayName }!!
        this.state.value = state.copy(boardTheme = action.boardDisplayName)
      }
      DeleteAccountClicked, PrivacyClicked, SupportClicked, LogoutClicked, NotificationsClicked -> {}
    }
  }

}

data class SettingsState(
  val theme: String = "Default",
  val boardTheme: String = "Default",
  val vibrate: Boolean = true,
  val sounds: Boolean = true,
  val ranks: Boolean = true,
  val coordinates: Boolean = true,

)

sealed interface SettingsAction {
  object NotificationsClicked : SettingsAction
  object VibrateClicked : SettingsAction
  object SoundsClicked : SettingsAction
  class ThemeClicked(val theme: String) : SettingsAction
  class BoardThemeClicked(val boardDisplayName: String) : SettingsAction
  object CoordinatesClicked: SettingsAction
  object RanksClicked: SettingsAction
  object LogoutClicked: SettingsAction
  object DeleteAccountClicked: SettingsAction
  object PrivacyClicked: SettingsAction
  object SupportClicked: SettingsAction
}