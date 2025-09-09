package io.zenandroid.onlinego.ui.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.reactivex.disposables.CompositeDisposable
import io.zenandroid.onlinego.data.model.BoardTheme
import io.zenandroid.onlinego.data.ogs.OGSWebSocketService
import io.zenandroid.onlinego.data.repositories.LoginStatus
import io.zenandroid.onlinego.data.repositories.SettingsRepository
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import io.zenandroid.onlinego.utils.addToDisposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Created by alex on 14/03/2018.
 */
class MainActivityViewModel(
  private val socketService: OGSWebSocketService,
  private val userSessionRepository: UserSessionRepository,
  private val settingsRepository: SettingsRepository,
  private val appCoroutineScope: CoroutineScope,
) : ViewModel() {

  private val subscriptions = CompositeDisposable()
  private val _state = MutableStateFlow(
    MainActivityState()
  )
  val state: StateFlow<MainActivityState> = _state.asStateFlow()
  private var socketConnectionCheckerJob: Job? = null

  init {
    viewModelScope.launch {
      combine(
        settingsRepository.boardThemeFlow,
        settingsRepository.appThemeFlow,
        settingsRepository.showCoordinatesFlow,
        settingsRepository.hasCompletedOnboardingFlow,
      ) { boardTheme, appTheme, showCoordinates, hasCompletedOnboarding ->
        PersistedSettings(boardTheme, appTheme, showCoordinates, hasCompletedOnboarding)
      }
        .collect { settings ->
          _state.update {
            it.copy(
              hasLoadedTheme = true,
              appTheme = settings.appTheme,
              boardTheme = settings.boardTheme,
              showCoordinates = settings.showCoordinates,
              hasCompletedOnboarding = settings.hasCompletedOnboarding,
            )
          }
        }
    }
  }

  fun onResume() {
    userSessionRepository.loggedInObservable.subscribe { loggedIn ->
      if (loggedIn is LoginStatus.LoggedIn) {
        socketConnectionCheckerJob?.cancel()
        socketConnectionCheckerJob =
          viewModelScope.launch(Dispatchers.IO) {
            socketService.ensureSocketConnected()
            socketService.resendAuth()
            while (true) {
              delay(10000)
              socketService.ensureSocketConnected()
            }
          }
      }

      _state.update {
        it.copy(
          isLoggedIn = loggedIn is LoginStatus.LoggedIn,
        )
      }
    }.addToDisposable(subscriptions)
  }

  fun onPause() {
    appCoroutineScope.launch(Dispatchers.IO) {
      socketConnectionCheckerJob?.cancel()
      socketConnectionCheckerJob = null
      subscriptions.clear()
      socketService.disconnect()
    }
  }

  fun onScreenReady() {
    _state.update {
      it.copy(
        screenDataLoaded = true,
      )
    }
  }
}

data class MainActivityState(
  val screenDataLoaded: Boolean = false,
  val hasLoadedTheme: Boolean = true,
  val isLoggedIn: Boolean? = null,
  val appTheme: String? = null,
  val boardTheme: BoardTheme? = null,
  val showCoordinates: Boolean = false,
  val hasCompletedOnboarding: Boolean? = null,
) {
  val isLoaded: Boolean
    get() = hasLoadedTheme && (hasCompletedOnboarding == false || screenDataLoaded)
}

private data class PersistedSettings(
  val boardTheme: BoardTheme,
  val appTheme: String,
  val showCoordinates: Boolean,
  val hasCompletedOnboarding: Boolean,
)