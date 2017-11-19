package io.zenandroid.onlinego.game

import android.graphics.Point
import io.reactivex.Observable
import io.zenandroid.onlinego.model.Position
import io.zenandroid.onlinego.model.StoneType
import io.zenandroid.onlinego.model.ogs.Player

/**
 * Created by alex on 10/11/2017.
 */
interface GameContract {

    interface View {
        var boardSize: Int
        var whitePlayer: Player?
        var blackPlayer: Player?
        var position: Position?
//        var highlightBlackName: Boolean
//        var highlightWhiteName: Boolean
        val cellSelection: Observable<Point>
        val cellHotTrack: Observable<Point>
        var interactive: Boolean
        var activeUIVisible: Boolean
        var passButtonEnabled: Boolean
        fun showCandidateMove(point: Point?, nextToMove: StoneType? = null)
        var confirmMoveUIVisible: Boolean
        var previousButtonEnabled: Boolean
        var nextButtonEnabled: Boolean
        var title: String?
        var subTitle: String?
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
    }
}