package io.zenandroid.onlinego.ogs

import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.gamelogic.Util
import io.zenandroid.onlinego.gamelogic.Util.isMyTurn
import io.zenandroid.onlinego.model.local.Clock
import io.zenandroid.onlinego.model.local.DbGame
import io.zenandroid.onlinego.model.local.DbPlayer
import io.zenandroid.onlinego.model.ogs.Game

/**
 * Created by alex on 08/11/2017.
 */
class ActiveGameRepository {

    private val activeGames = mutableMapOf<Long, Game>()

    private val myMoveCountSubject = BehaviorSubject.create<Int>()
    private val activeGamesSubject = PublishSubject.create<Game>()

    private val subscriptions = CompositeDisposable()

    private fun onNotification(game: Game) {
        if (game.phase == Game.Phase.FINISHED) {
            activeGames.remove(game.id)
        } else {
            if (activeGames[game.id] == null) {
                activeGamesSubject.onNext(game)
            }
            activeGames[game.id] = game
        }
        myMoveCountSubject.onNext(activeGames.values.count { isMyTurn(it) })
    }

    val myMoveCountObservable: Observable<Int>
        get() = myMoveCountSubject.distinctUntilChanged()

    val activeGamesObservable: Observable<Game>
        get() = activeGamesSubject.hide()

    val myTurnGamesList: List<Game>
        get() = activeGames.values.filter(Util::isMyTurn).toList()

    internal fun subscribe() {
        subscriptions.add(
                OGSServiceImpl.instance.connectToNotifications()
                        .subscribeOn(Schedulers.io())
                        .subscribe(this::onNotification)
        )
    }

    internal fun unsubscribe() {
        subscriptions.clear()
    }

    private fun setActiveGames(games : List<Game>) {
        OnlineGoApplication.instance.db.gameDao().insertAll(games.map {
            val whiteRating = (((it.white as? Map<*, *>)?.get("ratings") as? Map<*, *>)?.get("overall") as? Map<*, *>)?.get("rating") as? Double
            val blackRating = (((it.black as? Map<*, *>)?.get("ratings") as? Map<*, *>)?.get("overall") as? Map<*, *>)?.get("rating") as? Double
            DbGame(
                    id = it.id,
                    width = it.width,
                    height = it.height,
                    outcome = it.outcome,
                    playerToMoveId = it.json?.clock?.current_player,
                    whiteLost = it.white_lost,
                    blackLost = it.black_lost,
                    initialState = it.json?.initial_state,
                    whiteGoesFirst = it.json?.initial_player == "white",
                    moves = it.json?.moves,
                    removedStones = it.json?.removed,
                    whiteScore = it.json?.score?.white,
                    blackScore = it.json?.score?.black,
                    whitePlayer = DbPlayer(it.json!!.players!!.white!!.id, it.json!!.players!!.white!!.username!!, whiteRating),
                    blackPlayer = DbPlayer(it.json!!.players!!.black!!.id, it.json!!.players!!.black!!.username!!, blackRating),
                    clock = Clock.fromOGSClock(it.json!!.clock)
                    )
        })
        activeGames.clear()
        for(game in games) {
            activeGames[game.id] = game

            val gameConnection = OGSServiceImpl.instance.connectToGame(game.id)
            subscriptions.add(gameConnection)
            subscriptions.add(gameConnection.gameData
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe {
                        val whiteRating = (((game.white as? Map<*, *>)?.get("ratings") as? Map<*, *>)?.get("overall") as? Map<*, *>)?.get("rating") as? Double
                        val blackRating = (((game.black as? Map<*, *>)?.get("ratings") as? Map<*, *>)?.get("overall") as? Map<*, *>)?.get("rating") as? Double
                        OnlineGoApplication.instance.db.gameDao().update(
                                DbGame(
                                        id = game.id,
                                        width = it.width,
                                        height = it.height,
                                        outcome = it.outcome,
                                        playerToMoveId = it.clock.current_player,
                                        whiteLost = game.white_lost,
                                        blackLost = game.black_lost,
                                        initialState = it.initial_state,
                                        whiteGoesFirst = it.initial_player == "white",
                                        moves = it.moves,
                                        removedStones = it.removed,
                                        whiteScore = it.score?.white,
                                        blackScore = it.score?.black,
                                        whitePlayer = DbPlayer(it.players!!.white!!.id, it.players!!.white!!.username!!, whiteRating),
                                        blackPlayer = DbPlayer(it.players!!.black!!.id, it.players!!.black!!.username!!, blackRating),
                                        clock = Clock.fromOGSClock(it.clock)
                                )
                        )
                    })
            subscriptions.add(gameConnection.moves
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe { move ->
                        val whiteRating = (((game.white as? Map<*, *>)?.get("ratings") as? Map<*, *>)?.get("overall") as? Map<*, *>)?.get("rating") as? Double
                        val blackRating = (((game.black as? Map<*, *>)?.get("ratings") as? Map<*, *>)?.get("overall") as? Map<*, *>)?.get("rating") as? Double
                        OnlineGoApplication.instance.db.gameDao().update(
                                DbGame(
                                        id = game.id,
                                        width = game.width,
                                        height = game.height,
                                        outcome = game.outcome,
                                        playerToMoveId = game.json?.clock?.current_player,
                                        whiteLost = game.white_lost,
                                        blackLost = game.black_lost,
                                        initialState = game.json?.initial_state,
                                        whiteGoesFirst = game.json?.initial_player == "white",
                                        moves = game.json?.moves?.apply { add(move.move) },
                                        removedStones = game.json?.removed,
                                        whiteScore = game.json?.score?.white,
                                        blackScore = game.json?.score?.black,
                                        whitePlayer = DbPlayer(game.json!!.players!!.white!!.id, game.json!!.players!!.white!!.username!!, whiteRating),
                                        blackPlayer = DbPlayer(game.json!!.players!!.black!!.id, game.json!!.players!!.black!!.username!!, blackRating),
                                        clock = Clock.fromOGSClock(game.json!!.clock)
                                )
                        )
                    })
            subscriptions.add(gameConnection.clock
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
                    .subscribe {
                        val whiteRating = (((game.white as? Map<*, *>)?.get("ratings") as? Map<*, *>)?.get("overall") as? Map<*, *>)?.get("rating") as? Double
                        val blackRating = (((game.black as? Map<*, *>)?.get("ratings") as? Map<*, *>)?.get("overall") as? Map<*, *>)?.get("rating") as? Double
                        DbGame(
                                id = game.id,
                                width = game.width,
                                height = game.height,
                                outcome = game.outcome,
                                playerToMoveId = game.json?.clock?.current_player,
                                whiteLost = game.white_lost,
                                blackLost = game.black_lost,
                                initialState = game.json?.initial_state,
                                whiteGoesFirst = game.json?.initial_player == "white",
                                moves = game.json?.moves,
                                removedStones = game.json?.removed,
                                whiteScore = game.json?.score?.white,
                                blackScore = game.json?.score?.black,
                                whitePlayer = DbPlayer(game.json!!.players!!.white!!.id, game.json!!.players!!.white!!.username!!, whiteRating),
                                blackPlayer = DbPlayer(game.json!!.players!!.black!!.id, game.json!!.players!!.black!!.username!!, blackRating),
                                clock = Clock.fromOGSClock(it)
                        )
                    })
        }
        myMoveCountSubject.onNext(activeGames.values.count { isMyTurn(it) })
    }

    fun fetchActiveGames(): Flowable<List<DbGame>> {
        subscriptions.add(
            OGSServiceImpl.instance
                    .fetchActiveGames()
                    .subscribe(this::setActiveGames)
        )
        return OnlineGoApplication.instance.db.gameDao().getActiveGames()
    }
}