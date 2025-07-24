package io.zenandroid.onlinego.ui.screens.automatch

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.zenandroid.onlinego.data.model.ogs.Speed
import io.zenandroid.onlinego.data.repositories.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

private val durationsSmall = arrayOf(5, 10, 15)
private val durationsMedium = arrayOf(10, 20, 30)
private val durationsLarge = arrayOf(15, 25, 40)

class NewAutomatchChallengeViewModel(
  private val settingsRepository: SettingsRepository,
) : ViewModel() {

  val state: MutableStateFlow<AutomatchState> =
    MutableStateFlow(
      AutomatchState(
        small = true,
        medium = false,
        large = false,
        speeds = listOf(Speed.LIVE, Speed.RAPID, Speed.BLITZ)
      ).withDuration()
    )

  init {
    viewModelScope.launch {
      combine(
        settingsRepository.searchGameSmall,
        settingsRepository.searchGameMedium,
        settingsRepository.searchGameLarge,
        settingsRepository.searchGameSpeeds,
      ) {
        small, medium, large, speeds ->
        AutomatchState(
          small = small,
          medium = medium,
          large = large,
          speeds = speeds,
        ).withDuration()
      }.collect {
        state.value = it
      }
    }
  }

  fun onSmallCheckChanged(checked: Boolean) {
    viewModelScope.launch {
      settingsRepository.setSearchGameSmall(checked)
    }
  }

  fun onMediumCheckChanged(checked: Boolean) {
    viewModelScope.launch {
      settingsRepository.setSearchGameMedium(checked)
    }
  }

  fun onLargeCheckChanged(checked: Boolean) {
    viewModelScope.launch {
      settingsRepository.setSearchGameLarge(checked)
    }
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
    viewModelScope.launch {
      settingsRepository.setSearchGameSpeeds(speeds)
    }
  }

  private fun AutomatchState.withDuration(): AutomatchState {
    if (speeds.isEmpty() || !isAnySizeSelected) {
      return copy(duration = "")
    }
    if (speeds.contains(Speed.LONG)) {
      return copy(duration = "several days")
    }
    val durationsMin = when {
      small -> durationsSmall
      medium -> durationsMedium
      large -> durationsLarge
      else -> durationsSmall
    }
    val durationsMax = when {
      large -> durationsLarge
      medium -> durationsMedium
      small -> durationsSmall
      else -> durationsSmall
    }
    val minDuration = when {
      speeds.contains(Speed.BLITZ) -> durationsMin[0]
      speeds.contains(Speed.RAPID) -> durationsMin[1]
      speeds.contains(Speed.LIVE) -> durationsMin[2]
      else -> durationsMin[0]
    }
    val maxDuration = when {
      speeds.contains(Speed.LIVE) -> durationsMax[2]
      speeds.contains(Speed.RAPID) -> durationsMax[1]
      speeds.contains(Speed.BLITZ) -> durationsMax[0]
      else -> durationsMax[0]
    }
    return copy(
      duration = if (minDuration == maxDuration) {
        "$minDuration minutes"
      } else {
        "$minDuration to $maxDuration minutes"
      }
    )
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