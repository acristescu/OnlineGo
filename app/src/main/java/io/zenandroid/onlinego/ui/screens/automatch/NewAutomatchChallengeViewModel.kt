package io.zenandroid.onlinego.ui.screens.automatch

import android.preference.PreferenceManager
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.data.model.ogs.Speed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

private val durationsSmall = arrayOf(5, 10, 15)
private val durationsMedium = arrayOf(10, 20, 30)
private val durationsLarge = arrayOf(15, 25, 40)

class NewAutomatchChallengeViewModel : ViewModel() {
  companion object {
    private const val SEARCH_GAME_SMALL = "SEARCH_GAME_SMALL"
    private const val SEARCH_GAME_MEDIUM = "SEARCH_GAME_MEDIUM"
    private const val SEARCH_GAME_LARGE = "SEARCH_GAME_LARGE"
    private const val SEARCH_GAME_SPEEDS = "SEARCH_GAME_SPEEDS"
  }

  private val prefs = PreferenceManager.getDefaultSharedPreferences(OnlineGoApplication.instance)
  val state: MutableStateFlow<AutomatchState> =
    MutableStateFlow(
      AutomatchState(
        small = prefs.getBoolean(SEARCH_GAME_SMALL, true),
        medium = prefs.getBoolean(SEARCH_GAME_MEDIUM, false),
        large = prefs.getBoolean(SEARCH_GAME_LARGE, false),
        speeds = prefs.getString(SEARCH_GAME_SPEEDS, "BLITZ,RAPID,LIVE")!!
          .split(",")
          .filter { it.isNotBlank() }
          .map {
            Speed.valueOf(it.trim())
          },
      ).let { it.copy(duration = calculateDuration(it)) }
    )


  fun onSmallCheckChanged(checked: Boolean) {
    state.update {
      it.copy(
        small = checked,
      ).let { it.copy(duration = calculateDuration(it)) }
    }
    prefs.edit().putBoolean(SEARCH_GAME_SMALL, checked).apply()
  }

  fun onMediumCheckChanged(checked: Boolean) {
    state.update {
      it.copy(
        medium = checked,
      ).let { it.copy(duration = calculateDuration(it)) }
    }
    prefs.edit().putBoolean(SEARCH_GAME_MEDIUM, checked).apply()
  }

  fun onLargeCheckChanged(checked: Boolean) {
    state.update {
      it.copy(
        large = checked,
      ).let { it.copy(duration = calculateDuration(it)) }
    }
    prefs.edit().putBoolean(SEARCH_GAME_LARGE, checked).apply()
  }

  fun onSpeedChanged(speed: Speed, checked: Boolean) {
    val speeds = state.value.speeds.toMutableList()
    if (checked) {
      speeds.add(speed)
    } else {
      speeds.remove(speed)
    }
    if (speed in arrayOf(Speed.LIVE, Speed.RAPID, Speed.BLITZ) && checked) {
      speeds.remove(Speed.LONG)
    }
    if (speed == Speed.LONG && checked) {
      speeds.remove(Speed.LIVE)
      speeds.remove(Speed.RAPID)
      speeds.remove(Speed.BLITZ)
    }
    state.update {
      it.copy(
        speeds = speeds,
      ).let { it.copy(duration = calculateDuration(it)) }
    }
    prefs.edit().putString(SEARCH_GAME_SPEEDS, speeds.joinToString { it.toString() }).apply()
  }

  private fun calculateDuration(state: AutomatchState): String {
    if (state.speeds.isEmpty() || !state.isAnySizeSelected) {
      return ""
    }
    if (state.speeds.contains(Speed.LONG)) {
      return "several days"
    }
    val durationsMin = when {
      state.small -> durationsSmall
      state.medium -> durationsMedium
      state.large -> durationsLarge
      else -> durationsSmall
    }
    val durationsMax = when {
      state.large -> durationsLarge
      state.medium -> durationsMedium
      state.small -> durationsSmall
      else -> durationsSmall
    }
    val minDuration = when {
      state.speeds.contains(Speed.BLITZ) -> durationsMin[0]
      state.speeds.contains(Speed.RAPID) -> durationsMin[1]
      state.speeds.contains(Speed.LIVE) -> durationsMin[2]
      else -> durationsMin[0]
    }
    val maxDuration = when {
      state.speeds.contains(Speed.LIVE) -> durationsMax[2]
      state.speeds.contains(Speed.RAPID) -> durationsMax[1]
      state.speeds.contains(Speed.BLITZ) -> durationsMax[0]
      else -> durationsMax[0]
    }
    return if (minDuration == maxDuration) {
      "$minDuration minutes"
    } else {
      "$minDuration to $maxDuration minutes"
    }
  }
}

@Immutable
data class AutomatchState(
  val small: Boolean = true,
  val medium: Boolean = false,
  val large: Boolean = false,
  val speeds: List<Speed> = listOf(Speed.LIVE, Speed.RAPID, Speed.BLITZ),
  val duration: String = ""
) {
  val isAnySizeSelected: Boolean
    get() = small || medium || large
}