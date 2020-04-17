package io.zenandroid.onlinego.ogs

import android.graphics.Point
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.extensions.addToDisposable
import io.zenandroid.onlinego.gamelogic.Util
import io.zenandroid.onlinego.model.local.Message
import io.zenandroid.onlinego.model.local.Score
import io.zenandroid.onlinego.model.local.Time
import io.zenandroid.onlinego.model.ogs.Chat
import io.zenandroid.onlinego.model.ogs.GameData
import io.zenandroid.onlinego.model.ogs.OGSPlayer
import io.zenandroid.onlinego.model.ogs.Phase
import io.zenandroid.onlinego.utils.createJsonObject
import java.io.Closeable

/**
 * Created by alex on 06/11/2017.
 */
class GameConnection(
        private val gameId: Long,
        private val connectionLock: Any,
        gameDataObservable: Flowable<GameData>,
        movesObservable: Flowable<Move>,
        clockObservable: Flowable<OGSClock>,
        phaseObservable: Flowable<Phase>,
        removedStonesObservable: Flowable<RemovedStones>,
        chatObservable: Flowable<Chat>,
        undoRequestedObservable: Flowable<Int>
) : Disposable, Closeable {
    private var closed = false
    private var counter = 0

    private val gameDataSubject =  PublishSubject.create<GameData>()
    private val movesSubject =  PublishSubject.create<Move>()
    private val clockSubject =  PublishSubject.create<OGSClock>()
    private val phaseSubject =  PublishSubject.create<Phase>()
    private val removedStonesSubject =  PublishSubject.create<RemovedStones>()
    private val undoRequestSubject =  PublishSubject.create<Int>()

    val gameData: Observable<GameData> = gameDataSubject.hide()
    val moves: Observable<Move> = movesSubject.hide()
    val clock: Observable<OGSClock> = clockSubject.hide()
    val phase: Observable<Phase> = phaseSubject.hide()
    val removedStones: Observable<RemovedStones> = removedStonesSubject.hide()
    val undoRequested: Observable<Int> = undoRequestSubject.hide()

    var gameAuth: String? = null

    private val subscriptions = CompositeDisposable()

    init {
        gameDataObservable
                .doOnNext{ gameAuth = it.auth }
                .subscribe(gameDataSubject::onNext)
                .addToDisposable(subscriptions)

        movesObservable.subscribe(movesSubject::onNext).addToDisposable(subscriptions)
        clockObservable.subscribe(clockSubject::onNext).addToDisposable(subscriptions)
        phaseObservable.subscribe(phaseSubject::onNext).addToDisposable(subscriptions)
        removedStonesObservable.subscribe(removedStonesSubject::onNext).addToDisposable(subscriptions)
        chatObservable.subscribe {
            OnlineGoApplication.instance.chatRepository.addMessage(Message.fromOGSMessage(it, gameId))
        }
                .addToDisposable(subscriptions)
        undoRequestedObservable.subscribe(undoRequestSubject::onNext).addToDisposable(subscriptions)
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


    fun incrementCounter() {
        synchronized(connectionLock) {
            counter++
        }
    }

    fun decrementCounter() {
        synchronized(connectionLock) {
            counter--
            if (counter == 0) {
                subscriptions.clear()
                OGSServiceImpl.disconnectFromGame(gameId)
                closed = true
            }
        }
    }

    fun submitMove(move: Point) {
        val encodedMove = Util.getSGFCoordinates(move)
        OGSServiceImpl.emit("game/move", createJsonObject {
            put("auth", gameAuth)
            put("game_id", gameId)
            put("player_id", OGSServiceImpl.uiConfig?.user?.id)
            put("move", encodedMove)
        })
    }

    fun resign() {
        OGSServiceImpl.emit("game/resign", createJsonObject {
            put("auth", gameAuth)
            put("game_id", gameId)
            put("player_id", OGSServiceImpl.uiConfig?.user?.id)
        })
    }

    fun rejectRemovedStones() {
        OGSServiceImpl.emit("game/removed_stones/reject", createJsonObject {
            put("auth", gameAuth)
            put("game_id", gameId)
            put("player_id", OGSServiceImpl.uiConfig?.user?.id)
        })
    }

    fun submitRemovedStones(delta: Set<Point>, removing: Boolean) {
        val sb = StringBuilder()
        delta.forEach { sb.append(Util.getSGFCoordinates(it)) }
        OGSServiceImpl.emit("game/removed_stones/set", createJsonObject {
            put("auth", gameAuth)
            put("game_id", gameId)
            put("player_id", OGSServiceImpl.uiConfig?.user?.id)
            put("removed", removing)
            put("stones", sb.toString())
        })
    }

    fun acceptRemovedStones(removedSpots: Set<Point>) {
        val stones = removedSpots
                .asSequence()
                .sortedWith(compareBy(Point::y, Point::x))
                .joinToString(separator = "") {
                    Util.getSGFCoordinates(it)
                }
        OGSServiceImpl.emit("game/removed_stones/accept", createJsonObject {
            put("auth", gameAuth)
            put("game_id", gameId)
            put("player_id", OGSServiceImpl.uiConfig?.user?.id)
            put("strict_seki_mode", false)
            put("stones", stones)
        })
    }

    fun sendMessage(message: String, moveNumber: Int) {
        OGSServiceImpl.emit("game/chat", createJsonObject {
            put("body", message)
            put("game_id", gameId)
            put("move_number", moveNumber)
            put("type", "main")
        })
    }

    fun acceptUndo(moveNo: Int) {
        OGSServiceImpl.emit("game/undo/accept", createJsonObject {
            put("game_id", gameId)
            put("move_number", moveNo)
            put("player_id", OGSServiceImpl.uiConfig?.user?.id)
        })
    }

    fun abortGame() {
        OGSServiceImpl.emit("game/cancel", createJsonObject {
            put("game_id", gameId)
            put("player_id", OGSServiceImpl.uiConfig?.user?.id)
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
    var now: Long? = null,
    var paused_since: Long? = null,
    var start_mode: Boolean = false,
    var pause_control: Any? = null,
    var black_time: Any,// can be number or Time object
    var white_time: Any// can be number or Time object
) {
    var receivedAt: Long = 0
    val whiteTimeSimple get() = (white_time as? Number)?.toLong()
    val blackTimeSimple get() = (black_time as? Number)?.toLong()
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
    var speed: String? = null,
    var per_move: Int? = null,
    var main_time: Int? = null,
    var periods: Int? = null,
    var period_time: Int? = null,
    var stones_per_period: Int? = null,
    var total_time: Int? = null

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