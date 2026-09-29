@file:UseSerializers(
  OGSBooleanSerializer::class,
  LenientIntSerializer::class,
  LenientLongSerializer::class
)

package io.zenandroid.onlinego.data.model.ogs

import io.zenandroid.onlinego.data.model.local.Player
import kotlinx.serialization.Serializable
import io.zenandroid.onlinego.data.ogs.OGSBooleanSerializer
import kotlinx.serialization.UseSerializers
import io.zenandroid.onlinego.data.ogs.LenientIntSerializer
import io.zenandroid.onlinego.data.ogs.LenientLongSerializer

/**
 * Created by alex on 04/11/2017.
 */
@Serializable
data class OGSPlayer (
        var id: Long? = null,
        var username: String? = null,
        var rank: Float? = null,
        var professional: Boolean? = null,
        var accepted_stones: String? = null,
        var ratings: Ratings? = null,
        var egf: Double? = null,
        var country: String? = null,
        var icon: String? = null,
        var ui_class: String? = null
) {
  @Serializable
    data class Ratings(
            var overall: Rating? = null
    )

  @Serializable
    data class Rating(
            var deviation: Double? = null,
            var rating: Double? = null,
            var volatility: Float? = null,
            var games_played: Int? = null
    )

    companion object {
        fun fromPlayer(player: Player) =
            OGSPlayer(
                    id = player.id,
                    username = player.username,
                    ratings = Ratings(Rating(rating = player.rating)),
                    ui_class = player.ui_class
            )
    }
}