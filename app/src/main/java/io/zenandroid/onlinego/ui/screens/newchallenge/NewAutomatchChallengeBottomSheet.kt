package io.zenandroid.onlinego.ui.screens.newchallenge

import android.app.AlertDialog
import android.content.Context
import android.preference.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.view.LayoutInflater
import com.google.android.material.bottomsheet.BottomSheetBehavior
import io.zenandroid.onlinego.data.model.ogs.Size
import io.zenandroid.onlinego.data.model.ogs.Speed
import io.zenandroid.onlinego.databinding.BottomSheetNewAutomatchBinding

class NewAutomatchChallengeBottomSheet(context: Context, private val onSearch: (Speed, List<Size>) -> Unit) : BottomSheetDialog(context) {
    companion object {
        private const val SEARCH_GAME_SMALL = "SEARCH_GAME_SMALL"
        private const val SEARCH_GAME_MEDIUM = "SEARCH_GAME_MEDIUM"
        private const val SEARCH_GAME_LARGE = "SEARCH_GAME_LARGE"
        private const val SEARCH_GAME_SPEED = "SEARCH_GAME_SPEED"
    }

    private var selectedSpeed: Speed = Speed.NORMAL
    private val speedsArray = arrayOf(Speed.BLITZ, Speed.NORMAL, Speed.LONG)
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    private lateinit var binding: BottomSheetNewAutomatchBinding

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        binding = BottomSheetNewAutomatchBinding.inflate(inflater)

        setContentView(binding.root)

        setInitialState()

        binding.searchButton.setOnClickListener {
            dismiss()
            val selectedSizes = mutableListOf<Size>()
            if(binding.smallGameCheckbox.isChecked) {
                selectedSizes.add(Size.SMALL)
            }
            if(binding.mediumGameCheckbox.isChecked) {
                selectedSizes.add(Size.MEDIUM)
            }
            if(binding.largeGameCheckbox.isChecked) {
                selectedSizes.add(Size.LARGE)
            }
            onSearch.invoke(selectedSpeed, selectedSizes)
        }

        arrayOf(binding.smallGameCheckbox, binding.mediumGameCheckbox, binding.largeGameCheckbox).forEach {
            it.setOnCheckedChangeListener { _, _ ->
                onSizeChanged()
            }
        }

        val stringsArray = arrayOfNulls<CharSequence?>(speedsArray.size)
        speedsArray.forEachIndexed { index, speed ->
            stringsArray[index] = speed.getText().capitalize()
        }

        binding.speedRow.setOnClickListener {
            AlertDialog.Builder(context).setTitle("Choose speed")
                    .setItems(stringsArray) { _, which ->
                        selectedSpeed = speedsArray[which]
                        binding.speedTextView.text = selectedSpeed.getText().capitalize()
                        onSpeedChanged()
                    }
                    .setCancelable(true)
                    .create()
                    .show()
        }
        setCanceledOnTouchOutside(true)
        setCancelable(true)
        setOnShowListener {
            BottomSheetBehavior.from(findViewById(com.google.android.material.R.id.design_bottom_sheet)!!).apply {
                state = BottomSheetBehavior.STATE_EXPANDED
                skipCollapsed = true
            }
        }
    }

    private fun setInitialState() {
        binding.smallGameCheckbox.isChecked = prefs.getBoolean(SEARCH_GAME_SMALL, true)
        binding.mediumGameCheckbox.isChecked = prefs.getBoolean(SEARCH_GAME_MEDIUM, false)
        binding.largeGameCheckbox.isChecked = prefs.getBoolean(SEARCH_GAME_LARGE, false)
        selectedSpeed = Speed.valueOf(prefs.getString(SEARCH_GAME_SPEED, null) ?: Speed.NORMAL.toString())
        binding.speedTextView.text = selectedSpeed.getText().capitalize()
    }

    private fun onSizeChanged() {
        binding.searchButton.isEnabled = binding.smallGameCheckbox.isChecked || binding.mediumGameCheckbox.isChecked || binding.largeGameCheckbox.isChecked
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