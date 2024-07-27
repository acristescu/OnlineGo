package io.zenandroid.onlinego.data.repositories

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.squareup.moshi.JsonEncodingException
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
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
import io.zenandroid.onlinego.gamelogic.Util
import io.zenandroid.onlinego.gamelogic.Util.isMyTurn
import io.zenandroid.onlinego.utils.addToDisposable
import io.zenandroid.onlinego.utils.recordException
import io.zenandroid.onlinego.utils.timeLeftForCurrentPlayer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
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
): SocketConnectedRepository {

    private val activeDbGames = mutableMapOf<Long, Game>()
    private val gameConnections = mutableSetOf<Long>()

    private val myMoveCountSubject = BehaviorSubject.create<Int>()

    private val subscriptions = CompositeDisposable()

    private fun onNotification(game: OGSGame) {
        if(gameDao.getGameMaybe(game.id).blockingGet() == null) {
            FirebaseCrashlytics.getInstance().log("ActiveGameRepository: New game found from active_game notification ${game.id}")
            restService.fetchGame(game.id)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.single())
                    .map(Game.Companion::fromOGSGame)
                    .retryWhen(this::retryIOException)
                    .subscribe({
                        gameDao.insertAllGames(listOf(it))
                    }, { onError(it, "onNotification") })
                    .addToDisposable(subscriptions)
        }
    }

    val myMoveCountObservable: Observable<Int>
        @Synchronized get() = myMoveCountSubject.distinctUntilChanged()

    val myTurnGamesList: List<Game>
        @Synchronized get() = activeDbGames.values.filter(Util::isMyTurn).toList()

    // Game where it is your turn, ordered by remaining time to play
    var myTurnGames by mutableStateOf(emptyList<Game>())

    override fun onSocketConnected() {
        refreshActiveGames()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.single())
                .subscribe({}, { onError(it, "refreshActiveGames") })
                .addToDisposable(subscriptions)
        socketService.connectToActiveGames()
                .subscribeOn(Schedulers.io())
                .subscribe(this::onNotification) { onError(it, "connectToActiveGames") }
                .addToDisposable(subscriptions)
        gameDao
            .monitorActiveGamesWithNewMessagesCount(userSessionRepository.userId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this::setActiveGames) { onError(it, "monitorActiveGamesWithNewMessagesCount") }
            .addToDisposable(subscriptions)
    }

    override fun onSocketDisconnected() {
        subscriptions.clear()
        gameConnections.clear()
    }

    @Synchronized
    private fun isGameActive(id: Long) =
            activeDbGames.containsKey(id)

    private fun connectToGame(baseGame: Game, includeChat: Boolean = true) {
        val game = baseGame.copy()
        synchronized(gameConnections) {
            if (gameConnections.contains(game.id)) {
                if(includeChat) {
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
                .observeOn(Schedulers.single())
                .subscribe (
                        { onGameData(game.id, it) },
                        { onError(it, "gameData") }
                )
                .addToDisposable(subscriptions)
        gameConnection.moves
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.single())
                .subscribe (
                        { onGameMove(game.id, it) },
                        { onError(it, "moves") }
                )
                .addToDisposable(subscriptions)
        gameConnection.clock
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.single())
                .subscribe (
                        { onGameClock(game.id, it) },
                        { onError(it, "clock") }
                )
                .addToDisposable(subscriptions)
        gameConnection.phase
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.single())
                .subscribe (
                        { onGamePhase(game.id, it) },
                        { onError(it, "phase") }
                )
                .addToDisposable(subscriptions)
        gameConnection.removedStones
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.single())
                .subscribe (
                        { onGameRemovedStones(game.id, it) },
                        { onError(it, "removedStones") }
                )
                .addToDisposable(subscriptions)
        gameConnection.undoRequested
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.single())
                .subscribe (
                        { onUndoRequested(game.id, it) },
                        { onError(it, "undoRequested") }
                )
                .addToDisposable(subscriptions)
        gameConnection.removedStonesAccepted
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.single())
                .subscribe (
                        { onRemovedStonesAccepted(game.id, it) },
                        { onError(it, "removedStonesAccepted") }
                )
                .addToDisposable(subscriptions)
        gameConnection.undoAccepted
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.single())
                .subscribe (
                        { onUndoAccepted(game.id, it) },
                        { onError(it, "undoRequested") }
                )
                .addToDisposable(subscriptions)
    }

    private fun onGameRemovedStones(gameId: Long, stones: RemovedStones) {
        gameDao.updateRemovedStones(gameId, stones.all_removed ?: "")
    }

    private fun onRemovedStonesAccepted(gameId: Long, accepted: RemovedStonesAccepted) {
        gameDao.updateRemovedStonesAccepted(gameId, accepted.players?.white?.accepted_stones, accepted.players?.black?.accepted_stones)
    }

    private fun onUndoRequested(gameId: Long, moveNo: Int) {
        gameDao.updateUndoRequested(gameId, moveNo)
    }

    private fun onUndoAccepted(gameId: Long, moveNo: Int) {
        gameDao.updateUndoAccepted(gameId, moveNo)
    }

    private fun onGamePhase(gameId: Long, newPhase: Phase) {
        gameDao.updatePhase(gameId, newPhase)
    }

    private fun onGameClock(gameId: Long, clock: OGSClock) {
        gameDao.updateClock(
                id = gameId,
                playerToMoveId = clock.current_player,
                clock = Clock.fromOGSClock(clock),
                pause = clock.pause
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
        gameDao.addMoveToGame(gameId, move.move_number, Cell((move.move[0] as Double).toInt(), (move.move[1] as Double).toInt()))
    }

    @Synchronized
    private fun setActiveGames(games : List<Game>) {
        activeDbGames.clear()
        games.forEach {
            activeDbGames[it.id] = it
            connectToGame(it, false)
        }
        myTurnGames = activeDbGames.values.filter(Util::isMyTurn).toList().sortedBy { timeLeftForCurrentPlayer(it) }
        myMoveCountSubject.onNext(activeDbGames.values.count { isMyTurn(it) })
    }

    fun monitorGame(id: Long): Flowable<Game> {
        // TODO: Maybe check if the data is fresh enough to warrant skipping this call?
        restService.fetchGame(id)
                .map(Game.Companion::fromOGSGame)
                .map(::listOf)
                .retryWhen (this::retryIOException)
                .subscribe(
                        gameDao::insertAllGames,
                        { onError(it, "monitorGame") }
                ).addToDisposable(subscriptions)

        return gameDao.monitorGame(id)
                .doOnNext(this::connectToGame)
    }

    fun refreshGameData(id: Long) {
        restService.fetchGame(id)
            .map(Game.Companion::fromOGSGame)
            .map(::listOf)
            .retryWhen (this::retryIOException)
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
            .retryWhen (this::retryIOException)
            .flatMap {
                if((white && it.whitePlayer.rating != historicRating) || (!white && historicRating != it.blackPlayer.rating)) {
                    Single.just(listOf(it))
                } else {
                    Single.error(InvalidParameterException())
                }
            }.retryWhen {
                it.flatMap {
                    if(it is InvalidParameterException && retryCount < 10) {
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
        refreshGameData(id)

        return gameDao.monitorGameFlow(id)
            .onEach(this::connectToGame)
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
            restService.fetchActiveGames()
                    .map { it.map (Game.Companion::fromOGSGame)}
                    .doOnSuccess(gameDao::insertAllGames)
                    .doOnSuccess { FirebaseCrashlytics.getInstance().log("overview returned ${it.size} games") }
                    .map { it.map(Game::id).toSet() }
                    .map { gameDao.getActiveGameIds(userSessionRepository.userId) - it }
                    .doOnSuccess (this::updateGamesThatFinishedSinceLastUpdate)
                    .retryWhen (this::retryIOException)
                    .ignoreElement()

    private fun updateGamesThatFinishedSinceLastUpdate(gameIds: List<Long>) {
        FirebaseCrashlytics.getInstance().log("Found ${gameIds.size} games that are neither active nor marked as finished")
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
                    if(games.isNotEmpty()) {
                        gameDao.updateGames(games)
                        games.clear()
                    }

                    // request is throttled
                    if(e is retrofit2.HttpException && e.code() == 429) {
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
        return gameDao.monitorActiveGamesWithNewMessagesCount(userSessionRepository.userId)
                .distinctUntilChanged()
    }

    private fun onError(t: Throwable, request: String) {
        var message = request
        if(t is retrofit2.HttpException) {
            message = "$request: ${t.response()?.errorBody()?.string()}"
            if(t.code() == 429) {
                FirebaseCrashlytics.getInstance().setCustomKey("HIT_RATE_LIMITER", true)
            }
        }
        recordException(Exception(message, t))
        Log.e("ActiveGameRespository", message, t)
    }
}
