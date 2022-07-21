package io.zenandroid.onlinego.ui.screens.game_legacy

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.analytics.FirebaseAnalytics
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.utils.addToDisposable
import io.zenandroid.onlinego.ui.screens.game_legacy.GameContract.MenuItem
import io.zenandroid.onlinego.ui.screens.game_legacy.GameContract.MenuItem.*
import io.zenandroid.onlinego.ui.screens.game_legacy.GamePresenter.State.*
import io.zenandroid.onlinego.gamelogic.RulesManager
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.data.model.local.Message
import io.zenandroid.onlinego.data.model.ogs.Phase
import io.zenandroid.onlinego.data.ogs.*
import io.zenandroid.onlinego.data.repositories.*
import io.zenandroid.onlinego.ui.items.statuschips.*
import io.zenandroid.onlinego.utils.CountingIdlingResource
import io.zenandroid.onlinego.utils.computeTimeLeft
import io.zenandroid.onlinego.utils.formatMillis
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * Created by alex on 10/11/2017.
 */
@Deprecated("")
class GamePresenter(
        private val view: GameContract.View,
        private val socketService: OGSWebSocketService,
        private val userSessionRepository: UserSessionRepository,
        private val analytics: FirebaseAnalytics,
        private val gameRepository: ActiveGamesRepository,
        private val settingsRepository: SettingsRepository,
        private val chatRepository: ChatRepository,
        private val idlingResource: CountingIdlingResource,
        private val clockDriftRepository: ClockDriftRepository,
        private val gameId: Long,
        private val gameWidth: Int,
        private val gameHeight: Int,
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
    private var currentPosition = Position(19, 19)
    private var analysisPosition = Position(19, 19)
    private var analysisRootPosition = Position(19, 19)
    private var estimatePosition = Position(19, 19)
    private val userId = userSessionRepository.userId
    private var currentShownMove = -1
    private var game: Game? = null
    private var variation: MutableList<Cell> = mutableListOf()
    private var variationCurrentMove = 0
    private var undoPromptShownAtMoveNo = -1

    private var currentState : State = LOADING
    private var messages: List<Message>? = null
    private var stateToReturnFromEstimation: State = PLAYING

    private var timerIntervalMillis: AtomicLong? = null
    private var timerDisposable: Disposable? = null

    private val playingChip = PlayingChip {
        view.showChipDialog("Playing")
    }
    private val stoneRemovalChip = StoneRemovalChip {
        view.showChipDialog("Scoring")
    }
    private val finishedChip = FinishedChip {
        view.showChipDialog("Finished")
    }
    private val passedChip = PassedChip {
        view.showChipDialog("Passed")
    }
    private val analysisChip = AnalysisChip {
        view.showChipDialog("Analysis")
    }
    private val estimationChip = EstimationChip {
        view.showChipDialog("Estimation")
    }

    private val historyChip = Chip("History")

    private var resultsDialogPending = false

    private var candidateMove: Cell? = null

    override fun subscribe() {
        idlingResource.increment()
        view.apply {
            setLoading(currentState == LOADING)
            boardWidth = gameWidth
            boardHeight = gameHeight
            chatMyId = userId
            showCoordinates = settingsRepository.showCoordinates
        }

        socketService.resendAuth()

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

        clockTick(game)

        gameConnection = socketService.connectToGame(gameId, true).apply {
            subscriptions.add(this)
        }


        chatRepository.monitorGameChatRxJava(gameId)
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

        if(game == null) {
            idlingResource.decrement()
        }

        val newGameState = determineStateFromGame(newGame)

        when(currentState) {
            LOADING -> {
                view.setLoading(false)
                view.whitePlayer = newGame.whitePlayer
                view.blackPlayer = newGame.blackPlayer

                myGame = (newGame.blackPlayer.id == userId) || (newGame.whitePlayer.id == userId)
                currentShownMove = newGame.moves?.size ?: -1
                view.title = newGame.name

                currentState = newGameState
                clockTick(newGame)
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
                    clockTick(newGame)
                }

                if(newGameState == FINISHED) {
                    resultsDialogPending = true
                    view.hideOpponentMalkowichChat = false
                    showResultDialog(newGame)
                }
                currentState = newGameState
            }

            FINISHED -> {
                view.hideOpponentMalkowichChat = false
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
            val aborted = game.outcome == "Cancellation"
            when {
                aborted ->
                    view.showAbortedDialog()
                !myGame ->
                    view.showFinishedDialog()
                winner == userId ->
                    view.showYouWinDialog()
                else ->
                    view.showYouLoseDialog()
            }
        }
    }

    private fun onUserHotTrackedCell(point: Cell) {
        val position = if(currentState == ANALYSIS) analysisPosition else currentPosition
        if(currentState == ANALYSIS || currentState == PLAYING) {
            val validMove = RulesManager.makeMove(position, position.nextToMove, point) != null
            if (validMove) {
                candidateMove = point
                view.showCandidateMove(point, position.nextToMove)
            }
        }
    }

    private fun onUserSelectedCell(point: Cell) {
        when (currentState){
            ANALYSIS -> {
                candidateMove?.let { doAnalysisMove(it) }
                candidateMove = null
                view.showCandidateMove(null)
            }
            PLAYING ->
                candidateMove?.let {
                    val proposedPosition = RulesManager.makeMove(currentPosition, currentPosition.nextToMove, it)
                    val historySize = game?.moves?.size ?: 0
                    if(proposedPosition != null && historySize > 1 && RulesManager.replay(game!!, historySize - 1, false).hasTheSameStonesAs(proposedPosition)) {
                        view.showKoDialog()
                        candidateMove = null
                        view.showCandidateMove(null)
                        game?.let { showPlayControls(it) }
                    } else {
                        analytics.logEvent("candidate_move", null)
                        showConfirmMoveControls()
                    }
                }
            SCORING -> {
                analytics.logEvent("scoring_change_group", null)
                val (removing, delta) = RulesManager.toggleRemoved(currentPosition, point)
                if(delta.isNotEmpty()) {
                    gameConnection?.submitRemovedStones(delta, removing)
                }
            }
            else -> {
                Log.e(TAG, "onUserSelectedCell while state = $currentState")
            }
        }
    }

    private fun doAnalysisMove(candidateMove: Cell) {
        analytics.logEvent("analysis_move", null)
        RulesManager.makeMove(analysisPosition, analysisPosition.nextToMove, candidateMove)?.let {
            val potentialKOPosition = when {
                variationCurrentMove == -1 && currentShownMove > 1 -> RulesManager.replay(game!!, currentShownMove - 1, false)
                variation.size >= 1 -> {
                    var cursor = analysisRootPosition
                    variation.take(variationCurrentMove).forEach {
                        RulesManager.makeMove(cursor, cursor.nextToMove, it)?.let {
                            cursor = it
                        }
                    }
                    cursor
                }
                else -> null
            }

            if(potentialKOPosition?.hasTheSameStonesAs(it) == true) {
                view.showKoDialog()
                return
            }
            variationCurrentMove ++
            variation = variation.dropLast(variation.size - variationCurrentMove).toMutableList()
            variation.add(candidateMove)

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
                currentState = if(currentShownMove != game?.moves?.size) HISTORY else determineStateFromGame(game)
                candidateMove = null
                view.showCandidateMove(null)
                currentPosition = Position(19, 19)
                game?.let { refreshUI(it) }
            }
            PLAYING -> {
                if(candidateMove != null) {
                    analytics.logEvent("discard_move", null)
                    candidateMove = null
                    view.showCandidateMove(null)
                    game?.let { showPlayControls(it) }
                } else {
                    analytics.logEvent("abort_game", null)
                    view.showAbortGameConfirmation()
                }
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

    override fun onAbortGameConfirmed() {
        analytics.logEvent("abort_game_confirmed", null)
        gameConnection?.abortGame()
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
        analytics.logEvent("menu_button_pressed", null)
        val list = mutableListOf<MenuItem>(GAME_INFO)
        if(myGame && currentState in arrayOf(PLAYING, HISTORY, ANALYSIS)) {
            if(game?.moves?.size ?: 0 < 2) {
                list.add(ABORT_GAME)
            } else {
                list.add(RESIGN)
            }
        }
        if(myGame && currentState in arrayOf(PLAYING, HISTORY) && game?.phase == Phase.PLAY && game?.playerToMoveId == userId) {
            list.add(PASS)
        }
        if((currentState != ANALYSIS) && (!isAnalysisDisabled(game))) {
            list.add(ANALYZE)
        }
        if(canRequestUndo()) {
            list.add(REQUEST_UNDO)
        }
        val showCoordinates = settingsRepository.showCoordinates
        if(showCoordinates) {
            list.add(HIDE_COORDINATES)
        }
        else {
            list.add(SHOW_COORDINATES)
        }

        //download SGF doesn't work (disabled by OGS) when analysis is disabled
        if(!isAnalysisDisabled(game)) {
            list.add(ESTIMATE_SCORE)
            list.add(DOWNLOAD_SGF)
        }
        if(
                myGame
                && currentState in arrayOf(PLAYING, HISTORY, ANALYSIS)
                && game?.phase == Phase.PLAY
                && game?.playerToMoveId == userId
                && game?.undoRequested != null
        ) {
            list.add(ACCEPT_UNDO)
        }

        list.add(OPEN_IN_BROWSER)
        view.showMenu(list)
    }

    private fun canRequestUndo() =
            myGame && currentState == PLAYING
                    && game?.phase == Phase.PLAY
                    && game?.playerToMoveId != userId
                    && game?.moves?.isNotEmpty() == true
                    && game?.undoRequested != game?.moves?.size

    override fun onMenuItemSelected(item: MenuItem) {
        when(item) {
            GAME_INFO -> onGameInfoClicked()
            PASS -> onPassClicked()
            RESIGN -> onResignClicked()
            ESTIMATE_SCORE -> onEstimateClicked()
            ANALYZE -> onAnalyzeButtonClicked()
            SHOW_COORDINATES -> toggleCoordinates()
            HIDE_COORDINATES -> toggleCoordinates()
            DOWNLOAD_SGF -> onDownloadSGFClicked()
            ACCEPT_UNDO -> onAcceptUndo()
            REQUEST_UNDO -> onRequestUndo()
            ABORT_GAME -> onDiscardButtonPressed()
            OPEN_IN_BROWSER -> onOpenInBrowserClicked()
        }.let {  }
    }

    private fun onGameInfoClicked() {
        game?.let { view.showGameInfoDialog(it) }
    }

    private fun toggleCoordinates() {
        settingsRepository.showCoordinates = !settingsRepository.showCoordinates
        view.showCoordinates = settingsRepository.showCoordinates
    }

    private fun onDownloadSGFClicked() {
        view.navigateTo("https://online-go.com/api/v1/games/$gameId/sgf")
    }

    private fun onOpenInBrowserClicked() {
        view.navigateTo("https://online-go.com/game/$gameId")
    }

    private fun onEstimateClicked() {
        analytics.logEvent("estimate_clicked", null)
        game?.let { game ->
            val pos = when (currentState) {
                ANALYSIS -> analysisPosition
                HISTORY -> RulesManager.replay(game, currentShownMove, false)
                else -> currentPosition
            }
            stateToReturnFromEstimation = currentState
            currentState = ESTIMATION

            view.setLoading(true)
            Completable.fromAction { estimateTerritory(pos) }
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        view.setLoading(false)
                        refreshUI(game)
                    }
                    .addToDisposable(subscriptions)

        }
    }

    private fun estimateTerritory(pos: Position) {
        estimatePosition = RulesManager.determineTerritory(pos, game?.scoreStones == true)
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
        view.resignButtonVisible = false
        view.analyzeButtonVisible = !isAnalysisDisabled(game)
        view.analysisDisabledButtonVisible = isAnalysisDisabled(game)

        view.confirmButtonVisible = false
        view.discardButtonVisible = false
        view.autoButtonVisible = false

        view.passButtonEnabled = false
        view.undoButtonVisible = false
        view.undoButtonEnabled = false
    }

    private fun showPlayControls(game: Game) {
        view.bottomBarVisible = true
        view.menuButtonVisible = true

        view.nextButtonVisible = true
        view.previousButtonVisible = true
        view.resignButtonVisible = false
        view.analyzeButtonVisible = !isAnalysisDisabled(game)
        view.analysisDisabledButtonVisible = isAnalysisDisabled(game)

        view.confirmButtonVisible = false
        view.discardButtonVisible = game.moves?.size ?: 0 < 2
        view.autoButtonVisible = false

        view.passButtonVisible = (game.moves?.size ?: 0 >= 2) && game.phase == Phase.PLAY && game.playerToMoveId == userId
        view.passButtonEnabled = true

        view.undoButtonVisible = (game.moves?.size ?: 0 >= 2) && game.phase == Phase.PLAY && game.playerToMoveId != userId
        view.undoButtonEnabled = canRequestUndo()
    }

    private fun showAnalysisControls() {
        view.bottomBarVisible = true
        view.menuButtonVisible = true
        view.nextButtonVisible = true
        view.previousButtonVisible = true
        view.passButtonVisible = false
        view.resignButtonVisible = false
        view.analyzeButtonVisible = false
        view.analysisDisabledButtonVisible = false

        view.confirmButtonVisible = false
        view.discardButtonVisible = true
        view.autoButtonVisible = false

        view.passButtonEnabled = false
        view.undoButtonVisible = false
        view.undoButtonEnabled = false
    }

    private fun showStoneRemovalControls() {
        view.bottomBarVisible = true
        view.menuButtonVisible = false

        view.nextButtonVisible = false
        view.previousButtonVisible = false
        view.passButtonVisible = false
        view.resignButtonVisible = false
        view.analyzeButtonVisible = false
        view.analysisDisabledButtonVisible = false

        view.confirmButtonVisible = true
        view.discardButtonVisible = true
        view.autoButtonVisible = true
        view.undoButtonVisible = false
        view.undoButtonEnabled = false
    }

    private fun showSpectateControls() {
        view.bottomBarVisible = true
        view.menuButtonVisible = true

        view.nextButtonVisible = true
        view.previousButtonVisible = true
        view.passButtonVisible = false
        view.resignButtonVisible = false
        view.analyzeButtonVisible = true
        view.analysisDisabledButtonVisible = false

        view.confirmButtonVisible = false
        view.discardButtonVisible = false
        view.autoButtonVisible = false
        view.undoButtonVisible = false
        view.undoButtonEnabled = false
    }

    private fun showEstimationControls() {
        view.bottomBarVisible = true
        view.menuButtonVisible = false

        view.nextButtonVisible = false
        view.previousButtonVisible = false
        view.passButtonVisible = false
        view.resignButtonVisible = false
        view.analyzeButtonVisible = false
        view.analysisDisabledButtonVisible = false

        view.confirmButtonVisible = false
        view.discardButtonVisible = true
        view.autoButtonVisible = false
        view.undoButtonVisible = false
        view.undoButtonEnabled = false
    }

    private fun showConfirmMoveControls() {
        view.bottomBarVisible = true

        view.menuButtonVisible = false
        view.nextButtonVisible = false
        view.previousButtonVisible = false
        view.passButtonVisible = false
        view.resignButtonVisible = false
        view.analyzeButtonVisible = false
        view.analysisDisabledButtonVisible = false

        view.confirmButtonVisible = true
        view.discardButtonVisible = true
        view.autoButtonVisible = false
        view.undoButtonVisible = false
        view.undoButtonEnabled = false
    }

    private fun isAnalysisDisabled(game: Game?): Boolean {
        return game?.phase != Phase.FINISHED && game?.disableAnalysis == true
    }

    override fun onAutoButtonPressed() {
        if(game?.phase != Phase.STONE_REMOVAL) {
            return
        }
        analytics.logEvent("auto_clicked", null)

        view.setLoading(true)
        Completable.fromAction {
            val newPos = RulesManager.determineTerritory(currentPosition, game?.scoreStones == true)
            gameConnection?.submitRemovedStones(currentPosition.removedSpots, false)
            gameConnection?.submitRemovedStones(newPos.removedSpots, true)
        }
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                view.setLoading(false)
            }
            .addToDisposable(subscriptions)
    }

    private fun refreshUI(game: Game) {
        when (currentState) {
            ANALYSIS -> {
                replayAnalysis()
                view.position = analysisPosition
                val score = RulesManager.scorePositionOld(analysisPosition, game)
                view.whiteScore = score.first
                view.blackScore = score.second
            }
            HISTORY -> {
                val historyPosition = RulesManager.replay(game, currentShownMove, false)
                view.position = historyPosition
                val score = RulesManager.scorePositionOld(historyPosition, game)
                view.whiteScore = score.first
                view.blackScore = score.second
            }
            SCORING -> {
                currentPosition = RulesManager.replay(game, computeTerritory = true)
                view.position = currentPosition
                val score = RulesManager.scorePositionOld(currentPosition, game)
                view.whiteScore = score.first
                view.blackScore = score.second
            }
            ESTIMATION -> {
                val score = RulesManager.scorePositionOld(estimatePosition, game)
                view.whiteScore = score.first
                view.blackScore = score.second

                view.showTerritory = true
                view.position = estimatePosition
            }
            PLAYING, FINISHED, LOADING -> {
                currentPosition = RulesManager.replay(game, computeTerritory = false)
                view.position = currentPosition
                val score = RulesManager.scorePositionOld(currentPosition, game)
                view.whiteScore = score.first
                view.blackScore = score.second
            }
        }

        showControls(game)
        configureBoard(game)
        configureChips(game)
        configurePassedLabels()
        configurePreviousNextButtons()
        configurePlayerStatus()

        game.undoRequested?.let {
            if(it != undoPromptShownAtMoveNo && game.playerToMoveId == userId) {
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

    override fun onAcceptUndo() {
        analytics.logEvent("undo_accepted", null)
        gameConnection?.acceptUndo(undoPromptShownAtMoveNo)
        undoPromptShownAtMoveNo = -1
    }

    override fun onRequestUndo() {
        analytics.logEvent("undo_clicked", null)
        view.showUndoRequestConfirmation()
    }

    override fun onUndoRequestConfirmed() {
        analytics.logEvent("undo_requested", null)
        gameConnection?.requestUndo(game?.moves?.size ?: 0)
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
                val lastMoveWasAPass = game?.moves?.lastOrNull()?.x == -1
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
            LOADING, PLAYING -> {
                val lastMoveWasAPass = game.moves?.lastOrNull()?.x == -1
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

    private fun clockTick(game: Game?) {
        var desiredTimerIntervalMillis: Long? = null
        game?.clock?.let { clock ->
            val whiteToMove = game.playerToMoveId == game.whitePlayer.id
            val blackToMove = game.playerToMoveId == game.blackPlayer.id

            val whiteTimer = computeTimeLeft(clock, clock.whiteTimeSimple, clock.whiteTime, whiteToMove, game.pausedSince)
            val blackTimer = computeTimeLeft(clock, clock.blackTimeSimple, clock.blackTime, blackToMove, game.pausedSince)

            var timeLeft = null as Long?

            if (clock.startMode == true) {
                clock.expiration?.let { expiration ->
                    timeLeft = expiration - clockDriftRepository.serverTime
                    val startTimerDetails = TimerDetails(
                            expired = timeLeft!! < 0,
                            firstLine = formatMillis(timeLeft!!),
                            secondLine = "(start)",
                            timeLeft = timeLeft!!
                    )
                    if(whiteToMove) {
                        view.whiteTimer = startTimerDetails
                        view.blackTimer = blackTimer
                    } else {
                        view.whiteTimer = whiteTimer
                        view.blackTimer = startTimerDetails
                    }
                } ?: Unit
            } else {
                if((game.phase == Phase.PLAY || game.phase == Phase.STONE_REMOVAL) && currentState != LOADING) {

                    view.whiteTimer = whiteTimer
                    view.blackTimer = blackTimer

                    timeLeft = if(whiteToMove) whiteTimer.timeLeft else blackTimer.timeLeft

                }
            }
            desiredTimerIntervalMillis = timeLeft?.let {
                when(it) {
                    in 0 until 10_000 -> 100
                    in 10_000 until 3_600_000 -> 1_000
                    in 3_600_000 until 24 * 3_600_000 -> 60_000
                    else -> null
                }
            }
        }

        if(timerIntervalMillis?.toLong() != desiredTimerIntervalMillis) {
            rescheduleTimerTick(desiredTimerIntervalMillis)
        }
    }

    @Synchronized
    private fun rescheduleTimerTick(newTimerTick: Long?) {
        timerDisposable?.dispose()
        newTimerTick?.let {
            FirebaseCrashlytics.getInstance().log("GamePresenter: Scheduling timer with interval $newTimerTick milliseconds")
            timerDisposable = Observable.interval(it, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { clockTick(game) }
            timerIntervalMillis = AtomicLong(it)
        }
    }

    override fun onResignConfirmed() {
        analytics.logEvent("resign_confirmed", null)
        gameConnection?.resign()
    }


    override fun onPassConfirmed() {
        analytics.logEvent("pass_confirmed", null)
        gameConnection?.submitMove(Cell(-1, -1))
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
        analysisPosition = analysisRootPosition
        variation.take(variationCurrentMove + 1).let { truncatedVariation ->
            truncatedVariation.forEach {
                RulesManager.makeMove(analysisPosition, analysisPosition.nextToMove, it)?.let {
                    analysisPosition = it
                }
            }
            analysisPosition = analysisPosition.copy(
                variation = truncatedVariation
            )
        }
    }

    override fun onAnalyzeButtonClicked() {
        analytics.logEvent("analyze_clicked", null)
        analysisRootPosition = if(currentState == HISTORY) RulesManager.replay(game!!, currentShownMove, false) else currentPosition
        currentState = ANALYSIS
        variation.clear()
        variationCurrentMove = -1
        game?.let(this::refreshUI)
    }

    override fun onAnalysisDisabledButtonClicked() {
        analytics.logEvent("analysis_disabled_clicked", null)
        view.showAnalysisDisabledDialog()
    }

    private fun configurePlayerStatus() {
        when (currentState) {
            LOADING, FINISHED, ESTIMATION -> {
                view.setWhitePlayerStatus(null)
                view.setBlackPlayerStatus(null)
            }
            SCORING -> {
                if(game?.removedStones != null && game?.removedStones == game?.whitePlayer?.acceptedStones) {
                    view.setWhitePlayerStatus("Accepted", R.color.colorPrimary)
                } else {
                    view.setWhitePlayerStatus("Not accepted")
                }
                if(game?.removedStones != null && game?.removedStones == game?.blackPlayer?.acceptedStones) {
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
                    val requestedUndo = game?.undoRequested == game?.moves?.size
                    view.setWhitePlayerStatus( if(requestedUndo) "Undo requested!" else null, R.color.colorPrimary )
                }
                if(currentPosition.nextToMove == StoneType.BLACK) {
                    val prefix = if(game?.blackPlayer?.id == userId) "Your" else "Their"
                    view.setBlackPlayerStatus("$prefix turn")
                } else {
                    val requestedUndo = game?.undoRequested == game?.moves?.size
                    view.setBlackPlayerStatus(if(requestedUndo) "Undo requested!" else null, R.color.colorPrimary )
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
        view.setChips(emptyList())
        subscriptions.clear()
        timerDisposable?.dispose()
    }

    data class TimerDetails (
        var expired: Boolean,
        var firstLine: String? = null,
        var secondLine: String? = null,
        var timeLeft: Long
    )
}