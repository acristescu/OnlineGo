package io.zenandroid.onlinego.data.repositories

import co.touchlab.kermit.Logger
import io.zenandroid.onlinego.data.db.GameDao
import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.local.Clock
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.data.model.ogs.GameData
import io.zenandroid.onlinego.data.model.ogs.OGSGame
import io.zenandroid.onlinego.data.model.ogs.Phase
import io.zenandroid.onlinego.data.ogs.GameConnection
import io.zenandroid.onlinego.data.ogs.Move
import io.zenandroid.onlinego.data.ogs.OGSClock
import io.zenandroid.onlinego.data.ogs.OGSRestService
import io.zenandroid.onlinego.data.ogs.OGSWebSocketService
import io.zenandroid.onlinego.data.ogs.RemovedStones
import io.zenandroid.onlinego.data.ogs.RemovedStonesAccepted
import io.zenandroid.onlinego.data.ogs.UndoRequested
import io.zenandroid.onlinego.data.ogs.httpErrorBody
import io.zenandroid.onlinego.data.ogs.httpStatusCode
import io.zenandroid.onlinego.utils.CrashReporter
import io.zenandroid.onlinego.utils.timeLeftForCurrentPlayer
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import java.io.IOException
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.fetchAndUpdate
import kotlin.concurrent.atomics.update

/**
 * Created by alex on 08/11/2017.
 */
class ActiveGamesRepository(
  private val restService: OGSRestService,
  private val socketService: OGSWebSocketService,
  private val userSessionRepository: UserSessionRepository,
  private val gameDao: GameDao
) : SocketConnectedRepository {

  private val gameConnections = AtomicReference(persistentSetOf<Long>())
  private val trackedConnections = AtomicReference(persistentListOf<GameConnection>())

  private var flowScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  private fun onNotification(game: OGSGame) {
    if (game.ended != null) {
      // Note: this is a bug with the backend. When requesting a game that was ended ago
      // we get a new active_game notification, and we fetch the game twice
      return
    }
    if (gameDao.getGameNullable(game.id) == null) {
      Logger.i(
        "New game found from active_game notification ${game.id}",
        tag = "ActiveGamesRepository"
      )
      flowScope.launch {
        try {
          val fetchedGame = retryOnIOException { restService.fetchGame(game.id) }
          gameDao.insertAllGames(listOf(Game.fromOGSGame(fetchedGame)))
        } catch (e: Exception) {
          onError(e, "onNotification")
        }
      }
    }
  }

  // Game where it is your turn, ordered by remaining time to play
  private val _myTurnGames = MutableStateFlow<List<Game>>(emptyList())
  val myTurnGames: StateFlow<List<Game>> = _myTurnGames.asStateFlow()

  override fun onSocketConnected() {
    flowScope.launch {
      try {
        refreshActiveGames()
      } catch (e: Exception) {
        onError(e, "refreshActiveGames")
      }
    }
    flowScope.launch {
      try {
        socketService.connectToActiveGames().collect { onNotification(it) }
      } catch (e: Exception) {
        onError(e, "connectToActiveGames")
      }
    }
    flowScope.launch {
      try {
        userSessionRepository.userId
          .filterNotNull()
          .flatMapLatest { userId ->
            gameDao.monitorActiveGamesWithNewMessagesCount(userId).map { userId to it }
          }
          .distinctUntilChanged()
          .collect { setActiveGames(it.first, it.second) }
      } catch (e: Exception) {
        onError(e, "monitorActiveGamesWithNewMessagesCount")
      }
    }
  }

  override fun onSocketDisconnected() {
    trackedConnections.exchange(persistentListOf()).forEach { it.close() }
    flowScope.cancel()
    flowScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    gameConnections.store(persistentSetOf())
  }

  private suspend fun connectToGame(baseGame: Game, includeChat: Boolean = true) {
    val game = baseGame.copy()
    if (game.id in gameConnections.fetchAndUpdate { it.add(game.id) }) {
      if (includeChat) {
        socketService.enableChatOnConnection(game.id)
      }
      return
    }

    val gameConnection = socketService.connectToGame(game.id, includeChat)
    trackedConnections.update { it.add(gameConnection) }
    flowScope.launch {
      gameConnection.gameData.collect {
        try {
          onGameData(game.id, it)
        } catch (e: Exception) {
          onError(e, "gameData")
        }
      }
    }
    flowScope.launch {
      gameConnection.moves.collect {
        try {
          onGameMove(game.id, it)
        } catch (e: Exception) {
          onError(e, "moves")
        }
      }
    }
    flowScope.launch {
      gameConnection.clock.collect {
        try {
          onGameClock(game.id, it)
        } catch (e: Exception) {
          onError(e, "clock")
        }
      }
    }
    flowScope.launch {
      gameConnection.phase.collect {
        try {
          onGamePhase(game.id, it)
        } catch (e: Exception) {
          onError(e, "phase")
        }
      }
    }
    flowScope.launch {
      gameConnection.removedStones.collect {
        try {
          onGameRemovedStones(game.id, it)
        } catch (e: Exception) {
          onError(e, "removedStones")
        }
      }
    }
    flowScope.launch {
      gameConnection.undoRequested.collect {
        try {
          onUndoRequested(game.id, it)
        } catch (e: Exception) {
          onError(e, "undoRequested")
        }
      }
    }
    flowScope.launch {
      gameConnection.removedStonesAccepted.collect {
        try {
          onRemovedStonesAccepted(game.id, it)
        } catch (e: Exception) {
          onError(e, "removedStonesAccepted")
        }
      }
    }
    flowScope.launch {
      gameConnection.undoAccepted.collect {
        try {
          onUndoAccepted(game.id, it.move_number, it.undo_move_count)
        } catch (e: Exception) {
          onError(e, "undoAccepted")
        }
      }
    }
  }

  private fun onGameRemovedStones(gameId: Long, stones: RemovedStones) {
    gameDao.updateRemovedStones(gameId, stones.all_removed ?: "")
  }

  private fun onRemovedStonesAccepted(gameId: Long, accepted: RemovedStonesAccepted) {
    gameDao.updateRemovedStonesAccepted(
      gameId,
      accepted.players?.white?.accepted_stones,
      accepted.players?.black?.accepted_stones
    )
  }

  private fun onUndoRequested(gameId: Long, undoRequested: UndoRequested) {
    gameDao.updateUndoRequested(
      gameId,
      undoRequested.move_number,
      undoRequested.requested_by,
      undoRequested.undo_move_count
    )
  }

  private fun onUndoAccepted(gameId: Long, moveNo: Int, moveCount: Int) {
    gameDao.updateUndoAccepted(gameId, moveNo, moveCount)
  }

  private fun onGamePhase(gameId: Long, newPhase: Phase) {
    gameDao.updatePhase(gameId, newPhase)
  }

  private fun onGameClock(gameId: Long, clock: OGSClock) {
    gameDao.updateClock(
      id = gameId,
      playerToMoveId = clock.current_player,
      clock = Clock.fromOGSClock(clock)
    )
  }

  private fun onGameData(gameId: Long, gameData: GameData) {
    gameDao.updateGameData(
      id = gameId,
      outcome = gameData.outcome,
      phase = gameData.phase,
      playerToMoveId = gameData.clock?.current_player,
      initialState = gameData.initial_state,
      whiteGoesFirst = gameData.initial_player == "white",
      moves = gameData.moves.map { Cell((it[0] as Double).toInt(), (it[1] as Double).toInt()) },
      removedStones = gameData.removed,
      whiteScore = gameData.score?.white,
      blackScore = gameData.score?.black,
      clock = Clock.fromOGSClock(gameData.clock),
      undoRequested = gameData.undo_requested,
      whiteLost = gameData.winner?.let { it == gameData.black_player_id },
      blackLost = gameData.winner?.let { it == gameData.white_player_id },
      ended = gameData.end_time?.let { it * 1_000_000 }
    )
  }

  private fun onGameMove(gameId: Long, move: Move) {
    gameDao.addMoveToGame(
      gameId,
      move.move_number,
      Cell((move.move[0] as Double).toInt(), (move.move[1] as Double).toInt())
    )
  }

  private suspend fun setActiveGames(userId: Long, games: List<Game>) {
    games.forEach { connectToGame(it, false) }
    _myTurnGames.value =
      games
        .filter { it.playerToMoveId != null && it.playerToMoveId == userId }
        .sortedBy { timeLeftForCurrentPlayer(it) }
  }

  suspend fun refreshGameData(id: Long): Game? {
    try {
      val ogsGame = retryOnIOException { restService.fetchGame(id) }
      val game = Game.fromOGSGame(ogsGame)
      gameDao.insertAllGames(listOf(game))
      return game
    } catch (e: Exception) {
      onError(e, "refreshGameData")
      return null
    }
  }

  /**
   * Poll the server repeatedly until either your rating changes or a max number of attempts has
   * elapsed
   */
  fun pollServerForNewRating(id: Long, white: Boolean, historicRating: Double?) {
    flowScope.launch {
      try {
        var retryCount = 0
        while (retryCount < 20) {
          val game = retryOnIOException { restService.fetchGame(id) }
          val localGame = Game.fromOGSGame(game)
          if ((white && localGame.whitePlayer.rating != historicRating) || (!white && historicRating != localGame.blackPlayer.rating)) {
            gameDao.insertAllGames(listOf(localGame))
            return@launch
          }
          retryCount++
          delay(1000)
        }
      } catch (e: Exception) {
        onError(e, "pollServerForNewRating")
      }
    }
  }

  fun monitorGame(id: Long): Flow<Game> {
    return gameDao.monitorGame(id)
      .distinctUntilChanged()
      .onEach(this::connectToGame)
      .onStart { flowScope.launch { refreshGameData(id) } }
      .flowOn(Dispatchers.IO)
  }

  private suspend fun <T> retryOnIOException(block: suspend () -> T): T {
    while (true) {
      try {
        return block()
      } catch (e: SerializationException) {
        throw e
      } catch (e: IOException) {
        delay(15_000)
      }
    }
  }

  suspend fun refreshActiveGames() {
    val userId = userSessionRepository.userId.filterNotNull().first()
    val games = retryOnIOException { restService.fetchActiveGames() }
    val localGames = games.map(Game.Companion::fromOGSGame)
    gameDao.insertAllGames(localGames)
    Logger.i("overview returned ${localGames.size} games", tag = "ActiveGamesRepository")
    val activeGameIds = localGames.map(Game::id).toSet()
    val finishedGameIds = gameDao.getActiveGameIds(userId) - activeGameIds
    updateGamesThatFinishedSinceLastUpdate(finishedGameIds)
  }

  private suspend fun updateGamesThatFinishedSinceLastUpdate(gameIds: List<Long>) {
    Logger.i(
      "Found ${gameIds.size} games that are neither active nor marked as finished",
      tag = "ActiveGamesRepository"
    )
    val games = mutableListOf<Game>()
    gameIds.forEach {
      var backoffMillis = 10000L
      while (true) {
        try {
          games += Game.fromOGSGame(
            restService.fetchGame(it)
          )
          break
        } catch (e: Exception) {
          // Update whatever games we have so far before handling the error
          if (games.isNotEmpty()) {
            gameDao.updateGames(games)
            games.clear()
          }

          // request is throttled
          if (e.httpStatusCode == 429) {
            CrashReporter.setCustomKey("HIT_RATE_LIMITER", true)
            Logger.i(
              "Hit rate limiter backing off $backoffMillis milliseconds",
              tag = "ActiveGamesRepository"
            )
            delay(backoffMillis)
            backoffMillis *= 2
          } else {
            throw e
          }
        }
      }
    }
    gameDao.updateGames(games)
  }

  fun monitorActiveGames(): Flow<List<Game>> {
    return userSessionRepository.userId
      .filterNotNull()
      .flatMapLatest {
        gameDao.monitorActiveGamesWithNewMessagesCount(it)
          .distinctUntilChanged()
      }
  }

  private fun onError(t: Throwable, request: String) {
    if (t is CancellationException) {
      throw t
    }
    var message = request
    t.httpStatusCode?.let { code ->
      message = "$request: ${t.httpErrorBody}"
      if (code == 429) {
        CrashReporter.setCustomKey("HIT_RATE_LIMITER", true)
      }
    }
    CrashReporter.recordException(Exception(message, t))
    Logger.e(message, t, "ActiveGamesRepository")
  }
}