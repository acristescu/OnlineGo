package io.zenandroid.onlinego.ui.screens.newchallenge

import android.app.Activity.RESULT_OK
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.data.model.local.Player
import io.zenandroid.onlinego.data.model.ogs.OGSPlayer
import io.zenandroid.onlinego.databinding.BottomSheetNewChallengeBinding
import io.zenandroid.onlinego.ui.screens.main.MainActivity
import io.zenandroid.onlinego.ui.screens.newchallenge.selectopponent.SelectOpponentDialog
import io.zenandroid.onlinego.utils.egfToRank
import io.zenandroid.onlinego.utils.formatRank

class NewChallengeBottomSheet : BottomSheetDialogFragment() {

    private val PARAMS_KEY = "PARAMS"
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val challengeParamsAdapter = moshi.adapter(ChallengeParams::class.java)
    private val opponentAdapter = moshi.adapter(Player::class.java)
    private val prefs = PreferenceManager.getDefaultSharedPreferences(OnlineGoApplication.instance)
    private val challenge: ChallengeParams = getSavedChallengeParams()
    private var opponent: Player? = null
    private lateinit var binding: BottomSheetNewChallengeBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = BottomSheetNewChallengeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        challenge.opponent = null
        binding.apply {
            botView.apply {
                name = "Opponent"
                value = challenge.opponent?.let {
                    "${it.username} (${formatRank(egfToRank(it.ratings?.overall?.rating))})"
                } ?: "[Open Offer]"
                setOnClickListener {
                    fragmentManager?.let {
                        SelectOpponentDialog().apply {
                            setTargetFragment(this@NewChallengeBottomSheet, 1)
                            show(it, "SELECT_OPPONENT")
                        }
                    }
                }
            }
            colorView.apply {
                name = "You play"
                value = challenge.color
                valuesCallback = { listOf("Auto", "Black", "White") }
            }
            sizeView.apply {
                name = "Size"
                value = challenge.size
                valuesCallback = { listOf("9x9", "13x13", "19x19") }
            }
            handicapView.apply {
                name = "Handicap"
                value = challenge.handicap
                valuesCallback = { listOf("Auto", "0", "1", "2", "3", "4", "5") }
            }
            speedView.apply {
                name = "Speed"
                value = challenge.speed
                valuesCallback = { listOf("Blitz", "Live", "Correspondence") }
            }
            rankedView.apply {
                name = "Ranked"
                value = if (challenge.ranked) "Yes" else "No"
                valuesCallback = { listOf("Yes", "No") }
            }
            disableAnalysisView.apply {
                name = "Analysis"
                value = if (challenge.disable_analysis) "Disabled" else "Enabled"
                valuesCallback = { listOf("Enabled", "Disabled") }
            }

            privateView.apply {
                name = "Private"
                value = if (challenge.private) "Yes" else "No"
                valuesCallback = { listOf("Yes", "No") }
            }
            searchButton.setOnClickListener { this@NewChallengeBottomSheet.onSearchClicked() }

            isCancelable = true
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)

        dialog.setOnShowListener {
            BottomSheetBehavior.from(dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet)!!).apply {
                state = BottomSheetBehavior.STATE_EXPANDED
                skipCollapsed = true
            }
        }

        return dialog
    }

    private fun onSearchClicked() {
        challenge.apply {
            opponent = this@NewChallengeBottomSheet.opponent?.let(OGSPlayer.Companion::fromPlayer)
            color = binding.colorView.value
            handicap = binding.handicapView.value
            ranked = binding.rankedView.value == "Yes"
            size = binding.sizeView.value
            speed = binding.speedView.value
            disable_analysis = binding.disableAnalysisView.value == "Disabled"
            private = binding.privateView.value == "Yes"
        }
        dismiss()
        saveSettings()
        (activity as? MainActivity)?.onNewChallengeSearchClicked(challenge)
    }

    private fun getSavedChallengeParams() =
            prefs.getString(PARAMS_KEY, null)?.let ( challengeParamsAdapter::fromJson )
                    ?: ChallengeParams(
                            opponent = null,
                            color = "Auto",
                            ranked = true,
                            handicap = "0",
                            size = "9x9",
                            speed = "Live",
                            disable_analysis = false,
                            private = false
                    )

    private fun saveSettings() {
        prefs.edit()
                .putString(PARAMS_KEY, challengeParamsAdapter.toJson(challenge))
                .apply()
    }

    private fun selectOpponent(opponent: Player?) {
        binding.botView.value = opponent
                ?.let {"${it.username} (${formatRank(egfToRank(it.rating))})"}
                ?: "[Open Offer]"
        this.opponent = opponent
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == 1 && resultCode == RESULT_OK) {
            data?.getStringExtra("OPPONENT")?.let {
                selectOpponent(opponentAdapter.fromJson(it))
            }
        }
    }
}