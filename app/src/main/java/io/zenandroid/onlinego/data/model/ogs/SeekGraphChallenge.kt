package io.zenandroid.onlinego.data.model.ogs

import io.zenandroid.onlinego.utils.formatRank

/**
 * Created by alex on 08/12/2017.
 */
data class SeekGraphChallenge (
        var challenge_id: Int? = null,
        var delete: Int? = null,
        var name: String = "",
        var username: String = "",
        var ranked: Boolean = false,
        var rank: Double? = null,
        var min_rank: Double = 0.0,
        var max_rank: Double = 100.0,
        var handicap: Int = 0,
        var timePerMove: Int = 0,
        var width: Int = 0,
        var height: Int = 0//,
//        var timeControlParameters: JSONObject? = null
) {
    override fun toString(): String {
        return "$username (${formatRank(rank)})"
    }
}

