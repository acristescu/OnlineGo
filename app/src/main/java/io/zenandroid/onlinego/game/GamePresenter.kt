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
    private var activeGame: Boolean = false
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

        subscriptions.add(service.restApi.fetchGame(game.id)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
                .subscribe(this::processGame))

        subscriptions.add(view.cellSelection
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
                .subscribe(this::onUserSelectedCell))

        subscriptions.add(view.cellHotTrack
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
                .subscribe(this::onUserHotTrackedCell))

    }

    private fun processGame(game: Game) {
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
        view.confirmMoveUIVisible = true
    }

    override fun onDiscardButtonPressed() {
        candidateMove = null
        view.showCandidateMove(null)
        view.confirmMoveUIVisible = false
    }

    override fun onConfirmButtonPressed() {
        view.interactive = false
        candidateMove?.let { gameConnection.submitMove(it) }
        candidateMove = null
        view.confirmMoveUIVisible = false
    }

    private fun processGameData(gameData: GameData) {
        this.gameData = gameData

        if(!detailedPlayerDetailsSet) {
            view.whitePlayer = gameData.players?.white
            view.blackPlayer = gameData.players?.black
        }

        activeGame = (gameData.phase != Game.Phase.FINISHED) && ((game.blackId == userId) || (game.whiteId == userId))
        view.activeUIVisible = activeGame

        currentShownMove = gameData.moves.size
        refreshData()
        view.title = "${gameData.players?.black?.username} vs ${gameData.players?.white?.username}"
    }

    private fun refreshData() {
        currentPosition = RulesManager.replay(gameData)
        view.position = currentPosition
        determineHistoryParameters()
        when(gameData.phase) {
            Game.Phase.PLAY -> {
                val toMove = if (currentPosition.lastPlayerToMove == StoneType.BLACK) gameData.players?.black
                else gameData.players?.white
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