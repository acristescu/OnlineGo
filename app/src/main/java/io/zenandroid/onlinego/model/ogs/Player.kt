package io.zenandroid.onlinego.model.ogs

/**
 * Created by alex on 04/11/2017.
 */
data class Player (
        var id: Int? = null,
        var username: String? = null,
        var rank: Int? = null,
        var professional: Boolean? = null,
        var accepted: Boolean? = null,
        var ratings: Ratings
) {
    data class Ratings(
            var overall: Rating? = null
    )
    data class Rating(
            var deviation: Float? = null,
            var rating: Float? = null,
            var volatility: Float? = null,
            var games_played: Int? = null
    )
}