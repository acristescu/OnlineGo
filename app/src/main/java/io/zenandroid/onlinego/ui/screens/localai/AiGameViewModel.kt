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
import io.zenandroid.onlinego.data.model.katago.MoveInfo
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
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
  private var katagoJob: kotlinx.coroutines.Job? = null
  private var hintJob: kotlinx.coroutines.Job? = null
  private var ownershipJob: kotlinx.coroutines.Job? = null
  private var finalScoreJob: kotlinx.coroutines.Job? = null

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
          _state.update {
            it.copy(
              engineStarted = true,
              chatText = when {
                it.position == null && it.newGameDialogShown -> TextResource(R.string.ai_game_chat_ready)
                it.position == null && !it.newGameDialogShown -> TextResource(R.string.ai_game_chat_use_new_game_button)
                else -> it.chatText
              }
            )
          }
        }
      } catch (e: Exception) {
        recordException(e)
        _state.update {
          it.copy(
            boardIsInteractive = false,
            hintButtonVisible = false,
            ownershipButtonVisible = false,
            chatText = textResource(R.string.ai_game_chat_engine_error, e.message ?: "")
          )
        }
      }
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

      if (!json.isNullOrBlank()) {
        val newState = try {
          stateAdapter.fromJson(json)
        } catch (e: Exception) {
          Log.e("StatePersistenceMiddlew", "Cannot deserialize state", e)
          recordException(e)
          null
        }
        newState?.let { newState ->
          if (validState(newState)) {
            _state.update { state ->
              newState.copy(
                engineStarted = state.engineStarted,
                stateRestorePending = false,
                userIcon = userSessionRepository.uiConfig?.user?.icon,
              )
            }
          }
        }
      } else {
        _state.update {
          it.copy(
            newGameDialogShown = true,
            stateRestorePending = false
          )
        }
      }
    }
  }

  fun onViewPaused() {
    viewModelScope.launch {
      val json = stateAdapter.toJson(
        state.value.copy(
          aiAnalysis = null,
          aiQuickEstimation = null,
        )
      )
      settingsRepository.setAiGameState(json)
    }
  }

  fun onShowNewGameDialog() {
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
    // kill any in-flight Katago request(s) as they are now irrelevant
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
      )
    }
    updatePosition(newPosition)
  }

  fun onUserTappedCoordinate(coordinate: Cell) {
    val currentState = state.value
    if (!currentState.boardIsInteractive || currentState.position == null) return
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

  fun onUserHotTrackedCoordinate(coordinate: Cell) {
    _state.update { it.copy(candidateMove = coordinate) }
  }

  fun onUserPressedPass() {
    val currentState = state.value
    if (!currentState.boardIsInteractive || currentState.position == null) return
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
      if (!currentState.engineStarted || currentState.position == null) return@launch

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
      if (!currentState.engineStarted || currentState.position == null) return@launch

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
        val analysis = withContext(Dispatchers.IO) {
          KataGoAnalysisEngine.analyzeMoveSequence(
            sequence = currentState.history,
            maxVisits = currentState.difficulty.maxVisits,
            komi = currentState.position.komi,
            includeOwnership = false,
            includeMovesOwnership = false
          )
        }
        withContext(Dispatchers.Default) {
          val selectedMove = selectAiMove(
            moveInfos = analysis.moveInfos,
            temperature = currentState.difficulty.temperature,
            boardHeight = currentState.position.boardHeight,
            lastMove = currentState.position.lastMove,
            localitySigma = effectiveLocalitySigma(
              localitySigma = currentState.difficulty.localitySigma,
              plyNumber = currentState.history.size,
              boardWidth = currentState.position.boardWidth,
            ),
          )
          val move =
            Util.getCoordinatesFromGTP(selectedMove.move, currentState.position.boardHeight)
          val side = if (currentState.enginePlaysBlack) StoneType.BLACK else StoneType.WHITE
          val newPosition = RulesManager.makeMove(currentState.position, side, move)

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
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        recordException(e)
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

/**
 * Picks a move from KataGo's candidates with probability proportional to
 * visits^(1/temperature) - the same scheme AlphaZero/KataGo itself uses for move
 * selection during self-play. A move only accumulates visits if the search considers
 * it worth reading out, so a lone correct reply to a sharp threat (e.g. an atari) still
 * gets picked reliably even at high temperature, while flexible positions with several
 * comparably good moves get genuine variety. temperature == 0 always plays the top move.
 *
 * When localitySigma is non-null, candidates are additionally weighted by a Gaussian
 * falloff on their Chebyshev distance from lastMove, so weaker tiers favor replying near
 * the action instead of jumping to a distant "textbook" point. lastMove being a pass
 * exempts every candidate from this weighting, since distance is meaningless there.
 *
 * Pass is only ever returned when it's genuinely the top-ranked move (moveInfos[0]);
 * otherwise it's excluded from the sampling pool entirely, since a randomly-sampled pass
 * is a much costlier blunder than a slightly-suboptimal board move.
 */
@VisibleForTesting
fun selectAiMove(
  moveInfos: List<MoveInfo>,
  temperature: Float,
  boardHeight: Int,
  lastMove: Cell?,
  localitySigma: Float?,
  random: Random = Random,
): MoveInfo {
  val topMove = moveInfos[0]
  if (temperature <= 0f || topMove.move.equals("pass", ignoreCase = true)) return topMove

  val candidates = moveInfos.filterNot { it.move.equals("pass", ignoreCase = true) }
  if (candidates.isEmpty()) return topMove

  val weights = candidates.map { info ->
    val visitWeight = info.visits.toDouble().pow(1.0 / temperature)
    val localityWeight = if (localitySigma == null || lastMove == null || lastMove.isPass) {
      1.0
    } else {
      val cell = Util.getCoordinatesFromGTP(info.move, boardHeight)
      val distance = maxOf(abs(cell.x - lastMove.x), abs(cell.y - lastMove.y)).toDouble()
      exp(-(distance * distance) / (2.0 * localitySigma * localitySigma))
    }
    visitWeight * localityWeight
  }
  val total = weights.sum()
  var remaining = random.nextDouble() * total
  for ((info, weight) in candidates.zip(weights)) {
    remaining -= weight
    if (remaining <= 0) return info
  }
  return candidates.last()
}

/**
 * Suppresses the locality bias during the opening (before boardWidth plies have been
 * played), since a normal opening deliberately spreads across the whole board rather
 * than clustering near the previous move.
 */
@VisibleForTesting
fun effectiveLocalitySigma(localitySigma: Float?, plyNumber: Int, boardWidth: Int): Float? =
  if (plyNumber < boardWidth) null else localitySigma