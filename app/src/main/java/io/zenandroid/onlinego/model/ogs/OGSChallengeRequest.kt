package io.zenandroid.onlinego.model.ogs

import io.zenandroid.onlinego.ogs.TimeControl

data class OGSChallengeRequest (
        val initialized: Boolean,
        val min_ranking: Int = 0,
        val max_ranking: Int = 60,
        val challenger_color: String,
        val game: Game,
        val aga_ranked: Boolean
) {
    data class Game(
            val name: String?,
            val rules: String,
            val ranked: Boolean,
            val width: Int,
            val height: Int,
            val handicap: String,
            val komi_auto: String,
            val komi: Float? = null,
            val disable_analysis: Boolean,
            val initial_state: String? = null,
            val private: Boolean = false,
            val time_control: String,
            val time_control_parameters: TimeControl,
            val pause_on_weekends: Boolean = true
    )

}