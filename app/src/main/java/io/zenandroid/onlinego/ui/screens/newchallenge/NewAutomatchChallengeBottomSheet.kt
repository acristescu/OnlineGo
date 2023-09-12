package io.zenandroid.onlinego.ui.screens.newchallenge

import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.data.model.ogs.Speed
import io.zenandroid.onlinego.databinding.BottomSheetNewAutomatchBinding
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme

class NewAutomatchChallengeBottomSheet : BottomSheetDialogFragment() {
  companion object {
    private const val SEARCH_GAME_SMALL = "SEARCH_GAME_SMALL"
    private const val SEARCH_GAME_MEDIUM = "SEARCH_GAME_MEDIUM"
    private const val SEARCH_GAME_LARGE = "SEARCH_GAME_LARGE"
    private const val SEARCH_GAME_SPEED = "SEARCH_GAME_SPEED"
  }

  private var selectedSpeed: Speed = Speed.NORMAL
  private val speedsArray = arrayOf(Speed.BLITZ, Speed.NORMAL, Speed.LONG)
  val prefs = PreferenceManager.getDefaultSharedPreferences(OnlineGoApplication.instance)
  private lateinit var binding: BottomSheetNewAutomatchBinding

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    return ComposeView(requireContext()).apply {
      setContent {
        SideEffect {
          setInitialState()
        }
        OnlineGoTheme {
          NewAutomatchChallengeBottomSheetContent()
        }
      }
    }
  }

  init {
//        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
//        binding = BottomSheetNewAutomatchBinding.inflate(inflater)
//
//        setContentView(binding.root)
//
//        setInitialState()
//
//        binding.searchButton.setOnClickListener {
//            dismiss()
//            val selectedSizes = mutableListOf<Size>()
//            if(binding.smallGameCheckbox.isChecked) {
//                selectedSizes.add(Size.SMALL)
//            }
//            if(binding.mediumGameCheckbox.isChecked) {
//                selectedSizes.add(Size.MEDIUM)
//            }
//            if(binding.largeGameCheckbox.isChecked) {
//                selectedSizes.add(Size.LARGE)
//            }
//            onSearch.invoke(selectedSpeed, selectedSizes)
//        }
//
//        arrayOf(binding.smallGameCheckbox, binding.mediumGameCheckbox, binding.largeGameCheckbox).forEach {
//            it.setOnCheckedChangeListener { _, _ ->
//                onSizeChanged()
//            }
//        }
//
//        val stringsArray = arrayOfNulls<CharSequence?>(speedsArray.size)
//        speedsArray.forEachIndexed { index, speed ->
//            stringsArray[index] = speed.getText().capitalize()
//        }
//
//        binding.speedRow.setOnClickListener {
//            AlertDialog.Builder(context).setTitle("Choose speed")
//                    .setItems(stringsArray) { _, which ->
//                        selectedSpeed = speedsArray[which]
//                        binding.speedTextView.text = selectedSpeed.getText().capitalize()
//                        onSpeedChanged()
//                    }
//                    .setCancelable(true)
//                    .create()
//                    .show()
//        }
//        setCanceledOnTouchOutside(true)
//        setCancelable(true)
//        setOnShowListener {
//            BottomSheetBehavior.from(findViewById(com.google.android.material.R.id.design_bottom_sheet)!!).apply {
//                state = BottomSheetBehavior.STATE_EXPANDED
//                skipCollapsed = true
//            }
//        }
  }

  private fun setInitialState() {
//        binding.smallGameCheckbox.isChecked = prefs.getBoolean(SEARCH_GAME_SMALL, true)
//        binding.mediumGameCheckbox.isChecked = prefs.getBoolean(SEARCH_GAME_MEDIUM, false)
//        binding.largeGameCheckbox.isChecked = prefs.getBoolean(SEARCH_GAME_LARGE, false)
//        selectedSpeed = Speed.valueOf(prefs.getString(SEARCH_GAME_SPEED, null) ?: Speed.NORMAL.toString())
//        binding.speedTextView.text = selectedSpeed.getText().capitalize()

  }

  private fun onSizeChanged() {
    binding.searchButton.isEnabled =
      binding.smallGameCheckbox.isChecked || binding.mediumGameCheckbox.isChecked || binding.largeGameCheckbox.isChecked
    saveSettings()
  }

  private fun onSpeedChanged() {
    saveSettings()
  }

  private fun saveSettings() {
    prefs.edit()
      .putBoolean(SEARCH_GAME_SMALL, binding.smallGameCheckbox.isChecked)
      .putBoolean(SEARCH_GAME_MEDIUM, binding.mediumGameCheckbox.isChecked)
      .putBoolean(SEARCH_GAME_LARGE, binding.largeGameCheckbox.isChecked)
      .putString(SEARCH_GAME_SPEED, selectedSpeed.toString())
      .apply()
  }
}

@Composable
private fun NewAutomatchChallengeBottomSheetContent() {
  Column(Modifier.padding(16.dp)) {
    Text(text = "Try your hand at a game against a random opponent of similar rating to you.")
    Text(
      text = "Game size",
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(top = 16.dp)
    )
    Row {
      Checkbox(checked = true, onCheckedChange = { /*TODO*/ })
      Text(
        text = "9x9",
        modifier = Modifier.align(Alignment.CenterVertically)
      )
      Spacer(modifier = Modifier.weight(1f))
      Checkbox(checked = true, onCheckedChange = { /*TODO*/ })
      Text(
        text = "13x13",
        modifier = Modifier.align(Alignment.CenterVertically)
      )
      Spacer(modifier = Modifier.weight(1f))
      Checkbox(checked = true, onCheckedChange = { /*TODO*/ })
      Text(
        text = "19x19",
        modifier = Modifier.align(Alignment.CenterVertically)
      )
    }
    Text(
      text = "Time Controls",
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(top = 16.dp)
    )
    Box {
      var expanded by remember { mutableStateOf(false) }
      Text(
        text = "Blitz",
        color = MaterialTheme.colors.primary,
        modifier = Modifier.clickable {
          expanded = true
        }
      )
      DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        modifier = Modifier.fillMaxWidth()
      ) {
        DropdownMenuItem(onClick = {
          expanded = false
        }) {
          Text(text = "Blitz")
        }
        DropdownMenuItem(onClick = {
          expanded = false
        }) {
          Text(text = "Live")
        }
        DropdownMenuItem(onClick = {
          expanded = false
        }) {
          Text(text = "Correspondence")
        }
      }
    }
    Button(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 16.dp),
      onClick = { /*TODO*/ }
    ) {
      Text("Search")
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun NewAutomatchChallengeBottomSheetPreview() {
  OnlineGoTheme {
    Box(modifier = Modifier.fillMaxSize())
    NewAutomatchChallengeBottomSheetContent()
  }
}