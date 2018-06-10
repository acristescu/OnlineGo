package io.zenandroid.onlinego.ogs

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
import io.zenandroid.onlinego.model.local.DbGame
import io.zenandroid.onlinego.model.ogs.Game

/**
 * Created by alex on 08/11/2017.
 */
class ActiveGameRepository {

    private val activeDbGames = mutableMapOf<Long, DbGame>()
    private val gameConnections = mutableSetOf<Long>()

    private val myMoveCountSubject = BehaviorSubject.create<Int>()

    private val subscriptions = CompositeDisposable()

    private fun onNotification(game: Game) {
        subscriptions.add(
            OGSServiceImpl.instance.fetchGame(game.id)
                    .subscribeOn(Schedulers.io())
                    .map(DbGame.Companion::fromOGSGame)
                    .doOnSuccess { OnlineGoApplication.instance.db.gameDao().insertAll(listOf(it)) }
                    .subscribe(this::connectToGame)
        )
    }

    val myMoveCountObservable: Observable<Int>
        get() = myMoveCountSubject.distinctUntilChanged()

    val myTurnGamesList: List<DbGame>
        get() = activeDbGames.values.filter(Util::isMyTurn).toList()

    internal fun subscribe() {
        subscriptions.add(
                OGSServiceImpl.instance.connectToNotifications()
                        .subscribeOn(Schedulers.io())
                        .subscribe(this::onNotification)
        )
    }

    internal fun unsubscribe() {
        subscriptions.clear()
        gameConnections.clear()
    }

    private fun connectToGame(game: DbGame) {
        if(gameConnections.contains(game.id)) {
            return
        }
        gameConnections.add(game.id)

        val gameConnection = OGSServiceImpl.instance.connectToGame(game.id)
        subscriptions.add(gameConnection)
        subscriptions.add(gameConnection.gameData
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.single())
                .subscribe {
                    game.apply {
                        outcome = it.outcome
                        playerToMoveId = it.clock.current_player
                        initialState = it.initial_state
                        whiteGoesFirst = it.initial_player == "white"
                        moves = it.moves.apply { forEach { if(it.size == 3) it.removeAt(it.lastIndex) } }
                        removedStones = it.removed
                        whiteScore = it.score?.white
                        blackScore = it.score?.black
                        clock = Clock.fromOGSClock(it.clock)
                    }
                    OnlineGoApplication.instance.db.gameDao().update(game)
                })
        subscriptions.add(gameConnection.moves
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.single())
                .subscribe { move ->
                    game.moves?.add(mutableListOf(move.move[0], move.move[1]))
                    OnlineGoApplication.instance.db.gameDao().update(game)
                })
        subscriptions.add(gameConnection.clock
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.single())
                .subscribe {
                    game.apply {
                        clock = Clock.fromOGSClock(it)
                        playerToMoveId = it.current_player
                    }
                    OnlineGoApplication.instance.db.gameDao().update(game)
                })
        subscriptions.add(gameConnection.phase
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.single())
                .subscribe {
                    game.apply {
                        phase = it
                    }
                    OnlineGoApplication.instance.db.gameDao().update(game)
                })
        subscriptions.add(gameConnection.removedStones
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.single())
                .subscribe {
                    game.apply {
                        removedStones = it.all_removed
                    }
                    OnlineGoApplication.instance.db.gameDao().update(game)
                })

        myMoveCountSubject.onNext(activeDbGames.values.count { isMyTurn(it) })
    }

    private fun setActiveGames(games : List<Game>) {
        val dbGames = games.map(DbGame.Companion::fromOGSGame)
        OnlineGoApplication.instance.db.gameDao().insertAll(dbGames)
        activeDbGames.clear()
        dbGames.forEach {
            activeDbGames[it.id] = it
            connectToGame(it)
        }
//        myMoveCountSubject.onNext(activeDbGames.values.count { isMyTurn(it) })
    }

    private fun fetchGameFromOGS(id: Long) {
        subscriptions.add(
                OGSServiceImpl.instance
                        .fetchGame(id)
                        .map(DbGame.Companion::fromOGSGame)
                        .map(::listOf)
                        .subscribe(OnlineGoApplication.instance.db.gameDao()::insertAll)
        )
    }

    fun monitorGame(id: Long): Flowable<DbGame> {
        // TODO: Maybe check if the data is fresh enough to warrant skipping this call?
        fetchGameFromOGS(id)

        return OnlineGoApplication.instance.db.gameDao().monitorGame(id)
                .doOnNext(this::connectToGame)
    }

    fun getGameSingle(id: Long): Single<DbGame> {
        return OnlineGoApplication.instance.db.gameDao().monitorGame(id).take(1).firstOrError()
    }

    fun fetchActiveGames(): Flowable<List<DbGame>> {
        subscriptions.add(
            OGSServiceImpl.instance
                    .fetchActiveGames()
                    .subscribe(this::setActiveGames)
        )
        return OnlineGoApplication.instance.db.gameDao().monitorActiveGames(OGSServiceImpl.instance.uiConfig?.user?.id)
    }

    fun fetchHistoricGames(): Flowable<List<DbGame>> {
        subscriptions.add(
                OGSServiceImpl.instance
                        .fetchHistoricGames()
                        .map { it.map(Game::id) }
                        .map { it - OnlineGoApplication.instance.db.gameDao().getHistoricGamesThatDontNeedUpdating(it) }
                        .flattenAsObservable { it -> it }
                        .flatMapSingle { OGSServiceImpl.instance.fetchGame(it) }
                        .map (DbGame.Companion::fromOGSGame)
                        .toList()
                        .subscribe(OnlineGoApplication.instance.db.gameDao()::insertAll)
        )
        return OnlineGoApplication.instance.db.gameDao().monitorHistoricGames(OGSServiceImpl.instance.uiConfig?.user?.id)
    }
}