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
import io.zenandroid.onlinego.model.local.Game
import io.zenandroid.onlinego.model.ogs.Phase
import io.zenandroid.onlinego.ogs.ActiveGameRepository
import io.zenandroid.onlinego.ogs.GameConnection
import io.zenandroid.onlinego.ogs.OGSService
import io.zenandroid.onlinego.ogs.OGSServiceImpl
import io.zenandroid.onlinego.statuschips.*
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
    private var gameConnection: GameConnection? = null
    private var myGame: Boolean = false
    private var currentPosition = Position(19)
    private var analysisPosition = Position(19)
    private val userId = service.uiConfig?.user?.id
    private var currentShownMove = -1
    private var analysisMode = false
    private var game: Game? = null
    private var variation: MutableList<Point> = mutableListOf()
    private var variationCurrentMove = 0

    private val playingChip = PlayingChip {
        view.showInfoDialog("Playing phase",
                "The game is in the playing phase. Here the players will take " +
                        "turns placing stones and try to surround the most territory and capture " +
                        "opponents stones. This phase ends when both players pass their turns.")
    }
    private val stoneRemovalChip = StoneRemovalChip {
        view.showInfoDialog("Scoring phase",
                "The game is in the scoring phase. Here the players agree on " +
                        "the dead stones so that the server can count the points. An automatic " +
                        "estimation is already provided (and you can always reset the status " +
                        "to that by pressing the wand button below) but you can make modifications " +
                        "by tapping on the stone group that you think has the wrong status. " +
                        "Once you are happy with the status of the board, you can press the " +
                        "accept button below. When both players have accepted, the game is " +
                        "over and the score is counted. If you cannot agree with your opponent " +
                        "you can cancel the scoring phase and play on to prove which " +
                        "group is alive and which is dead.")
    }
    private val finishedChip = FinishedChip {
        view.showInfoDialog("Finished game",
                "The game is finished. If the outcome was decided by counting " +
                        "the points (e.g. not by timeout or one of the player resigning) " +
                        "you can see the score details by tapping on the game info button (not implemented yet)")
    }
    private val passedChip = PassedChip {
        view.showInfoDialog("Player has passed",
                "The last player to move has passed their turn. This means they think the " +
                        "game is over. If their opponent agrees and passes too, the game moves on " +
                        "to the scoring phase.")
    }

    private val analysisChip = AnalysisChip {
        view.showInfoDialog("Analysis mode",
                "You are now in analysis mode. You can try variants here without influencing " +
                        "the real game. Simply tap on the board to see how a move would look like. " +
                        "You can navigate forwards and back in the variation. When you are done, use " +
                        "the cancel button to return to the game."
                )
    }

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
        subscriptions.add(
                OGSServiceImpl.instance.loginWithToken().subscribe {
                    subscriptions.add(
                            OGSServiceImpl.instance.connectToGame(gameId).apply {
                                gameConnection = this
                            }
                    )
                }
        )

    }

    private fun onError(t: Throwable) {
        Log.e(TAG, t.message, t)
        view.showError(t)
    }

    private fun onGameChanged(newGame: Game) {
        if(newGame == game) {
            return
        }

        view.setLoading(false)

        view.whitePlayer = newGame.whitePlayer
        view.blackPlayer = newGame.blackPlayer
        view.komi = newGame.komi

        myGame = (newGame.blackPlayer.id == userId) || (newGame.whitePlayer.id == userId)

        currentShownMove = newGame.moves?.size ?: -1
        refreshUI(newGame)
        view.title = "${newGame.blackPlayer.username} vs ${newGame.whitePlayer.username}"

        view.passButtonEnabled = newGame.phase == Phase.PLAY && newGame.playerToMoveId == userId

        if(newGame.moves != game?.moves) {
            candidateMove = null
            view.showCandidateMove(null)
            currentShownMove = newGame.moves?.size ?: 0
        }
        game = newGame
    }

    private fun onUserHotTrackedCell(point: Point) {
        val position = if(analysisMode) analysisPosition else currentPosition
        if(game?.phase == Phase.PLAY || analysisMode) {
            val validMove = RulesManager.makeMove(position, position.nextToMove, point) != null
            if (validMove) {
                candidateMove = point
                view.showCandidateMove(point, position.nextToMove)
            }
        }
    }

    private fun onUserSelectedCell(point: Point) {
        when {
            analysisMode -> {
                candidateMove?.let { doAnalysisMove(it) }
                candidateMove = null
                view.showCandidateMove(null)
            }
            game?.phase == Phase.PLAY ->
                if(candidateMove != null) {
                    showConfirmMoveControls()
                }
            else -> {
                val newPos = currentPosition.clone()
                RulesManager.toggleRemoved(newPos, point)
                var delta = newPos.removedSpots - currentPosition.removedSpots
                var removing = true
                if(delta.isEmpty()) {
                    delta = currentPosition.removedSpots - newPos.removedSpots
                    removing = false
                }
                if(delta.isNotEmpty()) {
                    gameConnection?.submitRemovedStones(delta, removing)
                }
            }
        }
    }

    private fun doAnalysisMove(candidateMove: Point) {
        RulesManager.makeMove(analysisPosition, analysisPosition.nextToMove, candidateMove)?.let {
            variationCurrentMove ++
            variation = variation.dropLast(variation.size - variationCurrentMove).toMutableList()
            variation.add(candidateMove)
            it.variation = variation

            replayAnalysis()
        }
    }

    override fun onDiscardButtonPressed() {
        when {
            analysisMode -> {
                analysisMode = false
                candidateMove = null
                view.showCandidateMove(null)
                currentPosition = Position(19)
                game?.let { refreshUI(it) }
            }
            game?.phase == Phase.PLAY -> {
                candidateMove = null
                view.showCandidateMove(null)
                showPlayControls()
            }
            else -> gameConnection?.rejectRemovedStones()
        }
    }

    override fun onConfirmButtonPressed() {
        if(game?.phase == Phase.PLAY) {
            view.interactive = false
            candidateMove?.let { gameConnection?.submitMove(it) }
            candidateMove = null
            showPlayControls()
        } else {
            gameConnection?.acceptRemovedStones(currentPosition.removedSpots)
        }
    }

    private fun configureBoard() {
        when {
            analysisMode -> {
                view.showLastMove = variation.isEmpty()
                view.showTerritory = false
                view.fadeOutRemovedStones = false
                view.interactive = true
            }
            currentShownMove != game?.moves?.size -> {
                view.showLastMove = true
                view.showTerritory = false
                view.fadeOutRemovedStones = false
                view.interactive = false
            }
            game?.phase == Phase.PLAY -> {
                view.showLastMove = true
                view.showTerritory = false
                view.fadeOutRemovedStones = false
                view.interactive = game?.playerToMoveId == userId
            }
            game?.phase == Phase.STONE_REMOVAL -> {
                view.showLastMove = false
                view.showTerritory = true
                view.fadeOutRemovedStones = true
                view.interactive = true
            }
            game?.phase == Phase.FINISHED -> {
                view.showLastMove = false
                view.showTerritory = true
                view.fadeOutRemovedStones = true
                view.interactive = false
            }
        }
    }

    private fun showControls() {
        when {
            analysisMode -> showAnalysisControls()
            myGame && game?.phase == Phase.PLAY -> showPlayControls()
            myGame && game?.phase == Phase.STONE_REMOVAL -> showStoneRemovalControls()
            else -> showSpectateControls()
        }
    }

    private fun showPlayControls() {
        view.bottomBarVisible = true
        view.nextButtonVisible = true
        view.previousButtonVisible = true
        view.chatButtonVisible = true
        view.passButtonVisible = true
        view.resignButtonVisible = true
        view.analyzeButtonVisible = true

        view.confirmButtonVisible = false
        view.discardButtonVisible = false
        view.autoButtonVisible = false

        view.passButtonEnabled = game?.playerToMoveId == userId
    }

    private fun showAnalysisControls() {
        view.bottomBarVisible = true
        view.nextButtonVisible = true
        view.previousButtonVisible = true
        view.chatButtonVisible = true
        view.passButtonVisible = false
        view.resignButtonVisible = false
        view.analyzeButtonVisible = false

        view.confirmButtonVisible = false
        view.discardButtonVisible = true
        view.autoButtonVisible = false

        view.passButtonEnabled = false
    }

    override fun onAutoButtonPressed() {
        if(game?.phase != Phase.STONE_REMOVAL) {
            return
        }
        val newPos = currentPosition.clone()
        newPos.clearAllRemovedSpots()
        RulesManager.determineTerritory(newPos)
        gameConnection?.submitRemovedStones(currentPosition.removedSpots, false)
        gameConnection?.submitRemovedStones(newPos.removedSpots, true)

    }

    private fun showStoneRemovalControls() {
        view.bottomBarVisible = true
        view.nextButtonVisible = false
        view.previousButtonVisible = false
        view.chatButtonVisible = true
        view.passButtonVisible = false
        view.resignButtonVisible = false
        view.analyzeButtonVisible = false

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
        view.analyzeButtonVisible = true

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
        view.analyzeButtonVisible = false

        view.confirmButtonVisible = true
        view.discardButtonVisible = true
        view.autoButtonVisible = false
    }

    private fun computeTerritoryAsync(game: Game) {
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

    private fun refreshUI(game: Game) {
        showControls()
        configureBoard()

        if(analysisMode) {
            view.position = analysisPosition
            view.setChips(listOf(analysisChip))
            view.setBlackPlayerPassed(false)
            view.setWhitePlayerPassed(false)
        } else {
            val newPos = RulesManager.replay(game, computeTerritory = false)
            if (newPos != currentPosition) {
                currentPosition = newPos
                view.position = currentPosition

                val shouldComputeTerritory = game.phase == Phase.STONE_REMOVAL || game.phase == Phase.FINISHED
                if (shouldComputeTerritory) {
                    computeTerritoryAsync(game)
                }
            }

            determineHistoryParameters()
            when (game.phase) {
                Phase.PLAY -> {
                    val lastMoveWasAPass = game.moves?.lastOrNull()?.get(0) == -1
                    if (lastMoveWasAPass) {
                        view.setChips(listOf(playingChip, passedChip))
                        view.setBlackPlayerPassed(currentPosition.nextToMove == StoneType.WHITE)
                        view.setWhitePlayerPassed(currentPosition.nextToMove == StoneType.BLACK)
                    } else {
                        view.setChips(listOf(playingChip))
                        view.setBlackPlayerPassed(false)
                        view.setWhitePlayerPassed(false)
                    }
                }
                Phase.STONE_REMOVAL -> {
                    view.setChips(listOf(stoneRemovalChip))
                    view.setBlackPlayerPassed(false)
                    view.setWhitePlayerPassed(false)
                }
                Phase.FINISHED -> {
                    if (!finishedDialogShown) {
                        finishedDialogShown = true
                        val winner = if (game.blackLost == true) game.whitePlayer.id else game.blackPlayer.id
                        when {
                            !myGame ->
                                view.showFinishedDialog()
                            winner == userId ->
                                view.showYouWinDialog()
                            else ->
                                view.showYouLoseDialog()
                        }
                    }
                    view.setChips(listOf(finishedChip))
                    view.setBlackPlayerPassed(false)
                    view.setWhitePlayerPassed(false)
                }
            }
            view.activePlayer = currentPosition.nextToMove
        }
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
        gameConnection?.resign()
    }


    override fun onPassConfirmed() {
        gameConnection?.submitMove(Point(-1, -1))
    }

    override fun onNextButtonPressed() {
        if(analysisMode) {
            variationCurrentMove = (variationCurrentMove + 1).coerceIn(-1, variation.size - 1)
            replayAnalysis()
            return
        }
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
        if(analysisMode) {
            variationCurrentMove = (variationCurrentMove - 1).coerceIn(-1, variation.size - 1)
            replayAnalysis()
            return
        }
        game?.let { game ->
            game.moves?.let { moves ->
                currentShownMove--
                currentShownMove = currentShownMove.coerceIn(0, moves.size)
                determineHistoryParameters()
                view.position = RulesManager.replay(game, currentShownMove, false)
            }
        }
    }

    private fun replayAnalysis() {
        view.nextButtonEnabled = variationCurrentMove < variation.size - 1
        view.previousButtonEnabled = variationCurrentMove > -1
        analysisPosition = currentPosition.clone()
        variation.take(variationCurrentMove + 1).let { truncatedVariation ->
            truncatedVariation.forEach {
                RulesManager.makeMove(analysisPosition, analysisPosition.nextToMove, it)?.let {
                    it.nextToMove = analysisPosition.nextToMove.opponent
                    analysisPosition = it
                    analysisPosition.variation = truncatedVariation
                }
            }
        }
        game?.let { refreshUI(it) }
    }

    override fun onAnalyzeButtonPressed() {
        analysisMode = true
        analysisPosition = currentPosition.clone()
        variation.clear()
        variationCurrentMove = -1
        replayAnalysis()
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