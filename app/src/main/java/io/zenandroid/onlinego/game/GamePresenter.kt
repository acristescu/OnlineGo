package io.zenandroid.onlinego.game

import android.graphics.Point
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.SingleEmitter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.extensions.addToDisposable
import io.zenandroid.onlinego.game.GameContract.MenuItem
import io.zenandroid.onlinego.game.GameContract.MenuItem.*
import io.zenandroid.onlinego.game.GamePresenter.State.*
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
        private val analytics: FirebaseAnalytics,
        private val gameRepository: ActiveGameRepository,
        private val gameId: Long,
        private val gameSize: Int
) : GameContract.Presenter {

    enum class State {
        LOADING,
        PLAYING,
        SCORING,
        FINISHED,
        HISTORY,
        ANALYSIS,
        ESTIMATION
    }

    companion object {
        val TAG = GamePresenter::class.java.simpleName
    }

    private val subscriptions = CompositeDisposable()
    private var gameConnection: GameConnection? = null
    private var myGame: Boolean = false
    private var currentPosition = Position(19)
    private var analysisPosition = Position(19)
    private var estimatePosition = Position(19)
    private val userId = service.uiConfig?.user?.id
    private var currentShownMove = -1
    private var game: Game? = null
    private var variation: MutableList<Point> = mutableListOf()
    private var variationCurrentMove = 0
    private var undoPromptShownAtMoveNo = -1

    private var currentState : State = LOADING
    private var messages: List<Message>? = null
    private var stateToReturnFromEstimation: State = PLAYING

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

    private val estimationChip = EstimationChip {
        view.showInfoDialog("Score Estimation",
                "What you see on screen is a computer estimation of how the territory might " +
                        "be divided between the players and what the score might be if the game " +
                        "ended right now. It is very inaccurate and intended for a quick count for " +
                        "beginners and spectators. Quickly and accurately counting territory in ones head " +
                        "is a skill that is part of what makes a good GO player."
        )
    }

    private val historyChip = Chip("History")

    private var resultsDialogPending = false

    private var candidateMove: Point? = null

    private var deferredTerritoryComputation: Disposable? = null

    override fun subscribe() {
        view.setLoading(currentState == LOADING)
        view.boardSize = gameSize
        view.chatMyId = userId

        service.resendAuth()

        gameRepository.monitorGame(gameId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
                .subscribe(this::onGameChanged)
                .addToDisposable(subscriptions)
        view.cellSelection
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
                .subscribe(this::onUserSelectedCell)
                .addToDisposable(subscriptions)

        view.cellHotTrack
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
                .subscribe(this::onUserHotTrackedCell)
                .addToDisposable(subscriptions)

        Observable.interval(100, TimeUnit.MILLISECONDS)
                .takeWhile { currentState != FINISHED && game?.phase != Phase.FINISHED }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { clockTick() }
                .addToDisposable(subscriptions)

        OGSServiceImpl.instance.loginWithToken()
                .toSingle { OGSServiceImpl.instance.connectToGame(gameId) }
                .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
                .subscribe({
                    subscriptions.add(it)
                    gameConnection = it
                }, this::onError)
                .addToDisposable(subscriptions)

        OnlineGoApplication.instance.chatRepository.monitorGameChat(gameId)
                .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
                .subscribe({
                    messages = it
                    view.setNewMessagesCount(it.count { it.playerId != userId && !it.seen })
                    view.setMessageList(it)
                }, this::onError)
                .addToDisposable(subscriptions)

    }

    override fun onNewMessage(message: String) {
        analytics.logEvent("message_sent", null)
        gameConnection?.sendMessage(message, game?.moves?.size ?: 0)
    }

    private fun onError(t: Throwable) {
        Log.e(TAG, t.message, t)
        view.showError(t)
    }

    private fun onGameChanged(newGame: Game) {
        if(newGame == game) {
            return
        }

        val newGameState = determineStateFromGame(newGame)

        when(currentState) {
            LOADING -> {
                view.setLoading(false)
                view.whitePlayer = newGame.whitePlayer
                view.blackPlayer = newGame.blackPlayer

                myGame = (newGame.blackPlayer.id == userId) || (newGame.whitePlayer.id == userId)
                currentShownMove = newGame.moves?.size ?: -1
                view.title = "${newGame.blackPlayer.username} vs ${newGame.whitePlayer.username}"

                currentState = newGameState

            }

            ANALYSIS, ESTIMATION -> {}

            HISTORY -> {
                if(newGame.moves != game?.moves || newGameState == FINISHED) {
                    currentState = newGameState
                }
            }

            PLAYING -> {
                if(newGame.whitePlayer != game?.whitePlayer) {
                    view.whitePlayer = newGame.whitePlayer
                }
                if(newGame.blackPlayer != game?.blackPlayer) {
                    view.blackPlayer = newGame.blackPlayer
                }
                if(newGame.moves != game?.moves) {
                    candidateMove = null
                    view.showCandidateMove(null)
                    currentShownMove = newGame.moves?.size ?: 0
                }

                if(newGameState == FINISHED) {
                    resultsDialogPending = true
                    showResultDialog(newGame)
                }
                currentState = newGameState
            }

            FINISHED -> {
                if(resultsDialogPending) {
                    showResultDialog(newGame)
                }
            }

            SCORING -> {
                if(newGameState == FINISHED) {
                    resultsDialogPending = true
                    showResultDialog(newGame)
                }
                currentState = newGameState
            }
        }
        game = newGame
        refreshUI(newGame)
    }

    private fun showResultDialog(game: Game) {
        if (game.blackLost != game.whiteLost) {
            resultsDialogPending = false
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

    private fun onUserHotTrackedCell(point: Point) {
        val position = if(currentState == ANALYSIS) analysisPosition else currentPosition
        if(currentState == ANALYSIS || currentState == PLAYING) {
            val validMove = RulesManager.makeMove(position, position.nextToMove, point) != null
            if (validMove) {
                candidateMove = point
                view.showCandidateMove(point, position.nextToMove)
            }
        }
    }

    private fun onUserSelectedCell(point: Point) {
        when (currentState){
            ANALYSIS -> {
                candidateMove?.let { doAnalysisMove(it) }
                candidateMove = null
                view.showCandidateMove(null)
            }
            PLAYING ->
                if(candidateMove != null) {
                    analytics.logEvent("candidate_move", null)
                    showConfirmMoveControls()
                }
            SCORING -> {
                analytics.logEvent("scoring_change_group", null)
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
        analytics.logEvent("analysis_move", null)
        RulesManager.makeMove(analysisPosition, analysisPosition.nextToMove, candidateMove)?.let {
            variationCurrentMove ++
            variation = variation.dropLast(variation.size - variationCurrentMove).toMutableList()
            variation.add(candidateMove)
            it.variation = variation

            game?.let(this::refreshUI)
        }
    }

    private fun determineStateFromGame(game: Game?) =
        when(game?.phase) {
            Phase.PLAY -> PLAYING
            Phase.STONE_REMOVAL -> SCORING
            Phase.FINISHED -> FINISHED
            null -> LOADING
        }

    override fun onDiscardButtonPressed() {
        when (currentState){
            ANALYSIS -> {
                analytics.logEvent("analysis_cancel", null)
                currentState = determineStateFromGame(game)
                candidateMove = null
                view.showCandidateMove(null)
                currentPosition = Position(19)
                game?.let { refreshUI(it) }
            }
            PLAYING -> {
                analytics.logEvent("discard_move", null)
                candidateMove = null
                view.showCandidateMove(null)
                game?.let { showPlayControls(it) }
            }
            SCORING -> {
                analytics.logEvent("resume_from_scoring", null)
                gameConnection?.rejectRemovedStones()
            }
            ESTIMATION -> {
                analytics.logEvent("cancel_estimation", null)
                currentState = stateToReturnFromEstimation
                game?.let { refreshUI(it) }
            }
            else -> {
                Log.e(TAG, "onDiscardButtonPressed while state = $currentState")
            }
        }
    }

    override fun onConfirmButtonPressed() {
        when(currentState) {
            PLAYING -> {
                analytics.logEvent("confirm_move", null)
                view.interactive = false
                candidateMove?.let { gameConnection?.submitMove(it) }
                candidateMove = null
                game?.let { showPlayControls(it) }
            }
            SCORING -> {
                analytics.logEvent("accept_scoring", null)
                gameConnection?.acceptRemovedStones(currentPosition.removedSpots)
            }
            else -> {
                Log.e(TAG, "onConfirmButtonPressed while state = $currentState")
            }
        }
    }

    override fun onMenuButtonPressed() {
        val list = mutableListOf<MenuItem>(GAME_INFO)
        if(myGame && currentState in arrayOf(PLAYING, HISTORY, ANALYSIS)) {
            list.add(RESIGN)
        }
        if(myGame && currentState in arrayOf(PLAYING, HISTORY) && game?.phase == Phase.PLAY && game?.playerToMoveId == userId) {
            list.add(PASS)
        }
        if(currentState != ANALYSIS) {
            list.add(ANALYZE)
        }
        list.add(ESTIMATE_SCORE)
        list.add(DOWNLOAD_SGF)
        if(
                myGame
                && currentState in arrayOf(PLAYING, HISTORY, ANALYSIS)
                && game?.phase == Phase.PLAY && game?.playerToMoveId == userId
                && game?.undoRequested != null
        ) {
            list.add(ACCEPT_UNDO)
        }

        view.showMenu(list)
    }

    override fun onMenuItemSelected(item: MenuItem) {
        when(item) {
            GAME_INFO -> onGameInfoClicked()
            PASS -> onPassClicked()
            RESIGN -> onResignClicked()
            ESTIMATE_SCORE -> onEstimateClicked()
            ANALYZE -> onAnalyzeButtonClicked()
            DOWNLOAD_SGF -> onDownloadSGFClicked()
            ACCEPT_UNDO -> onUndoAccepted()
            REQUEST_UNDO -> TODO()
        }
    }

    private fun onGameInfoClicked() {
        game?.let { view.showGameInfoDialog(it) }
    }

    private fun onDownloadSGFClicked() {
        view.navigateTo("https://online-go.com/api/v1/games/$gameId/sgf")
    }

    private fun onEstimateClicked() {
        analytics.logEvent("estimate_clicked", null)
        game?.let { game ->
            stateToReturnFromEstimation = currentState
            currentState = ESTIMATION
            val pos = when (currentState) {
                ANALYSIS -> analysisPosition
                HISTORY -> RulesManager.replay(game, currentShownMove, false)
                else -> currentPosition
            }
            estimatePosition = pos.clone()
            RulesManager.determineTerritory(estimatePosition)

            refreshUI(game)
        }
    }

    private fun configureBoard(game: Game) {
        when(currentState) {
            ANALYSIS -> {
                view.showLastMove = variation.isEmpty()
                view.showTerritory = false
                view.fadeOutRemovedStones = false
                view.interactive = true
            }
            HISTORY -> {
                view.showLastMove = true
                view.showTerritory = false
                view.fadeOutRemovedStones = false
                view.interactive = false
            }
            PLAYING -> {
                view.showLastMove = true
                view.showTerritory = false
                view.fadeOutRemovedStones = false
                view.interactive =
                        (currentPosition.nextToMove == StoneType.WHITE && game.whitePlayer.id == userId) || (currentPosition.nextToMove == StoneType.BLACK && game.blackPlayer.id == userId)
            }
            SCORING -> {
                view.showLastMove = false
                view.showTerritory = true
                view.fadeOutRemovedStones = true
                view.interactive = true
            }
            FINISHED -> {
                view.showLastMove = false
                view.showTerritory = true
                view.fadeOutRemovedStones = true
                view.interactive = false
            }
            LOADING -> {
                view.showLastMove = false
                view.showTerritory = false
                view.fadeOutRemovedStones = false
                view.interactive = false
            }
            ESTIMATION -> {
                view.showLastMove = false
                view.showTerritory = true
                view.fadeOutRemovedStones = true
                view.interactive = false
            }
        }
    }

    private fun showControls(game: Game) {
        when {
            currentState == ANALYSIS -> showAnalysisControls()
            myGame && currentState == PLAYING -> showPlayControls(game)
            myGame && currentState == SCORING -> showStoneRemovalControls()
            currentState == HISTORY -> showHistoryControls(game)
            currentState == ESTIMATION -> showEstimationControls()
            else -> showSpectateControls()
        }
    }

    private fun showHistoryControls(game: Game) {
        view.bottomBarVisible = true
        view.menuButtonVisible = true

        view.nextButtonVisible = true
        view.previousButtonVisible = true
        view.passButtonVisible = myGame && game.phase == Phase.PLAY
        view.resignButtonVisible = myGame && game.phase == Phase.PLAY
        view.analyzeButtonVisible = true

        view.confirmButtonVisible = false
        view.discardButtonVisible = false
        view.autoButtonVisible = false

        view.passButtonEnabled = false
    }

    private fun showPlayControls(game: Game) {
        view.bottomBarVisible = true
        view.menuButtonVisible = true

        view.nextButtonVisible = true
        view.previousButtonVisible = true
        view.passButtonVisible = true
        view.resignButtonVisible = false
        view.analyzeButtonVisible = true

        view.confirmButtonVisible = false
        view.discardButtonVisible = false
        view.autoButtonVisible = false

        view.passButtonEnabled = game.phase == Phase.PLAY && game.playerToMoveId == userId
    }

    private fun showAnalysisControls() {
        view.bottomBarVisible = true
        view.menuButtonVisible = true
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

    private fun showStoneRemovalControls() {
        view.bottomBarVisible = true
        view.menuButtonVisible = false

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
        view.menuButtonVisible = true

        view.nextButtonVisible = true
        view.previousButtonVisible = true
        view.passButtonVisible = false
        view.resignButtonVisible = false
        view.analyzeButtonVisible = true

        view.confirmButtonVisible = false
        view.discardButtonVisible = false
        view.autoButtonVisible = false
    }

    private fun showEstimationControls() {
        view.bottomBarVisible = true
        view.menuButtonVisible = false

        view.nextButtonVisible = false
        view.previousButtonVisible = false
        view.passButtonVisible = false
        view.resignButtonVisible = false
        view.analyzeButtonVisible = false

        view.confirmButtonVisible = false
        view.discardButtonVisible = true
        view.autoButtonVisible = false
    }

    private fun showConfirmMoveControls() {
        view.bottomBarVisible = true

        view.menuButtonVisible = false
        view.nextButtonVisible = false
        view.previousButtonVisible = false
        view.passButtonVisible = false
        view.resignButtonVisible = false
        view.analyzeButtonVisible = false

        view.confirmButtonVisible = true
        view.discardButtonVisible = true
        view.autoButtonVisible = false
    }

    override fun onAutoButtonPressed() {
        if(game?.phase != Phase.STONE_REMOVAL) {
            return
        }
        analytics.logEvent("auto_clicked", null)
        val newPos = currentPosition.clone()
        newPos.clearAllRemovedSpots()
        RulesManager.determineTerritory(newPos)
        gameConnection?.submitRemovedStones(currentPosition.removedSpots, false)
        gameConnection?.submitRemovedStones(newPos.removedSpots, true)
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
        when (currentState) {
            ANALYSIS -> {
                replayAnalysis()
                view.position = analysisPosition
                view.whiteScore = analysisPosition.whiteCapturedCount + (game.komi ?: 0f)
                view.blackScore = analysisPosition.blackCapturedCount.toFloat()
            }
            HISTORY -> {
                val historyPosition = RulesManager.replay(game, currentShownMove, false)
                view.position = historyPosition
                view.whiteScore = historyPosition.whiteCapturedCount + (game.komi ?: 0f)
                view.blackScore = historyPosition.blackCapturedCount.toFloat()
            }
            SCORING -> {
                currentPosition = RulesManager.replay(game, computeTerritory = true)
                view.position = currentPosition
                val whiteDeadStones = currentPosition.removedSpots.filter { currentPosition.getStoneAt(it) == StoneType.WHITE }
                val blackDeadStones = currentPosition.removedSpots.filter { currentPosition.getStoneAt(it) == StoneType.BLACK }
                view.whiteScore = blackDeadStones.size + currentPosition.whiteTerritory.size + currentPosition.whiteCapturedCount + (game.komi ?: 0f)
                view.blackScore = whiteDeadStones.size + currentPosition.blackTerritory.size + currentPosition.blackCapturedCount.toFloat()
            }
            ESTIMATION -> {
                val whiteDeadStones = estimatePosition.removedSpots.filter { estimatePosition.getStoneAt(it) == StoneType.WHITE }
                val blackDeadStones = estimatePosition.removedSpots.filter { estimatePosition.getStoneAt(it) == StoneType.BLACK }
                if(game.scoreStones == false) {
                    estimatePosition.whiteTerritory.removeAll(estimatePosition.whiteStones)
                    estimatePosition.blackTerritory.removeAll(estimatePosition.blackStones)
                }
                view.whiteScore = blackDeadStones.size + estimatePosition.whiteTerritory.size + estimatePosition.whiteCapturedCount + (game.komi
                        ?: 0f)
                view.blackScore = whiteDeadStones.size + estimatePosition.blackTerritory.size + estimatePosition.blackCapturedCount.toFloat()

                view.showTerritory = true
                view.position = estimatePosition
            }
            PLAYING, FINISHED, LOADING -> {
                currentPosition = RulesManager.replay(game, computeTerritory = false)
                view.position = currentPosition
                view.whiteScore = game.whiteScore?.total?.toFloat() ?: currentPosition.whiteCapturedCount + (game.komi ?: 0f)
                view.blackScore = game.blackScore?.total?.toFloat() ?: currentPosition.blackCapturedCount.toFloat()
            }
        }

        showControls(game)
        configureBoard(game)
        configureChips(game)
        configurePassedLabels()
        configurePreviousNextButtons()
        configurePlayerStatus()

        game.undoRequested?.let {
            if(it != undoPromptShownAtMoveNo) {
                analytics.logEvent("undo_requested_by_opponent", null)
                undoPromptShownAtMoveNo = it
                view.showUndoPrompt()
            }
        }


        if(currentState == FINISHED) {
            view.whiteTimer = null
            view.blackTimer = null
        }

    }

    override fun onUndoAccepted() {
        analytics.logEvent("undo_accepted", null)
        gameConnection?.acceptUndo(undoPromptShownAtMoveNo)
        undoPromptShownAtMoveNo = -1
    }

    override fun onUndoRejected() {
        analytics.logEvent("undo_rejected", null)
    }

    override fun onChatClicked() {
        analytics.logEvent("chat_clicked", null)
        view.showChat()
    }

    private fun configurePassedLabels() {
        when (currentState) {
            PLAYING -> {
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

    private fun configureChips(game: Game) {
        when (currentState) {
            LOADING -> {}
            PLAYING -> {
                val lastMoveWasAPass = game.moves?.lastOrNull()?.get(0) == -1
                if (lastMoveWasAPass) {
                    view.setChips(listOf(playingChip, passedChip))
                } else {
                    view.setChips(listOf(playingChip))
                }
            }
            SCORING ->
                view.setChips(listOf(stoneRemovalChip))
            FINISHED ->
                view.setChips(listOf(finishedChip))
            HISTORY -> {
                historyChip.apply {
                    text = "Move $currentShownMove/${game.moves?.size ?: 0}"
                    notifyChanged()
                    view.setChips(listOf(this))
                }
            }
            ANALYSIS ->
                view.setChips(listOf(analysisChip))
            ESTIMATION -> {
                view.setChips(listOf(estimationChip))
            }
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
                if((game?.phase == Phase.PLAY || game?.phase == Phase.STONE_REMOVAL) && currentState != LOADING) {
                    view.whiteTimer = computeTimeLeft(clock, clock.whiteTimeSimple, clock.whiteTime, game?.playerToMoveId == game?.whitePlayer?.id)
                    view.blackTimer = computeTimeLeft(clock, clock.blackTimeSimple, clock.blackTime, game?.playerToMoveId == game?.blackPlayer?.id)
                }
            }
        }
    }

    override fun onResignConfirmed() {
        analytics.logEvent("resign_confirmed", null)
        gameConnection?.resign()
    }


    override fun onPassConfirmed() {
        analytics.logEvent("pass_confirmed", null)
        gameConnection?.submitMove(Point(-1, -1))
    }

    override fun onPassClicked() {
        analytics.logEvent("pass_clicked", null)
        view.showPassConfirmation()
    }

    override fun onResignClicked() {
        analytics.logEvent("resign_clicked", null)
        view.showResignConfirmation()
    }

    override fun onNextButtonPressed() {
        game?.let { game ->
            when (currentState) {
                ANALYSIS -> {
                    analytics.logEvent("next_analytics_clicked", null)
                    variationCurrentMove = (variationCurrentMove + 1).coerceIn(-1, variation.size - 1)
                }
                HISTORY -> {
                    analytics.logEvent("next_history_clicked", null)
                    game.moves?.let { moves ->
                        currentShownMove = (currentShownMove + 1).coerceIn(0, moves.size)
                        if (currentShownMove == moves.size) {
                            currentState = determineStateFromGame(game)
                        }
                    }
                }
                else -> {
                    Log.e(TAG, "onNextButtonPressed while state is $currentState")
                }
            }
            refreshUI(game)
        }
    }

    override fun onPreviousButtonPressed() {
        game?.let { game ->
            when (currentState) {
                ANALYSIS -> {
                    analytics.logEvent("previous_analytics_clicked", null)
                    variationCurrentMove = (variationCurrentMove - 1).coerceIn(-1, variation.size - 1)
                }
                PLAYING, HISTORY, FINISHED -> {
                    analytics.logEvent("previous_history_clicked", null)
                    currentState = HISTORY
                    game.moves?.let { moves ->
                        currentShownMove--
                        currentShownMove = currentShownMove.coerceIn(0, moves.size)
                    }
                }
                else -> {
                    Log.e(TAG, "onPreviousButtonPressed while state is $currentState")
                }
            }
            refreshUI(game)
        }
    }

    private fun replayAnalysis() {
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
    }

    override fun onAnalyzeButtonClicked() {
        analytics.logEvent("analyze_clicked", null)
        currentState = ANALYSIS
        analysisPosition = currentPosition.clone()
        currentShownMove = game?.moves?.size ?: currentShownMove
        variation.clear()
        variationCurrentMove = -1
        game?.let(this::refreshUI)
    }

    private fun configurePlayerStatus() {
        when (currentState) {
            LOADING, FINISHED, ESTIMATION -> {
                view.setWhitePlayerStatus(null)
                view.setBlackPlayerStatus(null)
            }
            SCORING -> {
                if(game?.removedStones == game?.whitePlayer?.acceptedStones) {
                    view.setWhitePlayerStatus("Accepted", R.color.colorPrimary)
                } else {
                    view.setWhitePlayerStatus("Not accepted")
                }
                if(game?.removedStones == game?.blackPlayer?.acceptedStones) {
                    view.setBlackPlayerStatus("Accepted", R.color.colorPrimary)
                } else {
                    view.setBlackPlayerStatus("Not accepted")
                }
            }
            PLAYING, HISTORY, ANALYSIS -> {
                if(currentPosition.nextToMove == StoneType.WHITE) {
                    val prefix = if(game?.whitePlayer?.id == userId) "Your" else "Their"
                    view.setWhitePlayerStatus("$prefix turn")
                } else {
                    view.setWhitePlayerStatus(null)
                }
                if(currentPosition.nextToMove == StoneType.BLACK) {
                    val prefix = if(game?.blackPlayer?.id == userId) "Your" else "Their"
                    view.setBlackPlayerStatus("$prefix turn")
                } else {
                    view.setBlackPlayerStatus(null)
                }
            }
        }
    }

    private fun configurePreviousNextButtons() {
        game?.let { game ->
            when(currentState) {
                ANALYSIS -> {
                    view.nextButtonEnabled = variationCurrentMove < variation.size - 1
                    view.previousButtonEnabled = variationCurrentMove > -1
                }
                else -> {
                    view.nextButtonEnabled = currentShownMove != game.moves?.size
                    view.previousButtonEnabled = currentShownMove > 0
                }
            }
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