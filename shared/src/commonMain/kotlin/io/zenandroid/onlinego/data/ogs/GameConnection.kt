package io.zenandroid.onlinego.data.ogs

import co.touchlab.kermit.Logger
import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.local.Message
import io.zenandroid.onlinego.data.model.ogs.Chat
import io.zenandroid.onlinego.data.model.ogs.GameData
import io.zenandroid.onlinego.data.model.ogs.Phase
import io.zenandroid.onlinego.data.repositories.ChatRepository
import io.zenandroid.onlinego.gamelogic.Util
import io.zenandroid.onlinego.utils.CrashReporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform.getKoin

private const val TAG = "GameConnection"

/**
 * Created by alex on 06/11/2017.
 */
class GameConnection(
  val userId: Long?,
  val gameId: Long,
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
) : AutoCloseable {
  internal var refCount = 0

  private val socketService: OGSWebSocketService = getKoin().get()
  private val chatRepository: ChatRepository = getKoin().get()

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
      Logger.e("$tag error ${it.message}", tag = TAG)
      CrashReporter.recordException(it)
      true
    }.collect { value ->
      action(value)
    }
  }

  override fun close() {
    socketService.release(this)
  }

  internal fun dispose() {
    scope.cancel()
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
