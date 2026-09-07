package io.zenandroid.onlinego.ui.screens.localai

import androidx.compose.runtime.Immutable
import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.model.katago.KataGoResponse.Response
import io.zenandroid.onlinego.data.model.katago.MoveInfo
import io.zenandroid.onlinego.ui.composables.TextResource

sealed interface DifficultyRank {
  data class Kyu(val n: Int) : DifficultyRank
  data class Dan(val n: Int) : DifficultyRank
}

/**
 * temperature scales how much weaker-than-best moves get sampled, and localitySigma
 * (null = disabled) biases weaker tiers toward replies near the board's last move; see
 * [AiGameViewModel.selectAiMove]. temperature == 0 always plays the top move.
 */
enum class AiDifficulty(
  val maxVisits: Int,
  val temperature: Float,
  val localitySigma: Float?,
  val rank: DifficultyRank,
) {
  KYU_20(maxVisits = 3, temperature = 2.5f, localitySigma = 3f, rank = DifficultyRank.Kyu(20)),
  KYU_18(maxVisits = 4, temperature = 2.2f, localitySigma = 4f, rank = DifficultyRank.Kyu(18)),
  KYU_16(maxVisits = 6, temperature = 1.8f, localitySigma = 5f, rank = DifficultyRank.Kyu(16)),
  KYU_14(maxVisits = 7, temperature = 1.7f, localitySigma = 5.5f, rank = DifficultyRank.Kyu(14)),
  KYU_12(maxVisits = 8, temperature = 1.6f, localitySigma = 6f, rank = DifficultyRank.Kyu(12)),
  KYU_10(maxVisits = 9, temperature = 1.5f, localitySigma = 6.5f, rank = DifficultyRank.Kyu(10)),
  KYU_8(maxVisits = 10, temperature = 1.4f, localitySigma = 7f, rank = DifficultyRank.Kyu(8)),
  KYU_6(maxVisits = 12, temperature = 1.3f, localitySigma = 7.5f, rank = DifficultyRank.Kyu(6)),
  KYU_4(maxVisits = 14, temperature = 1.15f, localitySigma = 8f, rank = DifficultyRank.Kyu(4)),
  KYU_2(maxVisits = 16, temperature = 1.0f, localitySigma = 9f, rank = DifficultyRank.Kyu(2)),
  DAN_1(maxVisits = 26, temperature = 0.7f, localitySigma = null, rank = DifficultyRank.Dan(1)),
  DAN_3(maxVisits = 40, temperature = 0.4f, localitySigma = null, rank = DifficultyRank.Dan(3)),
  DAN_4(maxVisits = 65, temperature = 0.2f, localitySigma = null, rank = DifficultyRank.Dan(4)),
  DAN_5(maxVisits = 100, temperature = 0f, localitySigma = null, rank = DifficultyRank.Dan(5)),
}

@Immutable
data class AiGameState(
  val engineStarted: Boolean = false,
  val position: Position? = null,
  val history: List<Position> = emptyList(),
  val boardSize: Int = 19,
  val enginePlaysBlack: Boolean = false,
  val handicap: Int = 0,
  val difficulty: AiDifficulty = AiDifficulty.KYU_2,
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