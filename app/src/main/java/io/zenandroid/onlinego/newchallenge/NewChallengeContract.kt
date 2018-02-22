package io.zenandroid.onlinego.newchallenge

import io.reactivex.Completable

/**
 * Created by alex on 22/02/2018.
 */
interface NewChallengeContract {
    enum class Size { SMALL, MEDIUM, LARGE }
    enum class Speed { BLITZ, NORMAL, LONG }

    interface View {
        fun showSpeedMenu(): Completable
        fun hideSpeedMenu(): Completable
        fun showSizeMenu(): Completable
        fun hideSizeMenu(): Completable
        fun setFadeOutState(fadedOut: Boolean)
        fun hideFab(): Completable
        fun showSearchDialog()
        fun showFab(): Completable
        fun updateDialogText(message: CharSequence)
    }
    interface Presenter {
        fun subscribe()
        fun unsubscribe()
        fun onMainFabClicked()
        fun onSpeedSelected(speed: Speed)
        fun onSizeSelected(size: Size)
        fun onDialogCancelled()
    }
}