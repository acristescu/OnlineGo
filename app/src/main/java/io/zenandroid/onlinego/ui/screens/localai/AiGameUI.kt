@file:OptIn(ExperimentalMaterial3Api::class)

package io.zenandroid.onlinego.ui.screens.localai

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.data.model.katago.KataGoResponse.Response
import io.zenandroid.onlinego.data.model.katago.RootInfo
import io.zenandroid.onlinego.ui.composables.Board
import io.zenandroid.onlinego.ui.composables.resolve
import io.zenandroid.onlinego.ui.composables.textResource
import io.zenandroid.onlinego.ui.screens.game.PlayerData
import io.zenandroid.onlinego.ui.screens.game.composables.PlayerCard
import io.zenandroid.onlinego.ui.screens.localai.composables.AiChatBox
import io.zenandroid.onlinego.ui.theme.OnlineGoPreviewTheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.koin.androidx.compose.koinViewModel
import kotlin.math.abs

sealed class AiGameBottomBarButton(
  val icon: androidx.compose.ui.graphics.vector.ImageVector,
  val labelResId: Int,
  val enabled: Boolean = true,
) {

  data class NewGame(
    val isEnabled: Boolean = true
  ) : AiGameBottomBarButton(
    icon = Icons.Filled.Casino,
    labelResId = R.string.ai_game_new,
    enabled = isEnabled
  )

  data class Pass(
    val isEnabled: Boolean = true
  ) : AiGameBottomBarButton(
    icon = Icons.Rounded.Stop,
    labelResId = R.string.ai_game_pass,
    enabled = isEnabled
  )

  data class Previous(
    val isEnabled: Boolean = true
  ) : AiGameBottomBarButton(
    icon = Icons.AutoMirrored.Filled.NavigateBefore,
    labelResId = R.string.ai_game_previous,
    enabled = isEnabled
  )

  data class Next(
    val isEnabled: Boolean = true
  ) : AiGameBottomBarButton(
    icon = Icons.AutoMirrored.Filled.NavigateNext,
    labelResId = R.string.ai_game_next,
    enabled = isEnabled
  )
}

@Composable
fun AiGameScreen(
  viewModel: AiGameViewModel = koinViewModel(),
  onNavigateBack: () -> Unit,
) {
  val lifecycle = LocalLifecycleOwner.current.lifecycle

  DisposableEffect(lifecycle) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_PAUSE) {
        viewModel.onViewPaused()
      }
    }

    lifecycle.addObserver(observer)
    onDispose {
      lifecycle.removeObserver(observer)
    }
  }

  val state by viewModel.state.collectAsStateWithLifecycle()

  AiGameUI(
    state = state,
    userIcon = state.userIcon,
    onUserTappedCoordinate = viewModel::onUserTappedCoordinate,
    onUserHotTrackedCoordinate = viewModel::onUserHotTrackedCoordinate,
    onUserPressedPass = viewModel::onUserPressedPass,
    onUserPressedPrevious = viewModel::onUserPressedPrevious,
    onUserPressedNext = viewModel::onUserPressedNext,
    onShowNewGameDialog = viewModel::onShowNewGameDialog,
    onUserAskedForHint = viewModel::onUserAskedForHint,
    onUserAskedForOwnership = viewModel::onUserAskedForOwnership,
    onNewGame = viewModel::onNewGame,
    onDismissNewGameDialog = viewModel::onDismissNewGameDialog,
    onDismissKoDialog = viewModel::onDismissKoDialog,
    onAcceptAiResignOffer = viewModel::onAiResignOfferAccepted,
    onDeclineAiResignOffer = viewModel::onAiResignOfferDeclined,
    onNavigateBack = onNavigateBack,
  )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun AiGameUI(
  state: AiGameState,
  userIcon: String?,
  onUserTappedCoordinate: (Cell) -> Unit,
  onUserHotTrackedCoordinate: (Cell) -> Unit,
  onUserPressedPass: () -> Unit,
  onUserPressedPrevious: () -> Unit,
  onUserPressedNext: () -> Unit,
  onShowNewGameDialog: () -> Unit,
  onUserAskedForHint: () -> Unit,
  onUserAskedForOwnership: () -> Unit,
  onNewGame: (Int, Boolean, Int, AiDifficulty) -> Unit,
  onDismissNewGameDialog: () -> Unit,
  onNavigateBack: () -> Unit,
  onDismissKoDialog: () -> Unit,
  onAcceptAiResignOffer: () -> Unit,
  onDeclineAiResignOffer: () -> Unit,
  initialEvalVisible: Boolean = true,
) {
  val configuration = LocalConfiguration.current
  val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
  var evalVisible by remember { mutableStateOf(initialEvalVisible) }
  val scoreLead = state.aiAnalysis?.rootInfo?.scoreLead ?: state.aiQuickEstimation?.scoreLead
  val winrate = state.aiAnalysis?.rootInfo?.winrate ?: state.aiQuickEstimation?.winrate

  if (isLandscape) {
    Row(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.surface)
        .systemBarsPadding()
    ) {
      Column(
        Modifier
          .width(0.dp)
          .weight(1f)
      ) {
        Header(state = state, onNavigateBack = onNavigateBack)
        AiPlayerRow(
          state = state,
          onUserAskedForHint = onUserAskedForHint,
          onUserAskedForOwnership = onUserAskedForOwnership,
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)
        )
        if (evalVisible) {
          AnalysisPanel(
            scoreLead = scoreLead,
            winrate = winrate,
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 8.dp),
          )
        }
        UserPlayerRow(
          state = state,
          userIcon = userIcon,
          evalVisible = evalVisible,
          onToggleEvalVisible = { evalVisible = !evalVisible },
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(horizontal = 12.dp)
        )
        AiGameBottomBar(
          state = state,
          onShowNewGameDialog = onShowNewGameDialog,
          onUserPressedPass = onUserPressedPass,
          onUserPressedPrevious = onUserPressedPrevious,
          onUserPressedNext = onUserPressedNext
        )
      }
      BoardSection(
        state = state,
        onUserTappedCoordinate = onUserTappedCoordinate,
        onUserHotTrackedCoordinate = onUserHotTrackedCoordinate,
        modifier = Modifier.fillMaxHeight()
      )
    }
  } else {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .systemBarsPadding()
    ) {
      Header(state = state, onNavigateBack = onNavigateBack)
      AiPlayerRow(
        state = state,
        onUserAskedForHint = onUserAskedForHint,
        onUserAskedForOwnership = onUserAskedForOwnership,
        modifier = Modifier
          .fillMaxWidth()
          .weight(1.5f)
      )
      BoardSection(
        state = state,
        onUserTappedCoordinate = onUserTappedCoordinate,
        onUserHotTrackedCoordinate = onUserHotTrackedCoordinate,
        modifier = Modifier
          .fillMaxWidth()
      )
      if (evalVisible) {
        AnalysisPanel(
          scoreLead = scoreLead,
          winrate = winrate,
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        )
      }
      UserPlayerRow(
        state = state,
        userIcon = userIcon,
        evalVisible = evalVisible,
        onToggleEvalVisible = { evalVisible = !evalVisible },
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
      )
      AiGameBottomBar(
        state = state,
        onShowNewGameDialog = onShowNewGameDialog,
        onUserPressedPass = onUserPressedPass,
        onUserPressedPrevious = onUserPressedPrevious,
        onUserPressedNext = onUserPressedNext
      )
    }
  }

  if (state.newGameDialogShown) {
    NewGameBottomSheet(
      currentBoardSize = state.boardSize,
      currentYouPlayBlack = !state.enginePlaysBlack,
      currentHandicap = state.handicap,
      currentDifficulty = state.difficulty,
      canStartGame = state.isGameReady,
      onDismiss = {
        onDismissNewGameDialog()
      },
      onNewGame = { size, youPlayBlack, handicap, difficulty ->
        onNewGame(size, youPlayBlack, handicap, difficulty)
      }
    )
  }

  if (state.koMoveDialogShowing) {
    AlertDialog(
      onDismissRequest = onDismissKoDialog,
      confirmButton = {
        TextButton(onClick = onDismissKoDialog) {
          Text(stringResource(R.string.ok))
        }
      },
      text = { Text(stringResource(R.string.ko_explanation)) },
      title = {
        Text(
          stringResource(R.string.illegal_ko_move),
          style = MaterialTheme.typography.titleLarge
        )
      },
    )
  }

  if (state.aiResignOfferShowing) {
    AlertDialog(
      onDismissRequest = onDeclineAiResignOffer,
      confirmButton = {
        TextButton(onClick = onAcceptAiResignOffer) {
          Text(stringResource(R.string.ai_resign_offer_accept))
        }
      },
      dismissButton = {
        TextButton(onClick = onDeclineAiResignOffer) {
          Text(stringResource(R.string.ai_resign_offer_decline))
        }
      },
      text = { Text(stringResource(R.string.ai_resign_offer_message)) },
      title = {
        Text(
          stringResource(R.string.ai_resign_offer_title),
          style = MaterialTheme.typography.titleLarge
        )
      },
    )
  }
}

@Composable
private fun BoardSection(
  state: AiGameState,
  onUserTappedCoordinate: (Cell) -> Unit,
  onUserHotTrackedCoordinate: (Cell) -> Unit,
  modifier: Modifier = Modifier
) {
  Board(
    boardWidth = state.boardSize,
    boardHeight = state.boardSize,
    position = state.position,
    hints = if (state.showHints) state.aiAnalysis?.moveInfos?.toImmutableList() else null,
    ownership = if (state.showAiEstimatedTerritory) state.aiAnalysis?.ownership?.toImmutableList() else null,
    candidateMove = state.candidateMove,
    candidateMoveType = if (state.enginePlaysBlack) StoneType.WHITE else StoneType.BLACK,
    interactive = state.boardIsInteractive && state.isGameReady,
    drawTerritory = state.showFinalTerritory,
    fadeOutRemovedStones = state.showFinalTerritory,
    onTapMove = onUserHotTrackedCoordinate,
    onTapUp = onUserTappedCoordinate,
    modifier = modifier
      .shadow(2.dp, MaterialTheme.shapes.medium)
      .clip(MaterialTheme.shapes.medium)
  )
}

@Composable
private fun Header(
  state: AiGameState,
  onNavigateBack: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(start = 4.dp, end = 16.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    IconButton(onClick = onNavigateBack) {
      Icon(
        Icons.AutoMirrored.Filled.ArrowBack,
        contentDescription = stringResource(R.string.back),
        tint = MaterialTheme.colorScheme.onSurface
      )
    }
    Column(modifier = Modifier.padding(start = 4.dp)) {
      Text(
        text = stringResource(R.string.local_ai_game),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface
      )
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = state.difficulty.rank.resolve(),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!state.isGameReady) {
          Spacer(Modifier.width(8.dp))
          CircularProgressIndicator(
            modifier = Modifier.size(12.dp),
            strokeWidth = 1.5.dp,
            color = MaterialTheme.colorScheme.primary
          )
        }
      }
    }
  }
}

@Composable
private fun AiPlayerRow(
  state: AiGameState,
  onUserAskedForHint: () -> Unit,
  onUserAskedForOwnership: () -> Unit,
  modifier: Modifier = Modifier,
) {
  OutlinedCard(
    modifier = modifier,
    border = CardDefaults.outlinedCardBorder(enabled = false),
    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
  ) {
    Column(
      modifier = Modifier
        .fillMaxHeight()
    ) {
      Row(
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        PlayerCard(
          player = aiPlayerData(state),
          timerMain = "",
          timerExtra = "",
          timerPercent = 0,
          timerFaded = false,
          timerShown = false,
          onUserClicked = {},
          onGameDetailsClicked = {},
          localAvatarRes = R.drawable.katago,
          modifier = Modifier.weight(1f),
        )
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
          modifier = Modifier
            .height(PlayerCardMaxHeight)
            .width(IntrinsicSize.Max)
            .padding(end = 16.dp)
        ) {
          if (state.ownershipButtonVisible) {
            Button(
              onClick = onUserAskedForOwnership,
              enabled = state.isGameReady,
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
              shape = MaterialTheme.shapes.small,
              modifier = Modifier
                .fillMaxWidth()
                .height(0.dp)
                .weight(1f),
            ) {
              Text(stringResource(R.string.territory), style = MaterialTheme.typography.labelMedium)
            }
          }
          if (state.hintButtonVisible) {
            Spacer(Modifier.height(4.dp))
            Button(
              onClick = onUserAskedForHint,
              enabled = state.isGameReady,
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
              shape = MaterialTheme.shapes.small,
              modifier = Modifier
                .fillMaxWidth()
                .height(0.dp)
                .weight(1f),
            ) {
              Text(stringResource(R.string.hint), style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.height(4.dp))
          }
        }
      }
      AiChatBox(
        text = when {
          state.engineFailedToStart -> state.chatText?.resolve()
          !state.isGameReady -> stringResource(R.string.ai_game_chat_engine_starting)
          else -> state.chatText?.resolve()
        },
        modifier = Modifier
          .fillMaxWidth()
          .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
      )
    }
  }
}

private val PlayerCardMaxHeight = 84.dp

@Composable
private fun UserPlayerRow(
  state: AiGameState,
  userIcon: String?,
  evalVisible: Boolean,
  onToggleEvalVisible: () -> Unit,
  modifier: Modifier = Modifier,
) {
  OutlinedCard(
    modifier = modifier,
    border = CardDefaults.outlinedCardBorder(enabled = false),
    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
  ) {
    Row(
      modifier = Modifier.fillMaxHeight(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      PlayerCard(
        player = userPlayerData(state, userIcon),
        timerMain = "",
        timerExtra = "",
        timerPercent = 0,
        timerFaded = false,
        timerShown = false,
        onUserClicked = {},
        onGameDetailsClicked = {},
        localAvatarRes = R.mipmap.placeholder,
        modifier = Modifier
          .weight(1f)
          .padding(vertical = 12.dp),
      )
      Button(
        onClick = onToggleEvalVisible,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.padding(end = 16.dp),
      ) {
        Text(
          text = stringResource(
            if (evalVisible) R.string.ai_game_hide_eval else R.string.ai_game_show_eval
          ),
          style = MaterialTheme.typography.labelMedium,
        )
      }
    }
  }
}

@Composable
private fun aiPlayerData(state: AiGameState): PlayerData {
  val aiPaysKomi = !state.enginePlaysBlack
  return PlayerData(
    name = stringResource(R.string.katago).trim(),
    details = state.position?.let { position ->
      val captures =
        if (state.enginePlaysBlack) position.blackCaptureCount else position.whiteCaptureCount
      val komi = position.komi
      if (aiPaysKomi && komi != null) textResource(
        R.string.ai_game_player_captures_and_komi,
        captures,
        komi
      )
      else textResource(R.string.ai_game_player_captures_only, captures)
    },
    rank = state.difficulty.rank.shortLabel(),
    flagCode = "",
    iconURL = null,
    color = if (state.enginePlaysBlack) StoneType.BLACK else StoneType.WHITE,
  )
}

@Composable
private fun userPlayerData(state: AiGameState, userIcon: String?): PlayerData {
  val userPaysKomi = state.enginePlaysBlack
  return PlayerData(
    name = stringResource(R.string.you).trim(),
    details = state.position?.let { position ->
      val captures =
        if (state.enginePlaysBlack) position.whiteCaptureCount else position.blackCaptureCount
      val komi = position.komi
      if (userPaysKomi && komi != null) textResource(
        R.string.ai_game_player_captures_and_komi,
        captures,
        komi
      )
      else textResource(R.string.ai_game_player_captures_only, captures)
    },
    rank = "",
    flagCode = "",
    iconURL = userIcon,
    color = if (state.enginePlaysBlack) StoneType.WHITE else StoneType.BLACK,
  )
}

private fun DifficultyRank.shortLabel(): String = when (this) {
  is DifficultyRank.Kyu -> "${n}k"
  is DifficultyRank.Dan -> "${n}d"
}

@Composable
private fun AnalysisPanel(
  scoreLead: Float?,
  winrate: Float?,
  modifier: Modifier = Modifier,
) {
  OutlinedCard(
    modifier = modifier,
    border = CardDefaults.outlinedCardBorder(enabled = false),
    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
  ) {
    Column(
      modifier = Modifier
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 12.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.Center,
    ) {
      val showEval = winrate != null
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (showEval) {
          val whitePercentage = winrate!! * 100f
          val blackPercentage = 100f - whitePercentage
          Text(
            text = stringResource(
              R.string.ai_game_eval_winrate_compact,
              blackPercentage,
              whitePercentage
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        } else {
          Text(
            text = EvalPlaceholder,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          if (scoreLead != null) {
            val leaderLetter = if (scoreLead > 0) "W" else "B"
            val magnitude = abs(scoreLead)
            Text(
              text = stringResource(
                R.string.ai_game_eval_score_lead_compact,
                leaderLetter,
                magnitude
              ),
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
      LinearProgressIndicator(
        progress = { if (showEval) 1f - winrate!! else 0f },
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 8.dp)
          .height(6.dp)
          .clip(RoundedCornerShape(3.dp)),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
      )
    }
  }
}

private const val EvalPlaceholder = "B — · — W"


@Composable
private fun AiGameBottomBar(
  state: AiGameState,
  onShowNewGameDialog: () -> Unit,
  onUserPressedPass: () -> Unit,
  onUserPressedPrevious: () -> Unit,
  onUserPressedNext: () -> Unit
) {
  val bottomBarButtons = listOf(
    AiGameBottomBarButton.NewGame(isEnabled = state.isGameReady) to onShowNewGameDialog,
    AiGameBottomBarButton.Pass(isEnabled = state.passButtonEnabled && state.isGameReady) to onUserPressedPass,
    AiGameBottomBarButton.Previous(isEnabled = state.previousButtonEnabled && state.isGameReady) to onUserPressedPrevious,
    AiGameBottomBarButton.Next(isEnabled = state.nextButtonEnabled && state.isGameReady) to onUserPressedNext,
  )
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 2.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    bottomBarButtons.forEach { (button, onClick) ->
      AiGameBottomBarButtonView(
        button = button,
        onClick = onClick,
        modifier = Modifier.weight(1f),
      )
    }
  }
}

@Composable
private fun AiGameBottomBarButtonView(
  button: AiGameBottomBarButton,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  ElevatedButton(
    onClick = onClick,
    enabled = button.enabled,
    shape = RoundedCornerShape(16.dp),
    contentPadding = PaddingValues(vertical = 10.dp, horizontal = 2.dp),
    modifier = modifier,
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Icon(button.icon, contentDescription = null, modifier = Modifier.size(20.dp))
      Spacer(Modifier.height(4.dp))
      Text(stringResource(button.labelResId), style = MaterialTheme.typography.labelSmall)
    }
  }
}

@Composable
private fun getHandicapDescription(handicap: Int): String {
  return when (handicap) {
    0 -> stringResource(R.string.ai_game_handicap_none)
    1 -> stringResource(R.string.ai_game_handicap_no_komi)
    else -> handicap.toString()
  }
}

@Composable
private fun DifficultyRank.resolve(): String = when (this) {
  is DifficultyRank.Kyu -> stringResource(R.string.ai_game_difficulty_kyu, n)
  is DifficultyRank.Dan -> stringResource(R.string.ai_game_difficulty_dan, n)
}

@Composable
private fun NewGameBottomSheet(
  currentBoardSize: Int,
  currentYouPlayBlack: Boolean,
  currentHandicap: Int,
  currentDifficulty: AiDifficulty,
  canStartGame: Boolean,
  onDismiss: () -> Unit,
  onNewGame: (size: Int, youPlayBlack: Boolean, handicap: Int, difficulty: AiDifficulty) -> Unit
) {
  var selectedSize by remember { mutableIntStateOf(currentBoardSize) }
  var youPlayBlack by remember { mutableStateOf(currentYouPlayBlack) }
  var handicap by remember { mutableFloatStateOf(currentHandicap.toFloat()) }
  var difficulty by remember { mutableStateOf(currentDifficulty) }
  val sheetState = rememberModalBottomSheetState(true)
  val difficultyListState = rememberLazyListState(
    initialFirstVisibleItemIndex = (AiDifficulty.entries.indexOf(difficulty) - 2).coerceAtLeast(0)
  )

  ModalBottomSheet(
    sheetState = sheetState,
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
  ) {
    Card(
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
      Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
        Text(
          text = stringResource(R.string.new_game),
          style = MaterialTheme.typography.headlineLarge,
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth(),
        )

        Text(
          text = stringResource(R.string.board_size),
          modifier = Modifier.padding(top = 16.dp)
        )
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.padding(vertical = 8.dp)
        ) {
          listOf(9, 13, 19).forEach { size ->
            FilterChip(
              selected = selectedSize == size,
              colors = FilterChipDefaults.elevatedFilterChipColors(
                selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
              ),
              onClick = { selectedSize = size },
              label = {
                Text("${size}x${size}")
              }
            )
          }
        }

        Text(
          text = stringResource(R.string.you_play),
          modifier = Modifier.padding(top = 16.dp)
        )
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.padding(vertical = 8.dp)
        ) {
          FilterChip(
            selected = youPlayBlack,
            colors = FilterChipDefaults.elevatedFilterChipColors(
              selectedLabelColor = MaterialTheme.colorScheme.onSurface,
              selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            ),
            onClick = { youPlayBlack = true },
            label = {
              Text(stringResource(R.string.Black))
            }
          )
          FilterChip(
            selected = !youPlayBlack,
            colors = FilterChipDefaults.elevatedFilterChipColors(
              selectedLabelColor = MaterialTheme.colorScheme.onSurface,
              selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            ),
            onClick = { youPlayBlack = false },
            label = {
              Text(stringResource(R.string.White))
            }
          )
        }

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(stringResource(R.string.handicap))
          Text(
            text = getHandicapDescription(handicap.toInt()),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
          )
        }
        Slider(
          value = handicap,
          onValueChange = { handicap = it },
          valueRange = 0f..9f,
          steps = 8,
          modifier = Modifier.padding(vertical = 8.dp)
        )

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(stringResource(R.string.difficulty))
          Text(
            text = difficulty.rank.resolve(),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
          )
        }
        Text(
          text = stringResource(R.string.ai_game_difficulty_scale_hint),
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        LazyRow(
          state = difficultyListState,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.padding(vertical = 8.dp)
        ) {
          items(AiDifficulty.entries) { entry ->
            FilterChip(
              selected = difficulty == entry,
              colors = FilterChipDefaults.elevatedFilterChipColors(
                selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
              ),
              onClick = { difficulty = entry },
              label = {
                Text(entry.rank.resolve())
              }
            )
          }
        }

        Button(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
          enabled = canStartGame,
          onClick = { onNewGame(selectedSize, youPlayBlack, handicap.toInt(), difficulty) }
        ) {
          Text(stringResource(R.string.start_game))
        }
        if (!canStartGame) {
          Text(
            text = stringResource(R.string.ai_game_chat_engine_starting),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 8.dp)
          )
        }
      }
    }
  }
}

private val previewState = AiGameState(
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
      winrate = 0.5f,
      scoreLead = 0.0f,
    )
  ),
  aiQuickEstimation = null
)

@Composable
@Preview
private fun AiGameUIPreview() {
  OnlineGoPreviewTheme {
    AiGameUI(
      state = previewState,
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

@Composable
@Preview
private fun AiGameUIPreviewNewGame() {
  OnlineGoPreviewTheme {
    AiGameUI(
      state = previewState.copy(newGameDialogShown = true),
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

@Composable
@Preview
private fun AiGameUIPreviewNoEvalYet() {
  OnlineGoPreviewTheme {
    AiGameUI(
      state = previewState.copy(
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

@Composable
@Preview
private fun AiGameUIPreviewEvalHidden() {
  OnlineGoPreviewTheme {
    AiGameUI(
      state = previewState,
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

@Composable
@Preview(
  name = "Landscape Preview",
  widthDp = 800,
  heightDp = 360
)
private fun PreviewLandscape() {

  OnlineGoPreviewTheme {
    AiGameUI(
      state = previewState,
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
