package io.zenandroid.onlinego.ogs

import android.graphics.Point
import io.reactivex.Flowable
import io.reactivex.disposables.Disposable
import io.zenandroid.onlinego.gamelogic.Util
import io.zenandroid.onlinego.model.ogs.Game
import io.zenandroid.onlinego.model.ogs.Player
import io.zenandroid.onlinego.utils.createJsonObject
import java.io.Closeable

/**
 * Created by alex on 06/11/2017.
 */
class GameConnection(val gameId: Long) : Disposable, Closeable {
    private var closed = false

    lateinit var gameData: Flowable<GameData>
    lateinit var moves: Flowable<Move>
    lateinit var clock: Flowable<Clock>
    lateinit var phase: Flowable<Game.Phase>
    lateinit var removedStones: Flowable<RemovedStones>

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
        val encodedMove = Util.getSGFCoordinates(move)
        OGSService.instance.emit("game/move", createJsonObject {
            put("auth", gameAuth)
            put("game_id", gameId)
            put("player_id", OGSService.instance.uiConfig?.user?.id)
            put("move", encodedMove)
        })
    }

    fun resign() {
        OGSService.instance.emit("game/resign", createJsonObject {
            put("auth", gameAuth)
            put("game_id", gameId)
            put("player_id", OGSService.instance.uiConfig?.user?.id)
        })
    }

    fun rejectRemovedStones() {
        OGSService.instance.emit("game/removed_stones/reject", createJsonObject {
            put("auth", gameAuth)
            put("game_id", gameId)
            put("player_id", OGSService.instance.uiConfig?.user?.id)
        })
    }

    fun submitRemovedStones(delta: Set<Point>, removing: Boolean) {
        val sb = StringBuilder()
        delta.forEach { sb.append(Util.getSGFCoordinates(it)) }
        OGSService.instance.emit("game/removed_stones/set", createJsonObject {
            put("auth", gameAuth)
            put("game_id", gameId)
            put("player_id", OGSService.instance.uiConfig?.user?.id)
            put("removed", removing)
            put("stones", sb.toString())
        })
    }

    fun acceptRemovedStones(removedSpots: Set<Point>) {
        val sb = StringBuilder()
        removedSpots
                .sortedWith(compareBy(Point::y, Point::x))
                .forEach { sb.append(Util.getSGFCoordinates(it)) }
        OGSService.instance.emit("game/removed_stones/accept", createJsonObject {
            put("auth", gameAuth)
            put("game_id", gameId)
            put("player_id", OGSService.instance.uiConfig?.user?.id)
            put("strict_seki_mode", false)
            put("stones", sb.toString())
        })
    }

}

data class GameData (
    var handicap: Int? = null,
    var disable_analysis: Boolean? = null,
    var _private: Boolean? = null,
    var height: Int,
    var time_control: TimeControl? = null,
    var ranked: Boolean? = null,
    //var meta_groups: List<Any>? = null,
    var komi: Float? = null,
    var game_id: Int? = null,
    var width: Int? = null,
    var rules: String? = null,
    var black_player_id: Int? = null,
    var pause_on_weekends: Boolean? = null,
    var white_player_id: Int? = null,
    var players: Players? = null,
    var game_name: String? = null,
    var phase: Game.Phase,
    //var history: List<Any>? = null,
    var initial_player: String? = null,
    var moves: List<List<Int>>,
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
    var clock: Clock,
    var removed: String? = null,
    var auth: String? = null,
    var game_chat_auth: String? = null,
    var winner: Int? = null,
    var outcome: String? = null,
    var end_time: Long? = null,
    var score: Scores? = null
)

data class Scores (
        var white: Score? = null,
        var black: Score? = null
)

data class Score (
        var handicap: Double? = null,
        var komi: Double? = null,
        var prisoners: Int? = null,
        var scoring_positions: String? = null,
        var stones: Int? = null,
        var territory: Int? = null,
        var total: Double? = null
)

data class InitialState (
    var black: String? = null,
    var white: String? = null
)

data class Clock (
    var game_id: Long,
    var current_player: Long,
    var black_player_id: Long,
    var white_player_id: Long,
    var title: String? = null,
    var last_move: Long? = null,
    var expiration: Long? = null,
    var now: Long,
    var paused_since: Long? = null,
    var black_time: Any? = null,// can be number or Time object
    var white_time: Any? = null// can be number or Time object
)

data class Time(
        //var data: JSONObject? = null
        var thinking_time: Float? = null,
        var skip_bonus: Boolean? = null
)

data class Players (
    var white: Player? = null,
    var black: Player? = null
)


data class TimeControl (
    var system: String? = null,
    var pause_on_weekends: Boolean? = null,
    var time_control: String? = null,
    var initial_time: Int? = null,
    var max_time: Int? = null,
    var time_increment: Int? = null,
    var speed: String? = null
)

//{game_id: 10528331, move_number: 202, move: [9, 17, 8509]}
data class Move(
        val game_id: Long,
        val move_number: Int,
        val move: List<Int>
)

//{"removed":true,"stones":"cidadfdgdieaeceifafhfighgihfhghhhiifigihii","all_removed":"daeafaecdfhfifdghgigfhghhhihcidieifigihiii"}
data class RemovedStones(
        val removed: Boolean?,
        val stones: String?,
        val all_removed: String?
)