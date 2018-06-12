package io.zenandroid.onlinego.ogs

import android.graphics.Point
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.internal.util.HalfSerializer.onNext
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.zenandroid.onlinego.gamelogic.Util
import io.zenandroid.onlinego.model.local.Game
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
class GameConnection(
        private val gameId: Long,
        gameDataObservable: Flowable<GameData>,
        movesObservable: Flowable<Move>,
        clockObservable: Flowable<OGSClock>,
        phaseObservable: Flowable<Phase>,
        removedStonesObservable: Flowable<RemovedStones>
) : Disposable, Closeable {
    private var closed = false
    private var counter = 0

    private val gameDataSubject =  PublishSubject.create<GameData>()
    private val movesSubject =  PublishSubject.create<Move>()
    private val clockSubject =  PublishSubject.create<OGSClock>()
    private val phaseSubject =  PublishSubject.create<Phase>()
    private val removedStonesSubject =  PublishSubject.create<RemovedStones>()

    val gameData: Observable<GameData> = gameDataSubject.hide()
    val moves: Observable<Move> = movesSubject.hide()
    val clock: Observable<OGSClock> = clockSubject.hide()
    val phase: Observable<Phase> = phaseSubject.hide()
    val removedStones: Observable<RemovedStones> = removedStonesSubject.hide()

    var gameAuth: String? = null

    private val subscriptions = CompositeDisposable()

    init {
        subscriptions.add(
                gameDataObservable.doOnNext{
                    gameAuth = it.auth
                }.subscribe(gameDataSubject::onNext)
        )
        subscriptions.add(movesObservable.subscribe(movesSubject::onNext))
        subscriptions.add(clockObservable.subscribe(clockSubject::onNext))
        subscriptions.add(phaseObservable.subscribe(phaseSubject::onNext))
        subscriptions.add(removedStonesObservable.subscribe(removedStonesSubject::onNext))
    }

    override fun close() {
        decrementCounter()
    }

    override fun isDisposed(): Boolean {
        return closed
    }

    override fun dispose() {
        close()
    }


    @Synchronized
    fun incrementCounter() {
        counter++
    }

    @Synchronized
    fun decrementCounter() {
        counter--
        if(counter == 0) {
            subscriptions.clear()
            OGSServiceImpl.instance.disconnectFromGame(gameId)
            closed = true
        }
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
        val move: MutableList<Long>
)

//{"removed":true,"stones":"cidadfdgdieaeceifafhfighgihfhghhhiifigihii","all_removed":"daeafaecdfhfifdghgigfhghhhihcidieifigihiii"}
data class RemovedStones(
        val removed: Boolean?,
        val stones: String?,
        val all_removed: String?
)