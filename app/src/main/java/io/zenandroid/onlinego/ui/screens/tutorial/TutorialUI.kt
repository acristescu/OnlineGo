@file:OptIn(ExperimentalMaterial3Api::class)

package io.zenandroid.onlinego.ui.screens.tutorial

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.data.model.local.Node
import io.zenandroid.onlinego.data.model.local.TutorialStep
import io.zenandroid.onlinego.ui.composables.Board
import io.zenandroid.onlinego.ui.screens.tutorial.TutorialAction.BoardCellHovered
import io.zenandroid.onlinego.ui.screens.tutorial.TutorialAction.BoardCellTapped
import io.zenandroid.onlinego.ui.screens.tutorial.TutorialAction.NextPressed
import io.zenandroid.onlinego.ui.screens.tutorial.TutorialAction.RetryPressed
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme
import org.koin.androidx.compose.koinViewModel


@Composable
fun TutorialScreen(
  viewModel: TutorialViewModel = koinViewModel(),
  onNavigateBack: () -> Unit,
) {
  val state by viewModel.state.collectAsState()

  TutorialContent(
    state = state,
    onNavigateBack = onNavigateBack,
    listener = viewModel::onAction
  )
}

@Composable
fun TutorialContent(
  state: TutorialState,
  listener: (TutorialAction) -> Unit,
  onNavigateBack: () -> Unit
) {
  Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
    // App bar
    TopAppBar(
      title = { Text(text = state.tutorial?.name ?: "") },
      navigationIcon = {
        IconButton(onClick = onNavigateBack) {
          Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
        }
      },
    )

    state.step?.let {
      when (LocalConfiguration.current.orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> {
          Row {
            Column(modifier = Modifier.weight(1f)) {
              Description(
                modifier = Modifier
                  .weight(1f)
                  .padding(start = 16.dp, end = 16.dp, top = 8.dp),
                state = state
              )
              ButtonBar(state, listener)
            }

            Board(state, listener)
          }
        }

        else -> {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .weight(1f)
              .padding(start = 16.dp, end = 16.dp, top = 8.dp),
          ) {
            Description(state = state)
          }

          Board(state, listener)
          ButtonBar(state, listener)
        }
      }
    }
  }
}

@ExperimentalComposeUiApi
@Composable
private fun Board(state: TutorialState, listener: (TutorialAction) -> Unit) {
  Board(
    modifier = Modifier
      .padding(12.dp)
      .shadow(6.dp, MaterialTheme.shapes.large),
    boardWidth = state.position?.boardWidth ?: 9,
    boardHeight = state.position?.boardHeight ?: 9,
    position = state.position,
    removedStones = state.removedStones,
    candidateMove = state.hoveredCell,
    candidateMoveType = StoneType.BLACK,
    onTapMove = { if (state.boardInteractive) listener(BoardCellHovered(it)) },
    onTapUp = { if (state.boardInteractive) listener(BoardCellTapped(it)) }
  )
}

@Composable
private fun Description(modifier: Modifier = Modifier, state: TutorialState) {
  Column(modifier = modifier.verticalScroll(rememberScrollState())) {
    Text(
      text = state.text ?: "",
      style = MaterialTheme.typography.bodySmall,
      fontSize = 18.sp,
      lineHeight = 21.sp,
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.fillMaxWidth()
    )
  }
}

@ExperimentalAnimationApi
@Composable
private fun ButtonBar(state: TutorialState, listener: (TutorialAction) -> Unit) {
  Box {
    Row(
      horizontalArrangement = Arrangement.SpaceAround,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 16.dp)
    ) {
      if (state.retryButtonVisible) {
        OutlinedButton(
          onClick = { listener.invoke(RetryPressed) },
          modifier = Modifier.weight(1f)
        ) {
          Icon(
            imageVector = Icons.Filled.Refresh,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(16.dp),
            contentDescription = null
          )
          Text(
            text = "RETRY",
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 8.dp)
          )
        }
      }
      if (state.nextButtonVisible) {
        Button(onClick = { listener.invoke(NextPressed) }, modifier = Modifier.weight(1f)) {
          Text(text = "NEXT")
        }
      }
    }
    Snackbar(
      visible = state.node?.success == true,
      text = "Nice one!",
      button = "NEXT",
      icon = R.drawable.ic_check_circle,
      modifier = Modifier.align(Alignment.Center),
      tint = MaterialTheme.colorScheme.secondary,
      listener = { listener.invoke(NextPressed) }
    )

    Snackbar(
      visible = state.node?.failed == true,
      text = state.node?.message ?: "That's not quite right!",
      button = "RETRY",
      icon = R.drawable.ic_x_circle,
      modifier = Modifier.align(Alignment.Center),
      tint = MaterialTheme.colorScheme.secondary,
      listener = { listener.invoke(RetryPressed) }
    )
  }
}

@ExperimentalAnimationApi
@Composable
private fun Snackbar(
  visible: Boolean,
  text: String,
  button: String,
  @DrawableRes icon: Int,
  tint: Color,
  modifier: Modifier = Modifier,
  listener: () -> Unit
) {
  AnimatedVisibility(
    visible = visible,
    enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
    exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
    modifier = modifier,
  ) {
    Surface(
      shadowElevation = 4.dp,
      border = BorderStroke(width = .5.dp, Color.LightGray),
      shape = MaterialTheme.shapes.medium,
      modifier = Modifier
        .clickable(onClick = {})
        .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
      Row(modifier = Modifier.fillMaxWidth()) {
        Image(
          painter = painterResource(icon),
          modifier = Modifier
            .align(Alignment.CenterVertically)
            .padding(start = 18.dp),
          contentDescription = null
        )
        Text(
          text = text,
          modifier = Modifier
            .weight(1f)
            .align(Alignment.CenterVertically)
            .padding(start = 24.dp)
        )
        TextButton(
          onClick = listener, modifier = Modifier
            .align(Alignment.CenterVertically)
            .padding(all = 4.dp)
        ) {
          Text(button, color = tint, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@ExperimentalComposeUiApi
@ExperimentalAnimationApi
@Preview
@Composable
private fun Preview() {
  OnlineGoTheme {
    TutorialContent(
      TutorialState(
        step = TutorialStep.Interactive(
          name = "Test Step",
          size = 9,
          init = "B[dd]W[ee]",
          text = "This is a test step for the tutorial.",
          branches = listOf(
            Node(move = "dd", reply = "ee", message = "Good move!", success = true)
          ),
        ),
      ), {}, {})
  }
}