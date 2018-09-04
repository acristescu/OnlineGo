package io.zenandroid.onlinego.game

import android.graphics.Point
import io.reactivex.Observable
import io.zenandroid.onlinego.model.Position
import io.zenandroid.onlinego.model.StoneType
import io.zenandroid.onlinego.model.local.Message
import io.zenandroid.onlinego.model.local.Player
import io.zenandroid.onlinego.statuschips.Chip

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
        var passButtonEnabled: Boolean
        fun showCandidateMove(point: Point?, nextToMove: StoneType? = null)
        var previousButtonEnabled: Boolean
        var nextButtonEnabled: Boolean
        var title: String?
        var nextButtonVisible: Boolean
        var analyzeButtonVisible: Boolean
        var previousButtonVisible: Boolean
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
        var chatMyId: Long?

        fun setLoading(loading: Boolean)
        fun showFinishedDialog()
        fun showYouWinDialog()
        fun showYouLoseDialog()
        fun setChips(chips: List<Chip>)
        fun showInfoDialog(title: String, contents: String)
        fun setBlackPlayerPassed(passed: Boolean)
        fun setWhitePlayerPassed(passed: Boolean)
        fun setMessageList(messages: List<Message>)
        fun showChat()
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
        fun onAnalyzeButtonPressed()
        fun onChatClicked()
        fun onNewMessage(message: String)
    }
}