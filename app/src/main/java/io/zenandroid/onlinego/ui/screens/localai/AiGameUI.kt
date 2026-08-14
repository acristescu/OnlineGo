@file:OptIn(ExperimentalMaterial3Api::class)

package io.zenandroid.onlinego.ui.screens.localai

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.data.model.katago.KataGoResponse.Response
import io.zenandroid.onlinego.data.model.katago.RootInfo
import io.zenandroid.onlinego.ui.composables.Board
import io.zenandroid.onlinego.ui.composables.BottomBar
import io.zenandroid.onlinego.ui.composables.BottomBarButton
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme
import io.zenandroid.onlinego.utils.processGravatarURL
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.koin.androidx.compose.koinViewModel
import kotlin.math.abs

sealed class AiGameBottomBarButton(
  override val icon: androidx.compose.ui.graphics.vector.ImageVector,
  override val labelResId: Int,
  override val label: String = "",
  override val repeatable: Boolean = false,
  override val enabled: Boolean = true,
  override val bubbleText: String? = null,
  override val highlighted: Boolean = false
) : BottomBarButton {


  data class NewGame(
    override val enabled: Boolean = true
  ) : AiGameBottomBarButton(
    icon = Icons.Filled.Casino,
    labelResId = R.string.ai_game_new,
    enabled = enabled
  )

  data class Pass(
    override val enabled: Boolean = true
  ) : AiGameBottomBarButton(
    icon = Icons.Rounded.Stop,
    labelResId = R.string.ai_game_pass,
    enabled = enabled
  )
  data class Previous(
    override val enabled: Boolean = true
  ) : AiGameBottomBarButton(
    icon = Icons.AutoMirrored.Filled.NavigateBefore,
    labelResId = R.string.ai_game_previous,
    enabled = enabled
  )

  data class Next(
    override val enabled: Boolean = true
  ) : AiGameBottomBarButton(
    icon = Icons.AutoMirrored.Filled.NavigateNext,
    labelResId = R.string.ai_game_next,
    enabled = enabled
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
    onNavigateBack = onNavigateBack,
  )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun AiGameUI(
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
  onNewGame: (Int, Boolean, Int) -> Unit,
  onDismissNewGameDialog: () -> Unit,
  onNavigateBack: () -> Unit,
  onDismissKoDialog: () -> Unit,
) {
  val configuration = LocalConfiguration.current
  val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

  if (isLandscape) {
    Row(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
      Column(
        Modifier
          .width(0.dp)
          .weight(1f)
      ) {
        InfoSection(
          state = state,
          userIcon = userIcon,
          onNavigateBack = onNavigateBack,
          onUserAskedForHint = onUserAskedForHint,
          onUserAskedForOwnership = onUserAskedForOwnership,
        )
        Spacer(Modifier.weight(0.5f))
        ScoreLeadAndWinrate(state, Modifier.padding(8.dp))
        Spacer(Modifier.weight(0.5f))
        AiGameBottomBar(
          state = state,
          onShowNewGameDialog = onShowNewGameDialog,
          onUserPressedPass = onUserPressedPass,
          onUserPressedPrevious = onUserPressedPrevious,
          onUserPressedNext = onUserPressedNext
        )
      }
      BoardSection(
        state,
        onUserTappedCoordinate,
        onUserHotTrackedCoordinate,
      )
    }
  } else {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
      InfoSection(
        state = state,
        userIcon = userIcon,
        onNavigateBack = onNavigateBack,
        onUserAskedForHint = onUserAskedForHint,
        onUserAskedForOwnership = onUserAskedForOwnership,
        modifier = Modifier
          .fillMaxWidth()
          .weight(0.5f)
      )
      BoardSection(
        state,
        onUserTappedCoordinate,
        onUserHotTrackedCoordinate,
        Modifier.fillMaxWidth()
      )
      ScoreLeadAndWinrate(state, Modifier.weight(0.5f))
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
    NewGameDialog(
      onDismiss = {
        onDismissNewGameDialog()
      },
      onNewGame = { size, youPlayBlack, handicap ->
        onNewGame(size, youPlayBlack, handicap)
      }
    )
  }

  if (state.koMoveDialogShowing) {
    AlertDialog(
      onDismissRequest = onDismissKoDialog,
      confirmButton = {
        TextButton(onClick = onDismissKoDialog) {
          Text("OK")
        }
      },
      text = { Text(stringResource(R.string.ko_explanation)) },
      title = { Text(stringResource(R.string.illegal_ko_move), style = MaterialTheme.typography.titleLarge) },
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
    interactive = state.boardIsInteractive,
    drawTerritory = state.showFinalTerritory,
    fadeOutRemovedStones = state.showFinalTerritory,
    onTapMove = onUserHotTrackedCoordinate,
    onTapUp = onUserTappedCoordinate,
    modifier = modifier
      .shadow(1.dp, MaterialTheme.shapes.medium)
      .clip(MaterialTheme.shapes.medium)
  )
}

@Composable
private fun InfoSection(
  state: AiGameState,
  userIcon: String?,
  onNavigateBack: () -> Unit,
  onUserAskedForHint: () -> Unit,
  onUserAskedForOwnership: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.Top
  ) {
    TopAppBar(
      colors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
      ),
      title = {
        Text(
          text = stringResource(R.string.local_ai_game),
          fontSize = 16.sp,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.onSurface
        )
      },
      navigationIcon = {
        IconButton(onClick = onNavigateBack) {
          Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.back),
            tint = MaterialTheme.colorScheme.onSurface
          )
        }
      },
    )
    if (!state.engineStarted) {
      LinearProgressIndicator(
        modifier = Modifier
          .fillMaxWidth()
          .height(1.dp),
        color = colorResource(R.color.colorTextBackground)
      )
    }
    PlayerInfoRow(state, userIcon, onUserAskedForHint, onUserAskedForOwnership)
    GameStatsRow(state)
  }
}

@Composable
private fun PlayerInfoRow(
  state: AiGameState,
  userIcon: String?,
  onUserAskedForHint: () -> Unit,
  onUserAskedForOwnership: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(8.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.Top
  ) {
    // Left player (AI)
    Column(
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Card(
        modifier = Modifier.size(64.dp),
        shape = CircleShape,
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.background
        ),
      ) {
        Image(
          painter = painterResource(R.drawable.ic_ai),
          contentDescription = "AI",
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Crop,
          colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
        )
      }
      Text(
        text = stringResource(R.string.katago) + (if (!state.enginePlaysBlack) "⚪" else "⚫"),
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 2.dp)
      )
    }
    // Chat bubble and action buttons
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier
        .weight(1f)
        .padding(horizontal = 8.dp)
    ) {
      state.chatText?.let { text ->
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(6.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.background
          ),
        ) {
          Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 12.sp,
            textAlign = TextAlign.Center
          )
        }
      }
      Row(
        modifier = Modifier.padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        if (state.ownershipButtonVisible) {
          OutlinedButton(
            onClick = onUserAskedForOwnership,
            modifier = Modifier
              .width(70.dp)
              .height(24.dp),
            contentPadding = PaddingValues(0.dp)
          ) {
            Text(
              text = stringResource(R.string.territory),
              fontSize = 10.sp,
              color = colorResource(R.color.colorTextSecondary)
            )
          }
        }
        if (state.hintButtonVisible) {
          OutlinedButton(
            onClick = onUserAskedForHint,
            modifier = Modifier
              .width(50.dp)
              .height(24.dp),
            contentPadding = PaddingValues(0.dp)
          ) {
            Text(
              text = stringResource(R.string.hint),
              fontSize = 10.sp,
              color = colorResource(R.color.colorTextSecondary)
            )
          }
        }
      }
    }
    // Right player (User)
    Column(
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Card(
        modifier = Modifier.size(64.dp),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        if (userIcon != null) {
          AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
              .data(processGravatarURL(userIcon, 64))
              .crossfade(true)
              .build(),
            contentDescription = stringResource(R.string.player),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(R.drawable.ic_person_outline)
          )
        } else {
          Image(
            painter = painterResource(R.drawable.ic_person_outline),
            contentDescription = stringResource(R.string.player),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
          )
        }
      }
      Text(
        text = (if (state.enginePlaysBlack) "⚪" else "⚫") + stringResource(R.string.you),
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
          .widthIn(max = 64.dp)
          .padding(top = 2.dp)
      )
    }
  }
}

@Composable
private fun GameStatsRow(state: AiGameState) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp, horizontal = 8.dp),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Column(horizontalAlignment = Alignment.End) {
      Text(
        text = state.position?.let {
          if (state.enginePlaysBlack) it.blackCaptureCount.toString() else it.whiteCaptureCount.toString()
        } ?: "",
        fontSize = 12.sp
      )
      Text(
        text = state.position?.let {
          if (state.enginePlaysBlack) "" else it.komi.toString()
        } ?: "",
        fontSize = 12.sp
      )
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = stringResource(R.string.prisoners),
        fontSize = 12.sp
      )
      Text(
        text = stringResource(R.string.komi),
        fontSize = 12.sp
      )
    }
    Column(horizontalAlignment = Alignment.Start) {
      Text(
        text = state.position?.let {
          if (state.enginePlaysBlack) it.whiteCaptureCount.toString() else it.blackCaptureCount.toString()
        } ?: "",
        fontSize = 12.sp
      )
      Text(
        text = state.position?.let {
          if (state.enginePlaysBlack) it.komi.toString() else ""
        } ?: "",
        fontSize = 12.sp
      )
    }
  }
}

@Composable
private fun ScoreLeadAndWinrate(state: AiGameState, modifier: Modifier = Modifier) {
  Column(modifier) {
    val scoreLead = state.aiAnalysis?.rootInfo?.scoreLead ?: state.aiQuickEstimation?.scoreLead
    scoreLead?.let {
      val leader = if (it > 0) stringResource(R.string.white) else stringResource(R.string.black)
      val lead = abs(it * 10).toInt() / 10f
      Text(
        text = stringResource(R.string.score_prediction_leads_by, leader, lead),
        fontSize = 12.sp,
        modifier = Modifier.padding(top = 4.dp)
      )
    }
    val winrate = state.aiAnalysis?.rootInfo?.winrate ?: state.aiQuickEstimation?.winrate
    winrate?.let {
      val winrateAsPercentage = (it * 1000).toInt() / 10f
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp)
      ) {
        Text(
          text = stringResource(R.string.white_s_chance_to_win, winrateAsPercentage),
          fontSize = 12.sp,
          modifier = Modifier.padding(top = 4.dp),
        )
        LinearProgressIndicator(
          progress = { winrateAsPercentage / 100f },
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .clip(RoundedCornerShape(3.dp)),
          color = Color.LightGray,
          trackColor = Color.Black,
        )
      }
    }
  }
}

@Composable
private fun AiGameBottomBar(
  state: AiGameState,
  onShowNewGameDialog: () -> Unit,
  onUserPressedPass: () -> Unit,
  onUserPressedPrevious: () -> Unit,
  onUserPressedNext: () -> Unit
) {
  val bottomBarButtons = listOf(
    AiGameBottomBarButton.NewGame(),
    AiGameBottomBarButton.Pass(enabled = state.passButtonEnabled),
    AiGameBottomBarButton.Previous(enabled = state.previousButtonEnabled),
    AiGameBottomBarButton.Next(enabled = state.nextButtonEnabled)
  )
  BottomBar(
    buttons = bottomBarButtons,
    bottomText = null,
    onButtonPressed = { button ->
      when (button) {
        is AiGameBottomBarButton.NewGame -> onShowNewGameDialog()
        is AiGameBottomBarButton.Pass -> onUserPressedPass()
        is AiGameBottomBarButton.Previous -> onUserPressedPrevious()
        is AiGameBottomBarButton.Next -> onUserPressedNext()
      }
    }
  )
}

private fun getHandicapDescription(handicap: Int): String {
  return when (handicap) {
    0 -> "none"
    1 -> "no komi"
    else -> handicap.toString()
  }
}

@Composable
private fun NewGameDialog(
  onDismiss: () -> Unit,
  onNewGame: (size: Int, youPlayBlack: Boolean, handicap: Int) -> Unit
) {
  var selectedSize by remember { mutableIntStateOf(19) }
  var youPlayBlack by remember { mutableStateOf(true) }
  var handicap by remember { mutableFloatStateOf(0f) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.new_game)) },
    text = {
      Column {
        Text(stringResource(R.string.board_size))
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

        Text(stringResource(R.string.you_play), modifier = Modifier.padding(top = 16.dp))
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
      }
    },
    confirmButton = {
      TextButton(
        onClick = { onNewGame(selectedSize, youPlayBlack, handicap.toInt()) }
      ) {
        Text(stringResource(R.string.start_game))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.cancel))
      }
    }
  )
}

@Composable
@Preview
private fun AiGameUIPreview() {
  OnlineGoTheme {
    AiGameUI(
      state = AiGameState(
        boardSize = 19,
        enginePlaysBlack = true,
        engineStarted = true,
        chatText = stringResource(R.string.hello_world),
        position = Position(
          boardWidth = 19,
          boardHeight = 19,
          blackCaptureCount = 0,
          whiteCaptureCount = 0,
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
      onNewGame = { _, _, _ -> },
      onDismissNewGameDialog = {},
      onDismissKoDialog = {},
      onNavigateBack = {}
    )
  }
}

@Composable
@Preview
private fun AiGameUIPreviewNewGame() {
  OnlineGoTheme {
    AiGameUI(
      state = AiGameState(
        boardSize = 19,
        enginePlaysBlack = true,
        engineStarted = true,
        chatText = stringResource(R.string.hello_world),
        position = Position(
          boardWidth = 19,
          boardHeight = 19,
          blackCaptureCount = 0,
          whiteCaptureCount = 0,
          komi = 6.5f,
        ),
        candidateMove = null,
        boardIsInteractive = true,
        showFinalTerritory = false,
        passButtonEnabled = true,
        previousButtonEnabled = true,
        nextButtonEnabled = true,
        newGameDialogShown = true,
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
      onNewGame = { _, _, _ -> },
      onDismissNewGameDialog = {},
      onDismissKoDialog = {},
      onNavigateBack = {}
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

  OnlineGoTheme {
    AiGameUI(
      state = AiGameState(
        boardSize = 19,
        enginePlaysBlack = true,
        engineStarted = true,
        chatText = stringResource(R.string.hello_world),
        position = Position(
          boardWidth = 19,
          boardHeight = 19,
          blackCaptureCount = 0,
          whiteCaptureCount = 0,
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
      onNewGame = { _, _, _ -> },
      onDismissNewGameDialog = {},
      onDismissKoDialog = {},
      onNavigateBack = {}
    )
  }
}