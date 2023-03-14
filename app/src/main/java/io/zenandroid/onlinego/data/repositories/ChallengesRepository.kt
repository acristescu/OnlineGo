package io.zenandroid.onlinego.data.repositories

import android.util.Log
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.zenandroid.onlinego.data.db.GameDao
import io.zenandroid.onlinego.data.model.local.Challenge
import io.zenandroid.onlinego.data.model.ogs.OGSChallenge
import io.zenandroid.onlinego.data.ogs.OGSRestService
import io.zenandroid.onlinego.data.ogs.OGSWebSocketService
import io.zenandroid.onlinego.utils.addToDisposable
import io.zenandroid.onlinego.utils.recordException

class ChallengesRepository(
        private val restService: OGSRestService,
        private val socketService: OGSWebSocketService,
        private val dao: GameDao
): SocketConnectedRepository {

    private val disposables = CompositeDisposable()
    private val TAG = ChallengesRepository::class.java.simpleName

    override fun onSocketConnected() {
        refreshChallenges()
                .subscribe({}, this::onError)
                .addToDisposable(disposables)
        socketService.connectToUIPushes()
                .filter { it.event == "challenge-list-updated" }
                .flatMapCompletable { refreshChallenges() }
                .subscribe({}, this::onError)
                .addToDisposable(disposables)
    }

    private fun storeChallenges(challenges: List<OGSChallenge>) {
        dao.replaceAllChallenges(challenges.map { Challenge.fromOGSChallenge (it) })
    }

    fun refreshChallenges() : Completable {
        Log.i(TAG, "Fetching challenges")
        return restService.fetchChallenges()
                .doOnSuccess(this::storeChallenges)
                .ignoreElement()
    }

    fun monitorChallenges(): Flowable<List<Challenge>> =
        dao.getChallenges().distinctUntilChanged()

    fun onError(throwable: Throwable) {
        Log.e(TAG, throwable.message, throwable)
        recordException(throwable)
    }

    override fun onSocketDisconnected() {
        disposables.clear()
    }
}