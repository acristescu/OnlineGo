package io.zenandroid.onlinego.ui.screens.automatch

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.zenandroid.onlinego.data.model.ogs.Size
import io.zenandroid.onlinego.data.model.ogs.Speed
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewAutomatchChallengeBottomSheet(
  viewModel: NewAutomatchChallengeViewModel = koinViewModel(),
  onDismiss: () -> Unit,
  onAutomatchSearchClicked: (List<Speed>, List<Size>) -> Unit
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val sheetState = rememberModalBottomSheetState(true)

  ModalBottomSheet(
    sheetState = sheetState,
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
  ) {
    NewAutomatchChallengeBottomSheetContent(
      state = state,
      onSmallCheckChanged = { viewModel.onSmallCheckChanged(it) },
      onMediumCheckChanged = { viewModel.onMediumCheckChanged(it) },
      onLargeCheckChanged = { viewModel.onLargeCheckChanged(it) },
      onSpeedChanged = viewModel::onSpeedChanged,
      onSearchClicked = {
        onDismiss()
        val selectedSizes = mutableListOf<Size>()
        if (state.small) {
          selectedSizes.add(Size.SMALL)
        }
        if (state.medium) {
          selectedSizes.add(Size.MEDIUM)
        }
        if (state.large) {
          selectedSizes.add(Size.LARGE)
        }
        onAutomatchSearchClicked(state.speeds, selectedSizes)
      })
  }


}

@Composable
private fun NewAutomatchChallengeBottomSheetContent(
  state: AutomatchState,
  onSmallCheckChanged: (Boolean) -> Unit,
  onMediumCheckChanged: (Boolean) -> Unit,
  onLargeCheckChanged: (Boolean) -> Unit,
  onSpeedChanged: (Speed, Boolean) -> Unit,
  onSearchClicked: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
  ) {
    Column(
      modifier.padding(16.dp)
    ) {
      Text(
        text = "Auto-match",
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 4.dp)
      )
      Text(text = "Try your hand at a game against a human opponent of similar rating to you.")
      Text(
        text = "Game size", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp)
      )
      Row {
        SizeCheckbox(checked = state.small, text = "9×9", onClick = onSmallCheckChanged)
        Spacer(modifier = Modifier.weight(1f))
        SizeCheckbox(checked = state.medium, text = "13×13", onClick = onMediumCheckChanged)
        Spacer(modifier = Modifier.weight(1f))
        SizeCheckbox(checked = state.large, text = "19×19", onClick = onLargeCheckChanged)
      }
      Text(
        text = "Time Controls",
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 16.dp)
      )
      Row {
        SizeCheckbox(
          checked = state.speeds.contains(Speed.BLITZ),
          text = "Blitz",
          onClick = { onSpeedChanged(Speed.BLITZ, it) })
        Spacer(modifier = Modifier.weight(1f))
        SizeCheckbox(
          checked = state.speeds.contains(Speed.RAPID),
          text = "Rapid",
          onClick = { onSpeedChanged(Speed.RAPID, it) })
        Spacer(modifier = Modifier.weight(1f))
        SizeCheckbox(
          checked = state.speeds.contains(Speed.LIVE),
          text = "Live",
          onClick = { onSpeedChanged(Speed.LIVE, it) })
      }
      Text(
        text = "or",
        fontStyle = FontStyle.Italic,
        modifier = Modifier
          .padding(top = 4.dp)
          .align(Alignment.CenterHorizontally)
      )
      Row {
        SizeCheckbox(
          checked = state.speeds.contains(Speed.LONG),
          text = "Correspondence",
          onClick = { onSpeedChanged(Speed.LONG, it) })
      }
      Text(
        text = "Expected duration: ${state.duration}",
        fontStyle = FontStyle.Italic,
        modifier = Modifier
          .padding(top = 16.dp)
          .align(Alignment.CenterHorizontally)
      )
      Button(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 16.dp),
        enabled = state.isAnySizeSelected && state.speeds.isNotEmpty(),
        onClick = onSearchClicked
      ) {
        Text("Search")
      }
    }
  }
}

@Composable
private fun RowScope.SizeCheckbox(checked: Boolean, text: String, onClick: (Boolean) -> Unit) {
  Checkbox(
    checked = checked, colors = CheckboxDefaults.colors(
      checkedColor = MaterialTheme.colorScheme.primary
    ), onCheckedChange = onClick
  )
  Text(
    text = text, modifier = Modifier
      .align(Alignment.CenterVertically)
      .clickable(
        interactionSource = remember { MutableInteractionSource() }, indication = null
      ) { onClick(!checked) })
}

@Preview(showBackground = true)
@Composable
private fun NewAutomatchChallengeBottomSheetPreview() {
  OnlineGoTheme {
    Box(modifier = Modifier.fillMaxSize())
    NewAutomatchChallengeBottomSheetContent(AutomatchState(), {}, {}, {}, { _, _ -> }, {})
  }
}