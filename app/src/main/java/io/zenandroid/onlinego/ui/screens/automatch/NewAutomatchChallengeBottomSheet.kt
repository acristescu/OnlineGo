package io.zenandroid.onlinego.ui.screens.automatch

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.zenandroid.onlinego.data.model.ogs.Size
import io.zenandroid.onlinego.data.model.ogs.Speed
import io.zenandroid.onlinego.ui.screens.main.MainActivity
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme
import io.zenandroid.onlinego.utils.rememberStateWithLifecycle
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Locale

class NewAutomatchChallengeBottomSheet : BottomSheetDialogFragment() {
  private val viewModel: NewAutomatchChallengeViewModel by viewModel()

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val dialog = super.onCreateDialog(savedInstanceState)

    dialog.setOnShowListener {
      BottomSheetBehavior.from(dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet)!!)
        .apply {
          state = BottomSheetBehavior.STATE_EXPANDED
          skipCollapsed = true
        }
    }

    return dialog
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    return ComposeView(requireContext()).apply {
      setContent {
        val state by rememberStateWithLifecycle(viewModel.state)
        OnlineGoTheme {
          NewAutomatchChallengeBottomSheetContent(
            state = state,
            onSmallCheckChanged = { viewModel.onSmallCheckChanged(it) },
            onMediumCheckChanged = { viewModel.onMediumCheckChanged(it) },
            onLargeCheckChanged = { viewModel.onLargeCheckChanged(it) },
            onSpeedChanged = viewModel::onSpeedChanged,
            onSearchClicked = {
              dismiss()
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
              (activity as? MainActivity)?.onAutomatchSearchClicked(state.speeds, selectedSizes)
            }
          )
        }
      }
    }
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
  Surface {
    Column(
      modifier
        .padding(16.dp)
    ) {
      Text(
        text = "Auto-match",
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 4.dp)
      )
      Text(text = "Try your hand at a game against a human opponent of similar rating to you.")
      Text(
        text = "Game size",
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 16.dp)
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
        SizeCheckbox(checked = state.speeds.contains(Speed.BLITZ), text = "Blitz", onClick = {onSpeedChanged(Speed.BLITZ, it)})
        Spacer(modifier = Modifier.weight(1f))
        SizeCheckbox(checked = state.speeds.contains(Speed.RAPID), text = "Rapid", onClick = {onSpeedChanged(Speed.RAPID, it)})
        Spacer(modifier = Modifier.weight(1f))
        SizeCheckbox(checked = state.speeds.contains(Speed.LIVE), text = "Live", onClick = {onSpeedChanged(Speed.LIVE, it)})
      }
      Text(
        text = "or",
        fontStyle = FontStyle.Italic,
        modifier = Modifier
          .padding(top = 4.dp)
          .align(Alignment.CenterHorizontally)
      )
      Row {
        SizeCheckbox(checked = state.speeds.contains(Speed.LONG), text = "Correspondence", onClick = {onSpeedChanged(Speed.LONG, it)})
      }
      Text(
        text = "Expected duration: ${state.duration}",
        fontStyle = FontStyle.Italic,
        modifier = Modifier.padding(top = 16.dp).align(Alignment.CenterHorizontally)
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
    checked = checked,
    colors = CheckboxDefaults.colors(
      checkedColor = MaterialTheme.colors.primary
    ),
    onCheckedChange = onClick
  )
  Text(
    text = text,
    modifier = Modifier
      .align(Alignment.CenterVertically)
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null
      ) { onClick(!checked) }
  )
}

@Preview(showBackground = true)
@Composable
private fun NewAutomatchChallengeBottomSheetPreview() {
  OnlineGoTheme {
    Box(modifier = Modifier.fillMaxSize())
    NewAutomatchChallengeBottomSheetContent(AutomatchState(), {}, {}, {}, {_,_ -> }, {})
  }
}