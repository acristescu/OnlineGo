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
 * humanSLProfile picks a KataGo Human SL rank/style ("rank_9k", "rank_3d", ...) to sample
 * moves from - see [AiGameViewModel.selectHumanMove]. DAN_5 alone leaves it null and just
 * plays KataGo's own top move instead - see [AiGameViewModel.selectBestMove].
 *
 * DAN_4 uses rank_4d rather than a higher rank: KataGo's docs note high-dan Human SL
 * profiles aren't backed by real search, so they mostly change style, not strength.
 *
 * maxVisits here is a floor - [AiGameViewModel.generateAiMove] raises it to at least the
 * device's CPU core count, since KataGo spins up that many search threads per query
 * regardless of the requested budget.
 */
enum class AiDifficulty(
  val maxVisits: Int,
  val humanSLProfile: String?,
  val rank: DifficultyRank,
) {
  KYU_20(maxVisits = 3, humanSLProfile = "rank_20k", rank = DifficultyRank.Kyu(20)),
  KYU_18(maxVisits = 3, humanSLProfile = "rank_18k", rank = DifficultyRank.Kyu(18)),
  KYU_16(maxVisits = 3, humanSLProfile = "rank_16k", rank = DifficultyRank.Kyu(16)),
  KYU_14(maxVisits = 3, humanSLProfile = "rank_14k", rank = DifficultyRank.Kyu(14)),
  KYU_12(maxVisits = 3, humanSLProfile = "rank_12k", rank = DifficultyRank.Kyu(12)),
  KYU_10(maxVisits = 3, humanSLProfile = "rank_10k", rank = DifficultyRank.Kyu(10)),
  KYU_8(maxVisits = 3, humanSLProfile = "rank_8k", rank = DifficultyRank.Kyu(8)),
  KYU_6(maxVisits = 3, humanSLProfile = "rank_6k", rank = DifficultyRank.Kyu(6)),
  KYU_4(maxVisits = 3, humanSLProfile = "rank_4k", rank = DifficultyRank.Kyu(4)),
  KYU_2(maxVisits = 3, humanSLProfile = "rank_2k", rank = DifficultyRank.Kyu(2)),
  DAN_1(maxVisits = 3, humanSLProfile = "rank_1d", rank = DifficultyRank.Dan(1)),
  DAN_3(maxVisits = 3, humanSLProfile = "rank_3d", rank = DifficultyRank.Dan(3)),
  DAN_4(maxVisits = 3, humanSLProfile = "rank_4d", rank = DifficultyRank.Dan(4)),
  DAN_5(maxVisits = 40, humanSLProfile = null, rank = DifficultyRank.Dan(5)),
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
  val consecutiveLowWinrateTurns: Int = 0,
  val aiResignOfferDeclined: Boolean = false,
  val aiResignOfferShowing: Boolean = false,
  val engineFailedToStart: Boolean = false,
) {
  val isGameReady: Boolean
    get() = engineStarted && !stateRestorePending
}