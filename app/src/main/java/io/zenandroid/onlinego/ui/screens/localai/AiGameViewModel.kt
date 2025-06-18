package io.zenandroid.onlinego.ui.screens.localai

import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.reactivex.disposables.CompositeDisposable
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.ai.KataGoAnalysisEngine
import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.gamelogic.RulesManager
import io.zenandroid.onlinego.gamelogic.RulesManager.isGameOver
import io.zenandroid.onlinego.gamelogic.Util
import io.zenandroid.onlinego.gamelogic.Util.toGTP
import io.zenandroid.onlinego.utils.moshiadapters.HashMapOfCellToStoneTypeMoshiAdapter
import io.zenandroid.onlinego.utils.moshiadapters.ResponseBriefMoshiAdapter
import io.zenandroid.onlinego.utils.recordException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

private const val STATE_KEY = "AIGAME_STATE_KEY"

class AiGameViewModel(
) : ViewModel() {

  private val _state = MutableStateFlow(AiGameState())
  val state: StateFlow<AiGameState> = _state.asStateFlow()
  val disposables: CompositeDisposable = CompositeDisposable()

  private val prefs =
    PreferenceManager.getDefaultSharedPreferences(OnlineGoApplication.instance.baseContext)

  private val stateAdapter = Moshi.Builder()
    .add(ResponseBriefMoshiAdapter())
    .add(HashMapOfCellToStoneTypeMoshiAdapter())
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
                it.position == null && it.newGameDialogShown -> "Ready!"
                it.position == null && !it.newGameDialogShown -> "Use the 'New Game' button to start a new game"
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
            chatText = "Error when starting KataGO: '${e.message}'"
          )
        }
      }
    }
  }

  private fun validState(state: AiGameState): Boolean {
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
    viewModelScope.launch {
      if (prefs.contains(STATE_KEY)) {
        val json = prefs.getString(STATE_KEY, "")!!
        val newState = try {
          stateAdapter.fromJson(json)
        } catch (e: java.lang.Exception) {
          Log.e("StatePersistenceMiddlew", "Cannot deserialize state", e)
          recordException(e)
          null
        }
        newState?.let {
          if (validState(it)) {
            _state.update { state ->
              it.copy(
                engineStarted = it.engineStarted,
                stateRestorePending = false
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
      val json = stateAdapter.toJson(state.value)
      prefs.edit { putString(STATE_KEY, json) }
    }
  }

  fun onShowNewGameDialog() {
    _state.update { it.copy(newGameDialogShown = true) }
  }

  fun onDismissNewGameDialog() {
    _state.update {
      it.copy(
        newGameDialogShown = false,
        chatText = if (it.position == null) "Use the 'New Game' button to start a new game" else it.chatText
      )
    }
  }

  fun onNewGame(size: Int, youPlayBlack: Boolean, handicap: Int) {
    val newPosition = RulesManager.initializePosition(size, handicap)
    _state.update {
      it.copy(
        boardSize = size,
        handicap = handicap,
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
        chatText = "",
        previousButtonEnabled = false,
        boardIsInteractive = false,
        redoPosStack = emptyList(),
        candidateMove = null,
        history = emptyList(),
        position = newPosition
      )
    }
    updatePosition(newPosition)
  }

  fun onUserTappedCoordinate(coordinate: Cell) {
    val currentState = state.value
    if (!currentState.boardIsInteractive || currentState.position == null) return

    val side = if (currentState.enginePlaysBlack) StoneType.WHITE else StoneType.BLACK
    val newPosition = RulesManager.makeMove(currentState.position, side, coordinate)

    if (newPosition != null) {
      updatePosition(newPosition)
    }
  }

  fun onUserHotTrackedCoordinate(coordinate: Cell) {
    _state.update { it.copy(candidateMove = coordinate) }
  }

  fun onUserPressedPass() {
    val currentState = state.value
    if (!currentState.boardIsInteractive || currentState.position == null) return

    val side = if (currentState.enginePlaysBlack) StoneType.WHITE else StoneType.BLACK
    val newPosition = RulesManager.makeMove(currentState.position, side, Cell.PASS)

    if (newPosition != null) {
      updatePosition(newPosition)
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
        chatText = "Ok, let's try again. Your turn!",
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
    viewModelScope.launch {
      val currentState = state.value
      if (!currentState.engineStarted || currentState.position == null) return@launch

      _state.update { it.copy(chatText = "Hmmm...") }

      try {
        KataGoAnalysisEngine.analyzeMoveSequence(
          sequence = currentState.history,
          maxVisits = 30,
          komi = currentState.position.komi,
          includeOwnership = false
        ).subscribe(
          { analysis ->
            _state.update {
              it.copy(
                showHints = true,
                aiAnalysis = analysis,
                chatText = "Here are a few moves to consider"
              )
            }
          },
          { recordException(it) }
        )
      } catch (e: Exception) {
        recordException(e)
      }
    }
  }

  fun onUserAskedForOwnership() {
    viewModelScope.launch {
      val currentState = state.value
      if (!currentState.engineStarted || currentState.position == null) return@launch

      if (currentState.showAiEstimatedTerritory) {
        _state.update {
          it.copy(
            showAiEstimatedTerritory = false,
            chatText = "Ok, your turn",
            boardIsInteractive = true
          )
        }
        return@launch
      }

      _state.update {
        it.copy(
          boardIsInteractive = false,
          chatText = "Ok, calculating current territory..."
        )
      }

      try {
        KataGoAnalysisEngine.analyzeMoveSequence(
          sequence = currentState.history,
          maxVisits = 30,
          komi = currentState.position.komi,
          includeOwnership = true
        ).subscribe(
          { analysis ->
            _state.update {
              it.copy(
                boardIsInteractive = true,
                aiAnalysis = analysis,
                showAiEstimatedTerritory = true,
                chatText = "Here's what I think the territories look like"
              )
            }
          },
          { recordException(it) }
        )
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
            "Game ended because of two passes. Final score is black ${it.finalBlackScore?.toInt()} to white ${it.finalWhiteScore}. Looks like I win this time."

          newVariation.isGameOver() && it.aiWon == false ->
            "Game ended because of two passes. Final score is black ${it.finalBlackScore?.toInt()} to white ${it.finalWhiteScore}. Congrats, looks like you got the better of me."

          newVariation.isGameOver() && it.aiWon == null ->
            "Game ended because of two passes. Hang on, I'm computing the final score."

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
        computeFinalScore()
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
        chatText = "I'm thinking..."
      )
    }

    try {
      disposables.add(
        KataGoAnalysisEngine.analyzeMoveSequence(
          sequence = currentState.history,
          maxVisits = 20,
          komi = currentState.position.komi,
          includeOwnership = false,
          includeMovesOwnership = false
        ).subscribe(
          { analysis ->
            val selectedMove = analysis.moveInfos[0]
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
                      "Game ended because of two passes. Final score is black ${it.finalBlackScore?.toInt()} to white ${it.finalWhiteScore}. Looks like I win this time."

                    newVariation.isGameOver() && it.aiWon == false ->
                      "Game ended because of two passes. Final score is black ${it.finalBlackScore?.toInt()} to white ${it.finalWhiteScore}. Congrats, looks like you got the better of me."

                    newVariation.isGameOver() && it.aiWon == null ->
                      "Game ended because of two passes. Hang on, I'm computing the final score."

                    else -> it.chatText
                  }
                )
              }

              if (newVariation.isGameOver()) {
                viewModelScope.launch {
                  computeFinalScore()
                }
              } else {
                _state.update {
                  it.copy(
                    boardIsInteractive = true,
                    passButtonEnabled = true
                  )
                }
              }
            }
          },
          { recordException(it) }
        )
      )
    } catch (e: Exception) {
      recordException(e)
    }
  }

  private fun computeFinalScore() {
    val currentState = state.value
    if (!currentState.engineStarted || currentState.position == null) return

    try {
      disposables.add(
        KataGoAnalysisEngine.analyzeMoveSequence(
          sequence = currentState.history,
          maxVisits = 10,
          komi = currentState.position.komi,
          includeOwnership = true
        ).subscribe(
          { analysis ->

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
                  "Game ended because of two passes. Final score is black $blackScore to white $whiteScore. Looks like I win this time."
                else
                  "Game ended because of two passes. Final score is black $blackScore to white $whiteScore. Congrats, looks like you got the better of me.",
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
          },
          { recordException(it) }
        )
      )
    } catch (e: Exception) {
      recordException(e)
    }
  }

  override fun onCleared() {
    super.onCleared()
    disposables.clear()
    viewModelScope.launch(Dispatchers.IO) {
      KataGoAnalysisEngine.stop()
    }
  }
}