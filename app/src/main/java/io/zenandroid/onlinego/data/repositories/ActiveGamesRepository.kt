package io.zenandroid.onlinego.data.repositories

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.squareup.moshi.JsonEncodingException
import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.zenandroid.onlinego.data.db.GameDao
import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.local.Clock
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.data.model.ogs.GameData
import io.zenandroid.onlinego.data.model.ogs.OGSGame
import io.zenandroid.onlinego.data.model.ogs.Phase
import io.zenandroid.onlinego.data.ogs.Move
import io.zenandroid.onlinego.data.ogs.OGSClock
import io.zenandroid.onlinego.data.ogs.OGSRestService
import io.zenandroid.onlinego.data.ogs.OGSWebSocketService
import io.zenandroid.onlinego.data.ogs.RemovedStones
import io.zenandroid.onlinego.data.ogs.RemovedStonesAccepted
import io.zenandroid.onlinego.data.ogs.UndoRequested
import io.zenandroid.onlinego.utils.addToDisposable
import io.zenandroid.onlinego.utils.recordException
import io.zenandroid.onlinego.utils.timeLeftForCurrentPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import java.io.IOException
import java.security.InvalidParameterException
import java.util.concurrent.TimeUnit

/**
 * Created by alex on 08/11/2017.
 */
class ActiveGamesRepository(
  private val restService: OGSRestService,
  private val socketService: OGSWebSocketService,
  private val userSessionRepository: UserSessionRepository,
  private val gameDao: GameDao
) : SocketConnectedRepository {

  private val activeDbGames = mutableMapOf<Long, Game>()
  private val gameConnections = mutableSetOf<Long>()

  private val myMoveCountSubject = BehaviorSubject.create<Int>()

  private val subscriptions = CompositeDisposable()

  private fun onNotification(game: OGSGame) {
    if (gameDao.getGameNullable(game.id) == null) {
      FirebaseCrashlytics.getInstance()
        .log("ActiveGameRepository: New game found from active_game notification ${game.id}")
      restService.fetchGame(game.id)
        .subscribeOn(Schedulers.io())
        .observeOn(Schedulers.io())
        .map(Game.Companion::fromOGSGame)
        .retryWhen(this::retryIOException)
        .subscribe({
          gameDao.insertAllGames(listOf(it))
        }, { onError(it, "onNotification") })
        .addToDisposable(subscriptions)
    }
  }

  // Game where it is your turn, ordered by remaining time to play
  private val _myTurnGames = MutableStateFlow<List<Game>>(emptyList())
  val myTurnGames: StateFlow<List<Game>> = _myTurnGames.asStateFlow()

  override fun onSocketConnected() {
    refreshActiveGames()
      .subscribeOn(Schedulers.io())
      .observeOn(Schedulers.io())
      .subscribe({}, { onError(it, "refreshActiveGames") })
      .addToDisposable(subscriptions)
    socketService.connectToActiveGames()
      .subscribeOn(Schedulers.io())
      .subscribe(this::onNotification) { onError(it, "connectToActiveGames") }
      .addToDisposable(subscriptions)
    userSessionRepository.userIdObservable.toFlowable(BackpressureStrategy.LATEST)
      .flatMap { userId ->
        gameDao.monitorActiveGamesWithNewMessagesCount(userId).map { userId to it }
      }
      .distinctUntilChanged()
      .subscribeOn(Schedulers.io())
      .observeOn(Schedulers.io())
      .subscribe(
        { setActiveGames(it.first, it.second) }) {
        onError(it, "monitorActiveGamesWithNewMessagesCount")
      }
      .addToDisposable(subscriptions)
  }

  override fun onSocketDisconnected() {
    subscriptions.clear()
    synchronized(gameConnections) {
      gameConnections.clear()
    }
  }

  private fun connectToGame(baseGame: Game, includeChat: Boolean = true) {
    val game = baseGame.copy()
    synchronized(gameConnections) {
      if (gameConnections.contains(game.id)) {
        if (includeChat) {
          socketService.enableChatOnConnection(game.id)
        }
        return
      }
      gameConnections.add(game.id)
    }

    val gameConnection = socketService.connectToGame(game.id, includeChat)
    gameConnection.addToDisposable(subscriptions)
    gameConnection.gameData
      .subscribeOn(Schedulers.io())
      .observeOn(Schedulers.io())
      .subscribe(
        { onGameData(game.id, it) },
        { onError(it, "gameData") }
      )
      .addToDisposable(subscriptions)
    gameConnection.moves
      .subscribeOn(Schedulers.io())
      .observeOn(Schedulers.io())
      .subscribe(
        { onGameMove(game.id, it) },
        { onError(it, "moves") }
      )
      .addToDisposable(subscriptions)
    gameConnection.clock
      .subscribeOn(Schedulers.io())
      .observeOn(Schedulers.io())
      .subscribe(
        { onGameClock(game.id, it) },
        { onError(it, "clock") }
      )
      .addToDisposable(subscriptions)
    gameConnection.phase
      .subscribeOn(Schedulers.io())
      .observeOn(Schedulers.io())
      .subscribe(
        { onGamePhase(game.id, it) },
        { onError(it, "phase") }
      )
      .addToDisposable(subscriptions)
    gameConnection.removedStones
      .subscribeOn(Schedulers.io())
      .observeOn(Schedulers.io())
      .subscribe(
        { onGameRemovedStones(game.id, it) },
        { onError(it, "removedStones") }
      )
      .addToDisposable(subscriptions)
    gameConnection.undoRequested
      .subscribeOn(Schedulers.io())
      .observeOn(Schedulers.io())
      .subscribe(
        { onUndoRequested(game.id, it) },
        { onError(it, "undoRequested") }
      )
      .addToDisposable(subscriptions)
    gameConnection.removedStonesAccepted
      .subscribeOn(Schedulers.io())
      .observeOn(Schedulers.io())
      .subscribe(
        { onRemovedStonesAccepted(game.id, it) },
        { onError(it, "removedStonesAccepted") }
      )
      .addToDisposable(subscriptions)
    gameConnection.undoAccepted
      .subscribeOn(Schedulers.io())
      .observeOn(Schedulers.io())
      .subscribe(
        { onUndoAccepted(game.id, it.move_number, it.undo_move_count) },
        { onError(it, "undoRequested") }
      )
      .addToDisposable(subscriptions)
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

  @Synchronized
  private fun setActiveGames(userId: Long, games: List<Game>) {
    activeDbGames.clear()
    games.forEach {
      activeDbGames[it.id] = it
      connectToGame(it, false)
    }
    _myTurnGames.value =
      activeDbGames.values
        .filter { it.playerToMoveId != null && it.playerToMoveId == userId }
        .toList()
        .sortedBy { timeLeftForCurrentPlayer(it) }
    myMoveCountSubject.onNext(activeDbGames.values.count { it.playerToMoveId != null && it.playerToMoveId == userId })
  }

  fun refreshGameData(id: Long) {
    restService.fetchGame(id)
      .map(Game.Companion::fromOGSGame)
      .map(::listOf)
      .retryWhen(this::retryIOException)
      .subscribe(
        gameDao::insertAllGames,
        { onError(it, "monitorGame") }
      ).addToDisposable(subscriptions)
  }

  /**
   * Poll the server repeatedly until either your rating changes or a max number of attempts has
   * elapsed
   */
  fun pollServerForNewRating(id: Long, white: Boolean, historicRating: Double?) {
    var retryCount = 0
    restService.fetchGame(id)
      .map(Game.Companion::fromOGSGame)
      .retryWhen(this::retryIOException)
      .flatMap {
        if ((white && it.whitePlayer.rating != historicRating) || (!white && historicRating != it.blackPlayer.rating)) {
          Single.just(listOf(it))
        } else {
          Single.error(InvalidParameterException())
        }
      }.retryWhen {
        it.flatMap {
          if (it is InvalidParameterException && retryCount < 20) {
            retryCount++
            Flowable.timer(1, TimeUnit.SECONDS)
          } else {
            Flowable.error(it)
          }
        }
      }
      .subscribe(
        gameDao::insertAllGames,
        { onError(it, "pollServerForNewRating") }
      ).addToDisposable(subscriptions)
  }

  fun monitorGameFlow(id: Long): Flow<Game> {
    return gameDao.monitorGameFlow(id)
      .distinctUntilChanged()
      .onEach(this::connectToGame)
      .onStart { refreshGameData(id) }
      .flowOn(Dispatchers.IO)
  }

  private fun retryIOException(it: Flowable<Throwable>) =
    it.flatMap {
      when (it) {
        is JsonEncodingException -> Flowable.error(it)
        is IOException -> Flowable.timer(15, TimeUnit.SECONDS)
        else -> Flowable.error(it)
      }
    }

  fun getGameSingle(id: Long): Single<Game> {
    return gameDao.monitorGame(id).take(1).firstOrError()
  }

  fun refreshActiveGames(): Completable =
    userSessionRepository.userIdObservable
      .firstOrError()
      .flatMapCompletable { userId ->
        restService.fetchActiveGames()
          .map { it.map(Game.Companion::fromOGSGame) }
          .doOnSuccess(gameDao::insertAllGames)
          .doOnSuccess {
            FirebaseCrashlytics.getInstance().log("overview returned ${it.size} games")
          }
          .map { it.map(Game::id).toSet() }
          .map { gameDao.getActiveGameIds(userId) - it }
          .doOnSuccess(this::updateGamesThatFinishedSinceLastUpdate)
          .retryWhen(this::retryIOException)
          .ignoreElement()
      }

  private fun updateGamesThatFinishedSinceLastUpdate(gameIds: List<Long>) {
    FirebaseCrashlytics.getInstance()
      .log("Found ${gameIds.size} games that are neither active nor marked as finished")
    val games = mutableListOf<Game>()
    gameIds.forEach {
      var backoffMillis = 10000L
      while (true) {
        try {
          games += Game.fromOGSGame(
            restService.fetchGame(it).blockingGet()
          )
          break
        } catch (e: Exception) {
          // Update whatever games we have so far before handling the error
          if (games.isNotEmpty()) {
            gameDao.updateGames(games)
            games.clear()
          }

          // request is throttled
          if (e is retrofit2.HttpException && e.code() == 429) {
            FirebaseCrashlytics.getInstance().apply {
              setCustomKey("HIT_RATE_LIMITER", true)
              log("Hit rate limiter backing off $backoffMillis milliseconds")
            }
            Thread.sleep(backoffMillis)
            backoffMillis *= 2
          } else {
            throw e
          }
        }
      }
    }
    gameDao.updateGames(games)
  }

  fun monitorActiveGames(): Flowable<List<Game>> {
    return userSessionRepository.userIdObservable.toFlowable(BackpressureStrategy.LATEST).flatMap {
      gameDao.monitorActiveGamesWithNewMessagesCount(it)
        .distinctUntilChanged()
    }
  }

  private fun onError(t: Throwable, request: String) {
    var message = request
    if (t is retrofit2.HttpException) {
      message = "$request: ${t.response()?.errorBody()?.string()}"
      if (t.code() == 429) {
        FirebaseCrashlytics.getInstance().setCustomKey("HIT_RATE_LIMITER", true)
      }
    }
    recordException(Exception(message, t))
    Log.e("ActiveGameRespository", message, t)
  }
}
