package io.zenandroid.onlinego.model.ogs

/**
 * Created by alex on 04/11/2017.
 */
data class Player (
        var id: Long,
        var username: String? = null,
        var rank: Int? = null,
        var professional: Boolean? = null,
        var accepted: Boolean? = null,
        var ratings: Ratings? = null,
        var egf: Double? = null,
        var country: String? = null,
        var icon: String? = null
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
}