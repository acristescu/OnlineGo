package io.zenandroid.onlinego.ogs

import android.graphics.Point
import io.reactivex.Flowable
import io.reactivex.disposables.Disposable
import io.zenandroid.onlinego.gamelogic.Util
import io.zenandroid.onlinego.model.local.Score
import io.zenandroid.onlinego.model.local.Time
import io.zenandroid.onlinego.model.ogs.GameData
import io.zenandroid.onlinego.model.ogs.Phase
import io.zenandroid.onlinego.model.ogs.OGSPlayer
import io.zenandroid.onlinego.utils.createJsonObject
import java.io.Closeable

/**
 * Created by alex on 06/11/2017.
 */
class GameConnection(val gameId: Long) : Disposable, Closeable {
    private var closed = false

    lateinit var gameData: Flowable<GameData>
    lateinit var moves: Flowable<Move>
    lateinit var clock: Flowable<OGSClock>
    lateinit var phase: Flowable<Phase>
    lateinit var removedStones: Flowable<RemovedStones>

    var gameAuth: String? = null

    override fun close() {
        OGSServiceImpl.instance.disconnectFromGame(gameId)
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
        OGSServiceImpl.instance.emit("game/move", createJsonObject {
            put("auth", gameAuth)
            put("game_id", gameId)
            put("player_id", OGSServiceImpl.instance.uiConfig?.user?.id)
            put("move", encodedMove)
        })
    }

    fun resign() {
        OGSServiceImpl.instance.emit("game/resign", createJsonObject {
            put("auth", gameAuth)
            put("game_id", gameId)
            put("player_id", OGSServiceImpl.instance.uiConfig?.user?.id)
        })
    }

    fun rejectRemovedStones() {
        OGSServiceImpl.instance.emit("game/removed_stones/reject", createJsonObject {
            put("auth", gameAuth)
            put("game_id", gameId)
            put("player_id", OGSServiceImpl.instance.uiConfig?.user?.id)
        })
    }

    fun submitRemovedStones(delta: Set<Point>, removing: Boolean) {
        val sb = StringBuilder()
        delta.forEach { sb.append(Util.getSGFCoordinates(it)) }
        OGSServiceImpl.instance.emit("game/removed_stones/set", createJsonObject {
            put("auth", gameAuth)
            put("game_id", gameId)
            put("player_id", OGSServiceImpl.instance.uiConfig?.user?.id)
            put("removed", removing)
            put("stones", sb.toString())
        })
    }

    fun acceptRemovedStones(removedSpots: Set<Point>) {
        val sb = StringBuilder()
        removedSpots
                .sortedWith(compareBy(Point::y, Point::x))
                .forEach { sb.append(Util.getSGFCoordinates(it)) }
        OGSServiceImpl.instance.emit("game/removed_stones/accept", createJsonObject {
            put("auth", gameAuth)
            put("game_id", gameId)
            put("player_id", OGSServiceImpl.instance.uiConfig?.user?.id)
            put("strict_seki_mode", false)
            put("stones", sb.toString())
        })
    }

}

data class Scores (
        var white: Score? = null,
        var black: Score? = null
)

data class OGSClock (
    var game_id: Long,
    var current_player: Long,
    var black_player_id: Long,
    var white_player_id: Long,
    var title: String? = null,
    var last_move: Long,
    var expiration: Long? = null,
    var now: Long,
    var paused_since: Long? = null,
    var start_mode: Boolean = false,
    var pause_control: Any? = null,
    var black_time: Any,// can be number or Time object
    var white_time: Any// can be number or Time object
) {
    var receivedAt: Long = 0
    val whiteTimeSimple get() = white_time as? Long
    val blackTimeSimple get() = black_time as? Long
    val whiteTime
        get() = (white_time as? Map<*, *>)?.let { Time.fromMap(it) }
    val blackTime
        get() = (black_time as? Map<*, *>)?.let { Time.fromMap(it) }

}

data class Players (
        var white: OGSPlayer? = null,
        var black: OGSPlayer? = null
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
        val move: MutableList<Int>
)

//{"removed":true,"stones":"cidadfdgdieaeceifafhfighgihfhghhhiifigihii","all_removed":"daeafaecdfhfifdghgigfhghhhihcidieifigihiii"}
data class RemovedStones(
        val removed: Boolean?,
        val stones: String?,
        val all_removed: String?
)