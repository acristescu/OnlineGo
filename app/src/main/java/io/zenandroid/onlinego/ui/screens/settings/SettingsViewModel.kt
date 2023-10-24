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
import io.zenandroid.onlinego.ui.screens.settings.SettingsAction.DetailedAnalysisClicked
import io.zenandroid.onlinego.ui.screens.settings.SettingsAction.LogoutClicked
import io.zenandroid.onlinego.ui.screens.settings.SettingsAction.MaxVisitsChanged
import io.zenandroid.onlinego.ui.screens.settings.SettingsAction.NotificationsClicked
import io.zenandroid.onlinego.ui.screens.settings.SettingsAction.PrivacyClicked
import io.zenandroid.onlinego.ui.screens.settings.SettingsAction.RanksClicked
import io.zenandroid.onlinego.ui.screens.settings.SettingsAction.SoundsClicked
import io.zenandroid.onlinego.ui.screens.settings.SettingsAction.SupportClicked
import io.zenandroid.onlinego.ui.screens.settings.SettingsAction.ThemeClicked
import io.zenandroid.onlinego.ui.views.BoardView
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
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
      maxVisits = settingsRepository.maxVisits.toDouble(),
      detailedAnalysis = settingsRepository.detailedAnalysis,
      username = userSessionRepository.uiConfig?.user?.username ?: "",
      avatarURL = userSessionRepository.uiConfig?.user?.icon,
    )
  )

  fun onAction(action: SettingsAction) {
    when (action) {
      CoordinatesClicked -> {
        settingsRepository.showCoordinates = !state.value.coordinates
        state.update { it.copy(coordinates = !it.coordinates) }
      }

      RanksClicked -> {
        settingsRepository.showRanks = !state.value.ranks
        state.update { it.copy(ranks = !it.ranks) }
      }

      is MaxVisitsChanged -> {
        settingsRepository.maxVisits = action.value.toInt()
        state.update { it.copy(maxVisits = action.value) }
      }

      DetailedAnalysisClicked -> {
        settingsRepository.detailedAnalysis = !settingsRepository.detailedAnalysis
        state.update { it.copy(detailedAnalysis = !it.detailedAnalysis) }
      }

      SoundsClicked -> {
        settingsRepository.sound = !state.value.sounds
        state.update { it.copy(sounds = !it.sounds) }
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

        state.update { it.copy(theme = action.theme) }
      }

      is BoardThemeClicked -> {
        settingsRepository.boardTheme =
          BoardTheme.entries.find { it.displayName == action.boardDisplayName }!!
        state.update { it.copy(boardTheme = action.boardDisplayName) }
      }

      is DeleteAccountClicked -> state.update {
        it.copy(passwordDialogVisible = true)
      }

      is DeleteAccountCanceled -> state.update {
        it.copy(
          passwordDialogVisible = false,
          modalVisible = false,
          deleteAccountError = null,
        )
      }

      is DeleteAccountConfirmed -> {
        state.update {
          it.copy(
            passwordDialogVisible = false,
            modalVisible = true,
          )
        }
        viewModelScope.launch {
          try {
            userSessionRepository.deleteAccount(action.password)
            state.update {
              it.copy(
                modalVisible = false,
                passwordDialogVisible = false,
                deleteAccountError = "Account deleted. Sorry to see you go! The app will close in 5s."
              )
            }
            delay(5000)
            userSessionRepository.logOut()
          } catch (e: Exception) {
            if (e.message?.contains("403") == true) {
              state.update {
                it.copy(
                  passwordDialogVisible = false,
                  modalVisible = false,
                  deleteAccountError = "Wrong password"
                )
              }
            } else {
              state.update {
                it.copy(
                  passwordDialogVisible = false,
                  modalVisible = false,
                  deleteAccountError = e.message,
                )
              }
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
  val maxVisits: Double = 30.0,
  val detailedAnalysis: Boolean = false,
  val username: String = "",
  val avatarURL: String? = null,
  val passwordDialogVisible: Boolean = false,
  val modalVisible: Boolean = false,
  val deleteAccountError: String? = null,
)

sealed interface SettingsAction {
  data object NotificationsClicked : SettingsAction
  data object SoundsClicked : SettingsAction
  data class ThemeClicked(val theme: String) : SettingsAction
  data class BoardThemeClicked(val boardDisplayName: String) : SettingsAction
  data object CoordinatesClicked : SettingsAction
  data class MaxVisitsChanged(val value: Double) : SettingsAction
  data object DetailedAnalysisClicked : SettingsAction
  data object RanksClicked : SettingsAction
  data object LogoutClicked : SettingsAction
  data object DeleteAccountClicked : SettingsAction
  data object DeleteAccountCanceled : SettingsAction
  data class DeleteAccountConfirmed(val password: String) : SettingsAction
  data object PrivacyClicked : SettingsAction
  data object SupportClicked : SettingsAction
}
