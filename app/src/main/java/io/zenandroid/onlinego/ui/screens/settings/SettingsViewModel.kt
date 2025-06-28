package io.zenandroid.onlinego.ui.screens.settings

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
  private val settingsRepository: SettingsRepository,
  private val userSessionRepository: UserSessionRepository,
) : ViewModel() {

  val userSettings: StateFlow<UserSettings> = combine(
    settingsRepository.appThemeFlow,
    settingsRepository.boardThemeFlow,
    settingsRepository.showRanksFlow,
    settingsRepository.showCoordinatesFlow,
    settingsRepository.soundFlow,
  ) { appTheme, boardTheme, showRanks, showCoordinates, sound ->
    UserSettings(
      theme = appTheme,
      boardTheme = boardTheme,
      showRanks = showRanks,
      showCoordinates = showCoordinates,
      soundEnabled = sound
    )
  }.stateIn(
    viewModelScope,
    SharingStarted.WhileSubscribed(5_000),
    settingsRepository.cachedUserSettings
  )

  val state: MutableStateFlow<SettingsState> = MutableStateFlow(
    SettingsState(
      username = userSessionRepository.uiConfig?.user?.username ?: "",
      avatarURL = userSessionRepository.uiConfig?.user?.icon,
    )
  )

  fun onAction(action: SettingsAction) {
    when (action) {
      CoordinatesClicked -> {
        viewModelScope.launch {
          settingsRepository.setShowCoordinates(!userSettings.value.showCoordinates)
        }
      }

      RanksClicked -> {
        viewModelScope.launch {
          settingsRepository.setShowRanks(!userSettings.value.showRanks)
        }
      }

      SoundsClicked -> {
        viewModelScope.launch {
          settingsRepository.setSound(!userSettings.value.soundEnabled)
        }
      }

      is ThemeClicked -> {
        viewModelScope.launch {
          settingsRepository.setAppTheme(action.theme)
          BoardView.unloadResources()
        }
      }

      is SettingsAction.Logout -> doLogout(action.context)

      is BoardThemeClicked -> {
        viewModelScope.launch {
          settingsRepository.setBoardTheme(
            BoardTheme.entries.find { it.displayName == action.boardDisplayName }!!
          )
        }
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

  private fun doLogout(context: Context?) {
    context?.let { FirebaseAnalytics.getInstance(it).logEvent("logout_clicked", null) }
    FirebaseCrashlytics.getInstance().sendUnsentReports()
    userSessionRepository.logOut()
    state.update {
      it.copy(isLoggedOut = true)
    }
  }
}

@Immutable
data class SettingsState(
  val username: String = "",
  val avatarURL: String? = null,
  val passwordDialogVisible: Boolean = false,
  val modalVisible: Boolean = false,
  val deleteAccountError: String? = null,
  val isLoggedOut: Boolean = false,
)

sealed interface SettingsAction {
  data object NotificationsClicked : SettingsAction
  data object SoundsClicked : SettingsAction
  data class ThemeClicked(val theme: String) : SettingsAction
  data class BoardThemeClicked(val boardDisplayName: String) : SettingsAction
  data object CoordinatesClicked : SettingsAction
  data object RanksClicked : SettingsAction
  data object LogoutClicked : SettingsAction
  data object DeleteAccountClicked : SettingsAction
  data object DeleteAccountCanceled : SettingsAction
  data class DeleteAccountConfirmed(val password: String) : SettingsAction
  data object PrivacyClicked : SettingsAction
  data object SupportClicked : SettingsAction
  data class Logout(val context: Context?) : SettingsAction
}

@Immutable
data class DialogData(
  val title: String,
  val message: String,
  val positiveButton: String,
  val negativeButton: String,
  val onPositive: () -> Unit,
)

@Immutable
data class UserSettings(
  val theme: String = "Default",
  val boardTheme: BoardTheme = BoardTheme.WOOD,
  val showRanks: Boolean = true,
  val showCoordinates: Boolean = false,
  val soundEnabled: Boolean = true,
  val graphByGames: Boolean = false,
)
