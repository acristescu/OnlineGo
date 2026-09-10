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
 *
 * rootPolicyTemperature flattens KataGo's own root move priors during search (1.0 =
 * engine default, no change). At the weakest tiers maxVisits is so low that the search
 * barely explores beyond its 1-2 favorite moves, starving moveInfos of real candidates;
 * raising this spreads the search itself across more moves within the same visit budget.
 *
 * comebackProbability (0 = disabled) is the chance, per move, that once the engine's own
 * winrate is above 70% it skips normal sampling and deliberately plays whichever
 * candidate's winrate is closest to 50% instead - see [AiGameViewModel.selectAiMove]. It
 * has no effect below that threshold, and is 0 for every Dan tier (no artificial mercy
 * there).
 */
enum class AiDifficulty(
  val maxVisits: Int,
  val temperature: Float,
  val localitySigma: Float?,
  val rootPolicyTemperature: Float,
  val comebackProbability: Float,
  val rank: DifficultyRank,
) {
  KYU_20(
    maxVisits = 3,
    temperature = 2.1f,
    localitySigma = 3f,
    rootPolicyTemperature = 2.0f,
    comebackProbability = 0.90f,
    rank = DifficultyRank.Kyu(20)
  ),
  KYU_18(
    maxVisits = 4,
    temperature = 1.85f,
    localitySigma = 4f,
    rootPolicyTemperature = 1.8f,
    comebackProbability = 0.80f,
    rank = DifficultyRank.Kyu(18)
  ),
  KYU_16(
    maxVisits = 6,
    temperature = 1.6f,
    localitySigma = 5f,
    rootPolicyTemperature = 1.6f,
    comebackProbability = 0.70f,
    rank = DifficultyRank.Kyu(16)
  ),
  KYU_14(
    maxVisits = 7,
    temperature = 1.35f,
    localitySigma = 5.5f,
    rootPolicyTemperature = 1.5f,
    comebackProbability = 0.60f,
    rank = DifficultyRank.Kyu(14)
  ),
  KYU_12(
    maxVisits = 8,
    temperature = 1.17f,
    localitySigma = 6f,
    rootPolicyTemperature = 1.4f,
    comebackProbability = 0.50f,
    rank = DifficultyRank.Kyu(12)
  ),
  KYU_10(
    maxVisits = 9,
    temperature = 1.13f,
    localitySigma = 6.5f,
    rootPolicyTemperature = 1.3f,
    comebackProbability = 0.40f,
    rank = DifficultyRank.Kyu(10)
  ),
  KYU_8(
    maxVisits = 10,
    temperature = 1.09f,
    localitySigma = 7f,
    rootPolicyTemperature = 1.2f,
    comebackProbability = 0.30f,
    rank = DifficultyRank.Kyu(8)
  ),
  KYU_6(
    maxVisits = 12,
    temperature = 1.07f,
    localitySigma = 7.5f,
    rootPolicyTemperature = 1.15f,
    comebackProbability = 0.20f,
    rank = DifficultyRank.Kyu(6)
  ),
  KYU_4(
    maxVisits = 14,
    temperature = 1.05f,
    localitySigma = 8f,
    rootPolicyTemperature = 1.1f,
    comebackProbability = 0.15f,
    rank = DifficultyRank.Kyu(4)
  ),
  KYU_2(
    maxVisits = 16,
    temperature = 0.95f,
    localitySigma = 9f,
    rootPolicyTemperature = 1.05f,
    comebackProbability = 0.10f,
    rank = DifficultyRank.Kyu(2)
  ),
  DAN_1(
    maxVisits = 18,
    temperature = 0.85f,
    localitySigma = null,
    rootPolicyTemperature = 1.0f,
    comebackProbability = 0f,
    rank = DifficultyRank.Dan(1)
  ),
  DAN_3(
    maxVisits = 28,
    temperature = 0.70f,
    localitySigma = null,
    rootPolicyTemperature = 1.0f,
    comebackProbability = 0f,
    rank = DifficultyRank.Dan(3)
  ),
  DAN_4(
    maxVisits = 40,
    temperature = 0.55f,
    localitySigma = null,
    rootPolicyTemperature = 1.0f,
    comebackProbability = 0f,
    rank = DifficultyRank.Dan(4)
  ),
  DAN_5(
    maxVisits = 50,
    temperature = 0f,
    localitySigma = null,
    rootPolicyTemperature = 1.0f,
    comebackProbability = 0f,
    rank = DifficultyRank.Dan(5)
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
  val difficulty: AiDifficulty = AiDifficulty.KYU_12,
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