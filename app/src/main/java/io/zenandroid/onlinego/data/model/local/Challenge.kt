package io.zenandroid.onlinego.data.model.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.zenandroid.onlinego.data.model.ogs.OGSChallenge

@Entity
data class Challenge(
        @PrimaryKey val id: Long,

        @Embedded(prefix = "challenger_")
        val challenger: Player?,

        @Embedded(prefix = "challenged_")
        val challenged: Player?
) {
        companion object {
                fun fromOGSChallenge(ogsChallenge: OGSChallenge) =
                        Challenge(
                                id = ogsChallenge.id,
                                challenger = ogsChallenge.challenger?.let { Player.fromOGSPlayer(it) },
                                challenged = ogsChallenge.challenged?.let { Player.fromOGSPlayer(it) }
                        )
        }
}