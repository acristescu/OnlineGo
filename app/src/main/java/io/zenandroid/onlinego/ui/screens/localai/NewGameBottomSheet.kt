package io.zenandroid.onlinego.ui.screens.localai

import android.content.Context
import android.preference.PreferenceManager
import android.view.LayoutInflater
import io.zenandroid.onlinego.R
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.android.synthetic.main.bottom_sheet_new_ai_game.*

private const val SIZE = "AI_GAME_SIZE"
private const val COLOR = "AI_GAME_COLOR"
private const val HANDICAP = "AI_GAME_HANDICAP"

class NewGameBottomSheet(context: Context, private val onOk: (Int, Boolean, Int) -> Unit) : BottomSheetDialog(context) {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)!!

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.bottom_sheet_new_ai_game, null)
        setContentView(view)

        setInitialState()

        playButton.setOnClickListener {
            dismiss()
            val selectedSize = when {
                smallGameButton.isChecked -> 9
                mediumGameButton.isChecked -> 13
                else -> 19
            }
            val youPlayBlack = blackButton.isChecked
            val handicap = handicapSlider.value.toInt()
            saveSettings()
            onOk.invoke(selectedSize, youPlayBlack, handicap)
        }

        handicapSlider.addOnChangeListener { _, value, _ ->
            handicapLabel.text = when(value) {
                0f -> "None"
                1f -> "No komi"
                else -> value.toInt().toString()
            }
        }

        setCanceledOnTouchOutside(true)
        setCancelable(true)
    }

    private fun setInitialState() {
        val checkedSizeButtonId = when(prefs.getInt(SIZE, 9)) {
            9 -> R.id.smallGameButton
            13 -> R.id.mediumGameButton
            19 -> R.id.largeGameButton
            else -> -1
        }
        sizeToggleGroup.check(checkedSizeButtonId)

        val checkedColorButtonId = when(prefs.getInt(COLOR, 0)) {
            0 -> R.id.blackButton
            1 -> R.id.whiteButton
            else -> -1
        }
        colorToggleGroup.check(checkedColorButtonId)

        handicapSlider.value = prefs.getFloat(HANDICAP, 0f)
    }

    private fun saveSettings() {
        val selectedSize = when {
            smallGameButton.isChecked -> 9
            mediumGameButton.isChecked -> 13
            else -> 19
        }
        prefs.edit()
                .putInt(SIZE, selectedSize)
                .putInt(COLOR, if(blackButton.isChecked) 0 else 1)
                .putFloat(HANDICAP, handicapSlider.value)
                .apply()
    }

}