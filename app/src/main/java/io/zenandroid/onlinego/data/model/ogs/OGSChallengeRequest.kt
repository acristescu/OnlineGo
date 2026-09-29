@file:UseSerializers(OGSBooleanSerializer::class, LenientIntSerializer::class)

package io.zenandroid.onlinego.data.model.ogs

import io.zenandroid.onlinego.data.ogs.TimeControl
import kotlinx.serialization.Serializable
import io.zenandroid.onlinego.data.ogs.OGSBooleanSerializer
import kotlinx.serialization.UseSerializers
import io.zenandroid.onlinego.data.ogs.LenientIntSerializer

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