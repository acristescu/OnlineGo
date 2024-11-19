package io.zenandroid.onlinego.ui.screens.automatch

import android.preference.PreferenceManager
import android.util.Log
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import androidx.lifecycle.ViewModel
import com.github.mikephil.charting.data.BubbleEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.data.model.ogs.SeekGraphChallenge
import io.zenandroid.onlinego.data.model.ogs.Speed
import io.zenandroid.onlinego.data.repositories.SeekGraphRepository
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import io.zenandroid.onlinego.utils.addToDisposable
import io.zenandroid.onlinego.utils.recordException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

private const val TAG = "NewAutomatchChallengeVM"

class NewAutomatchChallengeViewModel(
  private val userSessionRepository: UserSessionRepository,
  private val seekGraphRepository: SeekGraphRepository,
) : ViewModel() {
  companion object {
    private const val SEARCH_GAME_SMALL = "SEARCH_GAME_SMALL"
    private const val SEARCH_GAME_MEDIUM = "SEARCH_GAME_MEDIUM"
    private const val SEARCH_GAME_LARGE = "SEARCH_GAME_LARGE"
    private const val SEARCH_GAME_SPEED = "SEARCH_GAME_SPEED"
  }

  private val subscriptions = CompositeDisposable()

  private val prefs = PreferenceManager.getDefaultSharedPreferences(OnlineGoApplication.instance)
  private val _state = MutableStateFlow(
    AutomatchState(
      small = prefs.getBoolean(SEARCH_GAME_SMALL, true),
      medium = prefs.getBoolean(SEARCH_GAME_MEDIUM, false),
      large = prefs.getBoolean(SEARCH_GAME_LARGE, false),
      speed = Speed.valueOf(prefs.getString(SEARCH_GAME_SPEED, Speed.NORMAL.name)!!),
      rating = userSessionRepository.uiConfig?.user?.ranking ?: 0
    )
  )
  val state: StateFlow<AutomatchState> = _state

  init {
    seekGraphRepository.challengesSubject
      .observeOn(Schedulers.single())
      .subscribeOn(Schedulers.io())
      .map {
        it.sortedBy { challenge -> challenge.time_per_move }
      }
      .distinctUntilChanged()
      .subscribe(this::setSeekGraph, this::onError)
      .addToDisposable(subscriptions)
  }

  fun onChallenges(body: (List<SeekGraphChallenge>) -> Unit) {
    seekGraphRepository.challengesSubject
      .observeOn(Schedulers.single())
      .subscribeOn(Schedulers.io())
      .subscribe({ challenges ->
        body(challenges)
        Log.d(TAG, "$challenges")
      }, {
        Log.e(TAG, it.toString())
      })
      .addToDisposable(subscriptions)
  }


  fun onSmallCheckChanged(checked: Boolean) {
    _state.update { it.copy(small = checked) }
    prefs.edit().putBoolean(SEARCH_GAME_SMALL, checked).apply()
  }

  fun onMediumCheckChanged(checked: Boolean) {
    _state.update { it.copy(medium = checked) }
    prefs.edit().putBoolean(SEARCH_GAME_MEDIUM, checked).apply()
  }

  fun onLargeCheckChanged(checked: Boolean) {
    _state.update { it.copy(large = checked) }
    prefs.edit().putBoolean(SEARCH_GAME_LARGE, checked).apply()
  }

  fun onSpeedChanged(speed: Speed) {
    _state.update { it.copy(speed = speed) }
    prefs.edit().putString(SEARCH_GAME_SPEED, speed.toString()).apply()
  }

  private fun setSeekGraph(challenges: List<SeekGraphChallenge>) {
    _state.update {
      it.copy(challenges = challenges)
    }
  }

  private fun onError(t: Throwable) {
    Log.e(this::class.java.canonicalName, t.message, t)
    recordException(t)
  }
}

data class AutomatchState(
  val small: Boolean = true,
  val medium: Boolean = false,
  val large: Boolean = false,
  val speed: Speed = Speed.NORMAL,
  val challenges: List<SeekGraphChallenge> = emptyList(),
  val rating: Int = 0,
) {
  val isAnySizeSelected: Boolean
    get() = small || medium || large
}
