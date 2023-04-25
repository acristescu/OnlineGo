package io.zenandroid.onlinego.ui.screens.settings

import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.zenandroid.onlinego.data.model.BoardTheme
import io.zenandroid.onlinego.data.repositories.SettingsRepository
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import io.zenandroid.onlinego.ui.screens.settings.SettingsAction.BoardThemeClicked
import io.zenandroid.onlinego.ui.screens.settings.SettingsAction.CoordinatesClicked
import io.zenandroid.onlinego.ui.screens.settings.SettingsAction.DeleteAccountCanceled
import io.zenandroid.onlinego.ui.screens.settings.SettingsAction.DeleteAccountClicked
import io.zenandroid.onlinego.ui.screens.settings.SettingsAction.DeleteAccountConfirmed
import io.zenandroid.onlinego.ui.screens.settings.SettingsAction.LogoutClicked
import io.zenandroid.onlinego.ui.screens.settings.SettingsAction.NotificationsClicked
import io.zenandroid.onlinego.ui.screens.settings.SettingsAction.PrivacyClicked
import io.zenandroid.onlinego.ui.screens.settings.SettingsAction.RanksClicked
import io.zenandroid.onlinego.ui.screens.settings.SettingsAction.SoundsClicked
import io.zenandroid.onlinego.ui.screens.settings.SettingsAction.SupportClicked
import io.zenandroid.onlinego.ui.screens.settings.SettingsAction.ThemeClicked
import io.zenandroid.onlinego.ui.views.BoardView
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
  private val settingsRepository: SettingsRepository,
  private val userSessionRepository: UserSessionRepository,
) : ViewModel() {
  val state = MutableStateFlow(
    SettingsState(
      theme = settingsRepository.appTheme ?: "System Default",
      boardTheme = settingsRepository.boardTheme.displayName,
      sounds = settingsRepository.sound,
      ranks = settingsRepository.showRanks,
      coordinates = settingsRepository.showCoordinates,
      username = userSessionRepository.uiConfig?.user?.username ?: "",
      avatarURL = userSessionRepository.uiConfig?.user?.icon,
    )
  )

  fun onAction(action: SettingsAction) {
    val state = state.value
    when (action) {
      CoordinatesClicked -> {
        settingsRepository.showCoordinates = !state.coordinates
        this.state.value = state.copy(coordinates = !state.coordinates)
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
        viewModelScope.launch {
          delay(100)
          when (action.theme) {
            "Light" -> {
              AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }

            "Dark" -> {
              AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }

            else -> {
              if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
              } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
              }
            }
          }
          BoardView.unloadResources()
        }

        this.state.value = state.copy(theme = action.theme)
      }

      is BoardThemeClicked -> {
        settingsRepository.boardTheme =
          BoardTheme.values().find { it.displayName == action.boardDisplayName }!!
        this.state.value = state.copy(boardTheme = action.boardDisplayName)
      }

      is DeleteAccountClicked -> this.state.value = state.copy(
        passwordDialogVisible = true,
      )

      is DeleteAccountCanceled -> this.state.value = state.copy(
        passwordDialogVisible = false,
        modalVisible = false,
        deleteAccountError = null,
      )

      is DeleteAccountConfirmed -> {
        this.state.value = state.copy(
          passwordDialogVisible = false,
          modalVisible = true,
        )
        viewModelScope.launch {
          try {
            userSessionRepository.deleteAccount(action.password)
            this@SettingsViewModel.state.value = this@SettingsViewModel.state.value.copy(
              modalVisible = false,
              passwordDialogVisible = false,
              deleteAccountError = "Account deleted. Sorry to see you go! The app will close in 5s."
            )
            delay(5000)
            userSessionRepository.logOut()
          } catch (e: Exception) {
            if(e.message?.contains("403") == true) {
              this@SettingsViewModel.state.value = this@SettingsViewModel.state.value.copy(
                passwordDialogVisible = false,
                modalVisible = false,
                deleteAccountError = "Wrong password"
              )
            } else {
              this@SettingsViewModel.state.value = this@SettingsViewModel.state.value.copy(
                passwordDialogVisible = false,
                modalVisible = false,
                deleteAccountError = e.message,
              )
            }
          }
        }
      }

      PrivacyClicked, SupportClicked, LogoutClicked, NotificationsClicked -> {}
    }
  }

}

@Immutable
data class SettingsState(
  val theme: String = "Default",
  val boardTheme: String = "Default",
  val sounds: Boolean = true,
  val ranks: Boolean = true,
  val coordinates: Boolean = true,
  val username: String = "",
  val avatarURL: String? = null,
  val passwordDialogVisible: Boolean = false,
  val modalVisible: Boolean = false,
  val deleteAccountError: String? = null,
)

sealed interface SettingsAction {
  object NotificationsClicked : SettingsAction
  object SoundsClicked : SettingsAction
  class ThemeClicked(val theme: String) : SettingsAction
  class BoardThemeClicked(val boardDisplayName: String) : SettingsAction
  object CoordinatesClicked : SettingsAction
  object RanksClicked : SettingsAction
  object LogoutClicked : SettingsAction
  object DeleteAccountClicked : SettingsAction
  object DeleteAccountCanceled : SettingsAction
  class DeleteAccountConfirmed(val password: String) : SettingsAction
  object PrivacyClicked : SettingsAction
  object SupportClicked : SettingsAction
}