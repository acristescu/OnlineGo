package io.zenandroid.onlinego.ogs

import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.zenandroid.onlinego.model.ogs.Game

/**
 * Created by alex on 08/11/2017.
 */
object ActiveGameService {

    private val activeGames = mutableMapOf<Long, Game>()
    private var myMoveCount = 0
    private var lastPublishedMoveCount = -1

    private val myMoveCountSubject = BehaviorSubject.create<Int>()
    private val activeGamesSubject = PublishSubject.create<Game>()

    val myMoveCountObservable: Observable<Int>
        get() = myMoveCountSubject.hide()

    val activeGamesObservable: Observable<Game>
        get() = Observable.concat(
                    Observable.fromIterable(activeGames.values),
                    activeGamesSubject.hide()
        )

    init {
        OGSService.instance.connectToNotifications().subscribe({
            if (it.phase == Game.Phase.FINISHED) {
                val oldGameRecord = activeGames.remove(it.id)
                if(isMyTurn(oldGameRecord)) {
                    myMoveCount--
                }
            } else {
                if (activeGames[it.id] == null) {
                    activeGamesSubject.onNext(it)
                }
                if (isMyTurn(activeGames[it.id])) {
                    myMoveCount--
                }
                activeGames[it.id] = it
                if (isMyTurn(activeGames[it.id])) {
                    myMoveCount++
                }
            }
            if(lastPublishedMoveCount != myMoveCount) {
                lastPublishedMoveCount = myMoveCount
                myMoveCountSubject.onNext(myMoveCount)
            }
        })
    }

    private fun isMyTurn(gameNotification: Game?): Boolean {
        return gameNotification?.player_to_move == OGSService.instance.uiConfig?.user?.id
    }
}