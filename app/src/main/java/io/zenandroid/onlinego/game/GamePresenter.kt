package io.zenandroid.onlinego.game

import android.graphics.Point
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.gamelogic.RulesManager
import io.zenandroid.onlinego.model.Position
import io.zenandroid.onlinego.model.StoneType
import io.zenandroid.onlinego.model.ogs.Game
import io.zenandroid.onlinego.ogs.*

/**
 * Created by alex on 10/11/2017.
 */
class GamePresenter(
        private val view: GameContract.View,
        private val service: OGSService,
        private var game: Game
) : GameContract.Presenter {
    private val subscriptions = CompositeDisposable()
    private lateinit var gameData: GameData
    private lateinit var gameConnection: GameConnection
    private var myGame: Boolean = false
    private var currentPosition = Position(19)
    private val userId = OGSService.instance.uiConfig?.user?.id
    private var detailedPlayerDetailsSet = false
    private var currentShownMove = -1

    private var candidateMove: Point? = null

    override fun subscribe() {
        view.boardSize = game.width

        gameConnection = service.connectToGame(game.id)
        subscriptions.add(gameConnection)
        subscriptions.add(gameConnection.gameData
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
                .subscribe(this::processGameData))
        subscriptions.add(gameConnection.moves
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
                .subscribe(this::processMove))
        subscriptions.add(gameConnection.clock
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
                .subscribe(this::onClock))
        subscriptions.add(gameConnection.phase
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
                .subscribe(this::onPhase))
        subscriptions.add(gameConnection.removedStones
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
                .subscribe(this::onRemovedStones))

        subscriptions.add(service.restApi.fetchGame(game.id)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
                .subscribe(this::processRESTGame))

        subscriptions.add(view.cellSelection
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
                .subscribe(this::onUserSelectedCell))

        subscriptions.add(view.cellHotTrack
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
                .subscribe(this::onUserHotTrackedCell))

    }

    private fun processRESTGame(game: Game) {
        this.game = game
        processGameData(game.gamedata)

        detailedPlayerDetailsSet = true
        view.whitePlayer = game.players?.white
        view.blackPlayer = game.players?.black
    }

    private fun onUserHotTrackedCell(point: Point) {
        val nextToMove = currentPosition.lastPlayerToMove?.opponent ?: StoneType.BLACK
        val validMove = RulesManager.makeMove(currentPosition, nextToMove, point) != null
        if(validMove) {
            candidateMove = point
            view.showCandidateMove(point, nextToMove)
        }
    }

    private fun onUserSelectedCell(point: Point) {
        showConfirmMoveControls()
    }

    override fun onDiscardButtonPressed() {
        if(gameData.phase == Game.Phase.PLAY) {
            candidateMove = null
            view.showCandidateMove(null)
            showPlayControls()
        } else {
            gameConnection.rejectRemovedStones()
        }
    }

    override fun onConfirmButtonPressed() {
        view.interactive = false
        candidateMove?.let { gameConnection.submitMove(it) }
        candidateMove = null
        showPlayControls()
    }

    private fun processGameData(gameData: GameData) {
        this.gameData = gameData

        if(!detailedPlayerDetailsSet) {
            view.whitePlayer = gameData.players?.white
            view.blackPlayer = gameData.players?.black
        }

        myGame = (game.blackId == userId) || (game.whiteId == userId)
        showControls()
        configureBoard()

        currentShownMove = gameData.moves.size
        refreshData()
        view.title = "${gameData.players?.black?.username} vs ${gameData.players?.white?.username}"
    }

    private fun configureBoard() {
        when(gameData.phase) {
            Game.Phase.PLAY -> {
                view.showLastMove = true
                view.showTerritory = false
                view.fadeOutRemovedStones = false
            }
            Game.Phase.STONE_REMOVAL -> {
                view.showLastMove = false
                view.showTerritory = true
                view.fadeOutRemovedStones = true
            }
            Game.Phase.FINISHED -> {
                view.showLastMove = false
                view.showTerritory = true
                view.fadeOutRemovedStones = true
            }
        }
    }

    private fun showControls() {
        if(myGame && gameData.phase == Game.Phase.PLAY) {
            showPlayControls()
        } else if(myGame && gameData.phase == Game.Phase.STONE_REMOVAL) {
            showStoneRemovalControls()
        } else {
            showSpectateControls()
        }
    }

    private fun showPlayControls() {
        view.nextButtonVisible = true
        view.previousButtonVisible = true
        view.chatButtonVisible = true
        view.passButtonVisible = true
        view.resignButtonVisible = true

        view.confirmButtonVisible = false
        view.discardButtonVisible = false
        view.autoButtonVisible = false
    }

    private fun showStoneRemovalControls() {
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
        view.nextButtonVisible = false
        view.previousButtonVisible = false
        view.chatButtonVisible = false
        view.passButtonVisible = false
        view.resignButtonVisible = false

        view.confirmButtonVisible = true
        view.discardButtonVisible = true
        view.autoButtonVisible = false
    }

    private fun refreshData() {
        currentPosition = RulesManager.replay(gameData)
//        if(gameData.phase == Game.Phase.STONE_REMOVAL) {
//            RulesManager.determineTerritory(currentPosition)
//        }
        view.position = currentPosition
        determineHistoryParameters()
        when(gameData.phase) {
            Game.Phase.PLAY -> {
                val toMove =
                        if (currentPosition.lastPlayerToMove == StoneType.WHITE) gameData.players?.black else gameData.players?.white
                view.subTitle = "${toMove?.username}'s turn"
            }
            Game.Phase.STONE_REMOVAL -> {
                view.subTitle = "Stone removal"
            }
            Game.Phase.FINISHED -> {
                view.subTitle = "Finished"
            }
        }
//        view.highlightBlackName = turn == StoneType.BLACK
//        view.highlightWhiteName = turn == StoneType.WHITE
    }

    private fun processMove(move: Move) {
        candidateMove = null
        view.showCandidateMove(null)
        val newMoves = gameData.moves.toMutableList()
        newMoves += move.move
        gameData.moves = newMoves
        currentShownMove = gameData.moves.size
        refreshData()
    }

    private fun onClock(clock: Clock) {
        gameData.clock = clock

        view.interactive = gameData.phase == Game.Phase.PLAY && clock.current_player == userId
        view.passButtonEnabled = gameData.phase == Game.Phase.PLAY && clock.current_player == userId
    }

    private fun onPhase(phase: Game.Phase) {
        game.phase = phase
        gameData.phase = phase

        processGameData(gameData)
    }

    private fun onRemovedStones(removedStones: RemovedStones) {
        gameData.removed = removedStones.all_removed
        processGameData(gameData)
    }

    override fun onResignConfirmed() {
        gameConnection.resign()
    }


    override fun onPassConfirmed() {
        gameConnection.submitMove(Point(-1, -1))
    }

    override fun onNextButtonPressed() {
        if(currentShownMove < gameData.moves.size) {
            currentShownMove ++
        }

        currentShownMove.coerceIn(0, gameData.moves.size)
        determineHistoryParameters()
        view.position = RulesManager.replay(gameData, currentShownMove)
    }

    override fun onPreviousButtonPressed() {
        currentShownMove--

        currentShownMove.coerceIn(0, gameData.moves.size)
        determineHistoryParameters()
        view.position = RulesManager.replay(gameData, currentShownMove)
    }

    private fun determineHistoryParameters() {
        view.nextButtonEnabled = currentShownMove != gameData.moves.size
        view.previousButtonEnabled = currentShownMove > 0
    }

    override fun unsubscribe() {
        subscriptions.clear()
    }
}