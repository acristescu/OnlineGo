package io.zenandroid.onlinego.ui.screens.face2face

import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.Functions
import androidx.compose.material.icons.rounded.HighlightOff
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cash.molecule.AndroidUiDispatcher
import app.cash.molecule.RecompositionMode.ContextClock
import app.cash.molecule.launchMolecule
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.model.StoneType.BLACK
import io.zenandroid.onlinego.data.model.StoneType.WHITE
import io.zenandroid.onlinego.gamelogic.RulesManager
import io.zenandroid.onlinego.gamelogic.Util
import io.zenandroid.onlinego.gamelogic.Util.toGTP
import io.zenandroid.onlinego.ui.composables.BottomBarButton
import io.zenandroid.onlinego.ui.screens.face2face.Action.BoardCellDragged
import io.zenandroid.onlinego.ui.screens.face2face.Action.BoardCellTapUp
import io.zenandroid.onlinego.ui.screens.face2face.Action.BottomButtonPressed
import io.zenandroid.onlinego.ui.screens.face2face.Action.KOMoveDialogDismiss
import io.zenandroid.onlinego.ui.screens.face2face.Action.NewGameDialogDismiss
import io.zenandroid.onlinego.ui.screens.face2face.Action.NewGameParametersChanged
import io.zenandroid.onlinego.ui.screens.face2face.Button.CloseEstimate
import io.zenandroid.onlinego.ui.screens.face2face.Button.Estimate
import io.zenandroid.onlinego.ui.screens.face2face.Button.GameSettings
import io.zenandroid.onlinego.ui.screens.face2face.Button.Next
import io.zenandroid.onlinego.ui.screens.face2face.Button.Pass
import io.zenandroid.onlinego.ui.screens.face2face.Button.Previous
import io.zenandroid.onlinego.ui.screens.face2face.EstimateStatus.Idle
import io.zenandroid.onlinego.ui.screens.face2face.EstimateStatus.Success
import io.zenandroid.onlinego.ui.screens.face2face.EstimateStatus.Working
import io.zenandroid.onlinego.utils.analyticsReportScreen
import io.zenandroid.onlinego.utils.recordException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val HISTORY_KEY = "FACE_TO_FACE_HISTORY_KEY"
private const val BOARD_SIZE_KEY = "FACE_TO_FACE_BOARD_SIZE_KEY"
private const val HANDICAP_KEY = "FACE_TO_FACE_HANDICAP_KEY"

class FaceToFaceViewModel(
  private val prefs: SharedPreferences,
  private val analytics: FirebaseAnalytics,
  private val crashlytics: FirebaseCrashlytics,
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
  private var estimateStatus by mutableStateOf<EstimateStatus>(Idle)
  private var newGameDialogShowing by mutableStateOf(false)
  private var currentGameParameters by mutableStateOf(GameParameters(BoardSize.LARGE, 0))
  private var newGameParameters by mutableStateOf(GameParameters(BoardSize.LARGE, 0))

  init {
    analytics.logEvent("face_to_face_opened", null)
    viewModelScope.launch {
      loadSavedData()
    }
  }

  val state: StateFlow<FaceToFaceState> =
    if (testing) MutableStateFlow(FaceToFaceState.INITIAL)
    else moleculeScope.launchMolecule(mode = ContextClock) {
      molecule()
    }

  @VisibleForTesting
  @Composable
  fun molecule(): FaceToFaceState {
    val historyIndex = historyIndex
    val title = when {
      loading -> "Face to face · Loading"
      gameFinished == true -> "Face to face · Game Over"
      currentPosition.nextToMove == WHITE -> "Face to face · White's turn"
      currentPosition.nextToMove == BLACK -> "Face to face · Black's turn"
      else -> "Face to face"
    }

    val estimateStatus = estimateStatus
    val position = when {
      estimateStatus is Success -> estimateStatus.result
      else -> currentPosition
    }

    val previousButtonEnabled =
      !loading && history.isNotEmpty() && (historyIndex == null || historyIndex >= 0)
    val nextButtonEnabled =
      !loading && history.isNotEmpty() && historyIndex != null && historyIndex < history.size

    val (buttons, bottomText) = when {
      estimateStatus is Working -> emptyList<Button>() to "Estimating"
      estimateStatus is Success -> listOf(CloseEstimate) to null
      else -> listOf(
        GameSettings, Estimate, Pass, Previous(previousButtonEnabled), Next(nextButtonEnabled)
      ) to null
    }

    val extraStatus = when {
      estimateStatus is Success && estimateStatus.gameIsOver -> "Game is over!"
      estimateStatus is Success && !estimateStatus.gameIsOver -> "Recommendation: Game is not over!"
      else -> null
    }

    return FaceToFaceState(
      loading = loading,
      position = position,
      title = title,
      gameFinished = false,
      history = history,
      boardInteractive = !loading && estimateStatus is Idle,
      candidateMove = candidateMove,
      drawTerritory = estimateStatus is Success,
      fadeOutRemovedStones = estimateStatus is Success,
      showLastMove = estimateStatus !is Success,
      koMoveDialogShowing = koMoveDialogShowing,
      buttons = buttons,
      bottomText = bottomText,
      newGameDialogShowing = newGameDialogShowing,
      currentGameParameters = currentGameParameters,
      newGameParameters = newGameParameters,
      extraStatus = extraStatus,
    )
  }

  private suspend fun loadSavedData() {
    if (prefs.contains(HISTORY_KEY) && prefs.contains(BOARD_SIZE_KEY) && prefs.contains(HANDICAP_KEY)) {
      analytics.logEvent("face_to_face_loading", null)
      var historyString: String
      var sizeString: String
      var handicap: Int

      withContext(Dispatchers.IO) {
        historyString = prefs.getString(HISTORY_KEY, "")!!
        sizeString = prefs.getString(BOARD_SIZE_KEY, "")!!
        handicap = prefs.getInt(HANDICAP_KEY, 0)
      }
      history = historyString.split(" ")
        .filter { it.isNotEmpty() }
        .map {
          val parts = it.split(",")
          Cell(parts[0].toInt(), parts[1].toInt())
        }
      val size = BoardSize.entries.first { it.prettyName == sizeString }
      currentGameParameters = GameParameters(size, handicap)
      newGameParameters = currentGameParameters
    }
    currentPosition = try {
      historyPosition(history.lastIndex)
    } catch (e: Exception) {
      crashlytics.log("FaceToFaceViewModel Cannot load history $history")
      recordException(e)
      historyPosition(0)
    }
    loading = false
    analytics.logEvent("face_to_face_loaded", null)
  }

  override fun onCleared() {
    prefs.edit {
      putString(HISTORY_KEY, history.joinToString(separator = " ") { "${it.x},${it.y}" })
        .putString(BOARD_SIZE_KEY, currentGameParameters.size.toString())
        .putInt(HANDICAP_KEY, currentGameParameters.handicap)
    }
    super.onCleared()
  }

  fun onAction(action: Action) {
    when (action) {
      is BoardCellDragged -> candidateMove = action.cell
      is BoardCellTapUp -> onCellTapUp(action.cell)
      KOMoveDialogDismiss -> koMoveDialogShowing = false
      is BottomButtonPressed -> onButtonPressed(action.button)
      NewGameDialogDismiss -> newGameDialogShowing = false
      is NewGameParametersChanged -> newGameParameters = action.params
      Action.StartNewGame -> onStartNewGame()
    }
  }

  private fun onButtonPressed(button: Button) {
    when (button) {
      is Estimate -> doEstimation()
      is GameSettings -> newGameDialogShowing = true
      is Next -> onNextPressed()
      is Previous -> onPreviousPressed()
      is CloseEstimate -> estimateStatus = Idle
      is Pass -> onPassPressed()
    }
  }

  private fun doEstimation() {
    estimateStatus = Working
    viewModelScope.launch(Dispatchers.IO) {
      val estimate = RulesManager.determineTerritory(currentPosition, false)
      withContext(Dispatchers.Main) {
        val index = historyIndex ?: history.lastIndex
        val finished =
          index > currentGameParameters.size.width &&
              estimate.dame.size < currentGameParameters.size.width
        estimateStatus = Success(estimate, finished)
      }
    }
  }

  private fun onPreviousPressed() {
    crashlytics.log("FaceToFaceViewModel onPreviousPressed")
    val newIndex = historyIndex?.minus(1) ?: (history.lastIndex - 1)
    if (newIndex < -1) {
      //
      // Note: this can happen with repeating buttons: the button
      // could fire twice in the space between two frames
      // thus not giving a chance to the button to be disabled
      //
      return
    }
    val newPos = historyPosition(newIndex)
    historyIndex = newIndex
    currentPosition = newPos
  }

  private fun onNextPressed() {
    crashlytics.log("FaceToFaceViewModel onNextPressed")
    val newIndex = historyIndex?.plus(1) ?: history.lastIndex
    if (newIndex > history.lastIndex) {
      //
      // Note: this can happen with repeating buttons: the button
      // could fire twice in the space between two frames
      // thus not giving a chance to the button to be disabled
      //
      return
    }
    val newPos = historyPosition(newIndex)
    historyIndex = if (newIndex < history.lastIndex) newIndex else null
    currentPosition = newPos
  }

  private fun onStartNewGame() {
    crashlytics.log("FaceToFaceViewModel Starting new game")
    val params = newGameParameters
    currentPosition = RulesManager.initializePosition(params.size.height, params.handicap)
    estimateStatus = Idle
    history = emptyList()
    currentGameParameters = params
    historyIndex = null
    newGameDialogShowing = false
  }

  private fun historyPosition(index: Int) =
    RulesManager.replay(
      nextToMove = if (currentGameParameters.handicap > 0) WHITE else BLACK,
      moves = history.subList(0, index + 1),
      width = currentGameParameters.size.width,
      height = currentGameParameters.size.height,
      handicap = currentGameParameters.handicap
    ) ?: run {
      val historyString = history.toGTP(currentGameParameters.size.height)
      val whiteStones = currentPosition.whiteStones.toGTP(currentGameParameters.size.height)
      val blackStones = currentPosition.blackStones.toGTP(currentGameParameters.size.height)
      crashlytics.log("FaceToFaceViewModel Cannot replay history $historyString")
      recordException(IllegalStateException("Cannot replay history history=$historyString idx=$index historyIndex=$historyIndex currentPos.whiteStones=$whiteStones currentPos.blackStones=$blackStones"))
      Position(
        boardWidth = currentGameParameters.size.width,
        boardHeight = currentGameParameters.size.height,
        handicap = currentGameParameters.handicap,
      )
    }

  private fun onPassPressed() {
    onCellTapUp(Cell(-1, -1))
  }

  private fun onCellTapUp(cell: Cell) {
    crashlytics.log(
      "onCellTapUp ${
        Util.getGTPCoordinates(
          cell,
          currentGameParameters.size.height
        )
      }"
    )
    val pos = currentPosition
    val newPosition = RulesManager.makeMove(pos, pos.nextToMove, cell)
    if (newPosition != null) {
      val index = historyIndex ?: history.lastIndex
      val potentialKOPosition = if (index > 0 && !cell.isPass) {
        historyPosition(index - 1)
      } else null
      if (potentialKOPosition?.hasTheSameStonesAs(newPosition) == true) {
        crashlytics.log("FaceToFaceViewModel KO move detected")
        koMoveDialogShowing = true
      } else {
        currentPosition = newPosition
        history = history.subList(0, index + 1) + cell
        historyIndex = null
        if (index > 0 && cell.isPass && history[index].isPass) {
          doEstimation()
        }
      }
    }
    candidateMove = null
  }
}

@Immutable
data class FaceToFaceState(
  val position: Position?,
  val loading: Boolean,
  val title: String,
  val buttons: List<Button>,
  val bottomText: String?,
  val gameFinished: Boolean,
  val history: List<Cell>,
  val candidateMove: Cell?,
  val boardInteractive: Boolean,
  val drawTerritory: Boolean,
  val fadeOutRemovedStones: Boolean,
  val showLastMove: Boolean,
  val koMoveDialogShowing: Boolean,
  val newGameDialogShowing: Boolean,
  val currentGameParameters: GameParameters,
  val newGameParameters: GameParameters,
  val extraStatus: String?,
) {
  companion object {
    val INITIAL = FaceToFaceState(
      loading = true,
      title = "Face to face · Loading",
      position = Position(19, 19),
      gameFinished = false,
      history = emptyList(),
      boardInteractive = false,
      candidateMove = null,
      drawTerritory = false,
      fadeOutRemovedStones = false,
      showLastMove = true,
      koMoveDialogShowing = false,
      buttons = emptyList(),
      bottomText = null,
      newGameDialogShowing = false,
      currentGameParameters = GameParameters(BoardSize.LARGE, 0),
      newGameParameters = GameParameters(BoardSize.LARGE, 0),
      extraStatus = null,
    )
  }
}

@Immutable
data class GameParameters(
  val size: BoardSize,
  val handicap: Int,
)

enum class BoardSize(
  val width: Int,
  val height: Int,
  val prettyName: String,
) {
  SMALL(9, 9, "9 × 9"),
  MEDIUM(13, 13, "13 × 13"),
  LARGE(19, 19, "19 × 19");

  override fun toString(): String {
    return prettyName
  }
}

sealed class Button(
  override val icon: ImageVector,
  override val label: String,
  override val repeatable: Boolean = false,
  override val enabled: Boolean = true,
  override val bubbleText: String? = null,
  override val highlighted: Boolean = false,
) : BottomBarButton {
  object GameSettings : Button(Icons.Rounded.AddCircle, "New Game")
  object Estimate : Button(Icons.Rounded.Functions, "Estimate Score")
  class Previous(enabled: Boolean = true) : Button(
    repeatable = true,
    enabled = enabled,
    icon = Icons.Rounded.SkipPrevious,
    label = "Previous"
  )

  class Next(enabled: Boolean = true) :
    Button(repeatable = true, enabled = enabled, icon = Icons.Rounded.SkipNext, label = "Next")

  object CloseEstimate : Button(Icons.Rounded.HighlightOff, "Return")
  object Pass : Button(Icons.Rounded.Stop, "Pass")
}

sealed interface Action {
  class BoardCellDragged(val cell: Cell) : Action
  class BoardCellTapUp(val cell: Cell) : Action
  class BottomButtonPressed(val button: Button) : Action
  object KOMoveDialogDismiss : Action
  object NewGameDialogDismiss : Action
  class NewGameParametersChanged(val params: GameParameters) : Action
  object StartNewGame : Action
}

sealed interface EstimateStatus {
  object Idle : EstimateStatus
  object Working : EstimateStatus
  data class Success(val result: Position, val gameIsOver: Boolean) : EstimateStatus
}