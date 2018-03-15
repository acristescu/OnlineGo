package io.zenandroid.onlinego.newchallenge

import io.reactivex.Completable
import io.zenandroid.onlinego.ogs.Size
import io.zenandroid.onlinego.ogs.Speed

/**
 * Created by alex on 22/02/2018.
 */
interface NewChallengeContract {

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
        fun cancelDialog()
        fun navigateToGame(gameId: Long)
    }
    interface Presenter {
        fun onMainFabClicked()
        fun onSpeedSelected(speed: Speed)
        fun onSizeSelected(size: Size)
        fun onDialogCancelled()
    }
}