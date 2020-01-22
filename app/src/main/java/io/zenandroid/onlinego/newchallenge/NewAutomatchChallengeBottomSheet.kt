package io.zenandroid.onlinego.newchallenge

import android.app.AlertDialog
import android.content.Context
import android.preference.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.view.LayoutInflater
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.model.ogs.Size
import io.zenandroid.onlinego.model.ogs.Speed
import kotlinx.android.synthetic.main.bottom_sheet_new_automatch.*

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

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.bottom_sheet_new_automatch, null)
        setContentView(view)

        setInitialState()

        searchButton.setOnClickListener {
            dismiss()
            val selectedSizes = mutableListOf<Size>()
            if(smallGameCheckbox.isChecked) {
                selectedSizes.add(Size.SMALL)
            }
            if(mediumGameCheckbox.isChecked) {
                selectedSizes.add(Size.MEDIUM)
            }
            if(largeGameCheckbox.isChecked) {
                selectedSizes.add(Size.LARGE)
            }
            onSearch.invoke(selectedSpeed, selectedSizes)
        }

        arrayOf(smallGameCheckbox, mediumGameCheckbox, largeGameCheckbox).forEach {
            it.setOnCheckedChangeListener { _, _ ->
                onSizeChanged()
            }
        }

        val stringsArray = arrayOfNulls<CharSequence?>(speedsArray.size)
        speedsArray.forEachIndexed { index, speed ->
            stringsArray[index] = speed.getText().capitalize()
        }

        speedRow.setOnClickListener {
            AlertDialog.Builder(context).setTitle("Choose speed")
                    .setItems(stringsArray) { _, which ->
                        selectedSpeed = speedsArray[which]
                        speedTextView.text = selectedSpeed.getText().capitalize()
                        onSpeedChanged()
                    }
                    .setCancelable(true)
                    .create()
                    .show()
        }
        setCanceledOnTouchOutside(true)
        setCancelable(true)
    }

    private fun setInitialState() {
        smallGameCheckbox.isChecked = prefs.getBoolean(SEARCH_GAME_SMALL, true)
        mediumGameCheckbox.isChecked = prefs.getBoolean(SEARCH_GAME_MEDIUM, false)
        largeGameCheckbox.isChecked = prefs.getBoolean(SEARCH_GAME_LARGE, false)
        selectedSpeed = Speed.valueOf(prefs.getString(SEARCH_GAME_SPEED, null) ?: Speed.NORMAL.toString())
        speedTextView.text = selectedSpeed.getText().capitalize()
    }

    private fun onSizeChanged() {
        searchButton.isEnabled = smallGameCheckbox.isChecked || mediumGameCheckbox.isChecked || largeGameCheckbox.isChecked
        saveSettings()
    }

    private fun onSpeedChanged() {
        saveSettings()
    }

    private fun saveSettings() {
        prefs.edit()
                .putBoolean(SEARCH_GAME_SMALL, smallGameCheckbox.isChecked)
                .putBoolean(SEARCH_GAME_MEDIUM, mediumGameCheckbox.isChecked)
                .putBoolean(SEARCH_GAME_LARGE, largeGameCheckbox.isChecked)
                .putString(SEARCH_GAME_SPEED, selectedSpeed.toString())
                .apply()
    }

}