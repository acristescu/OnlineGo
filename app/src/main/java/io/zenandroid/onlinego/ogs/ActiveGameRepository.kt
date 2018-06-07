package io.zenandroid.onlinego.ogs

import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.gamelogic.Util
import io.zenandroid.onlinego.gamelogic.Util.isMyTurn
import io.zenandroid.onlinego.model.local.DbGame
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
                    blackScore = it.json?.score?.black
                    )
        })
        activeGames.clear()
        for(game in games) {
            activeGames[game.id] = game
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