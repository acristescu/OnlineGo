package io.zenandroid.onlinego.data.repositories

import android.util.Log
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.zenandroid.onlinego.data.model.ogs.SeekGraphChallenge
import io.zenandroid.onlinego.data.ogs.OGSWebSocketService
import io.zenandroid.onlinego.utils.addToDisposable

class SeekGraphRepository(
    private val socketService: OGSWebSocketService
): SocketConnectedRepository {

    private val subscriptions = CompositeDisposable()
    private var challenges = emptyMap<Long, SeekGraphChallenge>()
    val challengesSubject = BehaviorSubject.create<List<SeekGraphChallenge>>()

    override fun onSocketConnected() {
        socketService.connectToChallenges()
            .subscribeOn(Schedulers.io())
            .subscribe(this::storeChallenge) { Log.e("SeekGraphRepository", it.toString()) }
            .addToDisposable(subscriptions)
    }

    private fun storeChallenge(challenge: SeekGraphChallenge) {
        if (challenge.game_started) {
        } else if (challenge.delete != null) {
            challenges -= challenge.challenge_id!!
        } else {
            challenges += Pair(challenge.challenge_id!!, challenge)
        }
        challengesSubject.onNext(challenges.values.toList())
    }

    override fun onSocketDisconnected() {
        subscriptions.clear()
    }
}
