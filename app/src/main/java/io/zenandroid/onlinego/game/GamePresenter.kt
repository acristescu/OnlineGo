package io.zenandroid.onlinego.game

import android.graphics.Point
import android.util.Log
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.SingleEmitter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.gamelogic.RulesManager
import io.zenandroid.onlinego.model.Position
import io.zenandroid.onlinego.model.StoneType
import io.zenandroid.onlinego.model.local.DbGame
import io.zenandroid.onlinego.model.ogs.Game
import io.zenandroid.onlinego.ogs.ActiveGameRepository
import io.zenandroid.onlinego.ogs.GameConnection
import io.zenandroid.onlinego.ogs.OGSService
import io.zenandroid.onlinego.utils.computeTimeLeft
import java.util.concurrent.TimeUnit

/**
 * Created by alex on 10/11/2017.
 */
class GamePresenter(
        private val view: GameContract.View,
        private val service: OGSService,
        private val gameRepository: ActiveGameRepository,
        private val gameId: Long,
        private val gameSize: Int
) : GameContract.Presenter {

    companion object {
        val TAG = GamePresenter::class.java.simpleName
    }

    private val subscriptions = CompositeDisposable()
    private var gameConnection: GameConnection = GameConnection(gameId)
    private var myGame: Boolean = false
    private var currentPosition = Position(19)
    private val userId = service.uiConfig?.user?.id
    private var currentShownMove = -1
    private var game: DbGame? = null

    private var finishedDialogShown = false

    private var candidateMove: Point? = null

    private var deferredTerritoryComputation: Disposable? = null

    override fun subscribe() {
        view.setLoading(true)
        view.boardSize = gameSize

        service.resendAuth()

        subscriptions.add(
                gameRepository.monitorGame(gameId)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
                        .subscribe(this::onGameChanged)
        )
        subscriptions.add(view.cellSelection
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
                .subscribe(this::onUserSelectedCell))

        subscriptions.add(view.cellHotTrack
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
                .subscribe(this::onUserHotTrackedCell))

        subscriptions.add(Observable.interval(100, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { clockTick() }
        )

    }

    private fun onError(t: Throwable) {
        Log.e(TAG, t.message, t)
        view.showError(t)
    }

    private fun onGameChanged(newGame: DbGame) {
        if(newGame == game) {
            return
        }

        view.setLoading(false)

        view.whitePlayer = newGame.whitePlayer
        view.blackPlayer = newGame.blackPlayer
        view.komi = newGame.komi

        myGame = (newGame.blackPlayer.id == userId) || (newGame.whitePlayer.id == userId)

        showControls()
        configureBoard()

        currentShownMove = newGame.moves?.size ?: -1
        refreshUI(newGame)
        view.title = "${game?.blackPlayer?.username} vs ${game?.whitePlayer?.username}"

        view.passButtonEnabled = newGame.phase == Game.Phase.PLAY && newGame.playerToMoveId == userId

        if(newGame.moves != game?.moves) {
            candidateMove = null
            view.showCandidateMove(null)
            currentShownMove = newGame.moves?.size ?: 0
        }
        game = newGame
    }

    private fun onUserHotTrackedCell(point: Point) {
        if(game?.phase == Game.Phase.PLAY) {
            val validMove = RulesManager.makeMove(currentPosition, currentPosition.nextToMove, point) != null
            if (validMove) {
                candidateMove = point
                view.showCandidateMove(point, currentPosition.nextToMove)
            }
        }
    }

    private fun onUserSelectedCell(point: Point) {
        if(game?.phase == Game.Phase.PLAY) {
            if(candidateMove != null) {
                showConfirmMoveControls()
            }
        } else {
            val newPos = currentPosition.clone()
            RulesManager.toggleRemoved(newPos, point)
            var delta = newPos.removedSpots - currentPosition.removedSpots
            var removing = true
            if(delta.isEmpty()) {
                delta = currentPosition.removedSpots - newPos.removedSpots
                removing = false
            }
            if(delta.isNotEmpty()) {
                gameConnection.submitRemovedStones(delta, removing)
            }
        }
    }

    override fun onDiscardButtonPressed() {
        if(game?.phase == Game.Phase.PLAY) {
            candidateMove = null
            view.showCandidateMove(null)
            showPlayControls()
        } else {
            gameConnection.rejectRemovedStones()
        }
    }

    override fun onConfirmButtonPressed() {
        if(game?.phase == Game.Phase.PLAY) {
            view.interactive = false
            candidateMove?.let { gameConnection.submitMove(it) }
            candidateMove = null
            showPlayControls()
        } else {
            gameConnection.acceptRemovedStones(currentPosition.removedSpots)
        }
    }

    private fun configureBoard() {
        when(game?.phase) {
            Game.Phase.PLAY -> {
                view.showLastMove = true
                view.showTerritory = false
                view.fadeOutRemovedStones = false
                view.interactive = game?.playerToMoveId == userId
            }
            Game.Phase.STONE_REMOVAL -> {
                view.showLastMove = false
                view.showTerritory = true
                view.fadeOutRemovedStones = true
                view.interactive = true
            }
            Game.Phase.FINISHED -> {
                view.showLastMove = false
                view.showTerritory = true
                view.fadeOutRemovedStones = true
                view.interactive = false
            }
        }
    }

    private fun showControls() {
        if(myGame && game?.phase == Game.Phase.PLAY) {
            showPlayControls()
        } else if(myGame && game?.phase == Game.Phase.STONE_REMOVAL) {
            showStoneRemovalControls()
        } else {
            showSpectateControls()
        }
    }

    private fun showPlayControls() {
        view.bottomBarVisible = true
        view.nextButtonVisible = true
        view.previousButtonVisible = true
        view.chatButtonVisible = true
        view.passButtonVisible = true
        view.resignButtonVisible = true

        view.confirmButtonVisible = false
        view.discardButtonVisible = false
        view.autoButtonVisible = false

        view.passButtonEnabled = game?.playerToMoveId == userId
    }

    override fun onAutoButtonPressed() {
        if(game?.phase != Game.Phase.STONE_REMOVAL) {
            return
        }
        val newPos = currentPosition.clone()
        newPos.clearAllRemovedSpots()
        RulesManager.determineTerritory(newPos)
        gameConnection.submitRemovedStones(currentPosition.removedSpots, false)
        gameConnection.submitRemovedStones(newPos.removedSpots, true)

    }

    private fun showStoneRemovalControls() {
        view.bottomBarVisible = true
        view.nextButtonVisible = false
        view.previousButtonVisible = false
        view.chatButtonVisible = true
        view.passButtonVisible = false
        view.resignButtonVisible = false

        view.confirmButtonVisible = true
        view.discardButtonVisible = true
        view.autoButtonVisible = true
    }

    private fun showSpectateControls() {
        view.bottomBarVisible = true
        view.nextButtonVisible = true
        view.previousButtonVisible = true
        view.chatButtonVisible = true
        view.passButtonVisible = false
        view.resignButtonVisible = false

        view.confirmButtonVisible = false
        view.discardButtonVisible = false
        view.autoButtonVisible = false
    }

    private fun showConfirmMoveControls() {
        view.bottomBarVisible = true
        view.nextButtonVisible = false
        view.previousButtonVisible = false
        view.chatButtonVisible = false
        view.passButtonVisible = false
        view.resignButtonVisible = false

        view.confirmButtonVisible = true
        view.discardButtonVisible = true
        view.autoButtonVisible = false
    }

    private fun computeTerritoryAsync(game: DbGame) {
        deferredTerritoryComputation?.dispose()
        deferredTerritoryComputation = Single.create { emitter: SingleEmitter<Position> ->
            emitter.onSuccess(RulesManager.replay(game, computeTerritory = true))
        }
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { pos ->
                    view.position = pos
                }

         deferredTerritoryComputation?.let(subscriptions::add)
    }

    private fun refreshUI(game: DbGame) {
        val newPos = RulesManager.replay(game, computeTerritory = false)
        if(newPos != currentPosition) {
            currentPosition = newPos
            view.position = currentPosition

            val shouldComputeTerritory = game.phase == Game.Phase.STONE_REMOVAL || game.phase == Game.Phase.FINISHED
            if (shouldComputeTerritory) {
                computeTerritoryAsync(game)
            }
        }

        determineHistoryParameters()
        when(game.phase) {
            Game.Phase.PLAY -> {
                val toMove =
                        if (currentPosition.nextToMove == StoneType.BLACK)
                            game.blackPlayer
                        else game.whitePlayer
                view.subTitle = "${toMove.username}'s turn"
            }
            Game.Phase.STONE_REMOVAL -> {
                view.subTitle = "Stone removal"
            }
            Game.Phase.FINISHED -> {
                if(!finishedDialogShown) {
                    finishedDialogShown = true
                    when {
                        !myGame ->
                            view.showFinishedDialog()
                        game.blackPlayer.id == userId && game.whiteLost == true ->
                            view.showYouWinDialog()
                        else ->
                            view.showYouLoseDialog()
                    }
                }
                view.subTitle = "Finished"
            }
        }
        view.activePlayer = currentPosition.nextToMove
    }

    private fun clockTick() {
        game?.clock?.let { clock ->
            if (clock.startMode == true) {
                println("start mode not implemented yet")
                //TODO
//            } else if (clock.pause_control != null) {
//                println("pause not implemented yet")
                //TODO
            } else {
                view.whiteTimer = computeTimeLeft(clock, clock.whiteTimeSimple, clock.whiteTime, game?.playerToMoveId == game?.whitePlayer?.id)
                view.blackTimer = computeTimeLeft(clock, clock.blackTimeSimple, clock.blackTime, game?.playerToMoveId == game?.blackPlayer?.id)
            }
        }
    }

    override fun onResignConfirmed() {
        gameConnection.resign()
    }


    override fun onPassConfirmed() {
        gameConnection.submitMove(Point(-1, -1))
    }

    override fun onNextButtonPressed() {
        game?.let { game ->
            game.moves?.let { moves ->
                currentShownMove++
                currentShownMove = currentShownMove.coerceIn(0, moves.size)
                determineHistoryParameters()
                view.position = RulesManager.replay(game, currentShownMove, false)
            }
        }
    }

    override fun onPreviousButtonPressed() {
        game?.let { game ->
            game.moves?.let { moves ->
                currentShownMove--
                currentShownMove = currentShownMove.coerceIn(0, moves.size)
                determineHistoryParameters()
                view.position = RulesManager.replay(game, currentShownMove, false)
            }
        }
    }

    private fun determineHistoryParameters() {
        game?.let { game ->
            view.nextButtonEnabled = currentShownMove != game.moves?.size
            view.previousButtonEnabled = currentShownMove > 0
        }
    }

    override fun unsubscribe() {
        subscriptions.clear()
    }

    class TimerDetails {
        var expired = false
        var firstLine: String? = null
        var secondLine: String? = null
        var timeLeft: Long? = null
    }
}