package io.zenandroid.onlinego.model.ogs

import io.zenandroid.onlinego.ogs.TimeControl

data class OGSChallengeRequest (
        val initialized: Boolean,
        val min_ranking: Int = -1000,
        val max_ranking: Int = 1000,
        val challenger_color: String,
        val aga_ranked: Boolean,
        val game: Game

) {
    data class Game(
            val handicap: String,
            val time_control: String,
            val challenger_color: String,
            val rules: String,
            val ranked: Boolean,
            val width: Int,
            val height: Int,
            val komi_auto: String,
            val komi: Float? = null,
            val disable_analysis: Boolean,
            val pause_on_weekends: Boolean = true,
            val initial_state: String? = null,
            val private: Boolean = true,
            val name: String?,
            val time_control_parameters: TimeControl

    )

}