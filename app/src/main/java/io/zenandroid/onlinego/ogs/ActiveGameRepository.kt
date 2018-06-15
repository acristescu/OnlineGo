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
import io.zenandroid.onlinego.model.ogs.OGSGame
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Created by alex on 08/11/2017.
 */
class ActiveGameRepository {

    private val activeDbGames = mutableMapOf<Long, Game>()
    private val gameConnections = mutableSetOf<Long>()

    private val myMoveCountSubject = BehaviorSubject.create<Int>()

    private val subscriptions = CompositeDisposable()

    private fun onNotification(game: OGSGame) {
        subscriptions.add(
            OGSServiceImpl.instance.fetchGame(game.id)
                    .subscribeOn(Schedulers.io())
                    .map(Game.Companion::fromOGSGame)
                    .subscribe({
                        OnlineGoApplication.instance.db.gameDao().insertAll(listOf(it))
                    }, this::onError)
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
                        .subscribe(this::onNotification, this::onError)
        )
        subscriptions.add(
                OnlineGoApplication.instance.db.gameDao()
                        .monitorActiveGames(OGSServiceImpl.instance.uiConfig?.user?.id)
                        .subscribe(this::setActiveGames, this::onError)
        )
    }

    internal fun unsubscribe() {
        subscriptions.clear()
        gameConnections.clear()
    }

    private fun connectToGame(game: Game) {
        if(gameConnections.contains(game.id)) {
            return
        }
        gameConnections.add(game.id)

        val gameConnection = OGSServiceImpl.instance.connectToGame(game.id)
        subscriptions.add(gameConnection)
        subscriptions.add(gameConnection.gameData
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.single())
                .subscribe ({
                    game.apply {
                        outcome = it.outcome
                        playerToMoveId = it.clock.current_player
                        initialState = it.initial_state
                        whiteGoesFirst = it.initial_player == "white"
                        moves = it.moves.map { mutableListOf(it[0].toInt(), it[1].toInt()) }.toMutableList()
                        removedStones = it.removed
                        whiteScore = it.score?.white
                        blackScore = it.score?.black
                        clock = Clock.fromOGSClock(it.clock)
                    }
                    OnlineGoApplication.instance.db.gameDao().update(game)
                }, this::onError))
        subscriptions.add(gameConnection.moves
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.single())
                .subscribe ({ move ->
                    game.moves?.add(mutableListOf(move.move[0].toInt(), move.move[1].toInt()))
                    OnlineGoApplication.instance.db.gameDao().update(game)
                }, this::onError))
        subscriptions.add(gameConnection.clock
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.single())
                .subscribe ({
                    game.apply {
                        clock = Clock.fromOGSClock(it)
                        playerToMoveId = it.current_player
                    }
                    OnlineGoApplication.instance.db.gameDao().update(game)
                }, this::onError))
        subscriptions.add(gameConnection.phase
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.single())
                .subscribe ({
                    game.apply {
                        phase = it
                    }
                    OnlineGoApplication.instance.db.gameDao().update(game)
                }, this::onError))
        subscriptions.add(gameConnection.removedStones
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.single())
                .subscribe ({
                    game.apply {
                        removedStones = it.all_removed
                    }
                    OnlineGoApplication.instance.db.gameDao().update(game)
                }, this::onError))

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

    private fun fetchGameFromOGS(id: Long) {

    }

    fun monitorGame(id: Long): Flowable<Game> {
        // TODO: Maybe check if the data is fresh enough to warrant skipping this call?
        subscriptions.add(
                OGSServiceImpl.instance
                        .fetchGame(id)
                        .map(Game.Companion::fromOGSGame)
                        .map(::listOf)
                        .retryWhen (this::retryIOException)
                        .subscribe(OnlineGoApplication.instance.db.gameDao()::insertAll, this::onError)
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
                    .subscribe(OnlineGoApplication.instance.db.gameDao()::insertAll, this::onError)
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
                        .subscribe(OnlineGoApplication.instance.db.gameDao()::insertAll, this::onError)
        )
        return OnlineGoApplication.instance.db.gameDao().monitorHistoricGames(OGSServiceImpl.instance.uiConfig?.user?.id)
    }

    private fun onError(t: Throwable) {
        Log.e("ActiveGameRespository", t.message, t)
        throw t
    }
}