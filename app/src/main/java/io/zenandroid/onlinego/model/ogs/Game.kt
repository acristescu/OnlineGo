package io.zenandroid.onlinego.model.ogs

/**
 * Created by alex on 04/11/2017.
 */
data class Game (
        var white: Player? = null,
        var black: Player? = null,
        var id: Long,
        var phase: String? = null,
        var name: String? = null,
        var width: Int? = null,
        var height: Int? = null,
        var move_number: Int? = null,
        var paused: Int? = null,
        var private: Boolean? = null,
        var time_per_move: Int? = null,



        var related: Related? = null,
        var creator: Int? = null,
        var mode: String? = null,
        var source: String? = null,
        var rules: String? = null,
        var ranked: Boolean? = null,
        var handicap: Int? = null,
        var komi: String? = null,
        var time_control: String? = null,
        var time_control_parameters: String? = null,
        var disable_analysis: Boolean? = null,
        var tournament: Any? = null,
        var tournament_round: Int? = null,
        var ladder: Any? = null,
        var pause_on_weekends: Boolean? = null,
        var outcome: String? = null,
        var black_lost: Boolean? = null,
        var white_lost: Boolean? = null,
        var annulled: Boolean? = null,
        var started: String? = null,
        var ended: Any? = null,
        var sgf_filename: Any? = null
) {
    data class Related (
            var detail: String? = null
    )
}



