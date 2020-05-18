package io.zenandroid.onlinego.ui.screens.newchallenge

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.model.local.Player
import io.zenandroid.onlinego.data.model.ogs.OGSPlayer
import io.zenandroid.onlinego.ui.screens.newchallenge.selectopponent.SelectOpponentDialog
import io.zenandroid.onlinego.utils.egfToRank
import io.zenandroid.onlinego.utils.formatRank
import kotlinx.android.synthetic.main.bottom_sheet_new_challenge.*

class NewChallengeBottomSheet(
        context: Context,
        private val onSearch: (ChallengeParams) -> Unit
) : BottomSheetDialogFragment() {

    private val PARAMS_KEY = "PARAMS"
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val challengeParamsAdapter = moshi.adapter(ChallengeParams::class.java)
    private val opponentAdapter = moshi.adapter(Player::class.java)
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    private val challenge: ChallengeParams = getSavedChallengeParams()
    private var opponent: Player? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_sheet_new_challenge, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        challenge.opponent?.let {
            opponent = Player.fromOGSPlayer(it)
        }
        botView.apply {
            name = "Opponent"
            value = challenge.opponent?.let {
                "${it.username} (${formatRank(egfToRank(it.ratings?.overall?.rating))})"
            } ?: "(choose)"
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
        searchButton.setOnClickListener { this.onSearchClicked() }

        isCancelable = true
    }

    private fun onSearchClicked() {
        challenge.apply {
            opponent = this@NewChallengeBottomSheet.opponent?.let(OGSPlayer.Companion::fromPlayer)
            color = colorView.value
            handicap = handicapView.value
            ranked = rankedView.value == "Yes"
            size = sizeView.value
            speed = speedView.value
            disable_analysis = disableAnalysisView.value == "Disabled"
            private = privateView.value == "Yes"
        }
        if(challenge.opponent != null) {
            dismiss()
            saveSettings()
            onSearch(challenge)
        } else {
            Toast.makeText(context, "Please select an opponent", Toast.LENGTH_LONG).show()
        }
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
        botView.value = opponent
                ?.let {"${it.username} (${formatRank(egfToRank(it.rating))})"}
                ?: "(none)"
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