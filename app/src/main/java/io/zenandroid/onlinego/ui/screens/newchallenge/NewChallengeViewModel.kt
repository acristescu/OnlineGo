package io.zenandroid.onlinego.ui.screens.newchallenge

import android.preference.PreferenceManager
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.data.model.local.Player
import io.zenandroid.onlinego.data.model.ogs.OGSPlayer
import io.zenandroid.onlinego.utils.egfToRank
import io.zenandroid.onlinego.utils.formatRank
import kotlinx.coroutines.flow.MutableStateFlow

private const val PARAMS_KEY = "PARAMS"


class NewChallengeViewModel : ViewModel() {

  private val prefs = PreferenceManager.getDefaultSharedPreferences(OnlineGoApplication.instance)
  private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
  private val challengeParamsAdapter = moshi.adapter(ChallengeParams::class.java)
  private val opponentAdapter = moshi.adapter(Player::class.java)
  val state = MutableStateFlow(
    NewChallengeBottomSheetState(
      challenge = getSavedChallengeParams(),
      opponentText = getSavedChallengeParams().opponent?.let {
        "${it.username} (${
          formatRank(
            egfToRank(it.ratings?.overall?.rating),
            it.ratings?.overall?.deviation
          )
        })"
      } ?: "[Select Opponent]"
    )
  )

  private fun getSavedChallengeParams() =
    prefs.getString(PARAMS_KEY, null)?.let(challengeParamsAdapter::fromJson)
      ?: ChallengeParams(
        opponent = null,
        color = "Auto",
        ranked = true,
        handicap = "0",
        size = "9x9",
        speed = "Live",
        disable_analysis = false,
        private = false
      )

  fun onEvent(event: Event) {
    when (event) {
      is Event.OpponentSelected -> {
        val opponent = opponentAdapter.fromJson(event.opponent)
        state.value = state.value.copy(
          challenge = state.value.challenge.copy(
            opponent = opponent?.let { OGSPlayer.fromPlayer(it) }
          ),
          opponentText = opponent
            ?.let { "${it.username} (${formatRank(egfToRank(it.rating), it.deviation)})" }
            ?: "[Select Opponent]"
        )
      }

      is Event.ColorSelected -> {
        state.value = state.value.copy(
          challenge = state.value.challenge.copy(
            color = event.color
          )
        )
      }

      is Event.RankedSelected -> {
        state.value = state.value.copy(
          challenge = state.value.challenge.copy(
            ranked = event.ranked
          )
        )
      }

      is Event.HandicapSelected -> {
        state.value = state.value.copy(
          challenge = state.value.challenge.copy(
            handicap = event.handicap
          )
        )
      }

      is Event.SizeSelected -> {
        state.value = state.value.copy(
          challenge = state.value.challenge.copy(
            size = event.size
          )
        )
      }

      is Event.SpeedSelected -> {
        state.value = state.value.copy(
          challenge = state.value.challenge.copy(
            speed = event.speed
          )
        )
      }

      is Event.DisableAnalysisSelected -> {
        state.value = state.value.copy(
          challenge = state.value.challenge.copy(
            disable_analysis = event.disableAnalysis
          )
        )
      }

      is Event.PrivateSelected -> {
        state.value = state.value.copy(
          challenge = state.value.challenge.copy(
            private = event.private
          )
        )
      }

      is Event.ChallengeClicked -> {
        prefs.edit().putString(PARAMS_KEY, challengeParamsAdapter.toJson(state.value.challenge))
          .apply()
        state.value = state.value.copy(
          done = true
        )
      }

      is Event.OpponentClicked -> {}
    }
  }

  sealed interface Event {
    data object OpponentClicked : Event
    data class OpponentSelected(val opponent: String) : Event
    data class ColorSelected(val color: String) : Event
    data class RankedSelected(val ranked: Boolean) : Event
    data class HandicapSelected(val handicap: String) : Event
    data class SizeSelected(val size: String) : Event
    data class SpeedSelected(val speed: String) : Event
    data class DisableAnalysisSelected(val disableAnalysis: Boolean) : Event
    data class PrivateSelected(val private: Boolean) : Event
    data object ChallengeClicked : Event
  }
}

@Immutable
data class NewChallengeBottomSheetState(
  val challenge: ChallengeParams,
  val done: Boolean = false,
  val opponentText: String
)