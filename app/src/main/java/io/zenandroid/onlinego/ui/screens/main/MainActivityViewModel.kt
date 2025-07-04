package io.zenandroid.onlinego.ui.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.zenandroid.onlinego.data.model.BoardTheme
import io.zenandroid.onlinego.data.ogs.OGSWebSocketService
import io.zenandroid.onlinego.data.repositories.SettingsRepository
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import io.zenandroid.onlinego.utils.addToDisposable
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
    MainActivityState(
      isLoggedIn = userSessionRepository.isLoggedIn()
    )
  )
  val state: StateFlow<MainActivityState> = _state.asStateFlow()

  init {
    viewModelScope.launch {
      combine(
        settingsRepository.boardThemeFlow,
        settingsRepository.appThemeFlow,
        settingsRepository.showCoordinatesFlow,
      ) { boardTheme, appTheme, showCoordinates ->
        Triple(boardTheme, appTheme, showCoordinates)
      }
        .collect { (boardTheme, appTheme, showCoordinates) ->
          _state.update {
            it.copy(
              hasLoadedTheme = true,
              appTheme = appTheme,
              boardTheme = boardTheme,
              showCoordinates = showCoordinates,
            )
          }
        }
    }
  }

  fun onResume() {
    if (userSessionRepository.isLoggedIn()) {
      socketService.ensureSocketConnected()
      socketService.resendAuth()
    }

    _state.update {
      it.copy(
        isLoggedIn = userSessionRepository.isLoggedIn(),
      )
    }

    Observable.interval(10, TimeUnit.SECONDS).subscribe {
      if (userSessionRepository.isLoggedIn()) {
        socketService.ensureSocketConnected()
      }
    }.addToDisposable(subscriptions)

  }

  fun onPause() {
    subscriptions.clear()
    socketService.disconnect()
  }

  fun onMyGamesLoaded() {
    _state.update {
      it.copy(
        myGamesLoaded = true,
      )
    }
  }
}

data class MainActivityState(
  val myGamesLoaded: Boolean = false,
  val hasLoadedTheme: Boolean = true,
  val isLoggedIn: Boolean? = null,
  val appTheme: String? = null,
  val boardTheme: BoardTheme? = null,
  val showCoordinates: Boolean = false,
) {
  val isLoaded: Boolean
    get() = hasLoadedTheme && (isLoggedIn == false || (isLoggedIn == true && myGamesLoaded))
}