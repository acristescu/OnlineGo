package io.zenandroid.onlinego.data.model.ogs

import io.zenandroid.onlinego.data.ogs.TimeControl
import io.zenandroid.onlinego.utils.formatRank

/**
 * Created by alex on 08/12/2017.
 */
data class SeekGraphChallenge (
        var challenge_id: Long? = null,
        var game_started: Boolean = false,
        var delete: Int? = null,
        var name: String = "",
        var username: String = "",
        var ranked: Boolean = false,
        var rank: Double? = null,
        var min_rank: Double = 0.0,
        var max_rank: Double = 100.0,
        var handicap: Int = 0,
        var time_per_move: Double? = null,
        var width: Int = 0,
        var height: Int = 0,
        var time_control_parameters: TimeControl? = null
) {
    override fun toString(): String {
        return "$username (${formatRank(rank)})"
    }
}

