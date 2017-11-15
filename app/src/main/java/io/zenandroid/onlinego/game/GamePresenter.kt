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
import io.zenandroid.onlinego.utils.egfToRank
import io.zenandroid.onlinego.utils.formatRank

/**
 * Created by alex on 10/11/2017.
 */
class GamePresenter(
        private val view: GameContract.View,
        private val service: OGSService,
        private val game: Game
) : GameContract.Presenter {
    private val subscriptions = CompositeDisposable()
    private lateinit var gameData: GameData
    private lateinit var gameConnection: GameConnection
    private var activeGame: Boolean = false
    private var currentPosition = Position(19)
    private val userId = OGSService.instance.uiConfig?.user?.id

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

        subscriptions.add(view.cellSelection
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
                .subscribe(this::onUserSelectedCell))

    }

    private fun onUserSelectedCell(point: Point) {
        val nextToMove = currentPosition.lastPlayerToMove?.opponent ?: StoneType.BLACK
        val validMove = RulesManager.makeMove(currentPosition, nextToMove, point) != null
        if(!validMove) {
            view.unselectMove()
        } else {
            //
            // Move this to onSubmitButton
            //
            gameConnection.submitMove(point)
        }
    }

    private fun processGameData(gameData: GameData) {
        this.gameData = gameData

        view.blackName = gameData.players?.black?.username
        view.blackRank = formatRank(egfToRank(gameData.players?.black?.egf))
        view.whiteName = gameData.players?.white?.username
        view.whiteRank = formatRank(egfToRank(gameData.players?.white?.egf))

        activeGame = (gameData.phase != Game.Phase.FINISHED) && ((game.black?.id == userId) || (game.white?.id == userId))
        view.activeUIVisible = activeGame

        refreshData()
    }

    private fun refreshData() {
        currentPosition = Position(19)
        var turn = StoneType.BLACK
        for (move in gameData.moves) {
            currentPosition = RulesManager.makeMove(currentPosition, turn, Point(move[0], move[1]))!!
            turn = if (turn == StoneType.BLACK) StoneType.WHITE else StoneType.BLACK
        }
        view.position = currentPosition
        view.highlightBlackName = turn == StoneType.BLACK
        view.highlightWhiteName = turn == StoneType.WHITE
    }

    private fun processMove(move: Move) {
        val newMoves = gameData.moves.toMutableList()
        newMoves += move.move
        gameData.moves = newMoves
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

    override fun unsubscribe() {
        subscriptions.clear()
    }
}