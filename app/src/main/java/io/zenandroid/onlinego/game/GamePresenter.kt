package io.zenandroid.onlinego.game

import android.graphics.Point
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
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
        private val game: Game
) : GameContract.Presenter {
    private val subscriptions = CompositeDisposable()
    private lateinit var gameData: GameData
    private lateinit var gameConnection: GameConnection
    private var currentPosition = Position(19)

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
        val validMove = currentPosition.clone().makeMove(nextToMove, point)
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
        view.blackRank = formatRank(gameData.players?.black?.rank)
        view.whiteName = gameData.players?.white?.username
        view.whiteRank = formatRank(gameData.players?.white?.rank)

        refreshData()
    }

    private fun refreshData() {
        currentPosition = Position(19)
        var turn = StoneType.BLACK
        for (move in gameData.moves!!) {
            currentPosition.makeMove(turn, Point(move[0], move[1]))
            turn = if (turn == StoneType.BLACK) StoneType.WHITE else StoneType.BLACK
        }
        view.position = currentPosition
        view.highlightBlackName = turn == StoneType.BLACK
        view.highlightWhiteName = turn == StoneType.WHITE
    }

    private fun processMove(move: Move) {
        val newMoves = gameData.moves?.toMutableList() ?: mutableListOf()
        newMoves += move.move
        gameData.moves = newMoves
        refreshData()
    }

    private fun onClock(clock: Clock) {
        gameData.clock = clock

        view.interactive = clock.current_player == OGSService.instance.uiConfig?.user?.id
    }

    override fun unsubscribe() {
        subscriptions.clear()
    }

    private fun formatRank(rank: Int?): String {
        return when(rank) {
            null -> "?"
            in 0 .. 29 -> "${30 - rank}k"
            in 30 .. 100 -> "${(rank - 29)}d"
            else -> "???"
        }
    }
}