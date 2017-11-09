package io.zenandroid.onlinego.ogs

import io.reactivex.subjects.PublishSubject

/**
 * Created by alex on 08/11/2017.
 */
object ActiveGameService {

    private val activeGames = mutableMapOf<Long, ActiveGameNotification>()
    private var myMoveCount = 0
    private var lastPublishedMoveCount = -1

    val myMoveCountSubject = PublishSubject.create<Int>()

    init {
        OGSService.instance.connectToNotifications().subscribe({
            if(isMyTurn(activeGames[it.id])) {
                myMoveCount--
            }
            activeGames[it.id] = it
            if(isMyTurn(activeGames[it.id])) {
                myMoveCount++
            }
            if(lastPublishedMoveCount != myMoveCount) {
                lastPublishedMoveCount = myMoveCount
                myMoveCountSubject.onNext(myMoveCount)
            }
        })
    }

    private fun isMyTurn(gameNotification: ActiveGameNotification?): Boolean {
        return gameNotification?.player_to_move == OGSService.instance.uiConfig?.user?.id
    }
}