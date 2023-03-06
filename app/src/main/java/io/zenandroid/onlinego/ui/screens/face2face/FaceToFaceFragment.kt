package io.zenandroid.onlinego.ui.screens.face2face

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.AlertDialog
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Functions
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import io.zenandroid.onlinego.ui.composables.Board
import io.zenandroid.onlinego.ui.composables.TitleBar
import io.zenandroid.onlinego.ui.screens.face2face.Action.BoardCellDragged
import io.zenandroid.onlinego.ui.screens.face2face.Action.BoardCellTapUp
import io.zenandroid.onlinego.ui.screens.face2face.Action.KOMoveDialogDismiss
import io.zenandroid.onlinego.ui.screens.face2face.Action.NextButtonPressed
import io.zenandroid.onlinego.ui.screens.face2face.Action.PreviousButtonPressed
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme
import io.zenandroid.onlinego.utils.rememberStateWithLifecycle
import io.zenandroid.onlinego.utils.repeatingClickable
import org.koin.androidx.viewmodel.ext.android.viewModel


class FaceToFaceFragment: Fragment() {

  private val viewModel: FaceToFaceViewModel by viewModel()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    return ComposeView(requireContext()).apply {
      setContent {
        val state by rememberStateWithLifecycle(viewModel.state)

        FaceToFaceScreen(
          state = state,
          onUserAction = viewModel::onAction,
          onBackPressed = { requireActivity().onBackPressedDispatcher.onBackPressed() }
        )
      }
    }
  }
}

@Composable
fun FaceToFaceScreen(
  state : FaceToFaceState,
  onUserAction: (Action) -> Unit,
  onBackPressed: () -> Unit,
) {
  OnlineGoTheme {
    Column(
      Modifier
        .background(MaterialTheme.colors.surface)
        .fillMaxSize()
    ) {
      TitleBar(
        title = "Face to Face",
        titleIcon = null,
        onTitleClicked = null,
        onBack = onBackPressed,
        moreMenuItems = emptyList(),
    )
      Board(
        boardWidth = state.gameWidth,
        boardHeight = state.gameHeight,
        position = state.position,
        interactive = state.boardInteractive,
        boardTheme = state.boardTheme,
        drawCoordinates = state.showCoordinates,
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
      Spacer(modifier = Modifier.weight(1f))
      Row(modifier = Modifier.height(56.dp)) {
        BottomBarButton(label = "Game settings", icon = Icons.Rounded.Tune, enabled = true, repeatable = false, modifier = Modifier.weight(1f),) {}
        BottomBarButton(label = "Auto-score", icon = Icons.Rounded.Functions, enabled = true, repeatable = false, modifier = Modifier.weight(1f),) {}
        BottomBarButton(label = "Undo", icon = Icons.Rounded.SkipPrevious, enabled = state.previousButtonEnabled, repeatable = true, modifier = Modifier.weight(1f),) { onUserAction(PreviousButtonPressed) }
        BottomBarButton(label = "Redo", icon = Icons.Rounded.SkipNext, enabled = state.nextButtonEnabled, repeatable = true, modifier = Modifier.weight(1f),) { onUserAction(NextButtonPressed) }
      }
    }

    if(state.koMoveDialogShowing) {
      AlertDialog(
        onDismissRequest = { onUserAction(KOMoveDialogDismiss) },
        confirmButton = {
          TextButton(onClick = { onUserAction(KOMoveDialogDismiss) }) {
            Text("OK")
          }
        },
        text = { Text("That move would repeat the board position. That's called a KO, and it is not allowed. Try to make another move first, preferably a threat that the opponent can't ignore.") },
        title = { Text("Illegal KO move") },
      )
    }

  }
}

@Composable
fun BottomBarButton(
  label: String,
  icon: ImageVector,
  enabled: Boolean,
  repeatable: Boolean,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  Box(
    modifier = modifier
      .fillMaxHeight()
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .alpha(if (enabled) 1f else .4f)
        .clickable(enabled = enabled) {
          if (!repeatable) onClick()
        }
        .repeatingClickable(
          remember { MutableInteractionSource() },
          repeatable && enabled
        ) { onClick() },
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Icon(
        icon,
        null,
        modifier = Modifier.size(24.dp),
        tint = MaterialTheme.colors.onSurface,
      )
      Text(
        text = label,
        style = MaterialTheme.typography.h5,
        color = MaterialTheme.colors.onSurface,
      )
    }
  }
}

@Preview
@Composable
fun Preview() {
  FaceToFaceScreen(
    state = FaceToFaceState.INITIAL,
    onUserAction = {},
    onBackPressed = {},
  )
}