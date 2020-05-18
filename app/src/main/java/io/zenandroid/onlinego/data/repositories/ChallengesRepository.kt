package io.zenandroid.onlinego.data.repositories

import android.util.Log
import com.crashlytics.android.Crashlytics
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.utils.addToDisposable
import io.zenandroid.onlinego.data.model.local.Challenge
import io.zenandroid.onlinego.data.model.ogs.OGSChallenge
import io.zenandroid.onlinego.data.ogs.OGSServiceImpl

object ChallengesRepository {

    private val ogs = OGSServiceImpl
    private val dao = OnlineGoApplication.instance.db.gameDao()
    private val disposables = CompositeDisposable()
    private val TAG = ChallengesRepository.javaClass.simpleName

    fun subscribe() {
        refreshChallenges()
                .subscribe({}, this::onError)
                .addToDisposable(disposables)
        OGSServiceImpl.connectToUIPushes()
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
        return OGSServiceImpl.fetchChallenges()
                .doOnSuccess(this::storeChallenges)
                .ignoreElement()
    }

    fun monitorChallenges(): Flowable<List<Challenge>> =
        dao.getChallenges()

    fun onError(throwable: Throwable) {
        Log.e(TAG, throwable.message, throwable)
        Crashlytics.logException(throwable)
    }

    fun unsubscribe() {
        disposables.clear()
    }
}