package io.zenandroid.onlinego.data.repositories

import android.util.Log
import com.crashlytics.android.Crashlytics
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.data.db.GameDao
import io.zenandroid.onlinego.utils.addToDisposable
import io.zenandroid.onlinego.gamelogic.Util
import io.zenandroid.onlinego.gamelogic.Util.isMyTurn
import io.zenandroid.onlinego.data.model.local.Clock
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.data.model.ogs.GameData
import io.zenandroid.onlinego.data.model.ogs.OGSGame
import io.zenandroid.onlinego.data.model.ogs.Phase
import io.zenandroid.onlinego.data.ogs.*
import java.io.IOException
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
    private val connectedGameCache = hashMapOf<Long, Game>()

    private val myMoveCountSubject = BehaviorSubject.create<Int>()

    private val subscriptions = CompositeDisposable()

    private fun onNotification(game: OGSGame) {
        if(gameDao.getGameMaybe(game.id).blockingGet() == null) {
            Crashlytics.log(Log.VERBOSE, "ActiveGameRepository", "New game found from active_game notification ${game.id}")
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
                .subscribe(this::setActiveGames) { onError(it, "gameDao") }
                .addToDisposable(subscriptions)
    }

    override fun onSocketDisconnected() {
        subscriptions.clear()
        gameConnections.clear()
    }

    @Synchronized
    private fun isGameActive(id: Long) =
            activeDbGames.containsKey(id)

    internal fun connectToGame(baseGame: Game) {
        val game = baseGame.copy()
        synchronized(connectedGameCache) {
            connectedGameCache[game.id] = game
        }
        if(gameConnections.contains(game.id)) {
            return
        }
        gameConnections.add(game.id)

        val gameConnection = socketService.connectToGame(game.id)
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
                moves = gameData.moves.map { mutableListOf(it[0].toInt(), it[1].toInt()) }.toMutableList(),
                removedStones = gameData.removed,
                whiteScore = gameData.score?.white,
                blackScore = gameData.score?.black,
                clock = Clock.fromOGSClock(gameData.clock),
                undoRequested = gameData.undo_requested,
                whiteLost = gameData.winner?.let { it == gameData.black_player_id },
                blackLost = gameData.winner?.let { it == gameData.white_player_id },
                ended = gameData.end_time?.let { it * 1000 }
        )
    }

    private fun onGameMove(gameId: Long, move: Move) {
        synchronized(connectedGameCache) {
            connectedGameCache[gameId]?.let { game ->
                game.moves?.let {
                    val newMoves = it.toMutableList().apply {
                        add(mutableListOf(move.move[0].toInt(), move.move[1].toInt()))
                    }
                    gameDao.updateMoves(game.id, newMoves)
                }
            }
        }
    }

    @Synchronized
    private fun setActiveGames(games : List<Game>) {
        activeDbGames.clear()
        games.forEach {
            activeDbGames[it.id] = it
            connectToGame(it)
        }
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

    private fun retryIOException(it: Flowable<Throwable>) =
            it.flatMap {
                when (it) {
                    is IOException -> Flowable.timer(15, TimeUnit.SECONDS)
                    else -> Flowable.error<Long>(it)
                }
            }

    fun getGameSingle(id: Long): Single<Game> {
        return gameDao.monitorGame(id).take(1).firstOrError()
    }

    fun refreshActiveGames(): Completable =
            restService.fetchActiveGames()
                    .map { it.map (Game.Companion::fromOGSGame)}
                    .doOnSuccess(gameDao::insertAllGames)
                    .doOnSuccess { Crashlytics.log("overview returned ${it.size} games") }
                    .map { it.map(Game::id) }
                    .map { gameDao.getGamesThatShouldBeFinished(userSessionRepository.userId, it) }
                    .doOnSuccess { Crashlytics.log("Found ${it.size} games that are neither active nor marked as finished") }
                    .flattenAsObservable { it }
                    .flatMapSingle { restService.fetchGame(it) }
                    .map (Game.Companion::fromOGSGame)
                    .doOnNext { if(it.phase != Phase.FINISHED) Crashlytics.logException(Exception("Game ${it.id} ${it.phase} was not returned by overview but is not yet finished")) }
                    .toList()
                    .doOnSuccess(gameDao::updateGames)
                    .retryWhen (this::retryIOException)
                    .ignoreElement()

    fun monitorActiveGames(): Flowable<List<Game>> {
        return gameDao.monitorActiveGamesWithNewMessagesCount(userSessionRepository.userId)
                .distinctUntilChanged()
    }

    private fun onError(t: Throwable, request: String) {
        var message = request
        if(t is retrofit2.HttpException) {
            message = "$request: ${t.response()?.errorBody()?.string()}"
            if(t.code() == 429) {
                Crashlytics.setBool("HIT_RATE_LIMITER", true)
            }
        }
        Crashlytics.logException(Exception(message, t))
        Log.e("ActiveGameRespository", message, t)
    }
}