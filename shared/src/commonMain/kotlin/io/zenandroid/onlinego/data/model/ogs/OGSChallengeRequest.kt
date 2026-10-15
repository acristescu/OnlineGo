@file:UseSerializers(OGSBooleanSerializer::class, LenientIntSerializer::class)

package io.zenandroid.onlinego.data.model.ogs

import io.zenandroid.onlinego.data.ogs.LenientIntSerializer
import io.zenandroid.onlinego.data.ogs.OGSBooleanSerializer
import io.zenandroid.onlinego.data.ogs.TimeControl
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
data class OGSChallengeRequest (
        val initialized: Boolean,
        val min_ranking: Int = 0,
        val max_ranking: Int = 60,
        val challenger_color: String,
        val game: Game,
        val aga_ranked: Boolean
) {
        @Serializable
    data class Game(
                val name: String? = null,
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