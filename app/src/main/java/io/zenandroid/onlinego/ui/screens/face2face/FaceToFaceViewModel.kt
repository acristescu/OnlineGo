package io.zenandroid.onlinego.ui.screens.face2face

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cash.molecule.AndroidUiDispatcher
import app.cash.molecule.RecompositionClock.ContextClock
import app.cash.molecule.launchMolecule
import io.zenandroid.onlinego.data.model.BoardTheme
import io.zenandroid.onlinego.data.model.BoardTheme.WOOD
import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.repositories.SettingsRepository
import io.zenandroid.onlinego.gamelogic.RulesManager
import io.zenandroid.onlinego.ui.screens.face2face.Action.BoardCellDragged
import io.zenandroid.onlinego.ui.screens.face2face.Action.BoardCellTapUp
import io.zenandroid.onlinego.ui.screens.game.GameOverDialogDetails
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.annotation.concurrent.Immutable

class FaceToFaceViewModel(
  private val settingsRepository: SettingsRepository,
  testing: Boolean = false
) : ViewModel() {

  private val moleculeScope =
    if (testing) viewModelScope else CoroutineScope(viewModelScope.coroutineContext + AndroidUiDispatcher.Main)

  private var loading by mutableStateOf(true)
  private var currentPosition by mutableStateOf(Position(19, 19))
  private var candidateMove by mutableStateOf<Cell?>(null)
  private var koMoveDialogShowing by mutableStateOf(false)
  private var gameFinished by mutableStateOf<Boolean?>(null)
  private var gameOverDetails by mutableStateOf<GameOverDialogDetails?>(null)
  private var gameOverDialogShowing by mutableStateOf(false)

  val state: StateFlow<FaceToFaceState> =
    if (testing) MutableStateFlow(FaceToFaceState.INITIAL)
    else moleculeScope.launchMolecule(clock = ContextClock) {
      Molecule()
    }

  @Composable
  fun Molecule(): FaceToFaceState {

    return FaceToFaceState(
      loading = loading,
      position = currentPosition,
      gameWidth = 19,
      gameHeight = 19,
      boardInteractive = true,
      candidateMove = candidateMove,
      boardTheme = settingsRepository.boardTheme,
      showCoordinates = settingsRepository.showCoordinates,
      drawTerritory = false,
      fadeOutRemovedStones = false,
      showLastMove = true,
    )
  }

  fun onAction(action: Action) {
    when (action) {
      is BoardCellDragged -> candidateMove = action.cell
      is BoardCellTapUp -> onCellTapUp(action.cell)
    }
  }

  private fun onCellTapUp(cell: Cell) {
    viewModelScope.launch {
      val pos = currentPosition
      val newPosition = RulesManager.makeMove(pos, pos.nextToMove, cell)
      if(newPosition != null) {
        currentPosition = newPosition
      }
      candidateMove = null
    }
  }
}

@Immutable
data class FaceToFaceState(
  val position: Position?,
  val loading: Boolean,
  val gameWidth: Int,
  val gameHeight: Int,
  val candidateMove: Cell?,
  val boardInteractive: Boolean,
  val boardTheme: BoardTheme,
  val showCoordinates: Boolean,
  val drawTerritory: Boolean,
  val fadeOutRemovedStones: Boolean,
  val showLastMove: Boolean,
) {
  companion object {
    val INITIAL = FaceToFaceState(
      loading = true,
      position = Position(19, 19),
      gameWidth = 19,
      gameHeight = 19,
      boardInteractive = true,
      candidateMove = null,
      boardTheme = WOOD,
      showCoordinates = true,
      drawTerritory = false,
      fadeOutRemovedStones = false,
      showLastMove = true,
    )
  }
}

sealed interface Action {
  class BoardCellDragged(val cell: Cell) : Action
  class BoardCellTapUp(val cell: Cell) : Action
}