package io.zenandroid.onlinego.data.ogs

import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.local.Message
import io.zenandroid.onlinego.data.model.local.Score
import io.zenandroid.onlinego.data.model.local.Time
import io.zenandroid.onlinego.data.model.ogs.Chat
import io.zenandroid.onlinego.data.model.ogs.GameData
import io.zenandroid.onlinego.data.model.ogs.OGSPlayer
import io.zenandroid.onlinego.data.model.ogs.Phase
import io.zenandroid.onlinego.data.repositories.ChatRepository
import io.zenandroid.onlinego.gamelogic.Util
import io.zenandroid.onlinego.utils.recordException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext.get
import java.io.Closeable

private const val TAG = "GameConnection"

/**
 * Created by alex on 06/11/2017.
 */
class GameConnection(
  val userId: Long?,
  val gameId: Long,
  private val connectionLock: Any,
  var includeChat: Boolean,
  gameDataFlow: Flow<GameData>,
  movesFlow: Flow<Move>,
  clockFlow: Flow<OGSClock>,
  phaseFlow: Flow<Phase>,
  removedStonesFlow: Flow<RemovedStones>,
  chatFlow: Flow<Chat>,
  undoRequestedFlow: Flow<UndoRequested>,
  removedStonesAcceptedFlow: Flow<RemovedStonesAccepted>,
  undoAcceptedFlow: Flow<UndoAccepted>
) : Closeable {
  private var closed = false
  private var counter = 0

  private val socketService: OGSWebSocketService = get().get()
  private val chatRepository: ChatRepository = get().get()

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  private val _gameData = MutableSharedFlow<GameData>(extraBufferCapacity = 64)
  private val _moves = MutableSharedFlow<Move>(extraBufferCapacity = 64)
  private val _clock = MutableSharedFlow<OGSClock>(extraBufferCapacity = 64)
  private val _phase = MutableSharedFlow<Phase>(extraBufferCapacity = 64)
  private val _removedStones = MutableSharedFlow<RemovedStones>(extraBufferCapacity = 64)
  private val _undoRequested = MutableSharedFlow<UndoRequested>(extraBufferCapacity = 64)
  private val _removedStonesAccepted =
    MutableSharedFlow<RemovedStonesAccepted>(extraBufferCapacity = 64)
  private val _undoAccepted = MutableSharedFlow<UndoAccepted>(extraBufferCapacity = 64)

  val gameData: SharedFlow<GameData> = _gameData.asSharedFlow()
  val moves: SharedFlow<Move> = _moves.asSharedFlow()
  val clock: SharedFlow<OGSClock> = _clock.asSharedFlow()
  val phase: SharedFlow<Phase> = _phase.asSharedFlow()
  val removedStones: SharedFlow<RemovedStones> = _removedStones.asSharedFlow()
  val undoRequested: SharedFlow<UndoRequested> = _undoRequested.asSharedFlow()
  val removedStonesAccepted: SharedFlow<RemovedStonesAccepted> =
    _removedStonesAccepted.asSharedFlow()
  val undoAccepted: SharedFlow<UndoAccepted> = _undoAccepted.asSharedFlow()

  var gameAuth: String? = null

  init {
    scope.launch {
      gameDataFlow.collectWithRetry("gamedata") {
        gameAuth = it.auth; _gameData.emit(it)
      }
    }
    scope.launch { movesFlow.collectWithRetry("moves") { _moves.emit(it) } }
    scope.launch { clockFlow.collectWithRetry("clock") { _clock.emit(it) } }
    scope.launch { phaseFlow.collectWithRetry("phase") { _phase.emit(it) } }
    scope.launch { removedStonesFlow.collectWithRetry("removed_stones") { _removedStones.emit(it) } }
    scope.launch {
      chatFlow.collectWithRetry("chat") {
        chatRepository.addMessage(Message.fromOGSMessage(it, gameId))
      }
    }
    scope.launch { undoRequestedFlow.collectWithRetry("undo_requested") { _undoRequested.emit(it) } }
    scope.launch {
      removedStonesAcceptedFlow.collectWithRetry("removed_stones_accepted") {
        _removedStonesAccepted.emit(
          it
        )
      }
    }
    scope.launch { undoAcceptedFlow.collectWithRetry("undo_accepted") { _undoAccepted.emit(it) } }
  }

  private suspend fun <T> Flow<T>.collectWithRetry(tag: String, action: suspend (T) -> Unit) {
    retry {
      FirebaseCrashlytics.getInstance().log("E/$TAG: $tag error ${it.message}")
      recordException(it)
      true
    }.collect { value ->
      action(value)
    }
  }

  override fun close() {
    decrementCounter()
  }

  fun incrementCounter() {
    synchronized(connectionLock) {
      FirebaseCrashlytics.getInstance().log("Acquired connection lock incrementCounter")
      counter++
      FirebaseCrashlytics.getInstance().log("Released connection lock incrementCounter")
    }
  }

  fun decrementCounter() {
    synchronized(connectionLock) {
      FirebaseCrashlytics.getInstance().log("Acquired connection lock decrementCounter")
      counter--
      if (counter == 0) {
        scope.cancel()
        socketService.disconnectFromGame(gameId)
        closed = true
      }
      FirebaseCrashlytics.getInstance().log("Released connection lock decrementCounter")
    }
  }

  fun submitMove(move: Cell) {
    val encodedMove = Util.getSGFCoordinates(move)
    socketService.emit("game/move") {
      "auth" - gameAuth
      "game_id" - gameId
      "player_id" - userId
      "move" - encodedMove
    }
  }

  fun resign() {
    socketService.emit("game/resign") {
      "auth" - gameAuth
      "game_id" - gameId
      "player_id" - userId
    }
  }

  fun rejectRemovedStones() {
    socketService.emit("game/removed_stones/reject") {
      "auth" - gameAuth
      "game_id" - gameId
      "player_id" - userId
    }
  }

  fun submitRemovedStones(delta: Set<Cell>, removing: Boolean) {
    val sb = StringBuilder()
    delta.forEach { sb.append(Util.getSGFCoordinates(it)) }
    socketService.emit("game/removed_stones/set") {
      "auth" - gameAuth
      "game_id" - gameId
      "player_id" - userId
      "removed" - removing
      "stones" - sb.toString()
    }
  }

  fun acceptRemovedStones(removedSpots: Set<Cell>) {
    val stones = removedSpots
      .asSequence()
      .sortedWith(compareBy(Cell::y, Cell::x))
      .joinToString(separator = "") {
        Util.getSGFCoordinates(it)
      }
    socketService.emit("game/removed_stones/accept") {
      "auth" - gameAuth
      "game_id" - gameId
      "player_id" - userId
      "strict_seki_mode" - false
      "stones" - stones
    }
  }

  fun sendMessage(message: String, moveNumber: Int) {
    socketService.emit("game/chat") {
      "body" - message
      "game_id" - gameId
      "move_number" - moveNumber
      "type" - "main"
    }
  }

  fun acceptUndo(moveNo: Int) {
    socketService.emit("game/undo/accept") {
      "game_id" - gameId
      "move_number" - moveNo
      "player_id" - userId
    }
  }

  fun requestUndo(moveNo: Int) {
    socketService.emit("game/undo/request") {
      "game_id" - gameId
      "move_number" - moveNo
      "player_id" - userId
    }
  }

  fun abortGame() {
    socketService.emit("game/cancel") {
      "game_id" - gameId
      "player_id" - userId
    }
  }
}

data class Scores(
  var white: Score? = null,
  var black: Score? = null
)

data class OGSClock(
  var game_id: Long,
  var current_player: Long,
  var black_player_id: Long,
  var white_player_id: Long,
  var title: String? = null,
  var last_move: Long,
  var expiration: Double? = null,
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

data class Players(
  var white: OGSPlayer? = null,
  var black: OGSPlayer? = null
)

data class TimeControl(
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
  val move: MutableList<Any?>
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

data class UndoAccepted(
  val move_number: Int,
  val undo_move_count: Int
)

data class UndoRequested(
  val move_number: Int,
  val requested_by: Long,
  val undo_move_count: Int,
)