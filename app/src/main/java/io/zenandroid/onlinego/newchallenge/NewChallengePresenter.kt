package io.zenandroid.onlinego.newchallenge

import android.os.Bundle
import android.text.Html
import com.google.firebase.analytics.FirebaseAnalytics
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.zenandroid.onlinego.ogs.AutomatchChallenge
import io.zenandroid.onlinego.ogs.OGSServiceImpl
import io.zenandroid.onlinego.ogs.Size
import io.zenandroid.onlinego.ogs.Speed
import java.util.concurrent.TimeUnit

class NewChallengePresenter(val view: NewChallengeContract.View, val analytics: FirebaseAnalytics) : NewChallengeContract.Presenter {
    private enum class State {
        OFF, SPEED, SIZE, DIALOG
    }

    private var menuState = State.OFF
    private var searchStart = 0L
    private lateinit var timer: Disposable
    private var challenge : AutomatchChallenge? = null
    private var searchSpeed: Speed? = null
    private var searchSizes: List<Size>? = null

    override fun onMainFabClicked() {
        menuState = when(menuState) {
            State.OFF -> {
                analytics.logEvent("new_game_clicked", null)
                view.showSpeedMenu().subscribe()
                State.SPEED
            }
            State.SPEED -> {
                analytics.logEvent("new_game_cancelled_on_speed", null)
                view.hideSpeedMenu().subscribe()
                State.OFF
            }
            State.SIZE -> {
                analytics.logEvent("new_game_cancelled_on_size", null)
                view.hideSizeMenu().subscribe()
                State.OFF
            }
            else -> menuState
        }
        view.setFadeOutState(menuState != State.OFF)

    }

    override fun onSpeedSelected(speed: Speed) {
        analytics.logEvent("new_game_speed_clicked", Bundle().apply { putString("SPEED", speed.toString()) })
        view.hideSpeedMenu()
                .doOnComplete { menuState = State.SIZE }
                .andThen(view.showSizeMenu())
                .subscribe()
    }

    override fun onSizeSelected(size: Size) {
        analytics.logEvent("new_game_size_clicked", Bundle().apply { putString("SIZE", size.toString()) })
        view.hideSizeMenu().andThen(view.hideFab()).subscribe(this::startSearch)
    }

    private fun startSearch() {
    }

    override fun onStartSearch(sizes: List<Size>, speed: Speed) {
        searchSizes = sizes
        searchSpeed = speed
        val params = Bundle().apply {
            putString("SPEED", speed.toString())
            putString("SIZE", getSizesString())
        }
        analytics.logEvent("new_game_search", params)
        showDialog()
        challenge = OGSServiceImpl.instance.startGameSearch(sizes, speed)
        challenge?.start?.subscribe {
            val elapsedSeconds = (System.currentTimeMillis() - searchStart) / 1000
            analytics.logEvent("new_game_found", params.apply { putLong("ELAPSED", elapsedSeconds) })
            view.cancelDialog()
            view.navigateToGame(it.game_id)
        }
    }

    private fun showDialog() {
        view.setFadeOutState(false)
        searchStart = System.currentTimeMillis()
        view.showSearchDialog()
        timer = Observable.interval(0, 1, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { updateDialogText() }
        menuState = State.DIALOG
    }

    private fun updateDialogText() {
        val elapsedSeconds = (System.currentTimeMillis() - searchStart) / 1000
        view.updateDialogText(Html.fromHtml("Game size: <b>${getSizesString()}</b><br/>Speed: <b>$searchSpeed</b><br/>Time elapsed: ${elapsedSeconds}s"))
    }

    private fun getSizesString(): String {
        val sb = StringBuffer()
        searchSizes?.forEach {
            if(!sb.isEmpty()) {
                sb.append(',')
            }
            sb.append(it.toString())
        }
        return sb.toString()
    }
    override fun onDialogCancelled() {
        val elapsedSeconds = (System.currentTimeMillis() - searchStart) / 1000
        val params = Bundle().apply {
            putString("SPEED", searchSpeed.toString())
            putString("SIZE", getSizesString())
            putLong("ELAPSED", elapsedSeconds)
        }
        analytics.logEvent("new_game_cancel", params)
        challenge?.let(OGSServiceImpl.instance::cancelAutomatchChallenge)
        menuState = State.OFF
        timer.dispose()
        view.showFab().subscribe()
    }
}
