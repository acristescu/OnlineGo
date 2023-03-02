package io.zenandroid.onlinego.data.model.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.zenandroid.onlinego.data.model.ogs.OGSChallenge
import org.json.JSONObject

@Entity
data class Challenge(
        @PrimaryKey val id: Long,

        @Embedded(prefix = "challenger_")
        val challenger: Player?,

        @Embedded(prefix = "challenged_")
        val challenged: Player?,

        val gameId: Long?,
        val width: Int?,
        val height: Int?,
        val disabledAnalysis: Boolean?,
        val ranked: Boolean?,
        val handicap: Int?,
        val rules: String?,
        val speed: String?,
) {
        companion object {
                fun fromOGSChallenge(ogsChallenge: OGSChallenge): Challenge {
                        val params = ogsChallenge.game?.time_control_parameters?.let { JSONObject(it) } ?: JSONObject()
                        return Challenge(
                                id = ogsChallenge.id,
                                challenger = ogsChallenge.challenger?.let { Player.fromOGSPlayer(it) },
                                challenged = ogsChallenge.challenged?.let { Player.fromOGSPlayer(it) },
                                gameId = ogsChallenge.game?.id,
                                width = ogsChallenge.game?.width,
                                height = ogsChallenge.game?.height,
                                disabledAnalysis = ogsChallenge.game?.disable_analysis,
                                ranked = ogsChallenge.game?.ranked,
                                handicap = ogsChallenge.game?.handicap,
                                rules = ogsChallenge.game?.rules,
                                speed = params.optString("speed")
                        )
                }
        }
}