package io.zenandroid.onlinego.ui.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.zenandroid.onlinego.data.model.BoardTheme
import io.zenandroid.onlinego.data.ogs.OGSWebSocketService
import io.zenandroid.onlinego.data.repositories.LoginStatus
import io.zenandroid.onlinego.data.repositories.SettingsRepository
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import io.zenandroid.onlinego.utils.addToDisposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Created by alex on 14/03/2018.
 */
class MainActivityViewModel(
  private val socketService: OGSWebSocketService,
  private val userSessionRepository: UserSessionRepository,
  private val settingsRepository: SettingsRepository,
) : ViewModel() {

  private val subscriptions = CompositeDisposable()
  private val _state = MutableStateFlow(
    MainActivityState()
  )
  val state: StateFlow<MainActivityState> = _state.asStateFlow()

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
        viewModelScope.launch {
          settingsRepository.setHasCompletedOnboarding(true)
        }
        socketService.ensureSocketConnected()
        socketService.resendAuth()
      }

      _state.update {
        it.copy(
          isLoggedIn = loggedIn is LoginStatus.LoggedIn,
        )
      }
    }.addToDisposable(subscriptions)

    Observable.interval(10, TimeUnit.SECONDS).subscribe {
      if (userSessionRepository.isLoggedIn()) {
        socketService.ensureSocketConnected()
      }
    }.addToDisposable(subscriptions)

  }

  fun onPause() {
    viewModelScope.launch(Dispatchers.IO) {
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