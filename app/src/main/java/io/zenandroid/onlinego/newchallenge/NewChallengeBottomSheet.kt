package io.zenandroid.onlinego.newchallenge

import android.content.Context
import android.preference.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.view.LayoutInflater
import android.widget.Toast
import com.squareup.moshi.Moshi
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.ogs.BotsRepository
import io.zenandroid.onlinego.utils.egfToRank
import io.zenandroid.onlinego.utils.formatRank
import kotlinx.android.synthetic.main.bottom_sheet_new_challenge.*

class NewChallengeBottomSheet(
        context: Context,
        private val botsRepository: BotsRepository,
        private val onSearch: (ChallengeParams) -> Unit
) : BottomSheetDialog(context) {

    private val PARAMS_KEY = "PARAMS"
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    val challenge: ChallengeParams
    val moshi = Moshi.Builder().build().adapter(ChallengeParams::class.java)

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.bottom_sheet_new_challenge, null)
        setContentView(view)

        challenge = getSavedChallengeParams()

        botView.apply {
            name = "Bot"
            value = botsRepository.bots
                    .find { it.id == challenge.bot?.id }
                    ?.let {"${it.username} (${formatRank(egfToRank(it.ratings?.overall?.rating))})"}
                    ?: "(none)"
            valuesCallback = {
                botsRepository.bots
                        .sortedBy { it.ratings?.overall?.rating }
                        .map { "${it.username} (${formatRank(egfToRank(it.ratings?.overall?.rating))})" }
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

        searchButton.setOnClickListener { this.onSearchClicked() }

        setCanceledOnTouchOutside(true)
        setCancelable(true)
    }

    private fun onSearchClicked() {
        challenge.apply {
            bot = botsRepository.bots.find { it.username == botView.value.substringBefore(" (") }
            color = colorView.value
            handicap = handicapView.value
            ranked = rankedView.value == "Yes"
            size = sizeView.value
            speed = speedView.value
        }
        if(challenge.bot != null) {
            dismiss()
            saveSettings()
            onSearch(challenge)
        } else {
            Toast.makeText(context, "Please select an online bot", Toast.LENGTH_LONG).show()
        }
    }

    private fun getSavedChallengeParams() =
            prefs.getString(PARAMS_KEY, null)?.let ( moshi::fromJson )
                    ?: ChallengeParams(
                            bot = null,
                            color = "Auto",
                            ranked = true,
                            handicap = "0",
                            size = "9x9",
                            speed = "Live"
                    )

    private fun saveSettings() {
        prefs.edit()
                .putString(PARAMS_KEY, moshi.toJson(challenge))
                .apply()
    }

}