package io.zenandroid.onlinego.model.ogs

import io.zenandroid.onlinego.model.local.InitialState
import io.zenandroid.onlinego.ogs.OGSClock
import io.zenandroid.onlinego.ogs.Players
import io.zenandroid.onlinego.ogs.Scores
import io.zenandroid.onlinego.ogs.TimeControl

data class GameData (
        var handicap: Int? = null,
        var disable_analysis: Boolean? = null,
        var _private: Boolean? = null,
        var height: Int,
        var time_control: TimeControl? = null,
        var ranked: Boolean? = null,
        var komi: Float? = null,
        var game_id: Long,
        var width: Int,
        var rules: String? = null,
        var black_player_id: Long? = null,
        var pause_on_weekends: Boolean? = null,
        var white_player_id: Long? = null,
        var players: Players? = null,
        var game_name: String? = null,
        var phase: Phase,
        var initial_player: String? = null,
        var moves: MutableList<MutableList<Long>>,
        var allow_self_capture: Boolean? = null,
        var automatic_stone_removal: Boolean? = null,
        var free_handicap_placement: Boolean? = null,
        var aga_handicap_scoring: Boolean? = null,
        var allow_ko: Boolean? = null,
        var allow_superko: Boolean? = null,
        var superko_algorithm: String? = null,
        var score_territory: Boolean? = null,
        var score_territory_in_seki: Boolean? = null,
        var score_stones: Boolean? = null,
        var score_prisoners: Boolean? = null,
        var score_passes: Boolean? = null,
        var white_must_pass_last: Boolean? = null,
        var opponent_plays_first_after_resume: Boolean? = null,
        var strict_seki_mode: Boolean? = null,
        var initial_state: InitialState? = null,
        var start_time: Int? = null,
        var clock: OGSClock,
        var removed: String? = null,
        var auth: String? = null,
        var game_chat_auth: String? = null,
        var winner: Long? = null,
        var outcome: String? = null,
        var end_time: Long? = null,
        var score: Scores? = null,
        var undo_requested: Int? = null
)