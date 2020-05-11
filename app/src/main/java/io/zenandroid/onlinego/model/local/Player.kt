package io.zenandroid.onlinego.model.local

import androidx.room.PrimaryKey
import io.zenandroid.onlinego.model.ogs.OGSPlayer

/**
 * Created by alex on 05/06/2018.
 */
data class Player(
    @PrimaryKey var id: Long,
    var username: String,
    var rating: Double?,
    var country: String?,
    var icon: String?,
    var acceptedStones: String?,
    var ui_class: String?
) {
    companion object {
        fun fromOGSPlayer(ogsPlayer: OGSPlayer) =
                Player(
                        id = ogsPlayer.id!!,
                        username = ogsPlayer.username ?: "?",
                        rating = ogsPlayer.ratings?.overall?.rating,
                        country = ogsPlayer.country,
                        icon = ogsPlayer.icon,
                        acceptedStones = ogsPlayer.accepted_stones,
                        ui_class = ogsPlayer.ui_class
                )
    }
}