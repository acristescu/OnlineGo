package io.zenandroid.onlinego.ogs

import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.zenandroid.onlinego.gamelogic.Util
import io.zenandroid.onlinego.gamelogic.Util.isMyTurn
import io.zenandroid.onlinego.model.ogs.Game

/**
 * Created by alex on 08/11/2017.
 */
object ActiveGameService {

    private val activeGames = mutableMapOf<Long, Game>()

    private val myMoveCountSubject = BehaviorSubject.create<Int>()
    private val activeGamesSubject = PublishSubject.create<Game>()
    private val finishedGameSubject = PublishSubject.create<Game>()

    val myMoveCountObservable: Observable<Int>
        get() = myMoveCountSubject.distinctUntilChanged()

    val finishedGameObservable: Observable<Game>
        get() = finishedGameSubject.hide()

    val activeGamesObservable: Observable<Game>
        get() = Observable.concat(
                    Observable.fromIterable(activeGames.values),
                    activeGamesSubject.hide()
        )

    val activeGamesList: List<Game>
        get() = activeGames.values.toList()

    val myTurnGamesList: List<Game>
        get() = activeGames.values.filter(Util::isMyTurn).toList()

    init {
        OGSServiceImpl.instance.connectToNotifications().subscribe {
            if (it.phase == Game.Phase.FINISHED) {
                activeGames.remove(it.id)
                finishedGameSubject.onNext(it)
            } else {
                if (activeGames[it.id] == null) {
                    activeGamesSubject.onNext(it)
                }
                activeGames[it.id] = it
            }
            myMoveCountSubject.onNext(activeGames.values.count { isMyTurn(it) })
        }
    }
}