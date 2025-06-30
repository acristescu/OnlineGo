@file:OptIn(ExperimentalMaterial3Api::class)

package io.zenandroid.onlinego.ui.screens.localai

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import org.koin.androidx.compose.koinViewModel
import kotlin.math.abs

sealed class AiGameBottomBarButton(
  override val icon: androidx.compose.ui.graphics.vector.ImageVector,
  override val label: String,
  override val repeatable: Boolean = false,
  override val enabled: Boolean = true,
  override val bubbleText: String? = null,
  override val highlighted: Boolean = false
) : BottomBarButton {
  data class NewGame(
    override val enabled: Boolean = true
  ) : AiGameBottomBarButton(
    icon = Icons.Filled.Casino,
    label = "New",
    enabled = enabled
  )

  data class Pass(
    override val enabled: Boolean = true
  ) : AiGameBottomBarButton(
    icon = Icons.Rounded.Stop,
    label = "Pass",
    enabled = enabled
  )

  data class Previous(
    override val enabled: Boolean = true
  ) : AiGameBottomBarButton(
    icon = Icons.AutoMirrored.Filled.NavigateBefore,
    label = "Previous",
    enabled = enabled
  )

  data class Next(
    override val enabled: Boolean = true
  ) : AiGameBottomBarButton(
    icon = Icons.AutoMirrored.Filled.NavigateNext,
    label = "Next",
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

  val state by viewModel.state.collectAsState()

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
    onNavigateBack = onNavigateBack
  )
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterialApi::class)
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
  onNavigateBack: () -> Unit
) {
  var showNewGameDialog by remember { mutableStateOf(false) }

  // Update dialog state based on state
  LaunchedEffect(state.newGameDialogShown) {
    showNewGameDialog = state.newGameDialogShown
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.surface)
  ) {
    TopAppBar(
      title = {
        Text(
          text = "Local AI Game",
          fontSize = 16.sp,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.onSurface
        )
      },
      navigationIcon = {
        IconButton(onClick = onNavigateBack) {
          Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = MaterialTheme.colorScheme.onSurface
          )
        }
      },
    )
    // Progress bar
    if (!state.engineStarted) {
      LinearProgressIndicator(
        modifier = Modifier
          .fillMaxWidth()
          .height(1.dp),
        color = colorResource(R.color.colorTextBackground)
      )
    }

    Spacer(modifier = Modifier.weight(1f))
    // Top section with player info
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.Top
    ) {
      // Left player (AI)
      Column(
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Card(
          modifier = Modifier
            .size(80.dp),
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
          text = "KataGO " + (if (!state.enginePlaysBlack) "⚪" else "⚫"),
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(top = 4.dp)
        )
      }

      // Chat bubble and action buttons
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
          .weight(1f)
          .padding(horizontal = 20.dp)
      ) {
        // Chat bubble
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

        // Action buttons
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
                text = "Territory",
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
                text = "Hint",
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
          modifier = Modifier.size(80.dp),
          shape = RoundedCornerShape(4.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
          ),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
          if (userIcon != null) {
            AsyncImage(
              model = ImageRequest.Builder(LocalContext.current)
                .data(processGravatarURL(userIcon, 80))
                .crossfade(true)
                .build(),
              contentDescription = "Player",
              modifier = Modifier.fillMaxSize(),
              contentScale = ContentScale.Crop,
              placeholder = painterResource(R.drawable.ic_person_outline)
            )
          } else {
            Image(
              painter = painterResource(R.drawable.ic_person_outline),
              contentDescription = "Player",
              modifier = Modifier.fillMaxSize(),
              contentScale = ContentScale.Crop
            )
          }
        }

        Text(
          text = (if (state.enginePlaysBlack) "⚪" else "⚫") + " You",
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier
            .widthIn(max = 80.dp)
            .padding(top = 4.dp)
        )
      }
    }

    // Game stats
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      // Left stats
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

      // Center labels
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          text = "Prisoners",
          fontSize = 12.sp
        )
        Text(
          text = "Komi",
          fontSize = 12.sp
        )
      }

      // Right stats
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

    Spacer(modifier = Modifier.weight(1f))
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(1f)
        .padding(4.dp),
      shape = RoundedCornerShape(4.dp),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
      Board(
        modifier = Modifier.fillMaxSize(),
        boardWidth = state.boardSize,
        boardHeight = state.boardSize,
        position = state.position,
        hints = if (state.showHints) state.aiAnalysis?.moveInfos else null,
        ownership = state.aiAnalysis?.ownership,
        candidateMove = state.candidateMove,
        candidateMoveType = if (state.enginePlaysBlack) StoneType.WHITE else StoneType.BLACK,
        interactive = state.boardIsInteractive,
        drawTerritory = state.showFinalTerritory,
        fadeOutRemovedStones = state.showFinalTerritory,
        onTapMove = onUserHotTrackedCoordinate,
        onTapUp = onUserTappedCoordinate
      )
    }

    Spacer(modifier = Modifier.weight(1f))
    // Score lead
    val scoreLead = state.aiAnalysis?.rootInfo?.scoreLead ?: state.aiQuickEstimation?.scoreLead
    scoreLead?.let {
      val leader = if (it > 0) "white" else "black"
      val lead = abs(it * 10).toInt() / 10f
      Text(
        text = "Score prediction: $leader leads by $lead",
        fontSize = 12.sp,
        modifier = Modifier.padding(start = 14.dp, top = 4.dp)
      )
    }

    // Winrate
    val winrate = state.aiAnalysis?.rootInfo?.winrate ?: state.aiQuickEstimation?.winrate
    winrate?.let {
      val winrateAsPercentage = (it * 1000).toInt() / 10f
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 4.dp)
      ) {
        Text(
          text = "White's chance to win: $winrateAsPercentage%",
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

    Spacer(modifier = Modifier.weight(1f))

    // Control buttons using BottomBar
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

// New Game Dialog
    if (showNewGameDialog) {
      NewGameDialog(
        onDismiss = {
          showNewGameDialog = false
          onDismissNewGameDialog()
        },
        onNewGame = { size, youPlayBlack, handicap ->
          showNewGameDialog = false
          onNewGame(size, youPlayBlack, handicap)
        }
      )
    }
  }
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
    title = { Text("New Game") },
    text = {
      Column {
        Text("Board Size")
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

        Text("You play", modifier = Modifier.padding(top = 16.dp))
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
              Text("Black")
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
              Text("White")
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
          Text("Handicap")
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
        Text("Start Game")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
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
        chatText = "Hello world!",
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
          moveInfos = emptyList(),
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
        chatText = "Hello world!",
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
          moveInfos = emptyList(),
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
      onNavigateBack = {}
    )
  }
}