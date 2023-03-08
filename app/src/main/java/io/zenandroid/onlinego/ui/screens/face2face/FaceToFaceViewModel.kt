package io.zenandroid.onlinego.ui.screens.face2face

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import app.cash.molecule.AndroidUiDispatcher
import app.cash.molecule.RecompositionClock.ContextClock
import app.cash.molecule.launchMolecule
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.data.model.BoardTheme
import io.zenandroid.onlinego.data.model.BoardTheme.WOOD
import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.model.StoneType.WHITE
import io.zenandroid.onlinego.data.model.StoneType.BLACK
import io.zenandroid.onlinego.data.repositories.SettingsRepository
import io.zenandroid.onlinego.gamelogic.RulesManager
import io.zenandroid.onlinego.ui.screens.face2face.Action.BoardCellDragged
import io.zenandroid.onlinego.ui.screens.face2face.Action.BoardCellTapUp
import io.zenandroid.onlinego.ui.screens.face2face.Action.KOMoveDialogDismiss
import io.zenandroid.onlinego.ui.screens.face2face.Action.NextButtonPressed
import io.zenandroid.onlinego.ui.screens.face2face.Action.PreviousButtonPressed
import io.zenandroid.onlinego.ui.screens.game.GameOverDialogDetails
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.annotation.concurrent.Immutable

private const val STATE_KEY = "FACE_TO_FACE_STATE_KEY"

class FaceToFaceViewModel(
  private val settingsRepository: SettingsRepository,
  testing: Boolean = false
) : ViewModel() {

  private val moleculeScope =
    if (testing) viewModelScope else CoroutineScope(viewModelScope.coroutineContext + AndroidUiDispatcher.Main)

  private var loading by mutableStateOf(true)
  private var currentPosition by mutableStateOf(Position(19, 19))
  private var candidateMove by mutableStateOf<Cell?>(null)
  private var history by mutableStateOf<List<Cell>>(emptyList())
  private var historyIndex by mutableStateOf<Int?>(null)
  private var koMoveDialogShowing by mutableStateOf(false)
  private var gameFinished by mutableStateOf<Boolean?>(null)
  private var gameOverDetails by mutableStateOf<GameOverDialogDetails?>(null)
  private var gameOverDialogShowing by mutableStateOf(false)

  private val prefs = PreferenceManager.getDefaultSharedPreferences(OnlineGoApplication.instance.baseContext)

  val state: StateFlow<FaceToFaceState> =
    if (testing) MutableStateFlow(FaceToFaceState.INITIAL)
    else moleculeScope.launchMolecule(clock = ContextClock) {
      Molecule()
    }

  @Composable
  fun Molecule(): FaceToFaceState {
    LaunchedEffect(null) {
      loadSavedData()
    }

    val historyIndex = historyIndex
    val title = when {
      loading -> "Face to face · Loading"
      gameFinished == true -> "Face to face · Game Over"
      currentPosition.nextToMove == WHITE -> "Face to face · White's turn"
      currentPosition.nextToMove == BLACK -> "Face to face · Black's turn"
      else -> "Face to face"
    }

    return FaceToFaceState(
      loading = loading,
      position = currentPosition,
      title = title,
      gameWidth = 19,
      gameHeight = 19,
      handicap = 0,
      gameFinished = false,
      history = history,
      previousButtonEnabled = history.isNotEmpty() && (historyIndex == null || historyIndex >= 0),
      nextButtonEnabled = history.isNotEmpty() && historyIndex != null && historyIndex < history.size,
      boardInteractive = true,
      candidateMove = candidateMove,
      boardTheme = settingsRepository.boardTheme,
      showCoordinates = settingsRepository.showCoordinates,
      drawTerritory = false,
      fadeOutRemovedStones = false,
      showLastMove = true,
      koMoveDialogShowing = koMoveDialogShowing,
    )
  }

  private suspend fun loadSavedData() {
    if(prefs.contains(STATE_KEY)) {
      val historyString = withContext(Dispatchers.IO) {
        prefs.getString(STATE_KEY, "")
      }
      historyString?.let {
        history = it.split(" ")
          .filter { it.isNotEmpty() }
          .map {
            val parts = it.split(",")
            Cell(parts[0].toInt(), parts[1].toInt())
          }
        currentPosition = historyPosition(history.lastIndex)
      }
    }
    loading = false
  }

  override fun onCleared() {
    prefs.edit().putString(STATE_KEY, history.joinToString(separator = " ") { "${it.x},${it.y}" }).apply()
    super.onCleared()
  }

  fun onAction(action: Action) {
    when (action) {
      is BoardCellDragged -> candidateMove = action.cell
      is BoardCellTapUp -> onCellTapUp(action.cell)
      NextButtonPressed -> onNextPressed()
      PreviousButtonPressed -> onPreviousPressed()
      KOMoveDialogDismiss -> koMoveDialogShowing = false
    }
  }

  private fun onPreviousPressed() {
    val newIndex = historyIndex?.minus(1) ?: (history.lastIndex - 1)
    val newPos = historyPosition(newIndex)
    historyIndex = newIndex
    currentPosition = newPos
  }

  private fun onNextPressed() {
    val newIndex = historyIndex?.plus(1) ?: history.lastIndex
    val newPos = historyPosition(newIndex)
    historyIndex = if(newIndex < history.lastIndex) newIndex else null
    currentPosition = newPos
  }

  private fun historyPosition(index: Int) =
    RulesManager.buildPos(
      moves = history.subList(0, index + 1),
      boardWidth = currentPosition.boardWidth,
      boardHeight = currentPosition.boardHeight,
      handicap = currentPosition.handicap
    )!!

  private fun onCellTapUp(cell: Cell) {
    viewModelScope.launch {
      val pos = currentPosition
      val newPosition = RulesManager.makeMove(pos, pos.nextToMove, cell)
      if(newPosition != null) {
        val index = historyIndex ?: history.lastIndex
        val potentialKOPosition = if(index > 0) {
          historyPosition(index - 1)
        } else null
        if(potentialKOPosition?.hasTheSameStonesAs(newPosition) == true) {
          koMoveDialogShowing = true
        } else {
          currentPosition = newPosition
          history = history.subList(0, index + 1) + cell
          historyIndex = null
        }
      }
      candidateMove = null
    }
  }
}

@Immutable
data class FaceToFaceState(
  val position: Position?,
  val loading: Boolean,
  val title: String,
  val gameWidth: Int,
  val gameHeight: Int,
  val handicap: Int,
  val gameFinished: Boolean,
  val history: List<Cell>,
  val previousButtonEnabled: Boolean,
  val nextButtonEnabled: Boolean,
  val candidateMove: Cell?,
  val boardInteractive: Boolean,
  val boardTheme: BoardTheme,
  val showCoordinates: Boolean,
  val drawTerritory: Boolean,
  val fadeOutRemovedStones: Boolean,
  val showLastMove: Boolean,
  val koMoveDialogShowing: Boolean,
) {
  companion object {
    val INITIAL = FaceToFaceState(
      loading = true,
      title = "Face to face · Loading",
      position = Position(19, 19),
      gameWidth = 19,
      gameHeight = 19,
      handicap = 0,
      gameFinished = false,
      history = emptyList(),
      previousButtonEnabled = false,
      nextButtonEnabled = false,
      boardInteractive = true,
      candidateMove = null,
      boardTheme = WOOD,
      showCoordinates = true,
      drawTerritory = false,
      fadeOutRemovedStones = false,
      showLastMove = true,
      koMoveDialogShowing = false,
    )
  }
}

sealed interface Action {
  class BoardCellDragged(val cell: Cell) : Action
  class BoardCellTapUp(val cell: Cell) : Action
  object PreviousButtonPressed: Action
  object NextButtonPressed: Action
  object KOMoveDialogDismiss: Action
}