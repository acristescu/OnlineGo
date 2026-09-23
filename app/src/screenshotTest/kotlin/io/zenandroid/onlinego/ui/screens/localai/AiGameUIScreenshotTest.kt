package io.zenandroid.onlinego.ui.screens.localai

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.model.katago.KataGoResponse.Response
import io.zenandroid.onlinego.data.model.katago.RootInfo
import io.zenandroid.onlinego.ui.composables.textResource
import io.zenandroid.onlinego.ui.theme.OnlineGoPreviewTheme
import kotlinx.collections.immutable.persistentListOf

private val screenshotState = AiGameState(
  boardSize = 19,
  enginePlaysBlack = true,
  engineStarted = true,
  stateRestorePending = false,
  chatText = textResource(R.string.ai_game_chat_game_over_ai_won, 61.5f, 58f),
  position = Position(
    boardWidth = 19,
    boardHeight = 19,
    blackCaptureCount = 4,
    whiteCaptureCount = 2,
    komi = 6.5f,
  ),
  candidateMove = null,
  boardIsInteractive = true,
  showFinalTerritory = false,
  passButtonEnabled = true,
  previousButtonEnabled = true,
  nextButtonEnabled = true,
  newGameDialogShown = false,
  ownershipButtonVisible = true,
  hintButtonVisible = true,
  aiAnalysis = Response(
    id = "aaa",
    turnNumber = 1,
    moveInfos = persistentListOf(),
    policy = null,
    rootInfo = RootInfo(
      winrate = 0.72f,
      scoreLead = 4.5f,
    )
  ),
  aiQuickEstimation = null
)

@PreviewTest
@Preview(name = "AI game - light, long chat bubble", showBackground = true)
@Composable
fun AiGameUiScreenshotLight() {
  OnlineGoPreviewTheme(darkTheme = false) {
    AiGameUI(
      state = screenshotState,
      userIcon = null,
      onUserTappedCoordinate = {},
      onUserHotTrackedCoordinate = {},
      onUserPressedPass = {},
      onUserPressedPrevious = {},
      onUserPressedNext = {},
      onShowNewGameDialog = {},
      onUserAskedForHint = {},
      onUserAskedForOwnership = {},
      onNewGame = { _, _, _, _ -> },
      onDismissNewGameDialog = {},
      onDismissKoDialog = {},
      onAcceptAiResignOffer = {},
      onDeclineAiResignOffer = {},
      onNavigateBack = {}
    )
  }
}

@PreviewTest
@Preview(name = "AI game - eval hidden", showBackground = true)
@Composable
fun AiGameUiScreenshotEvalHidden() {
  OnlineGoPreviewTheme(darkTheme = false) {
    AiGameUI(
      state = screenshotState,
      userIcon = null,
      onUserTappedCoordinate = {},
      onUserHotTrackedCoordinate = {},
      onUserPressedPass = {},
      onUserPressedPrevious = {},
      onUserPressedNext = {},
      onShowNewGameDialog = {},
      onUserAskedForHint = {},
      onUserAskedForOwnership = {},
      onNewGame = { _, _, _, _ -> },
      onDismissNewGameDialog = {},
      onDismissKoDialog = {},
      onAcceptAiResignOffer = {},
      onDeclineAiResignOffer = {},
      onNavigateBack = {},
      initialEvalVisible = false,
    )
  }
}

@PreviewTest
@Preview(name = "AI game - dark, long chat bubble", showBackground = true, uiMode = 0x20)
@Composable
fun AiGameUiScreenshotDark() {
  OnlineGoPreviewTheme(darkTheme = true) {
    AiGameUI(
      state = screenshotState,
      userIcon = null,
      onUserTappedCoordinate = {},
      onUserHotTrackedCoordinate = {},
      onUserPressedPass = {},
      onUserPressedPrevious = {},
      onUserPressedNext = {},
      onShowNewGameDialog = {},
      onUserAskedForHint = {},
      onUserAskedForOwnership = {},
      onNewGame = { _, _, _, _ -> },
      onDismissNewGameDialog = {},
      onDismissKoDialog = {},
      onAcceptAiResignOffer = {},
      onDeclineAiResignOffer = {},
      onNavigateBack = {}
    )
  }
}

@PreviewTest
@Preview(name = "AI game - loading (engine not ready)", showBackground = true)
@Composable
fun AiGameUiScreenshotLoading() {
  OnlineGoPreviewTheme(darkTheme = false) {
    AiGameUI(
      state = screenshotState.copy(
        engineStarted = false,
        stateRestorePending = false,
        boardIsInteractive = true,
        passButtonEnabled = true,
        previousButtonEnabled = true,
        nextButtonEnabled = true,
      ),
      userIcon = null,
      onUserTappedCoordinate = {},
      onUserHotTrackedCoordinate = {},
      onUserPressedPass = {},
      onUserPressedPrevious = {},
      onUserPressedNext = {},
      onShowNewGameDialog = {},
      onUserAskedForHint = {},
      onUserAskedForOwnership = {},
      onNewGame = { _, _, _, _ -> },
      onDismissNewGameDialog = {},
      onDismissKoDialog = {},
      onAcceptAiResignOffer = {},
      onDeclineAiResignOffer = {},
      onNavigateBack = {}
    )
  }
}

@PreviewTest
@Preview(name = "AI game - short chat bubble", showBackground = true)
@Composable
fun AiGameUiScreenshotShortMessage() {
  OnlineGoPreviewTheme(darkTheme = false) {
    AiGameUI(
      state = screenshotState.copy(chatText = textResource(R.string.ai_game_chat_ready)),
      userIcon = null,
      onUserTappedCoordinate = {},
      onUserHotTrackedCoordinate = {},
      onUserPressedPass = {},
      onUserPressedPrevious = {},
      onUserPressedNext = {},
      onShowNewGameDialog = {},
      onUserAskedForHint = {},
      onUserAskedForOwnership = {},
      onNewGame = { _, _, _, _ -> },
      onDismissNewGameDialog = {},
      onDismissKoDialog = {},
      onAcceptAiResignOffer = {},
      onDeclineAiResignOffer = {},
      onNavigateBack = {}
    )
  }
}

@PreviewTest
@Preview(name = "AI game - new game, no eval yet", showBackground = true)
@Composable
fun AiGameUiScreenshotNoEvalYet() {
  OnlineGoPreviewTheme(darkTheme = false) {
    AiGameUI(
      state = screenshotState.copy(
        aiAnalysis = null,
        aiQuickEstimation = null,
        chatText = textResource(R.string.ai_game_chat_your_turn),
      ),
      userIcon = null,
      onUserTappedCoordinate = {},
      onUserHotTrackedCoordinate = {},
      onUserPressedPass = {},
      onUserPressedPrevious = {},
      onUserPressedNext = {},
      onShowNewGameDialog = {},
      onUserAskedForHint = {},
      onUserAskedForOwnership = {},
      onNewGame = { _, _, _, _ -> },
      onDismissNewGameDialog = {},
      onDismissKoDialog = {},
      onAcceptAiResignOffer = {},
      onDeclineAiResignOffer = {},
      onNavigateBack = {}
    )
  }
}

@PreviewTest
@Preview(name = "AI game - no hint or territory buttons", showBackground = true)
@Composable
fun AiGameUiScreenshotNoAnalysisButtons() {
  OnlineGoPreviewTheme(darkTheme = false) {
    AiGameUI(
      state = screenshotState.copy(
        chatText = textResource(R.string.ai_game_chat_ready),
        ownershipButtonVisible = false,
        hintButtonVisible = false,
      ),
      userIcon = null,
      onUserTappedCoordinate = {},
      onUserHotTrackedCoordinate = {},
      onUserPressedPass = {},
      onUserPressedPrevious = {},
      onUserPressedNext = {},
      onShowNewGameDialog = {},
      onUserAskedForHint = {},
      onUserAskedForOwnership = {},
      onNewGame = { _, _, _, _ -> },
      onDismissNewGameDialog = {},
      onDismissKoDialog = {},
      onAcceptAiResignOffer = {},
      onDeclineAiResignOffer = {},
      onNavigateBack = {}
    )
  }
}

@PreviewTest
@Preview(name = "AI game - landscape", widthDp = 800, heightDp = 360, showBackground = true)
@Composable
fun AiGameUiScreenshotLandscape() {
  OnlineGoPreviewTheme(darkTheme = false) {
    AiGameUI(
      state = screenshotState,
      userIcon = null,
      onUserTappedCoordinate = {},
      onUserHotTrackedCoordinate = {},
      onUserPressedPass = {},
      onUserPressedPrevious = {},
      onUserPressedNext = {},
      onShowNewGameDialog = {},
      onUserAskedForHint = {},
      onUserAskedForOwnership = {},
      onNewGame = { _, _, _, _ -> },
      onDismissNewGameDialog = {},
      onDismissKoDialog = {},
      onAcceptAiResignOffer = {},
      onDeclineAiResignOffer = {},
      onNavigateBack = {}
    )
  }
}
