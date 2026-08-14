@file:OptIn(ExperimentalMaterial3Api::class)

package io.zenandroid.onlinego.ui.screens.face2face

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import io.zenandroid.onlinego.R.drawable
import io.zenandroid.onlinego.R.mipmap
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.data.model.StoneType.BLACK
import io.zenandroid.onlinego.data.model.StoneType.WHITE
import io.zenandroid.onlinego.ui.composables.Board
import io.zenandroid.onlinego.ui.composables.BottomBar
import io.zenandroid.onlinego.ui.composables.TitleBar
import io.zenandroid.onlinego.ui.screens.face2face.Action.BoardCellDragged
import io.zenandroid.onlinego.ui.screens.face2face.Action.BoardCellTapUp
import io.zenandroid.onlinego.ui.screens.face2face.Action.BottomButtonPressed
import io.zenandroid.onlinego.ui.screens.face2face.Action.KOMoveDialogDismiss
import io.zenandroid.onlinego.ui.screens.face2face.Action.NewGameDialogDismiss
import io.zenandroid.onlinego.ui.screens.face2face.Action.StartNewGame
import io.zenandroid.onlinego.ui.screens.face2face.Button.Estimate
import io.zenandroid.onlinego.ui.screens.face2face.Button.GameSettings
import io.zenandroid.onlinego.ui.screens.face2face.Button.Next
import io.zenandroid.onlinego.ui.screens.face2face.Button.Previous
import io.zenandroid.onlinego.ui.screens.game.ExtraStatusField
import io.zenandroid.onlinego.ui.screens.face2face.session.FaceToFaceLanDiscoveredHost
import org.koin.androidx.compose.koinViewModel
import java.lang.Float.max

@Composable
fun FaceToFaceScreen(
  viewModel: FaceToFaceViewModel = koinViewModel(),
  onNavigateBack: () -> Unit,
) {
  val state by viewModel.state.collectAsStateWithLifecycle()

  FaceToFaceContent(state, viewModel::onAction, onNavigateBack)
}

@Composable
private fun FaceToFaceContent(
  state: FaceToFaceState,
  onUserAction: (Action) -> Unit,
  onBackPressed: () -> Unit,
) {
  if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {
    Column(
      Modifier
        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
        .fillMaxSize()
    ) {
      TitleBar(
        title = state.title,
        titleIcon = null,
        onTitleClicked = null,
        onBack = onBackPressed,
        moreMenuItems = emptyList(),
      )

      Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
          .padding(horizontal = 16.dp)
          .padding(top = 16.dp, bottom = 16.dp)
          .fillMaxWidth()
      ) {
        Column {
          UserImage(BLACK)
          Text(
            text = state.blackPlayerLabel,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.width(84.dp)
          )
        }
        state.position?.let { ScoreSheet(it) } ?: Spacer(modifier = Modifier.weight(1f))
        Column {
          UserImage(WHITE, Modifier.padding(start = 4.dp))
          Text(
            text = state.whitePlayerLabel,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.width(84.dp)
          )
        }
      }
      Board(
        boardWidth = state.position?.boardWidth ?: 19,
        boardHeight = state.position?.boardHeight ?: 19,
        position = state.position,
        interactive = state.boardInteractive,
        drawTerritory = state.drawTerritory,
        drawLastMove = state.showLastMove,
        fadeOutRemovedStones = state.fadeOutRemovedStones,
        candidateMove = state.candidateMove,
        candidateMoveType = state.position?.nextToMove,
        onTapMove = { onUserAction(BoardCellDragged(it)) },
        onTapUp = { onUserAction(BoardCellTapUp(it)) },
        modifier = Modifier
          .shadow(1.dp, MaterialTheme.shapes.medium)
          .clip(MaterialTheme.shapes.medium)
      )
      ExtraStatusField(
        text = state.extraStatus,
        modifier = Modifier
          .background(Color(0xFF867484))
          .fillMaxWidth()
          .padding(4.dp)
          .align(Alignment.CenterHorizontally),
      )
      Spacer(modifier = Modifier.weight(1f))
      BottomBar(
        buttons = state.buttons,
        bottomText = state.bottomText,
        onButtonPressed = { onUserAction(BottomButtonPressed(it as Button)) }
      )
    }
  } else {
    Row(
      Modifier
        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
        .fillMaxSize()
    ) {
      Column(
        Modifier
          .width(0.dp)
          .weight(1f)
      ) {
        TitleBar(
          title = state.title,
          titleIcon = null,
          onTitleClicked = null,
          onBack = onBackPressed,
          moreMenuItems = emptyList(),
        )

        Row(
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 16.dp)
            .fillMaxWidth()
        ) {
          Column {
            UserImage(BLACK)
            Text(
              text = state.blackPlayerLabel,
              textAlign = TextAlign.Center,
              color = MaterialTheme.colorScheme.onSurface,
              style = MaterialTheme.typography.headlineMedium,
              modifier = Modifier.width(84.dp)
            )
          }
          state.position?.let { ScoreSheet(it) } ?: Spacer(modifier = Modifier.weight(1f))
          Column {
            UserImage(WHITE, Modifier.padding(start = 4.dp))
            Text(
              text = state.whitePlayerLabel,
              textAlign = TextAlign.Center,
              color = MaterialTheme.colorScheme.onSurface,
              style = MaterialTheme.typography.headlineMedium,
              modifier = Modifier.width(84.dp)
            )
          }
        }
        Spacer(modifier = Modifier.weight(1f))
        BottomBar(
          buttons = state.buttons,
          bottomText = state.bottomText,
          onButtonPressed = { onUserAction(BottomButtonPressed(it as Button)) }
        )
      }
      Board(
        boardWidth = state.position?.boardWidth ?: 19,
        boardHeight = state.position?.boardHeight ?: 19,
        position = state.position,
        interactive = state.boardInteractive,
        drawTerritory = state.drawTerritory,
        drawLastMove = state.showLastMove,
        fadeOutRemovedStones = state.fadeOutRemovedStones,
        candidateMove = state.candidateMove,
        candidateMoveType = state.position?.nextToMove,
        onTapMove = { onUserAction(BoardCellDragged(it)) },
        onTapUp = { onUserAction(BoardCellTapUp(it)) },
        modifier = Modifier
          .shadow(1.dp, MaterialTheme.shapes.medium)
          .clip(MaterialTheme.shapes.medium)
      )
    }
  }

  if (state.koMoveDialogShowing) {
    AlertDialog(
      onDismissRequest = { onUserAction(KOMoveDialogDismiss) },
      confirmButton = {
        TextButton(onClick = { onUserAction(KOMoveDialogDismiss) }) {
          Text("OK")
        }
      },
      text = { Text("That move would repeat the board position. That's called a KO, and it is not allowed. Try to make another move first, preferably a threat that the opponent can't ignore.") },
      title = { Text("Illegal KO move", style = MaterialTheme.typography.titleLarge) },
    )
  }

  if (state.newGameDialogShowing) {
    NewGameDialog(
      onUserAction = onUserAction,
      newGameParameters = state.newGameParameters,
      setupMessage = state.setupMessage,
      discoveredHosts = state.discoveredHosts,
    )
  }
}

@Composable
private fun ScoreSheet(pos: Position) {
  CompositionLocalProvider(
    LocalContentColor provides MaterialTheme.colorScheme.onSurface,
    LocalTextStyle provides MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
  ) {
    val hasDeadStones =
      pos.blackDeadStones.isNotEmpty() || pos.whiteDeadStones.isNotEmpty()
    val hasTerritory =
      pos.blackTerritory.isNotEmpty() || pos.whiteTerritory.isNotEmpty()
    val whiteScore =
      (pos.komi ?: 0f) + pos.whiteTerritory.size + pos.whiteCaptureCount + pos.blackDeadStones.size
    val blackScore = pos.blackTerritory.size + pos.blackCaptureCount + pos.whiteDeadStones.size

    val maxWhite = max(whiteScore, pos.whiteCaptureCount.toFloat())
    val padding = when {
      maxWhite >= 100 -> 3
      maxWhite >= 10 -> 2
      else -> 1
    }
    Column(
      horizontalAlignment = Alignment.End,
    ) {
      Text(
        text = "0".padStart(padding + 2, ' '),
      )
      Text(text = pos.blackCaptureCount.toString())
      if (hasDeadStones) {
        Text(text = pos.whiteDeadStones.size.toString())
      }
      if (hasTerritory) {
        Text(text = pos.blackTerritory.size.toString())
        Text(text = blackScore.toString())
      }
    }
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.padding(horizontal = 8.dp)
    ) {
      Text(text = "komi")
      Text(text = "captures")
      if (hasDeadStones) {
        Text(text = "dead")
      }
      if (hasTerritory) {
        Text(text = "territory")
        Text(text = "total")
      }
    }
    Column {
      Text(text = (pos.komi ?: 0f).toString().padStart(padding + 2, ' '))
      Text(text = pos.whiteCaptureCount.toString().padStart(padding, ' '))
      if (hasDeadStones) {
        Text(text = pos.blackDeadStones.size.toString().padStart(padding, ' '))
      }
      if (hasTerritory) {
        Text(text = pos.whiteTerritory.size.toString().padStart(padding, ' '))
        Text(text = whiteScore.toString().padStart(padding, ' '))
      }
    }
  }
}

@Composable
private fun NewGameDialog(
  onUserAction: (Action) -> Unit,
  newGameParameters: GameParameters,
  setupMessage: String?,
  discoveredHosts: List<FaceToFaceLanDiscoveredHost>,
) {
  val emulatorMode = remember { isProbablyAndroidEmulator() }
  var hostAddressDraft by rememberSaveable(newGameParameters.mode, newGameParameters.hostAddress) {
    mutableStateOf(newGameParameters.hostAddress)
  }
  val joinError = newGameParameters.mode == MatchMode.WIFI_JOIN && setupMessage != null
  val joinSupportingText = when {
    joinError -> setupMessage
    emulatorMode -> "Android Emulator tip: use 10.0.2.2 for the guest address."
    else -> "Enter the host device's local network address."
  }
  val joinPlaceholder = if (emulatorMode) "10.0.2.2" else "192.168.1.42"
  val connectEnabled = when (newGameParameters.mode) {
    MatchMode.WIFI_JOIN -> hostAddressDraft.trim().isNotEmpty()
    else -> true
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0x88000000))
  )
  Dialog(onDismissRequest = { onUserAction(NewGameDialogDismiss) }) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier
        .background(
          color = MaterialTheme.colorScheme.surface,
          shape = RoundedCornerShape(10.dp)
        )
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Text(
        text = "New Game",
        modifier = Modifier
          .align(Alignment.CenterHorizontally)
          .padding(bottom = 16.dp),
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.headlineLarge,
      )
      SettingsRow(
        label = "Mode",
        options = MatchMode.entries,
        selected = newGameParameters.mode,
        onSelectionChanged = {
          onUserAction(Action.NewGameParametersChanged(newGameParameters.copy(mode = it)))
        }
      )
      if (newGameParameters.mode != MatchMode.WIFI_JOIN) {
        SettingsRow(
          label = "Size",
          options = BoardSize.entries,
          selected = newGameParameters.size,
          onSelectionChanged = {
            onUserAction(Action.NewGameParametersChanged(newGameParameters.copy(size = it)))
          }
        )
        SettingsRow(
          label = "Handicap",
          options = (0..9).toList(),
          selected = newGameParameters.handicap,
          onSelectionChanged = {
            onUserAction(Action.NewGameParametersChanged(newGameParameters.copy(handicap = it)))
          }
        )
      }
      if (newGameParameters.mode == MatchMode.WIFI_JOIN) {
        TextField(
          value = hostAddressDraft,
          onValueChange = {
            hostAddressDraft = it
          },
          label = { Text("Host address") },
          placeholder = { Text(joinPlaceholder) },
          supportingText = {
            Text(
              text = joinSupportingText,
              color = if (joinError) {
                MaterialTheme.colorScheme.error
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant
              }
            )
          },
          isError = joinError,
          keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Uri,
            imeAction = ImeAction.Done,
            autoCorrectEnabled = false,
          ),
          keyboardActions = KeyboardActions(
            onDone = {
              if (connectEnabled) {
                onUserAction(
                  Action.NewGameParametersChanged(newGameParameters.copy(hostAddress = hostAddressDraft))
                )
                onUserAction(StartNewGame)
              }
            }
          ),
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
        )
        if (discoveredHosts.isNotEmpty()) {
          Text(
            text = "Available hosts",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 16.dp)
          )
          discoveredHosts.forEach { host ->
            TextButton(
              onClick = {
                hostAddressDraft = host.endpoint
              },
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = "${host.displayName} (${host.endpoint})",
                modifier = Modifier.fillMaxWidth(),
              )
            }
          }
        }
      }
      if (setupMessage != null && newGameParameters.mode != MatchMode.WIFI_JOIN) {
        Text(
          text = setupMessage,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          style = MaterialTheme.typography.bodyMedium,
          textAlign = TextAlign.Center,
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
        )
      }
      Button(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 32.dp),
        enabled = connectEnabled,
        onClick = {
          if (newGameParameters.mode == MatchMode.WIFI_JOIN) {
            onUserAction(
              Action.NewGameParametersChanged(newGameParameters.copy(hostAddress = hostAddressDraft))
            )
          }
          onUserAction(StartNewGame)
        }
      ) {
        Text(
          text = when (newGameParameters.mode) {
            MatchMode.HOTSEAT -> "START NEW GAME"
            MatchMode.WIFI_HOST -> "START HOSTING"
            MatchMode.WIFI_JOIN -> "CONNECT"
          }
        )
      }
    }
  }
}

@Composable
private fun <T> SettingsRow(
  label: String,
  options: List<T>,
  selected: T,
  onSelectionChanged: (T) -> Unit
) {
  Row {
    Text(
      text = label,
      modifier = Modifier.align(CenterVertically),
      color = MaterialTheme.colorScheme.onSurface,
      style = MaterialTheme.typography.bodyMedium,
    )
    Spacer(modifier = Modifier.weight(1f))
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
      expanded = expanded,
      onExpandedChange = { expanded = it },
      modifier = Modifier.width(130.dp)
    ) {
      TextField(
        readOnly = true,
        value = selected.toString(),
        onValueChange = { },
        trailingIcon = {
          ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
        },
        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
          focusedTextColor = MaterialTheme.colorScheme.onSurface,
        ),
        modifier = Modifier.menuAnchor()
      )
      ExposedDropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
      ) {
        options.forEach { selectionOption ->
          DropdownMenuItem(
            onClick = {
              onSelectionChanged(selectionOption)
              expanded = false
            },
            text = {
              Text(text = selectionOption.toString())
            }
          )
        }
      }
    }
  }
}

@Composable
private fun UserImage(
  color: StoneType,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .size(84.dp)
      .aspectRatio(1f, true)
  ) {
    val shape = RoundedCornerShape(14.dp)
    Image(
      painter = painterResource(id = mipmap.placeholder),
      contentDescription = "Avatar",
      modifier = Modifier
        .size(84.dp)
        .fillMaxSize()
        .padding(bottom = 4.dp, end = 4.dp)
        .shadow(2.dp, shape)
        .clip(shape)
    )
    Box(
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(end = 4.dp)
    ) {
      val shield =
        if (color == BLACK) drawable.black_shield else drawable.white_shield
      Image(
        painter = painterResource(id = shield),
        contentDescription = null,
      )
    }
  }
}

@Preview
@Composable
fun Preview() {
  FaceToFaceContent(
    state = FaceToFaceState.INITIAL.copy(
      buttons = listOf(GameSettings, Estimate, Previous(false), Next(false))
    ),
    onUserAction = {},
    onBackPressed = {},
  )
}

@Preview
@Composable
fun PreviewKODialog() {
  FaceToFaceContent(
    state = FaceToFaceState.INITIAL.copy(
      koMoveDialogShowing = true,
      buttons = listOf(GameSettings, Estimate, Previous(false), Next(false))
    ),
    onUserAction = {},
    onBackPressed = {},
  )
}

@Preview
@Composable
fun PreviewNewGameDialog() {
  FaceToFaceContent(
    state = FaceToFaceState.INITIAL.copy(
      newGameDialogShowing = true,
      buttons = listOf(GameSettings, Estimate, Previous(false), Next(false))
    ),
    onUserAction = {},
    onBackPressed = {},
  )
}

@Preview(
  device = "spec:parent=pixel_5,orientation=landscape",
  showBackground = true,
  showSystemUi = true
)
@Composable
fun PreviewLandscape() {
  FaceToFaceContent(
    state = FaceToFaceState.INITIAL.copy(
      buttons = listOf(GameSettings, Estimate, Previous(false), Next(false))
    ),
    onUserAction = {},
    onBackPressed = {},
  )
}
