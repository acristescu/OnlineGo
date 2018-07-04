package io.zenandroid.onlinego.ogs

import android.util.Log
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.gamelogic.Util
import io.zenandroid.onlinego.gamelogic.Util.isMyTurn
import io.zenandroid.onlinego.model.local.Clock
import io.zenandroid.onlinego.model.local.Game
import io.zenandroid.onlinego.model.ogs.GameData
import io.zenandroid.onlinego.model.ogs.OGSGame
import io.zenandroid.onlinego.model.ogs.Phase
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Created by alex on 08/11/2017.
 */
class ActiveGameRepository {

    private val activeDbGames = mutableMapOf<Long, Game>()
    private val gameConnections = mutableSetOf<Long>()
    private val connectedGameCache = hashMapOf<Long, Game>()

    private val myMoveCountSubject = BehaviorSubject.create<Int>()

    private val subscriptions = CompositeDisposable()

    private fun onNotification(game: OGSGame) {
        subscriptions.add(
            OGSServiceImpl.instance.fetchGame(game.id)
                    .subscribeOn(Schedulers.io())
                    .map(Game.Companion::fromOGSGame)
                    .retryWhen (this::retryIOException)
                    .subscribe({
                        OnlineGoApplication.instance.db.gameDao().insertAll(listOf(it))
                    }, { this.onError(it, "onNotification") })
        )
    }

    val myMoveCountObservable: Observable<Int>
        @Synchronized get() = myMoveCountSubject.distinctUntilChanged()

    val myTurnGamesList: List<Game>
        get() = activeDbGames.values.filter(Util::isMyTurn).toList()

    internal fun subscribe() {
        subscriptions.add(
                OGSServiceImpl.instance.connectToNotifications()
                        .subscribeOn(Schedulers.io())
                        .subscribe(this::onNotification) { this.onError(it, "connectToNotifications") }
        )
        subscriptions.add(
                OnlineGoApplication.instance.db.gameDao()
                        .monitorActiveGames(OGSServiceImpl.instance.uiConfig?.user?.id)
                        .subscribe(this::setActiveGames) { this.onError(it, "gameDao") }
        )
    }

    internal fun unsubscribe() {
        subscriptions.clear()
        gameConnections.clear()
    }

    private fun connectToGame(baseGame: Game) {
        val game = baseGame.copy()
        synchronized(connectedGameCache) {
            connectedGameCache[game.id] = game
        }
        if(gameConnections.contains(game.id)) {
            return
        }
        gameConnections.add(game.id)

        val gameConnection = OGSServiceImpl.instance.connectToGame(game.id)
        subscriptions.add(gameConnection)
        subscriptions.add(gameConnection.gameData
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.single())
                .subscribe (
                        { onGameData(game.id, it) },
                        { this.onError(it, "gameData") }
                ))
        subscriptions.add(gameConnection.moves
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.single())
                .subscribe (
                        { onGameMove(game.id, it) },
                        { this.onError(it, "moves") }
                ))
        subscriptions.add(gameConnection.clock
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.single())
                .subscribe (
                        { onGameClock(game.id, it) },
                        { this.onError(it, "clock") }
                ))
        subscriptions.add(gameConnection.phase
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.single())
                .subscribe (
                        { onGamePhase(game.id, it) },
                        { this.onError(it, "phase") }
                ))
        subscriptions.add(gameConnection.removedStones
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.single())
                .subscribe (
                        { onGameRemovedStones(game.id, it) },
                        { this.onError(it, "removedStones") }
                ))

    }

    private fun onGameRemovedStones(gameId: Long, it: RemovedStones) {
        synchronized(connectedGameCache) {
            connectedGameCache[gameId]?.apply {
                removedStones = it.all_removed
                OnlineGoApplication.instance.db.gameDao().update(this)
            }
        }
    }

    private fun onGamePhase(gameId: Long, newPhase: Phase) {
        synchronized(connectedGameCache) {
            connectedGameCache[gameId]?.apply {
                phase = newPhase
                OnlineGoApplication.instance.db.gameDao().update(this)
            }
        }
    }

    private fun onGameClock(gameId: Long, it: OGSClock) {
        synchronized(connectedGameCache) {
            connectedGameCache[gameId]?.apply {
                clock = Clock.fromOGSClock(it)
                playerToMoveId = it.current_player
                OnlineGoApplication.instance.db.gameDao().update(this)
            }
        }
    }

    private fun onGameData(gameId: Long, gameData: GameData) {
        synchronized(connectedGameCache) {
            connectedGameCache[gameId]?.apply {
                outcome = gameData.outcome
                playerToMoveId = gameData.clock.current_player
                initialState = gameData.initial_state
                whiteGoesFirst = gameData.initial_player == "white"
                moves = gameData.moves.map { mutableListOf(it[0].toInt(), it[1].toInt()) }.toMutableList()
                removedStones = gameData.removed
                whiteScore = gameData.score?.white
                blackScore = gameData.score?.black
                clock = Clock.fromOGSClock(gameData.clock)
                OnlineGoApplication.instance.db.gameDao().update(this)
            }
        }
    }

    private fun onGameMove(gameId: Long, move: Move ) {
        synchronized(connectedGameCache) {
            connectedGameCache[gameId]?.apply {
                moves?.add(mutableListOf(move.move[0].toInt(), move.move[1].toInt()))
                OnlineGoApplication.instance.db.gameDao().update(this)
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
        subscriptions.add(
                OGSServiceImpl.instance
                        .fetchGame(id)
                        .map(Game.Companion::fromOGSGame)
                        .map(::listOf)
                        .retryWhen (this::retryIOException)
                        .subscribe(OnlineGoApplication.instance.db.gameDao()::insertAll)
                        { this.onError(it, "monitorGame") }
        )

        return OnlineGoApplication.instance.db.gameDao().monitorGame(id)
                .doOnNext(this::connectToGame)
    }

    private fun retryIOException(it: Flowable<Throwable>) =
            it.map { it as? IOException ?: throw it }
                    .delay(15, TimeUnit.SECONDS)


    fun getGameSingle(id: Long): Single<Game> {
        return OnlineGoApplication.instance.db.gameDao().monitorGame(id).take(1).firstOrError()
    }

    fun fetchActiveGames(): Flowable<List<Game>> {
        subscriptions.add(
            OGSServiceImpl.instance
                    .fetchActiveGames()
                    .flattenAsObservable { it -> it }
                    .map (Game.Companion::fromOGSGame)
                    .toList()
                    .retryWhen (this::retryIOException)
                    .subscribe(OnlineGoApplication.instance.db.gameDao()::insertAll)
                    { this.onError(it, "fetchActiveGames") }
        )
        return OnlineGoApplication.instance.db.gameDao().monitorActiveGames(OGSServiceImpl.instance.uiConfig?.user?.id)
    }

    fun fetchHistoricGames(): Flowable<List<Game>> {
        subscriptions.add(
                OGSServiceImpl.instance
                        .fetchHistoricGames()
                        .map { it.map(OGSGame::id) }
                        .map { it - OnlineGoApplication.instance.db.gameDao().getHistoricGamesThatDontNeedUpdating(it) }
                        .flattenAsObservable { it -> it }
                        .flatMapSingle { OGSServiceImpl.instance.fetchGame(it) }
                        .map (Game.Companion::fromOGSGame)
                        .toList()
                        .retryWhen (this::retryIOException)
                        .subscribe(OnlineGoApplication.instance.db.gameDao()::insertAll)
                        { this.onError(it, "fetchHistoricGames") }
        )
        return OnlineGoApplication.instance.db.gameDao().monitorHistoricGames(OGSServiceImpl.instance.uiConfig?.user?.id)
    }

    private fun onError(t: Throwable, request: String) {
        Log.e("ActiveGameRespository", t.message, t)
        throw RuntimeException("ActiveGameRespository $request", t)
    }
}