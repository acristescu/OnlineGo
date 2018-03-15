package io.zenandroid.onlinego.ogs

import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.zenandroid.onlinego.gamelogic.Util
import io.zenandroid.onlinego.gamelogic.Util.isMyTurn
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

    }

    private fun setActiveGames(games : List<Game>) {
        activeGames.clear()
        for(game in games) {
            activeGames[game.id] = game
        }
        myMoveCountSubject.onNext(activeGames.values.count { isMyTurn(it) })
    }

    fun fetchActiveGames() = OGSServiceImpl.instance
            .fetchActiveGames()
            .doOnSuccess(this::setActiveGames)
}