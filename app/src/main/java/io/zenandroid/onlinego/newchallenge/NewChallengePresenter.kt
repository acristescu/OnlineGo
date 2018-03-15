package io.zenandroid.onlinego.newchallenge

import android.text.Html
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.zenandroid.onlinego.ogs.AutomatchChallenge
import io.zenandroid.onlinego.ogs.OGSServiceImpl
import io.zenandroid.onlinego.ogs.Size
import io.zenandroid.onlinego.ogs.Speed
import java.util.concurrent.TimeUnit

class NewChallengePresenter(val view: NewChallengeContract.View) : NewChallengeContract.Presenter {
    private enum class State {
        OFF, SPEED, SIZE, DIALOG
    }

    private var menuState = State.OFF
    private lateinit var selectedSpeed: Speed
    private lateinit var selectedSize: Size
    private var searchStart = 0L
    private lateinit var timer: Disposable
    private var challenge : AutomatchChallenge? = null

    override fun onMainFabClicked() {
        menuState = when(menuState) {
            State.OFF -> {
                view.showSpeedMenu().subscribe()
                State.SPEED
            }
            State.SPEED -> {
                view.hideSpeedMenu().subscribe()
                State.OFF
            }
            State.SIZE -> {
                view.hideSizeMenu().subscribe()
                State.OFF
            }
            else -> menuState
        }
        view.setFadeOutState(menuState != State.OFF)

    }

    override fun onSpeedSelected(speed: Speed) {
        selectedSpeed = speed
        view.hideSpeedMenu()
                .doOnComplete { menuState = State.SIZE }
                .andThen(view.showSizeMenu())
                .subscribe()
    }

    override fun onSizeSelected(size: Size) {
        selectedSize = size
        view.hideSizeMenu().andThen(view.hideFab()).subscribe(this::startSearch)
    }

    private fun startSearch() {
        showDialog()
        challenge = OGSServiceImpl.instance.startGameSearch(selectedSize, selectedSpeed)
        challenge?.start?.subscribe {
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
        view.updateDialogText(Html.fromHtml("Game size: <b>${selectedSize.getText()}</b><br/>Speed: <b>$selectedSpeed</b><br/>Time elapsed: ${elapsedSeconds}s"))
    }

    override fun onDialogCancelled() {
        challenge?.let(OGSServiceImpl.instance::cancelAutomatchChallenge)
        menuState = State.OFF
        timer.dispose()
        view.showFab().subscribe()
    }
}
