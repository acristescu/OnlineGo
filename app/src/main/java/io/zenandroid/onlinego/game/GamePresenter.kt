package io.zenandroid.onlinego.game

import android.graphics.Point
import android.util.Log
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.gamelogic.RulesManager
import io.zenandroid.onlinego.model.Position
import io.zenandroid.onlinego.model.StoneType
import io.zenandroid.onlinego.model.ogs.Game
import io.zenandroid.onlinego.ogs.*
import java.util.concurrent.TimeUnit

/**
 * Created by alex on 10/11/2017.
 */
class GamePresenter(
        private val view: GameContract.View,
        private val service: OGSService,
        private var game: Game
) : GameContract.Presenter {
    private val subscriptions = CompositeDisposable()
    private var gameData: GameData? = null
    private lateinit var gameConnection: GameConnection
    private var myGame: Boolean = false
    private var currentPosition = Position(19)
    private val userId = service.uiConfig?.user?.id
    private var detailedPlayerDetailsSet = false
    private var currentShownMove = -1
    private var clock: Clock? = null

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

        subscriptions.add(Observable.interval(100, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({clockTick()})
        )

    }

    private fun processRESTGame(game: Game) {
        this.game = game
        processGameData(game.gamedata)

        detailedPlayerDetailsSet = true
        view.whitePlayer = game.players?.white
        view.blackPlayer = game.players?.black
    }

    private fun onUserHotTrackedCell(point: Point) {
        if(gameData?.phase == Game.Phase.PLAY) {
            val nextToMove = currentPosition.lastPlayerToMove?.opponent ?: StoneType.BLACK
            val validMove = RulesManager.makeMove(currentPosition, nextToMove, point) != null
            if (validMove) {
                candidateMove = point
                view.showCandidateMove(point, nextToMove)
            }
        }
    }

    private fun onUserSelectedCell(point: Point) {
        if(gameData?.phase == Game.Phase.PLAY) {
            if(candidateMove != null) {
                showConfirmMoveControls()
            }
        } else {
            val newPos = currentPosition.clone()
            RulesManager.toggleRemoved(newPos, point)
            var delta = newPos.removedSpots.minus(currentPosition.removedSpots)
            var removing = true
            if(delta.isEmpty()) {
                delta = currentPosition.removedSpots.minus(newPos.removedSpots)
                removing = false
            }
            if(delta.isNotEmpty()) {
                gameConnection.submitRemovedStones(delta, removing)
            }
        }
    }

    override fun onDiscardButtonPressed() {
        if(gameData?.phase == Game.Phase.PLAY) {
            candidateMove = null
            view.showCandidateMove(null)
            showPlayControls()
        } else {
            gameConnection.rejectRemovedStones()
        }
    }

    override fun onConfirmButtonPressed() {
        if(gameData?.phase == Game.Phase.PLAY) {
            view.interactive = false
            candidateMove?.let { gameConnection.submitMove(it) }
            candidateMove = null
            showPlayControls()
        } else {
            gameConnection.acceptRemovedStones(currentPosition.removedSpots)
        }
    }

    private fun processGameData(gameData: GameData) {
        this.gameData = gameData
        if(clock == null) {
            clock = gameData.clock
        }

        if(!detailedPlayerDetailsSet) {
            view.whitePlayer = gameData.players?.white
            view.blackPlayer = gameData.players?.black
        }

        myGame = (game.blackId == userId) || (game.whiteId == userId)
        showControls()
        configureBoard()

        currentShownMove = gameData.moves.size
        refreshData(gameData)
        view.title = "${gameData.players?.black?.username} vs ${gameData.players?.white?.username}"
    }

    private fun configureBoard() {
        when(gameData?.phase) {
            Game.Phase.PLAY -> {
                view.showLastMove = true
                view.showTerritory = false
                view.fadeOutRemovedStones = false
                view.interactive = clock?.current_player == userId
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
        if(myGame && gameData?.phase == Game.Phase.PLAY) {
            showPlayControls()
        } else if(myGame && gameData?.phase == Game.Phase.STONE_REMOVAL) {
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

        view.passButtonEnabled = clock?.current_player == userId
    }

    override fun onAutoButtonPressed() {
        if(gameData?.phase != Game.Phase.STONE_REMOVAL) {
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

    private fun refreshData(gameData: GameData) {
        val shouldComputeTerritory = gameData.phase == Game.Phase.STONE_REMOVAL || gameData.phase == Game.Phase.FINISHED
        currentPosition = RulesManager.replay(gameData, computeTerritory = shouldComputeTerritory)
        view.position = currentPosition
        determineHistoryParameters()
        when(gameData.phase) {
            Game.Phase.PLAY -> {
                val toMove =
                        if (currentPosition.nextToMove == StoneType.BLACK)
                            gameData.players?.black
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
        view.activePlayer = currentPosition.nextToMove
    }

    private fun processMove(move: Move) {
        gameData?.let { gameData ->
            candidateMove = null
            view.showCandidateMove(null)
            val newMoves = gameData.moves.toMutableList()
            newMoves += move.move
            gameData.moves = newMoves
            currentShownMove = gameData.moves.size
            refreshData(gameData)
        }
    }

    private fun clockTick() {
        clock?.let { clock ->
            if (clock.start_mode) {
                println("start mode not implemented yet")
                //TODO
            } else if (clock.pause_control != null) {
                println("pause not implemented yet")
                //TODO
            } else {
                view.whiteTimer = computeTimeLeft(clock, clock.white_time, clock.current_player == clock.white_player_id)
                view.blackTimer = computeTimeLeft(clock, clock.black_time, clock.current_player == clock.black_player_id)
            }
        }
    }

    private fun computeTimeLeft(clock: Clock, playerTimeAny: Any, currentPlayer: Boolean): TimerDetails {
        val timer = TimerDetails()

        val now = System.currentTimeMillis()
        if(clock.receivedAt == 0L) {
            clock.receivedAt = now
        }
        val nowDelta = clock.receivedAt - clock.now
        val baseTime = clock.last_move + nowDelta
        var timeLeft = 0L
        if(playerTimeAny is Long) {
            // Simple timer
            timeLeft = playerTimeAny - if(currentPlayer) now else baseTime
        } else if (playerTimeAny is Map<*, *>) {

            val playerTime = Time.fromMap(playerTimeAny)
            timeLeft = baseTime + playerTime.thinking_time * 1000 - if(currentPlayer) now else baseTime
            if(playerTime.moves_left != null) {

                // Canadian timer
                if(timeLeft < 0 || playerTime.thinking_time == 0L) {
                    timeLeft = baseTime + (playerTime.thinking_time + playerTime.block_time!!) * 1000 - if(currentPlayer) now else baseTime
                }
                timer.secondLine = "+${formatMillis(playerTime.block_time!! * 1000)} / ${playerTime.moves_left}"
            } else if(playerTime.periods != null) {

                // Byo Yomi timer
                var periodsLeft = playerTime.periods
                if(timeLeft < 0 || playerTime.thinking_time == 0L) {
                    val periodOffset = Math.floor((-timeLeft / 1000.0) / playerTime.period_time!!).coerceAtLeast(0.0)

                    while(timeLeft < 0) {
                        timeLeft += playerTime.period_time * 1000
                    }

                    periodsLeft = playerTime.periods - periodOffset.toLong()
                    if(periodsLeft < 0) {
                        timeLeft = 0
                    }
                }
                if(!currentPlayer && timeLeft == 0L) {
                    timeLeft = playerTime.period_time!! * 1000
                }
                timer.secondLine = "$periodsLeft x ${formatMillis(playerTime.period_time!! * 1000)}"
            }
        } else {
            Log.e("GamePresenter", "Unknown clock object $playerTimeAny")
        }

        timer.expired = timeLeft <= 0
        timer.firstLine = formatMillis(timeLeft)
        return timer
    }

    internal fun formatMillis(millis: Long): String = when {
        millis < 10_000 -> "%.1fs".format(millis / 1000f)
        millis < 60_000 -> "%.0fs".format(millis / 1000f)
        millis < 3_600_000 -> "%d : %02d".format(millis / 60_000, (millis % 60_000) / 1000)
        millis < 24 * 3_600_000 -> "%dh %02dm".format(millis / 3_600_000, (millis % 3_600_000) / 60_000)
        millis < 7 * 24 * 3_600_000 -> "%d day(s)".format(millis / 86_400_000)
        else -> "%d week(s)".format(millis/(7 * 24 * 3_600_000))
    }

    private fun onClock(clock: Clock) {
        this.clock = clock

        gameData?.let (this::processGameData)
//        view.interactive = gameData.phase == Game.Phase.PLAY && clock.current_player == userId
//        view.passButtonEnabled = gameData.phase == Game.Phase.PLAY && clock.current_player == userId
    }

    private fun onPhase(phase: Game.Phase) {
        game.phase = phase
        gameData?.phase = phase

        gameData?.let (this::processGameData)

    }

    private fun onRemovedStones(removedStones: RemovedStones) {
        gameData?.removed = removedStones.all_removed
        gameData?.let (this::processGameData)
    }

    override fun onResignConfirmed() {
        gameConnection.resign()
    }


    override fun onPassConfirmed() {
        gameConnection.submitMove(Point(-1, -1))
    }

    override fun onNextButtonPressed() {
        gameData?.let { gameData ->
            if (currentShownMove < gameData.moves.size) {
                currentShownMove++
            }

            currentShownMove.coerceIn(0, gameData.moves.size)
            determineHistoryParameters()
            view.position = RulesManager.replay(gameData, currentShownMove, false)
        }
    }

    override fun onPreviousButtonPressed() {
        gameData?.let { gameData ->

            currentShownMove--

            currentShownMove.coerceIn(0, gameData.moves.size)
            determineHistoryParameters()
            view.position = RulesManager.replay(gameData, currentShownMove, false)
        }
    }

    private fun determineHistoryParameters() {
        gameData?.let { gameData ->
            view.nextButtonEnabled = currentShownMove != gameData.moves.size
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
    }
}