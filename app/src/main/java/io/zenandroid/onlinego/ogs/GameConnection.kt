package io.zenandroid.onlinego.ogs

import android.graphics.Point
import io.reactivex.Flowable
import io.reactivex.disposables.Disposable
import java.io.Closeable

/**
 * Created by alex on 06/11/2017.
 */
class GameConnection(val gameId: Long) : Disposable, Closeable {
    private var closed = false

    lateinit var gameData: Flowable<GameData>
    lateinit var moves: Flowable<Move>

    var gameAuth: String? = null

    override fun close() {
        OGSService.instance.disconnectFromGame(gameId)
        closed = true
    }

    override fun isDisposed(): Boolean {
        return closed
    }

    override fun dispose() {
        close()
    }

    fun submitMove(move: Point) {
        OGSService.instance.submitMove(move, gameId, gameAuth)
    }
}

class GameData {

    var handicap: Int? = null
    var disable_analysis: Boolean? = null
    var _private: Boolean? = null
    var height: Int? = null
    var time_control: Time_control? = null
    var ranked: Boolean? = null
    //var meta_groups: List<Any>? = null
    var komi: Float? = null
    var game_id: Int? = null
    var width: Int? = null
    var rules: String? = null
    var black_player_id: Int? = null
    var pause_on_weekends: Boolean? = null
    var white_player_id: Int? = null
    var players: Players3? = null
    var game_name: String? = null
    var phase: String? = null
    //var history: List<Any>? = null
    var initial_player: String? = null
    var moves: List<List<Int>>? = null
    var allow_self_capture: Boolean? = null
    var automatic_stone_removal: Boolean? = null
    var free_handicap_placement: Boolean? = null
    var aga_handicap_scoring: Boolean? = null
    var allow_ko: Boolean? = null
    var allow_superko: Boolean? = null
    var superko_algorithm: String? = null
    var score_territory: Boolean? = null
    var score_territory_in_seki: Boolean? = null
    var score_stones: Boolean? = null
    var score_prisoners: Boolean? = null
    var score_passes: Boolean? = null
    var white_must_pass_last: Boolean? = null
    var opponent_plays_first_after_resume: Boolean? = null
    var strict_seki_mode: Boolean? = null
    var initial_state: Initial_state? = null
    var start_time: Int? = null
    var clock: Clock? = null
    var removed: String? = null
    var auth: String? = null
    var game_chat_auth: String? = null
    var winner: Int? = null
    var outcome: String? = null
    var end_time: Long? = null

}


class Initial_state {

    var black: String? = null
    var white: String? = null

}

class Clock {

    var game_id: Int? = null
    var current_player: Int? = null
    var black_player_id: Int? = null
    var white_player_id: Int? = null
    var title: String? = null
    var last_move: Long? = null
    var expiration: Long? = null
    var black_time: Any? = null// can be number or Time object
    var white_time: Any? = null// can be number or Time object

}

class Time(

        //var data: JSONObject? = null
        var thinking_time: Float? = null,
        var skip_bonus: Boolean? = null

)

class Players3 {

    var white: Player3? = null
    var black: Player3? = null

}

class Player3 {

    var username: String? = null
    var professional: Boolean? = null
    //var egf: Int? = null
    var rank: Int? = null
    var id: Int? = null

}

class Time_control {

    var system: String? = null
    var pause_on_weekends: Boolean? = null
    var time_control: String? = null
    var initial_time: Int? = null
    var max_time: Int? = null
    var time_increment: Int? = null
    var speed: String? = null

}
//{game_id: 10528331, move_number: 202, move: [9, 17, 8509]}
data class Move(
        val game_id: Long,
        val move_number: Int,
        val move: List<Int>
)