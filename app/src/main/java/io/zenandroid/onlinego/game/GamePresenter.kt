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
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.gamelogic.RulesManager
import io.zenandroid.onlinego.model.Position
import io.zenandroid.onlinego.model.StoneType
import io.zenandroid.onlinego.model.local.Game
import io.zenandroid.onlinego.model.local.Message
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

    private enum class State {
        LOADING,
        PLAYING,
        SCORING,
        FINISHED,
        HISTORY,
        ANALYSIS
    }

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
    private var game: Game? = null
    private var variation: MutableList<Point> = mutableListOf()
    private var variationCurrentMove = 0

    private var currentState = State.LOADING
    private var messages: List<Message>? = null

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

    private val historyChip = Chip("History")

    private var finishedDialogShown = false

    private var candidateMove: Point? = null

    private var deferredTerritoryComputation: Disposable? = null

    override fun subscribe() {
        view.setLoading(true)
        view.boardSize = gameSize
        view.chatMyId = userId

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
                .takeWhile { currentState != State.FINISHED && game?.phase != Phase.FINISHED }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { clockTick() }
        )
        subscriptions.add(
                OGSServiceImpl.instance.loginWithToken()
                        .toSingle { OGSServiceImpl.instance.connectToGame(gameId) }
                        .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
                        .subscribe({
                            subscriptions.add(it)
                            gameConnection = it
                            //
                            // Hack alert: there is no way for us to determine if there are no
                            // messages or the server is just slow to respond. That's just how
                            // this Websockets API is implemented. Hence we give the server some
                            // half a second to respond and then assume that's the whole history
                            //
//                            Handler().postDelayed({
//                                sendAutoMessage()
//                            }, 500
//                            )
                        }, this::onError)
        )
        subscriptions.add(
                OnlineGoApplication.instance.chatRepository.monitorGameChat(gameId)
                        .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
                        .subscribe({
                            messages = it
                            view.setNewMessagesCount(it.count { it.playerId != userId && !it.seen })
                            view.setMessageList(it)
                        }, this::onError)
        )

    }

    override fun onNewMessage(message: String) {
        gameConnection?.sendMessage(message, game?.moves?.size ?: 0)
    }

    private fun sendAutoMessage() {
        messages?.let { messages ->
            for(message in messages) {
                if(message.text.startsWith("[Auto message]") && message.playerId == userId) {
                    return
                }
            }
        }
        gameConnection?.sendMessage("[Auto message] This player is using MrAlex's OnlineGo Android app (https://goo.gl/tiAeU6 ). Sharing variations in chat is not supported.",
                game?.moves?.size ?: 0)
    }

    private fun onError(t: Throwable) {
        Log.e(TAG, t.message, t)
        view.showError(t)
    }

    private fun onGameChanged(newGame: Game) {
        if(newGame == game) {
            return
        }

        if(currentState != State.ANALYSIS && currentState != State.HISTORY) {
            currentState = determineStateFromGame(newGame)
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
        val position = if(currentState == State.ANALYSIS) analysisPosition else currentPosition
        if(currentState == State.ANALYSIS || currentState == State.PLAYING) {
            val validMove = RulesManager.makeMove(position, position.nextToMove, point) != null
            if (validMove) {
                candidateMove = point
                view.showCandidateMove(point, position.nextToMove)
            }
        }
    }

    private fun onUserSelectedCell(point: Point) {
        when (currentState){
            State.ANALYSIS -> {
                candidateMove?.let { doAnalysisMove(it) }
                candidateMove = null
                view.showCandidateMove(null)
            }
            State.PLAYING ->
                if(candidateMove != null) {
                    showConfirmMoveControls()
                }
            State.SCORING -> {
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
            else -> {
                Log.e(TAG, "onUserSelectedCell while state = $currentState")
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

    private fun determineStateFromGame(game: Game?) =
        when(game?.phase) {
            Phase.PLAY -> State.PLAYING
            Phase.STONE_REMOVAL -> State.SCORING
            Phase.FINISHED -> State.FINISHED
            null -> State.LOADING
        }

    override fun onDiscardButtonPressed() {
        when (currentState){
            State.ANALYSIS -> {
                currentState = determineStateFromGame(game)
                candidateMove = null
                view.showCandidateMove(null)
                currentPosition = Position(19)
                game?.let { refreshUI(it) }
            }
            State.PLAYING -> {
                candidateMove = null
                view.showCandidateMove(null)
                showPlayControls()
            }
            State.SCORING -> gameConnection?.rejectRemovedStones()
            else -> {
                Log.e(TAG, "onDiscardButtonPressed while state = $currentState")
            }
        }
    }

    override fun onConfirmButtonPressed() {
        when(currentState) {
            State.PLAYING -> {
                view.interactive = false
                candidateMove?.let { gameConnection?.submitMove(it) }
                candidateMove = null
                showPlayControls()
            }
            State.SCORING -> {
                gameConnection?.acceptRemovedStones(currentPosition.removedSpots)
            }
            else -> {
                Log.e(TAG, "onConfirmButtonPressed while state = $currentState")
            }
        }
    }

    private fun configureBoard(game: Game) {
        when(currentState) {
            State.ANALYSIS -> {
                view.showLastMove = variation.isEmpty()
                view.showTerritory = false
                view.fadeOutRemovedStones = false
                view.interactive = true
            }
            State.HISTORY -> {
                view.showLastMove = true
                view.showTerritory = false
                view.fadeOutRemovedStones = false
                view.interactive = false
            }
            State.PLAYING -> {
                view.showLastMove = true
                view.showTerritory = false
                view.fadeOutRemovedStones = false
                view.interactive =
                        (currentPosition.nextToMove == StoneType.WHITE && game.whitePlayer.id == userId) || (currentPosition.nextToMove == StoneType.BLACK && game.blackPlayer.id == userId)
            }
            State.SCORING -> {
                view.showLastMove = false
                view.showTerritory = true
                view.fadeOutRemovedStones = true
                view.interactive = true
            }
            State.FINISHED -> {
                view.showLastMove = false
                view.showTerritory = true
                view.fadeOutRemovedStones = true
                view.interactive = false
            }
            GamePresenter.State.LOADING -> {
                view.showLastMove = false
                view.showTerritory = false
                view.fadeOutRemovedStones = false
                view.interactive = false
            }
        }
    }

    private fun showControls() {
        when {
            currentState == State.ANALYSIS -> showAnalysisControls()
            myGame && currentState == State.PLAYING -> showPlayControls()
            myGame && currentState == State.SCORING -> showStoneRemovalControls()
            else -> showSpectateControls()
        }
    }

    private fun showPlayControls() {
        view.bottomBarVisible = true
        view.nextButtonVisible = true
        view.previousButtonVisible = true
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
        configureBoard(game)
        configureChips()
        configurePassedLabels()

        when (currentState) {
            State.ANALYSIS -> {
                view.position = analysisPosition
            }
            else -> {
                val newPos = RulesManager.replay(game, computeTerritory = false)
                if (newPos != currentPosition) {
                    currentPosition = newPos
                    view.position = currentPosition
                    view.interactive = (currentPosition.nextToMove == StoneType.WHITE && game.whitePlayer.id == userId) || (currentPosition.nextToMove == StoneType.BLACK && game.blackPlayer.id == userId)
                    currentState = determineStateFromGame(game)

                    val shouldComputeTerritory = game.phase == Phase.STONE_REMOVAL || game.phase == Phase.FINISHED
                    if (shouldComputeTerritory) {
                        computeTerritoryAsync(game)
                    }
                }

                configurePreviousNextButtons()
                if (game.phase == Phase.FINISHED) {
                    view.whiteTimer = null
                    view.blackTimer = null
                    if (!finishedDialogShown && game.blackLost != game.whiteLost) {
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
                }
                view.activePlayer = currentPosition.nextToMove
            }
        }
    }

    override fun onChatClicked() {
        view.showChat()
    }

    private fun configurePassedLabels() {
        when (currentState) {
            GamePresenter.State.PLAYING -> {
                val lastMoveWasAPass = game?.moves?.lastOrNull()?.get(0) == -1
                if (lastMoveWasAPass) {
                    view.setBlackPlayerPassed(currentPosition.nextToMove == StoneType.WHITE)
                    view.setWhitePlayerPassed(currentPosition.nextToMove == StoneType.BLACK)
                } else {
                    view.setBlackPlayerPassed(false)
                    view.setWhitePlayerPassed(false)
                }
            }
            else -> {
                view.setBlackPlayerPassed(false)
                view.setWhitePlayerPassed(false)
            }
        }
    }

    private fun configureChips() {
        when (currentState) {
            State.LOADING -> {}
            State.PLAYING -> {
                val lastMoveWasAPass = game?.moves?.lastOrNull()?.get(0) == -1
                if (lastMoveWasAPass) {
                    view.setChips(listOf(playingChip, passedChip))
                } else {
                    view.setChips(listOf(playingChip))
                }
            }
            State.SCORING ->
                view.setChips(listOf(stoneRemovalChip))
            State.FINISHED ->
                view.setChips(listOf(finishedChip))
            State.HISTORY -> {
                historyChip.apply {
                    text = "Move $currentShownMove/${game?.moves?.size ?: 0}"
                    notifyChanged()
                    view.setChips(listOf(this))
                }
            }
            State.ANALYSIS ->
                view.setChips(listOf(analysisChip))
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
        when(currentState) {
            State.ANALYSIS -> {
                variationCurrentMove = (variationCurrentMove + 1).coerceIn(-1, variation.size - 1)
                replayAnalysis()
            }
            State.HISTORY -> {
                game?.let { game ->
                    game.moves?.let { moves ->
                        currentShownMove++
                        currentShownMove = currentShownMove.coerceIn(0, moves.size)
                        configurePreviousNextButtons()
                        if(currentShownMove == moves.size) {
                            currentState = determineStateFromGame(game)
                        }
                        view.position = RulesManager.replay(game, currentShownMove, false)
                        configureBoard(game)
                        configureChips()
                    }
                }
            }
            else -> {
                Log.e(TAG, "onNextButtonPressed while state is $currentState")
            }
        }
    }

    override fun onPreviousButtonPressed() {
        when(currentState) {
            State.ANALYSIS -> {
                variationCurrentMove = (variationCurrentMove - 1).coerceIn(-1, variation.size - 1)
                replayAnalysis()
            }
            State.PLAYING, State.HISTORY, State.FINISHED -> {
                currentState = State.HISTORY
                game?.let { game ->
                    game.moves?.let { moves ->
                        currentShownMove--
                        currentShownMove = currentShownMove.coerceIn(0, moves.size)
                        configurePreviousNextButtons()
                        view.position = RulesManager.replay(game, currentShownMove, false)
                        configureBoard(game)
                        configureChips()
                    }
                }
            }
            else -> {
                Log.e(TAG, "onPreviousButtonPressed while state is $currentState")
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
        currentState = State.ANALYSIS
        analysisPosition = currentPosition.clone()
        currentShownMove = game?.moves?.size ?: currentShownMove
        variation.clear()
        variationCurrentMove = -1
        replayAnalysis()
    }

    private fun configurePreviousNextButtons() {
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