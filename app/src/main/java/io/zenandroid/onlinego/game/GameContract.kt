package io.zenandroid.onlinego.game

import android.graphics.Point
import io.reactivex.Observable
import io.zenandroid.onlinego.model.Position
import io.zenandroid.onlinego.model.StoneType
import io.zenandroid.onlinego.model.local.DbPlayer

/**
 * Created by alex on 10/11/2017.
 */
interface GameContract {

    interface View {
        var boardSize: Int
        var whitePlayer: DbPlayer?
        var blackPlayer: DbPlayer?
        var position: Position?
//        var highlightBlackName: Boolean
//        var highlightWhiteName: Boolean
        val cellSelection: Observable<Point>
        val cellHotTrack: Observable<Point>
        var interactive: Boolean
        var passButtonEnabled: Boolean
        fun showCandidateMove(point: Point?, nextToMove: StoneType? = null)
        var previousButtonEnabled: Boolean
        var nextButtonEnabled: Boolean
        var title: String?
        var subTitle: String?
        var nextButtonVisible: Boolean
        var previousButtonVisible: Boolean
        var chatButtonVisible: Boolean
        var passButtonVisible: Boolean
        var resignButtonVisible: Boolean
        var confirmButtonVisible: Boolean
        var discardButtonVisible: Boolean
        var autoButtonVisible: Boolean
        var bottomBarVisible: Boolean
        var showLastMove: Boolean
        var showTerritory: Boolean
        var fadeOutRemovedStones: Boolean
        var whiteTimer: GamePresenter.TimerDetails?
        var blackTimer: GamePresenter.TimerDetails?
        var activePlayer: StoneType?
        fun showError(t: Throwable)
        var komi: Float?
        fun setLoading(loading: Boolean)
        fun showFinishedDialog()
        fun showYouWinDialog()
        fun showYouLoseDialog()
    }

    interface Presenter {
        fun subscribe()
        fun unsubscribe()
        fun onResignConfirmed()
        fun onPassConfirmed()

        fun onPreviousButtonPressed()
        fun onNextButtonPressed()
        fun onDiscardButtonPressed()
        fun onConfirmButtonPressed()
        fun onAutoButtonPressed()
    }
}