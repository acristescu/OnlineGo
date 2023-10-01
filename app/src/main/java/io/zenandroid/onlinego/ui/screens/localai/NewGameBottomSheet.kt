package io.zenandroid.onlinego.ui.screens.localai

import android.content.Context
import android.preference.PreferenceManager
import android.view.LayoutInflater
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.databinding.BottomSheetNewAiGameBinding

private const val SIZE = "AI_GAME_SIZE"
private const val HANDICAP = "AI_GAME_HANDICAP"
private const val AI_BLACK = "AI_GAME_PLAY_BLACK"
private const val AI_WHITE = "AI_GAME_PLAY_WHITE"

class NewGameBottomSheet(context: Context, private val onOk: (Int, Boolean, Boolean, Int) -> Unit, private val onLoad: () -> Unit, private val onSave: () -> Unit) : BottomSheetDialog(context) {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)!!
    private lateinit var binding: BottomSheetNewAiGameBinding

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        binding = BottomSheetNewAiGameBinding.inflate(inflater)
        val view = binding.root
        setContentView(view)

        setInitialState()

        binding.playButton.setOnClickListener {
            dismiss()
            val selectedSize = when {
                binding.smallGameButton.isChecked -> 9
                binding.mediumGameButton.isChecked -> 13
                else -> 19
            }
            val youPlayBlack = binding.blackButton.isChecked
            val youPlayWhite = binding.whiteButton.isChecked
            val handicap = binding.handicapSlider.value.toInt()
            saveSettings()
            onOk.invoke(selectedSize, youPlayBlack, youPlayWhite, handicap)
        }

        binding.loadButton.setOnClickListener {
            dismiss()
            saveSettings()
            onLoad()
        }

        binding.saveButton.setOnClickListener {
            dismiss()
            saveSettings()
            onSave()
        }

        binding.handicapSlider.addOnChangeListener { _, value, _ ->
            setLabel(value)
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

    private fun setLabel(handicap: Float) {
        binding.handicapLabel.text = when (handicap) {
            0f -> "None"
            1f -> "No komi"
            else -> handicap.toInt().toString()
        }
    }

    private fun setInitialState() {
        val checkedSizeButtonId = when(prefs.getInt(SIZE, 9)) {
            9 -> R.id.smallGameButton
            13 -> R.id.mediumGameButton
            19 -> R.id.largeGameButton
            else -> -1
        }
        binding.sizeToggleGroup.check(checkedSizeButtonId)

        if(prefs.getBoolean(AI_WHITE, false)) {
            binding.colorToggleGroup.check(R.id.whiteButton)
        }
        if(prefs.getBoolean(AI_BLACK, false)) {
            binding.colorToggleGroup.check(R.id.blackButton)
        }

        binding.handicapSlider.value = prefs.getFloat(HANDICAP, 0f)
        setLabel(binding.handicapSlider.value)
    }

    private fun saveSettings() {
        val selectedSize = when {
            binding.smallGameButton.isChecked -> 9
            binding.mediumGameButton.isChecked -> 13
            else -> 19
        }
        prefs.edit()
                .putInt(SIZE, selectedSize)
                .putBoolean(AI_BLACK, binding.blackButton.isChecked)
                .putBoolean(AI_WHITE, binding.whiteButton.isChecked)
                .putFloat(HANDICAP, binding.handicapSlider.value)
                .apply()
    }
}
