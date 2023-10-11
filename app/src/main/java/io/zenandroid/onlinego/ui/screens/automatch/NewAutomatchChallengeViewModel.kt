package io.zenandroid.onlinego.ui.screens.automatch

import android.preference.PreferenceManager
import androidx.lifecycle.ViewModel
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.data.model.ogs.Speed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class NewAutomatchChallengeViewModel : ViewModel() {
  companion object {
    private const val SEARCH_GAME_SMALL = "SEARCH_GAME_SMALL"
    private const val SEARCH_GAME_MEDIUM = "SEARCH_GAME_MEDIUM"
    private const val SEARCH_GAME_LARGE = "SEARCH_GAME_LARGE"
    private const val SEARCH_GAME_SPEED = "SEARCH_GAME_SPEED"
  }

  private val prefs = PreferenceManager.getDefaultSharedPreferences(OnlineGoApplication.instance)
  val state = MutableStateFlow(
    AutomatchState(
      small = prefs.getBoolean(SEARCH_GAME_SMALL, true),
      medium = prefs.getBoolean(SEARCH_GAME_MEDIUM, false),
      large = prefs.getBoolean(SEARCH_GAME_LARGE, false),
      speed = Speed.valueOf(prefs.getString(SEARCH_GAME_SPEED, Speed.NORMAL.name)!!)
    )
  )

  fun onSmallCheckChanged(checked: Boolean) {
    state.update { it.copy(small = checked) }
    prefs.edit().putBoolean(SEARCH_GAME_SMALL, checked).apply()
  }

  fun onMediumCheckChanged(checked: Boolean) {
    state.update { it.copy(medium = checked) }
    prefs.edit().putBoolean(SEARCH_GAME_MEDIUM, checked).apply()
  }

  fun onLargeCheckChanged(checked: Boolean) {
    state.update { it.copy(large = checked) }
    prefs.edit().putBoolean(SEARCH_GAME_LARGE, checked).apply()
  }

  fun onSpeedChanged(speed: Speed) {
    state.update { it.copy(speed = speed) }
    prefs.edit().putString(SEARCH_GAME_SPEED, speed.toString()).apply()
  }

}

data class AutomatchState(
  val small: Boolean = true,
  val medium: Boolean = false,
  val large: Boolean = false,
  val speed: Speed = Speed.NORMAL,
) {
  val isAnySizeSelected: Boolean
    get() = small || medium || large
}