package io.zenandroid.onlinego.data.model.ogs

import io.zenandroid.onlinego.data.model.local.Player

/**
 * Created by alex on 04/11/2017.
 */
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
    data class Ratings(
            var overall: Rating? = null
    )
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