package io.zenandroid.onlinego.data.model.ogs

import io.zenandroid.onlinego.data.ogs.Players
import org.threeten.bp.Instant
import java.util.*

/**
 * Created by alex on 04/11/2017.
 */
data class OGSGame (
        var white: Any? = null,
        var black: Any? = null,

        var id: Long,

        var phase: Phase?,

        var name: String? = null,
        var width: Int,
        var height: Int,
        var move_number: Int? = null,
        var paused: Long? = null,
        var private: Boolean? = null,
        var time_per_move: Int? = null,
        var player_to_move: Long? = null,
        var ended: Instant? = null,

        var json: GameData? = null,

        var related: Related? = null,
        var creator: Int? = null,
        var mode: String? = null,
        var source: String? = null,
        var rules: String? = null,
        var ranked: Boolean? = null,
        var handicap: Int? = null,
        var komi: Float? = null,
        var time_control: String? = null,
        var time_control_parameters: String? = null,
        var disable_analysis: Boolean? = null,
//        var tournament: Any? = null,
//        var tournament_round: Int? = null,
//        var ladder: Any? = null,
        var pause_on_weekends: Boolean? = null,
        var outcome: String? = null,
        var black_lost: Boolean? = null,
        var white_lost: Boolean? = null,
        var annulled: Boolean? = null,
        var started: String? = null,
//        var ended: Any? = null,
//        var sgf_filename: Any? = null,
        var players: Players? = null,

        internal val gamedata: GameData? = null
) {
    data class Related (
            var detail: String? = null
    )

    val blackPlayer: OGSPlayer?
        get() = black as? OGSPlayer

    val blackId: Long?
        get() = (black as? Double ?: (black as? Map<*, *>)?.get("id") as? Double)?.toLong()

    val whitePlayer: OGSPlayer?
        get() = white as? OGSPlayer

    val whiteId: Long?
        get() = (white as? Double ?: (white as? Map<*, *>)?.get("id") as? Double)?.toLong()
}
