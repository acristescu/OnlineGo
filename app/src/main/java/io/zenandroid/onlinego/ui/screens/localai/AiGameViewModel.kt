package io.zenandroid.onlinego.ui.screens.localai

import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.ai.KataGoAnalysisEngine
import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.data.model.katago.KataGoResponse.Response
import io.zenandroid.onlinego.data.model.katago.MoveInfo
import io.zenandroid.onlinego.data.model.katago.OverrideSettings
import io.zenandroid.onlinego.data.model.katago.RootInfo
import io.zenandroid.onlinego.data.repositories.SettingsRepository
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import io.zenandroid.onlinego.gamelogic.RulesManager
import io.zenandroid.onlinego.gamelogic.RulesManager.isGameOver
import io.zenandroid.onlinego.gamelogic.Util
import io.zenandroid.onlinego.gamelogic.Util.toGTP
import io.zenandroid.onlinego.ui.composables.TextResource
import io.zenandroid.onlinego.ui.composables.textResource
import io.zenandroid.onlinego.utils.moshiadapters.AiDifficultyMoshiAdapter
import io.zenandroid.onlinego.utils.moshiadapters.HashMapOfCellToStoneTypeMoshiAdapter
import io.zenandroid.onlinego.utils.moshiadapters.ResponseBriefMoshiAdapter
import io.zenandroid.onlinego.utils.recordException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.random.Random

class AiGameViewModel(
  private val userSessionRepository: UserSessionRepository,
  private val settingsRepository: SettingsRepository,
  private val applicationCoroutineScope: CoroutineScope,
) : ViewModel() {

  private val _state = MutableStateFlow(
    AiGameState(
      userIcon = userSessionRepository.uiConfig?.user?.icon,
    )
  )
  val state: StateFlow<AiGameState> = _state.asStateFlow()
  private var katagoJob: Job? = null
  private var hintJob: Job? = null
  private var ownershipJob: Job? = null
  private var finalScoreJob: Job? = null

  private val stateAdapter = Moshi.Builder()
    .add(ResponseBriefMoshiAdapter())
    .add(HashMapOfCellToStoneTypeMoshiAdapter())
    .add(AiDifficultyMoshiAdapter())
    .add(KotlinJsonAdapterFactory())
    .build()
    .adapter(AiGameState::class.java)

  init {
    startEngine()
    restoreState()
  }

  private fun startEngine() {
    viewModelScope.launch {
      try {
        withContext(Dispatchers.IO) {
          KataGoAnalysisEngine.start()
          _state.update { it.copy(engineStarted = true) }
        }
        onLoadingComplete()
      } catch (e: Exception) {
        recordException(e)
        _state.update {
          it.copy(
            boardIsInteractive = false,
            hintButtonVisible = false,
            ownershipButtonVisible = false,
            engineFailedToStart = true,
            chatText = textResource(R.string.ai_game_chat_engine_error, e.message ?: "")
          )
        }
      }
    }
  }

  private fun onLoadingComplete() {
    val currentState = state.value
    if (!currentState.isGameReady) return

    val position = currentState.position
    if (position == null) {
      _state.update {
        it.copy(
          chatText = if (it.newGameDialogShown) TextResource(R.string.ai_game_chat_ready)
          else TextResource(R.string.ai_game_chat_use_new_game_button)
        )
      }
      return
    }
    if (currentState.history.isGameOver()) {
      if (currentState.aiWon == null) {
        finalScoreJob?.cancel()
        finalScoreJob = viewModelScope.launch { computeFinalScore() }
      }
      return
    }
    if (isEnginesTurn(position, currentState.enginePlaysBlack)) {
      generateAiMove()
    }
  }

  private suspend fun validState(state: AiGameState): Boolean {
    if (state.history.isNotEmpty()) {
      val whiteInitial = state.history[0].whiteStones
      val blackInitial = state.history[0].blackStones
      val moves = mutableListOf<Cell>()
      state.history.drop(1).forEach {
        if (it.lastMove == null || it.boardHeight != state.boardSize) {
          FirebaseCrashlytics.getInstance()
            .log("Invalid position in history: lastMove=${it.lastMove} boardHeight=${it.boardHeight} boardSize=${state.boardSize}")
          return false
        }
        moves.add(it.lastMove)
        val pos = RulesManager.buildPos(
          moves,
          state.boardSize,
          state.boardSize,
          state.handicap,
          whiteInitialState = whiteInitial,
          blackInitialState = blackInitial
        )
        if (pos == null) {
          FirebaseCrashlytics.getInstance()
            .log("Invalid history: ${moves.toGTP(it.boardHeight)} whiteInitial=$whiteInitial blackInitial=$blackInitial")
          return false
        }
      }
    }
    return true
  }

  private fun restoreState() {
    viewModelScope.launch(Dispatchers.Default) {
      val json = settingsRepository.aiGameStateFlow.first()

      val newState = if (!json.isNullOrBlank()) {
        try {
          stateAdapter.fromJson(json)?.takeIf { validState(it) }
        } catch (e: Exception) {
          Log.e("AiGameViewModel", "Cannot deserialize state", e)
          recordException(e)
          null
        }
      } else {
        null
      }

      if (newState != null) {
        _state.update { state ->
          newState.copy(
            engineStarted = state.engineStarted,
            engineFailedToStart = state.engineFailedToStart,
            stateRestorePending = false,
            userIcon = userSessionRepository.uiConfig?.user?.icon,
          )
        }
      } else {
        _state.update {
          it.copy(
            newGameDialogShown = true,
            stateRestorePending = false
          )
        }
      }
      onLoadingComplete()
    }
  }

  fun onViewPaused() {
    viewModelScope.launch {
      val json = stateAdapter.toJson(
        state.value.copy(
          aiAnalysis = null,
        )
      )
      settingsRepository.setAiGameState(json)
    }
  }

  fun onShowNewGameDialog() {
    if (!state.value.isGameReady) return
    _state.update { it.copy(newGameDialogShown = true) }
  }

  fun onDismissNewGameDialog() {
    _state.update {
      it.copy(
        newGameDialogShown = false,
        chatText = if (it.position == null) TextResource(R.string.ai_game_chat_use_new_game_button) else it.chatText
      )
    }
  }

  fun onNewGame(size: Int, youPlayBlack: Boolean, handicap: Int, difficulty: AiDifficulty) {
    if (!state.value.isGameReady) return
    katagoJob?.cancel()
    hintJob?.cancel()
    ownershipJob?.cancel()
    finalScoreJob?.cancel()
    val newPosition = RulesManager.initializePosition(size, handicap)
    _state.update {
      it.copy(
        boardSize = size,
        handicap = handicap,
        difficulty = difficulty,
        enginePlaysBlack = !youPlayBlack,
        newGameDialogShown = false,
        showHints = false,
        aiWon = null,
        finalWhiteScore = null,
        finalBlackScore = null,
        showFinalTerritory = false,
        hintButtonVisible = true,
        ownershipButtonVisible = true,
        showAiEstimatedTerritory = false,
        nextButtonEnabled = false,
        passButtonEnabled = false,
        chatText = null,
        previousButtonEnabled = false,
        boardIsInteractive = false,
        redoPosStack = emptyList(),
        candidateMove = null,
        history = emptyList(),
        position = newPosition,
        aiAnalysis = null,
        aiQuickEstimation = null,
        stateRestorePending = false,
        consecutiveLowWinrateTurns = 0,
        aiResignOfferDeclined = false,
        aiResignOfferShowing = false,
      )
    }
    updatePosition(newPosition)
  }

  fun onUserTappedCoordinate(coordinate: Cell) {
    val currentState = state.value
    if (!currentState.isGameReady || !currentState.boardIsInteractive || currentState.position == null) return
    viewModelScope.launch(Dispatchers.Default) {

      val side = if (currentState.enginePlaysBlack) StoneType.WHITE else StoneType.BLACK
      val newPosition = RulesManager.makeMove(currentState.position, side, coordinate)

      if (newPosition != null) {
        val potentialKOPosition = if (currentState.history.size > 1 && !coordinate.isPass) {
          currentState.history[currentState.history.size - 2]
        } else null
        if (potentialKOPosition?.hasTheSameStonesAs(newPosition) == true) {
          _state.update {
            it.copy(
              candidateMove = null,
              koMoveDialogShowing = true,
              chatText = TextResource(R.string.ai_game_chat_invalid_move)
            )
          }
        } else {
          updatePosition(newPosition)
        }
      } else {
        _state.update {
          it.copy(
            candidateMove = null
          )
        }
      }
    }
  }

  fun onDismissKoDialog() {
    _state.update {
      it.copy(
        koMoveDialogShowing = false,
        candidateMove = null,
        chatText = TextResource(R.string.ai_game_chat_invalid_move)
      )
    }
  }

  fun onAiResignOfferAccepted() {
    _state.update {
      it.copy(
        aiResignOfferShowing = false,
        boardIsInteractive = false,
        passButtonEnabled = false,
        nextButtonEnabled = false,
        previousButtonEnabled = true,
        hintButtonVisible = false,
        ownershipButtonVisible = false,
        showHints = false,
        showAiEstimatedTerritory = false,
        candidateMove = null,
        aiWon = false,
        chatText = TextResource(R.string.ai_game_chat_ai_resigned),
      )
    }
  }

  fun onAiResignOfferDeclined() {
    _state.update {
      it.copy(
        aiResignOfferShowing = false,
        aiResignOfferDeclined = true,
        consecutiveLowWinrateTurns = 0,
      )
    }
    generateAiMove()
  }

  fun onUserHotTrackedCoordinate(coordinate: Cell) {
    _state.update { it.copy(candidateMove = coordinate) }
  }

  fun onUserPressedPass() {
    val currentState = state.value
    if (!currentState.isGameReady || !currentState.boardIsInteractive || currentState.position == null) return
    viewModelScope.launch(Dispatchers.Default) {

      val side = if (currentState.enginePlaysBlack) StoneType.WHITE else StoneType.BLACK
      val newPosition = RulesManager.makeMove(currentState.position, side, Cell.PASS)

      if (newPosition != null) {
        updatePosition(newPosition)
      }
    }
  }

  fun onUserPressedPrevious() {
    val currentState = state.value
    if (!currentState.isGameReady) return
    val newHistory = currentState.history.dropLast(2)
    _state.update {
      it.copy(
        position = newHistory.lastOrNull(),
        redoPosStack = it.redoPosStack + it.history.takeLast(2),
        history = newHistory,
        previousButtonEnabled = newHistory.size > 2,
        showHints = false,
        hintButtonVisible = true,
        ownershipButtonVisible = true,
        showFinalTerritory = false,
        showAiEstimatedTerritory = false,
        nextButtonEnabled = true,
        boardIsInteractive = true,
        passButtonEnabled = true,
        chatText = TextResource(R.string.ai_game_chat_lets_try_again),
        aiWon = null,
        finalBlackScore = null,
        finalWhiteScore = null
      )
    }
  }

  fun onUserPressedNext() {
    val currentState = state.value
    if (!currentState.isGameReady) return
    val newHistory = currentState.history + currentState.redoPosStack.takeLast(2)
    _state.update {
      it.copy(
        position = newHistory.lastOrNull(),
        history = newHistory,
        redoPosStack = it.redoPosStack.dropLast(2),
        previousButtonEnabled = true,
        showHints = false,
        nextButtonEnabled = it.redoPosStack.size > 2
      )
    }
  }

  fun onUserAskedForHint() {
    hintJob?.cancel()
    hintJob = viewModelScope.launch {
      val currentState = state.value
      if (!currentState.isGameReady || currentState.position == null) return@launch

      _state.update { it.copy(chatText = TextResource(R.string.ai_game_chat_hmmm)) }

      try {
        val analysis = withContext(Dispatchers.IO) {
          KataGoAnalysisEngine.analyzeMoveSequence(
            sequence = currentState.history,
            maxVisits = 30,
            komi = currentState.position.komi,
            includeOwnership = false
          )
        }
        _state.update {
          it.copy(
            showHints = true,
            aiAnalysis = analysis,
            chatText = TextResource(R.string.ai_game_chat_moves_to_consider)
          )
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        recordException(e)
      }
    }
  }

  fun onUserAskedForOwnership() {
    ownershipJob?.cancel()
    ownershipJob = viewModelScope.launch {
      val currentState = state.value
      if (!currentState.isGameReady || currentState.position == null) return@launch

      if (currentState.showAiEstimatedTerritory) {
        _state.update {
          it.copy(
            showAiEstimatedTerritory = false,
            chatText = TextResource(R.string.ai_game_chat_ok_your_turn),
            boardIsInteractive = true
          )
        }
        return@launch
      }

      _state.update {
        it.copy(
          boardIsInteractive = false,
          chatText = TextResource(R.string.ai_game_chat_calculating_territory)
        )
      }

      try {
        val analysis = withContext(Dispatchers.IO) {
          KataGoAnalysisEngine.analyzeMoveSequence(
            sequence = currentState.history,
            maxVisits = 30,
            komi = currentState.position.komi,
            includeOwnership = true
          )
        }
        _state.update {
          it.copy(
            boardIsInteractive = true,
            aiAnalysis = analysis,
            showAiEstimatedTerritory = true,
            chatText = TextResource(R.string.ai_game_chat_territories)
          )
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        recordException(e)
      }
    }
  }

  private fun updatePosition(newPosition: Position) {
    val currentState = state.value
    val newVariation = if (currentState.history.lastOrNull() == newPosition) {
      currentState.history
    } else {
      currentState.history + newPosition
    }

    _state.update {
      it.copy(
        position = newPosition,
        history = newVariation,
        nextButtonEnabled = false,
        redoPosStack = emptyList(),
        boardIsInteractive = false,
        showHints = false,
        chatText = when {
          newVariation.isGameOver() && it.aiWon == true ->
            textResource(
              R.string.ai_game_chat_game_over_ai_won,
              it.finalBlackScore?.toInt().toString(),
              it.finalWhiteScore.toString()
            )

          newVariation.isGameOver() && it.aiWon == false ->
            textResource(
              R.string.ai_game_chat_game_over_player_won,
              it.finalBlackScore?.toInt().toString(),
              it.finalWhiteScore.toString()
            )

          newVariation.isGameOver() && it.aiWon == null ->
            TextResource(R.string.ai_game_chat_game_over_computing_score)

          else -> it.chatText
        },
        showAiEstimatedTerritory = false,
        showFinalTerritory = newVariation.isGameOver() && it.aiWon != null,
        hintButtonVisible = !newVariation.isGameOver(),
        ownershipButtonVisible = !newVariation.isGameOver(),
        candidateMove = null,
      )
    }

    if (newVariation.isGameOver()) {
      if (currentState.aiWon == null) {
        finalScoreJob?.cancel()
        finalScoreJob = viewModelScope.launch { computeFinalScore() }
      }
    } else {
      val isBlacksTurn = newPosition.nextToMove != StoneType.WHITE
      if (isBlacksTurn == currentState.enginePlaysBlack) {
        generateAiMove()
      } else {
        _state.update {
          it.copy(
            boardIsInteractive = true,
            passButtonEnabled = true,
            hintButtonVisible = true,
            ownershipButtonVisible = true,
            previousButtonEnabled = it.history.size > 2,
            nextButtonEnabled = false,
          )
        }
      }
    }
  }

  private fun generateAiMove() {
    val currentState = state.value
    if (!currentState.engineStarted || currentState.position == null) return

    _state.update {
      it.copy(
        boardIsInteractive = false,
        passButtonEnabled = false,
        previousButtonEnabled = false,
        nextButtonEnabled = false,
        hintButtonVisible = false,
        ownershipButtonVisible = false,
        chatText = TextResource(R.string.ai_game_chat_im_thinking)
      )
    }

    katagoJob?.cancel()
    katagoJob = viewModelScope.launch {
      try {
        val difficulty = currentState.difficulty
        // KataGo spins up this many search threads per query regardless of maxVisits; a
        // lower budget just wastes most of them in a thread-scheduling race for the rest.
        val effectiveMaxVisits = maxOf(difficulty.maxVisits, KataGoAnalysisEngine.searchThreads)
        val analysis = withContext(Dispatchers.IO) {
          KataGoAnalysisEngine.analyzeMoveSequence(
            sequence = currentState.history,
            maxVisits = effectiveMaxVisits,
            komi = currentState.position.komi,
            includeOwnership = false,
            includeMovesOwnership = false,
            includePolicy = difficulty.humanSLProfile != null,
            overrideSettings = difficulty.humanSLProfile?.let { OverrideSettings(humanSLProfile = it) },
          )
        }
        Log.d(
          "AiMoveDebug",
          "tier=${difficulty.name} maxVisits=${effectiveMaxVisits} " +
              "humanSLProfile=${difficulty.humanSLProfile} " +
              "candidates=${analysis.moveInfos.size} " +
              "moves=${
                analysis.moveInfos.sortedByDescending { it.visits }
                  .joinToString { "${it.move}(v=${it.visits},wr=%.3f)".format(it.winrate) }
              }"
        )

        val engineWinrate =
          orientedWinrateForEngine(analysis.rootInfo.winrate ?: 0.5f, currentState.enginePlaysBlack)

        hopelessPassMove(engineWinrate, analysis.moveInfos)?.let { passMove ->
          withContext(Dispatchers.Default) {
            applySelectedMove(passMove, analysis, currentState, currentState.position)
          }
          return@launch
        }

        if (!currentState.aiResignOfferDeclined && currentState.history.size > AI_RESIGN_MIN_MOVE_NUMBER) {
          if (engineWinrate < AI_RESIGN_WINRATE_THRESHOLD) {
            val newCount = currentState.consecutiveLowWinrateTurns + 1
            if (newCount >= AI_RESIGN_CONSECUTIVE_TURNS) {
              _state.update {
                it.copy(
                  consecutiveLowWinrateTurns = newCount,
                  aiResignOfferShowing = true,
                  boardIsInteractive = false,
                )
              }
              return@launch
            }
            _state.update { it.copy(consecutiveLowWinrateTurns = newCount) }
          } else {
            _state.update { it.copy(consecutiveLowWinrateTurns = 0) }
          }
        }

        withContext(Dispatchers.Default) {
          val selectedMove = difficulty.humanSLProfile?.let { profile ->
            selectHumanMove(
              humanPolicy = analysis.humanPolicy
                ?: throw IllegalStateException("Tier ${difficulty.name} (humanSLProfile=$profile) got a response with no humanPolicy"),
              moveInfos = analysis.moveInfos,
              rootInfo = analysis.rootInfo,
              boardWidth = currentState.position.boardWidth,
              boardHeight = currentState.position.boardHeight,
            )
          } ?: selectBestMove(analysis.moveInfos)
          applySelectedMove(selectedMove, analysis, currentState, currentState.position)
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        recordException(e)
      }
    }
  }

  private suspend fun applySelectedMove(
    selectedMove: MoveInfo,
    analysis: Response,
    currentState: AiGameState,
    position: Position,
  ) {
    Log.d(
      "AiMoveDebug",
      "picked=${selectedMove.move}(v=${selectedMove.visits},wr=%.3f)".format(selectedMove.winrate)
    )
    val move = Util.getCoordinatesFromGTP(selectedMove.move, position.boardHeight)
    val side = if (currentState.enginePlaysBlack) StoneType.BLACK else StoneType.WHITE
    val newPosition = RulesManager.makeMove(position, side, move)

    if (newPosition == null) {
      recordException(Exception("KataGO wants to play move ${selectedMove.move} ($move), but RulesManager rejects it as invalid"))
    } else {
      val newVariation = if (currentState.history.lastOrNull() == newPosition) {
        currentState.history
      } else {
        currentState.history + newPosition
      }
      _state.update {
        it.copy(
          position = newPosition,
          history = newVariation,
          nextButtonEnabled = false,
          aiAnalysis = analysis,
          aiQuickEstimation = selectedMove,
          previousButtonEnabled = newVariation.size > 2,
          showFinalTerritory = newVariation.isGameOver(),
          chatText = when {
            newVariation.isGameOver() && it.aiWon == true ->
              textResource(
                R.string.ai_game_chat_game_over_ai_won,
                it.finalBlackScore?.toInt().toString(),
                it.finalWhiteScore.toString()
              )

            newVariation.isGameOver() && it.aiWon == false ->
              textResource(
                R.string.ai_game_chat_game_over_player_won,
                it.finalBlackScore?.toInt().toString(),
                it.finalWhiteScore.toString()
              )

            newVariation.isGameOver() && it.aiWon == null ->
              TextResource(R.string.ai_game_chat_game_over_computing_score)

            selectedMove.move.equals("pass", ignoreCase = true) ->
              TextResource(R.string.ai_game_chat_ai_passed)

            else -> TextResource(R.string.ai_game_chat_your_turn)
          }
        )
      }

      if (newVariation.isGameOver()) {
        computeFinalScore()
      } else {
        _state.update {
          it.copy(
            boardIsInteractive = true,
            passButtonEnabled = true,
            hintButtonVisible = true,
            ownershipButtonVisible = true
          )
        }
      }
    }
  }

  private suspend fun computeFinalScore() {
    val currentState = state.value
    if (!currentState.engineStarted || currentState.position == null) return

    try {
      val analysis = withContext(Dispatchers.IO) {
        KataGoAnalysisEngine.analyzeMoveSequence(
          sequence = currentState.history,
          maxVisits = 10,
          komi = currentState.position.komi,
          includeOwnership = true
        )
      }

      val blackTerritory = mutableSetOf<Cell>()
      val whiteTerritory = mutableSetOf<Cell>()
      val removedSpots = mutableSetOf<Cell>()

      analysis.ownership?.forEachIndexed { index, value ->
        val y = index / currentState.position.boardWidth
        val x = index % currentState.position.boardWidth
        val cell = Cell(x, y)
        when {
          value > 0.6 -> whiteTerritory.add(cell)
          value < -0.6 -> blackTerritory.add(cell)
          abs(value) <= 0.6 -> removedSpots.add(cell)
        }
      }

      val blackScore = blackTerritory.size + currentState.position.blackCaptureCount
      val whiteScore =
        whiteTerritory.size + currentState.position.whiteCaptureCount + (currentState.position.komi
          ?: 0f)
      val aiWon =
        if (currentState.enginePlaysBlack) blackScore > whiteScore else whiteScore > blackScore

      _state.update {
        it.copy(
          position = currentState.position.copy(
            blackTerritory = blackTerritory,
            whiteTerritory = whiteTerritory,
            removedSpots = removedSpots,
            whiteCaptureCount = currentState.position.whiteCaptureCount,
            blackCaptureCount = currentState.position.blackCaptureCount
          ),
          history = it.history.dropLast(1) + currentState.position,
          nextButtonEnabled = false,
          passButtonEnabled = false,
          redoPosStack = emptyList(),
          boardIsInteractive = false,
          chatText = if (aiWon)
            textResource(
              R.string.ai_game_chat_game_over_ai_won,
              blackScore.toString(),
              whiteScore.toString()
            )
          else
            textResource(
              R.string.ai_game_chat_game_over_player_won,
              blackScore.toString(),
              whiteScore.toString()
            ),
          finalWhiteScore = whiteScore,
          finalBlackScore = blackScore.toFloat(),
          aiWon = aiWon,
          previousButtonEnabled = true,
          showAiEstimatedTerritory = false,
          showFinalTerritory = true,
          hintButtonVisible = false,
          ownershipButtonVisible = false,
          showHints = false,
          candidateMove = null,
          aiAnalysis = analysis
        )
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      recordException(e)
    }
  }

  override fun onCleared() {
    super.onCleared()
    katagoJob?.cancel()
    hintJob?.cancel()
    ownershipJob?.cancel()
    finalScoreJob?.cancel()
    applicationCoroutineScope.launch(Dispatchers.IO) {
      KataGoAnalysisEngine.stop()
    }
  }
}

private const val AI_RESIGN_WINRATE_THRESHOLD = 0.02f
private const val AI_RESIGN_CONSECUTIVE_TURNS = 5
private const val AI_RESIGN_MIN_MOVE_NUMBER = 20
private const val AI_HOPELESS_PASS_WINRATE_THRESHOLD = 0.01f

/**
 * At this low a visit budget, whether pass ends up as moveInfos[0] is mostly a thread-
 * scheduling race, not a real signal that some other move is better. Once winrate says
 * the position is essentially decided, pass is effectively free either way, so it's
 * enough for pass to be considered a candidate at all - it doesn't need to win that race.
 */
@VisibleForTesting
fun hopelessPassMove(engineWinrate: Float, moveInfos: List<MoveInfo>): MoveInfo? {
  if (engineWinrate >= AI_HOPELESS_PASS_WINRATE_THRESHOLD) return null
  return moveInfos.find { it.move.equals("pass", ignoreCase = true) }
}

/**
 * katago.cfg sets reportAnalysisWinratesAs = WHITE, so every winrate KataGo reports is
 * always relative to White, regardless of whose turn it is or who the engine is playing.
 * This orients it to the engine's own side, which the resignation check needs.
 */
@VisibleForTesting
fun orientedWinrateForEngine(rawWinrate: Float, enginePlaysBlack: Boolean): Float =
  if (enginePlaysBlack) 1f - rawWinrate else rawWinrate

/**
 * Samples a move by weighted draw over KataGo's humanPolicy array (illegal points marked
 * -1, filtered before sampling) - the official Human SL recipe, no temperature needed since
 * humanPolicy is already a calibrated probability distribution. Row-major flattening,
 * boardWidth*boardHeight index is pass (same convention as `ownership`).
 *
 * Returns a MoveInfo: reuses the real search stats when the sampled point was also
 * explored, otherwise synthesizes one from rootInfo.
 *
 * Exception: if the real search already ranks pass as its top move, play it directly -
 * once a position is truly decided, humanPolicy alone still only gives pass a modest,
 * human-like weight and could keep the game going indefinitely.
 */
@VisibleForTesting
fun selectHumanMove(
  humanPolicy: List<Float>,
  moveInfos: List<MoveInfo>,
  rootInfo: RootInfo,
  boardWidth: Int,
  boardHeight: Int,
  random: Random = Random,
): MoveInfo {
  moveInfos.firstOrNull()?.takeIf { it.move.equals("pass", ignoreCase = true) }?.let { return it }

  val legal = humanPolicy.withIndex().filter { it.value >= 0f }
  check(legal.isNotEmpty()) { "humanPolicy had no legal moves" }
  val total = legal.sumOf { it.value.toDouble() }
  var remaining = random.nextDouble() * total
  var chosenIndex = legal.last().index
  for ((index, weight) in legal) {
    remaining -= weight
    if (remaining <= 0.0) {
      chosenIndex = index
      break
    }
  }
  val cell = if (chosenIndex == boardWidth * boardHeight) {
    Cell.PASS
  } else {
    Cell(chosenIndex % boardWidth, chosenIndex / boardWidth)
  }
  val gtpMove = Util.getGTPCoordinates(cell, boardHeight)
  return moveInfos.find { it.move.equals(gtpMove, ignoreCase = true) }
    ?: MoveInfo(
      move = gtpMove,
      visits = 0,
      winrate = rootInfo.winrate ?: 0.5f,
      scoreStdev = rootInfo.scoreStdev ?: 0f,
      scoreLead = rootInfo.scoreLead ?: 0f,
      scoreSelfplay = rootInfo.scoreSelfplay ?: 0f,
      prior = humanPolicy[chosenIndex],
      utility = rootInfo.utility ?: 0f,
      lcb = 0f,
      utilityLcb = 0f,
      order = -1,
      pv = emptyList(),
      pvVisits = null,
    )
}

/** Dan 5 skips Human SL sampling and plays KataGo's own top-ranked candidate outright - relies on KataGo's JSON already ordering moveInfos best-first. */
@VisibleForTesting
fun selectBestMove(moveInfos: List<MoveInfo>): MoveInfo = moveInfos[0]

@VisibleForTesting
fun isEnginesTurn(position: Position, enginePlaysBlack: Boolean): Boolean =
  (position.nextToMove != StoneType.WHITE) == enginePlaysBlack