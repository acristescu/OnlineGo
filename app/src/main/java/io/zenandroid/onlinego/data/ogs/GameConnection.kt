package io.zenandroid.onlinego.data.ogs

import android.graphics.Point
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.utils.addToDisposable
import io.zenandroid.onlinego.gamelogic.Util
import io.zenandroid.onlinego.data.model.local.Message
import io.zenandroid.onlinego.data.model.local.Score
import io.zenandroid.onlinego.data.model.local.Time
import io.zenandroid.onlinego.data.model.ogs.Chat
import io.zenandroid.onlinego.data.model.ogs.GameData
import io.zenandroid.onlinego.data.model.ogs.OGSPlayer
import io.zenandroid.onlinego.data.model.ogs.Phase
import io.zenandroid.onlinego.data.repositories.ChatRepository
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import io.zenandroid.onlinego.gamelogic.Util.getCurrentUserId
import io.zenandroid.onlinego.utils.createJsonObject
import org.koin.core.context.KoinContextHandler.get
import java.io.Closeable

private const val TAG = "GameConnection"
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
        undoRequestedObservable: Flowable<Int>,
        removedStonesAcceptedObservable: Flowable<RemovedStonesAccepted>,
        undoAcceptedObservable: Flowable<Int>
) : Disposable, Closeable {
    private var closed = false
    private var counter = 0

    private val socketService: OGSWebSocketService = get().get()
    private val chatRepository: ChatRepository = get().get()

    private val gameDataSubject = PublishSubject.create<GameData>()
    private val movesSubject = PublishSubject.create<Move>()
    private val clockSubject = PublishSubject.create<OGSClock>()
    private val phaseSubject = PublishSubject.create<Phase>()
    private val removedStonesSubject = PublishSubject.create<RemovedStones>()
    private val undoRequestSubject = PublishSubject.create<Int>()
    private val removedStonesAcceptedSubject = PublishSubject.create<RemovedStonesAccepted>()
    private val undoAcceptedSubject = PublishSubject.create<Int>()

    val gameData: Observable<GameData> = gameDataSubject.hide()
    val moves: Observable<Move> = movesSubject.hide()
    val clock: Observable<OGSClock> = clockSubject.hide()
    val phase: Observable<Phase> = phaseSubject.hide()
    val removedStones: Observable<RemovedStones> = removedStonesSubject.hide()
    val undoRequested: Observable<Int> = undoRequestSubject.hide()
    val removedStonesAccepted: Observable<RemovedStonesAccepted> = removedStonesAcceptedSubject.hide()
    val undoAccepted: Observable<Int> = undoAcceptedSubject.hide()

    var gameAuth: String? = null

    private val subscriptions = CompositeDisposable()

    init {
        gameDataObservable
                .retryOnError("gamedata")
                .doOnNext{ gameAuth = it.auth }
                .subscribe(gameDataSubject::onNext)
                .addToDisposable(subscriptions)

        movesObservable
                .retryOnError("moves")
                .subscribe(movesSubject::onNext)
                .addToDisposable(subscriptions)
        clockObservable
                .retryOnError("clock")
                .subscribe(clockSubject::onNext)
                .addToDisposable(subscriptions)

        phaseObservable
                .retryOnError("phase")
                .subscribe(phaseSubject::onNext)
                .addToDisposable(subscriptions)

        removedStonesObservable
                .retryOnError("removed_stones")
                .subscribe(removedStonesSubject::onNext)
                .addToDisposable(subscriptions)

        chatObservable
                .retryOnError("chat")
                .subscribe {
                    chatRepository.addMessage(Message.fromOGSMessage(it, gameId))
                }
                .addToDisposable(subscriptions)

        undoRequestedObservable
                .retryOnError("undo_requested")
                .subscribe(undoRequestSubject::onNext)
                .addToDisposable(subscriptions)

        removedStonesAcceptedObservable
                .retryOnError("removed_stones_accepted")
                .subscribe(removedStonesAcceptedSubject::onNext)
                .addToDisposable(subscriptions)

        undoAcceptedObservable
                .retryOnError("undo_accepted")
                .subscribe(undoAcceptedSubject::onNext)
                .addToDisposable(subscriptions)
    }

    private fun <T> Flowable<T>.retryOnError(requestTag: String): Flowable<T> {
        return this.doOnError {
            FirebaseCrashlytics.getInstance().log("E/$TAG: $requestTag error ${it.message}")
            FirebaseCrashlytics.getInstance().recordException(it)
        }
                .retry()
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
                socketService.disconnectFromGame(gameId)
                closed = true
            }
        }
    }

    fun submitMove(move: Point) {
        val encodedMove = Util.getSGFCoordinates(move)
        socketService.emit("game/move", createJsonObject {
            put("auth", gameAuth)
            put("game_id", gameId)
            put("player_id", getCurrentUserId())
            put("move", encodedMove)
        })
    }

    fun resign() {
        socketService.emit("game/resign", createJsonObject {
            put("auth", gameAuth)
            put("game_id", gameId)
            put("player_id", getCurrentUserId())
        })
    }

    fun rejectRemovedStones() {
        socketService.emit("game/removed_stones/reject", createJsonObject {
            put("auth", gameAuth)
            put("game_id", gameId)
            put("player_id", getCurrentUserId())
        })
    }

    fun submitRemovedStones(delta: Set<Point>, removing: Boolean) {
        val sb = StringBuilder()
        delta.forEach { sb.append(Util.getSGFCoordinates(it)) }
        socketService.emit("game/removed_stones/set", createJsonObject {
            put("auth", gameAuth)
            put("game_id", gameId)
            put("player_id", getCurrentUserId())
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
        socketService.emit("game/removed_stones/accept", createJsonObject {
            put("auth", gameAuth)
            put("game_id", gameId)
            put("player_id", getCurrentUserId())
            put("strict_seki_mode", false)
            put("stones", stones)
        })
    }

    fun sendMessage(message: String, moveNumber: Int) {
        socketService.emit("game/chat", createJsonObject {
            put("body", message)
            put("game_id", gameId)
            put("move_number", moveNumber)
            put("type", "main")
        })
    }

    fun acceptUndo(moveNo: Int) {
        socketService.emit("game/undo/accept", createJsonObject {
            put("game_id", gameId)
            put("move_number", moveNo)
            put("player_id", getCurrentUserId())
        })
    }

    fun requestUndo(moveNo: Int) {
        socketService.emit("game/undo/request", createJsonObject {
            put("game_id", gameId)
            put("move_number", moveNo)
            put("player_id", getCurrentUserId())
        })
    }

    fun abortGame() {
        socketService.emit("game/cancel", createJsonObject {
            put("game_id", gameId)
            put("player_id", getCurrentUserId())
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
//    val paused_since: Long?, // NEVER USE THIS as it is set even on unpaused games
    val pause: Pause? = null,
    var start_mode: Boolean = false,
    var black_time: Any?,// can be number or Time object
    var white_time: Any?// can be number or Time object
) {
    var receivedAt: Long = 0
    val whiteTimeSimple get() = (white_time as? Number)?.toLong()
    val blackTimeSimple get() = (black_time as? Number)?.toLong()
    val whiteTime
        get() = (white_time as? Map<*, *>)?.let { Time.fromMap(it) }
    val blackTime
        get() = (black_time as? Map<*, *>)?.let { Time.fromMap(it) }

}

data class Pause(
        val pause_control: Any?,
        val paused: Boolean?,
        val paused_since: Long?
)

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
        val removed: Any?,
        val stones: String?,
        val all_removed: String?
)

data class RemovedStonesAccepted(
        val player_id: Long?,
        val stones: String?,
        val players: Players?
)