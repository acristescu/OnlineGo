package io.zenandroid.onlinego.newchallenge

import android.text.Html
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import java.util.concurrent.TimeUnit

class NewChallengePresenter(val view: NewChallengeContract.View) : NewChallengeContract.Presenter {
    private enum class State {
        OFF, SPEED, SIZE, DIALOG
    }

    private var menuState = State.OFF
    private lateinit var selectedSpeed: NewChallengeContract.Speed
    private lateinit var selectedSize: NewChallengeContract.Size
    private var searchStart = 0L
    private lateinit var timer: Disposable

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

    override fun onSpeedSelected(speed: NewChallengeContract.Speed) {
        selectedSpeed = speed
        view.hideSpeedMenu()
                .doOnComplete { menuState = State.SIZE }
                .andThen(view.showSizeMenu())
                .subscribe()
    }

    override fun onSizeSelected(size: NewChallengeContract.Size) {
        selectedSize = size
        view.hideSizeMenu().andThen(view.hideFab()).subscribe(this::showDialog)
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
        view.updateDialogText(Html.fromHtml("Game size: <b>$selectedSize</b><br/>Speed: <b>$selectedSpeed</b><br/>Time elapsed: ${elapsedSeconds}s"))
    }

    override fun subscribe() {}
    override fun unsubscribe() {}

    override fun onDialogCancelled() {
        menuState = State.OFF
        timer.dispose()
        view.showFab().subscribe()
    }
}
