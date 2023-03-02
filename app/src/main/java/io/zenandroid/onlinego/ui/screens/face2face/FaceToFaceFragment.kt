package io.zenandroid.onlinego.ui.screens.face2face

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import io.zenandroid.onlinego.ui.composables.Board
import io.zenandroid.onlinego.ui.screens.face2face.Action.BoardCellDragged
import io.zenandroid.onlinego.ui.screens.face2face.Action.BoardCellTapUp
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme
import io.zenandroid.onlinego.utils.rememberStateWithLifecycle
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

        FaceToFaceScreen(state, viewModel::onAction)
      }
    }
  }
}

@Composable
fun FaceToFaceScreen(state : FaceToFaceState, onUserAction: (Action) -> Unit) {
  OnlineGoTheme {
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
      modifier = Modifier.shadow(1.dp, MaterialTheme.shapes.medium)
        .clip(MaterialTheme.shapes.medium)
    )
  }
}