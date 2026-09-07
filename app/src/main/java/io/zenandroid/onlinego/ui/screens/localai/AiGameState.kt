package io.zenandroid.onlinego.ui.screens.localai

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.model.katago.KataGoResponse.Response
import io.zenandroid.onlinego.data.model.katago.MoveInfo
import io.zenandroid.onlinego.ui.composables.TextResource

/**
 * temperature scales how much weaker-than-best moves get sampled, and localitySigma
 * (null = disabled) biases weaker tiers toward replies near the board's last move; see
 * [AiGameViewModel.selectAiMove]. temperature == 0 always plays the top move.
 */
enum class AiDifficulty(
  val maxVisits: Int,
  val temperature: Float,
  val localitySigma: Float?,
  @StringRes val labelResId: Int,
) {
  BEGINNER(
    maxVisits = 6,
    temperature = 1.8f,
    localitySigma = 5f,
    labelResId = R.string.ai_game_difficulty_beginner
  ),
  EASY(
    maxVisits = 10,
    temperature = 1.4f,
    localitySigma = 7f,
    labelResId = R.string.ai_game_difficulty_easy
  ),
  NORMAL(
    maxVisits = 16,
    temperature = 1.0f,
    localitySigma = 9f,
    labelResId = R.string.ai_game_difficulty_normal
  ),
  HARD(
    maxVisits = 40,
    temperature = 0.4f,
    localitySigma = null,
    labelResId = R.string.ai_game_difficulty_hard
  ),
  STRONGEST(
    maxVisits = 100,
    temperature = 0f,
    localitySigma = null,
    labelResId = R.string.ai_game_difficulty_strongest
  ),
}

@Immutable
data class AiGameState(
  val engineStarted: Boolean = false,
  val position: Position? = null,
  val history: List<Position> = emptyList(),
  val boardSize: Int = 19,
  val enginePlaysBlack: Boolean = false,
  val handicap: Int = 0,
  val difficulty: AiDifficulty = AiDifficulty.NORMAL,
  val boardIsInteractive: Boolean = false,
  val candidateMove: Cell? = null,
  val passButtonEnabled: Boolean = false,
  val nextButtonEnabled: Boolean = false,
  val previousButtonEnabled: Boolean = false,
  val redoPosStack: List<Position> = emptyList(),
  val newGameDialogShown: Boolean = false,
  val chatText: TextResource? = null,
  val showHints: Boolean = false,
  val showAiEstimatedTerritory: Boolean = false,
  val showFinalTerritory: Boolean = false,
  val hintButtonVisible: Boolean = false,
  val ownershipButtonVisible: Boolean = false,
  val finalWhiteScore: Float? = null,
  val finalBlackScore: Float? = null,
  val aiWon: Boolean? = null,
  val stateRestorePending: Boolean = true,
  val aiAnalysis: Response? = null,
  val aiQuickEstimation: MoveInfo? = null,
  val userIcon: String? = null,
  val koMoveDialogShowing: Boolean = false,
)