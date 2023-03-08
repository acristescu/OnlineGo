package io.zenandroid.onlinego.ui.screens.face2face

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextAlign.Companion
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest.Builder
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.R.mipmap
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
import io.zenandroid.onlinego.ui.screens.face2face.Button.Estimate
import io.zenandroid.onlinego.ui.screens.face2face.Button.GameSettings
import io.zenandroid.onlinego.ui.screens.face2face.Button.Next
import io.zenandroid.onlinego.ui.screens.face2face.Button.Previous
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme
import io.zenandroid.onlinego.utils.rememberStateWithLifecycle
import io.zenandroid.onlinego.utils.repeatingClickable
import org.koin.androidx.viewmodel.ext.android.viewModel


class FaceToFaceFragment: Fragment() {

  private val viewModel: FaceToFaceViewModel by viewModel()
  private val analytics by lazy { OnlineGoApplication.instance.analytics }

  override fun onResume() {
    super.onResume()
    analytics.setCurrentScreen(requireActivity(), javaClass.simpleName, null)
  }

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
        title = state.title,
        titleIcon = null,
        onTitleClicked = null,
        onBack = onBackPressed,
        moreMenuItems = emptyList(),
      )

      Row(modifier = Modifier
        .padding(horizontal = 16.dp)
        .padding(top = 16.dp, bottom = 4.dp)
        .fillMaxWidth()
      ) {
        UserImage(BLACK)
        Spacer(modifier = Modifier.weight(1f))
        UserImage(WHITE)
      }
      Row(modifier = Modifier
        .padding(horizontal = 16.dp)
        .padding(bottom = 16.dp)
      ) {
        Text(
          text = "Player 1",
          textAlign = TextAlign.Center,
          color = MaterialTheme.colors.onSurface,
          style = MaterialTheme.typography.h3,
          modifier = Modifier.width(84.dp)
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
          text = "Player 2",
          textAlign = TextAlign.Center,
          color = MaterialTheme.colors.onSurface,
          style = MaterialTheme.typography.h3,
          modifier = Modifier.width(84.dp)
        )
      }
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
      BottomBar(
        buttons = state.buttons,
        bottomText = state.bottomText,
        onButtonPressed = { onUserAction(BottomButtonPressed(it as Button)) }
      )
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
        title = { Text("Illegal KO move") },
      )
    }
  }
}

@Composable
private fun UserImage(color: StoneType) {
  Box(modifier = Modifier
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
    Box(modifier = Modifier
      .align(Alignment.BottomEnd)
      .padding(end = 4.dp)
    ) {
      val shield =
        if (color == StoneType.BLACK) R.drawable.black_shield else R.drawable.white_shield
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
  FaceToFaceScreen(
    state = FaceToFaceState.INITIAL.copy(
      buttons = listOf(GameSettings, Estimate, Previous(false), Next(false) )
    ),
    onUserAction = {},
    onBackPressed = {},
  )
}