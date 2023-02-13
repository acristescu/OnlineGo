package io.zenandroid.onlinego.data.model.local

import androidx.room.PrimaryKey
import io.zenandroid.onlinego.data.model.ogs.OGSPlayer

/**
 * Created by alex on 05/06/2018.
 */
data class Player(
    val id: Long,
    val username: String,
    val rating: Double?,
    val historicRating: Double?,
    val country: String?,
    val icon: String?,
    val acceptedStones: String?,
    val ui_class: String?,
    val deviation: Double?,
) {
    companion object {
        fun fromOGSPlayer(ogsPlayer: OGSPlayer) =
                Player(
                        id = ogsPlayer.id!!,
                        username = ogsPlayer.username ?: "?",
                        rating = ogsPlayer.ratings?.overall?.rating,
                        historicRating = null,
                        country = ogsPlayer.country,
                        icon = ogsPlayer.icon,
                        acceptedStones = ogsPlayer.accepted_stones,
                        ui_class = ogsPlayer.ui_class,
                        deviation = ogsPlayer.ratings?.overall?.deviation,
                )
    }
}