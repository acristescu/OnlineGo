package io.zenandroid.onlinego.model.local

import android.arch.persistence.room.Embedded
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import io.zenandroid.onlinego.model.ogs.OGSChallenge

@Entity
data class Challenge(
        @PrimaryKey var id: Long,

        @Embedded(prefix = "challenger_")
        var challenger: Player?,

        @Embedded(prefix = "challenged_")
        var challenged: Player?
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