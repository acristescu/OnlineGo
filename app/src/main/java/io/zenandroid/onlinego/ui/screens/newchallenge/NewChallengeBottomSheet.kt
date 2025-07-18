@file:OptIn(ExperimentalMaterial3Api::class)

package io.zenandroid.onlinego.ui.screens.newchallenge

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.zenandroid.onlinego.data.model.ogs.ChallengeParams
import io.zenandroid.onlinego.utils.PreviewBackground
import io.zenandroid.onlinego.utils.rememberStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@Composable
fun NewChallengeBottomSheet(
  viewModel: NewChallengeViewModel = koinViewModel(),
  onDismiss: () -> Unit,
  onNewChallengeSearchClicked: (ChallengeParams) -> Unit
) {
  val state by rememberStateWithLifecycle(viewModel.state)

  if (state.done) {
    SideEffect {
      viewModel.onEvent(NewChallengeViewModel.Event.Reset)
      onNewChallengeSearchClicked(state.challenge)
    }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
  ) {
    NewChallengeBottomSheetContent(
      state = state,
      onEvent = {
        viewModel.onEvent(it)
      }
    )
    if (state.selectOpponentDialogShowing) {
      SelectOpponentDialog(
        onDialogDismiss = {
          viewModel.onEvent(
            NewChallengeViewModel.Event.SelectOpponentDialogDismissed(it)
          )
        }
      )
    }
  }
}

@Composable
private fun NewChallengeBottomSheetContent(
  state: NewChallengeBottomSheetState,
  onEvent: (NewChallengeViewModel.Event) -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
  ) {
    Column(
      modifier = modifier
        .padding(16.dp)
    ) {
      Row {
        Spacer(modifier = Modifier.weight(.3f))
        Column(modifier = Modifier.weight(1f)) {
          NameValuePair(
            name = "Opponent",
            value = state.opponentText,
            onClick = { onEvent(NewChallengeViewModel.Event.OpponentClicked) }
          )
          NameValuePair(
            name = "Your color",
            value = state.challenge.color,
            possibleValues = listOf("Auto", "Black", "White"),
            onValueClick = { onEvent(NewChallengeViewModel.Event.ColorSelected(it)) },
            modifier = Modifier.padding(top = 12.dp)
          )
          NameValuePair(
            name = "Board Size",
            value = state.challenge.size,
            possibleValues = listOf("9×9", "13×13", "19×19"),
            onValueClick = { onEvent(NewChallengeViewModel.Event.SizeSelected(it)) },
            modifier = Modifier.padding(top = 12.dp)
          )
          NameValuePair(
            name = "Analysis",
            value = if (state.challenge.disable_analysis) "Disabled" else "Enabled",
            possibleValues = listOf("Enabled", "Disabled"),
            onValueClick = { onEvent(NewChallengeViewModel.Event.DisableAnalysisSelected(it == "Disabled")) },
            modifier = Modifier.padding(top = 12.dp)
          )
        }
        Spacer(modifier = Modifier.weight(.3f))
        Column(modifier = Modifier.weight(1f)) {
          NameValuePair(
            name = "Handicap",
            value = state.challenge.handicap,
            onValueClick = { onEvent(NewChallengeViewModel.Event.HandicapSelected(it)) },
            possibleValues = listOf("Auto", "0", "1", "2", "3", "4", "5")
          )
          NameValuePair(
            name = "Speed",
            value = state.challenge.speed,
            possibleValues = listOf("Blitz", "Live", "Correspondence"),
            onValueClick = { onEvent(NewChallengeViewModel.Event.SpeedSelected(it)) },
            modifier = Modifier.padding(top = 12.dp)
          )
          NameValuePair(
            name = "Ranked",
            value = if (state.challenge.ranked) "Yes" else "No",
            possibleValues = listOf("Yes", "No"),
            onValueClick = { onEvent(NewChallengeViewModel.Event.RankedSelected(it == "Yes")) },
            modifier = Modifier.padding(top = 12.dp)
          )
          NameValuePair(
            name = "Private",
            value = if (state.challenge.private) "Yes" else "No",
            possibleValues = listOf("Yes", "No"),
            onValueClick = { onEvent(NewChallengeViewModel.Event.PrivateSelected(it == "Yes")) },
            modifier = Modifier.padding(top = 12.dp)
          )
        }
        Spacer(modifier = Modifier.weight(.3f))
      }
      Button(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 16.dp),
        enabled = state.challenge.opponent != null,
        onClick = { onEvent(NewChallengeViewModel.Event.ChallengeClicked) }
      ) {
        Text("Send Challenge")
      }
    }
  }
}

@Composable
private fun NameValuePair(
  name: String,
  value: String,
  modifier: Modifier = Modifier,
  possibleValues: List<String> = emptyList(),
  onValueClick: (String) -> Unit = {},
  onClick: () -> Unit = {}
) {
  var menuOpen by remember { mutableStateOf(false) }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .clickable {
        if (possibleValues.isNotEmpty()) {
          menuOpen = true
        } else {
          onClick()
        }
      }
  ) {
    Text(
      text = name,
      fontSize = 14.sp,
      modifier = Modifier.padding(bottom = 4.dp)
    )
    Box {
      Text(
        text = value,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.primary
      )
      if (possibleValues.isNotEmpty()) {
        DropdownMenu(
          expanded = menuOpen,
          onDismissRequest = { menuOpen = false },
        ) {
          possibleValues.forEach {
            key(it) {
              DropdownMenuItem(
                onClick = {
                  menuOpen = false
                  onValueClick(it)
                }, text = {
                  Text(text = it)
                }
              )
            }
          }
        }
      }
    }
  }
}

@Preview
@Composable
private fun Preview() {
  PreviewBackground {
    NewChallengeBottomSheetContent(
      NewChallengeBottomSheetState(
        challenge = ChallengeParams(
          opponent = null,
          color = "Auto",
          ranked = true,
          handicap = "0",
          size = "9×9",
          speed = "Live",
          disable_analysis = false,
          private = false
        ),
        opponentText = "[Select opponent]",
        done = false
      ),
      onEvent = {}
    )
  }
}