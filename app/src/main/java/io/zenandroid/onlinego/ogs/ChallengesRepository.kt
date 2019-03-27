package io.zenandroid.onlinego.ogs

import android.util.Log
import com.crashlytics.android.Crashlytics
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.extensions.addToDisposable
import io.zenandroid.onlinego.model.local.Challenge
import io.zenandroid.onlinego.model.ogs.OGSChallenge

object ChallengesRepository {

    private val ogs = OGSServiceImpl
    private val dao = OnlineGoApplication.instance.db.gameDao()
    private val disposables = CompositeDisposable()
    private val TAG = ChallengesRepository.javaClass.simpleName

    fun subscribe() {
        refreshChallenges()
                .subscribe({}, Crashlytics::logException)
                .addToDisposable(disposables)
        ogs.connectToUIPushes()
                .filter { it.event == "challenge-list-updated" }
                .flatMapCompletable { refreshChallenges() }
                .subscribe({}, Crashlytics::logException)
                .addToDisposable(disposables)
    }

    private fun storeChallenges(challenges: List<OGSChallenge>) {
        dao.replaceAllChallenges(challenges.map { Challenge.fromOGSChallenge (it) })
    }

    fun refreshChallenges() : Completable {
        Log.i(TAG, "Fetching challenges")
        return ogs.fetchChallenges()
                .doOnSuccess(this::storeChallenges)
                .ignoreElement()
    }

    fun monitorChallenges(): Flowable<List<Challenge>> =
        dao.getChallenges()

    fun unsubscribe() {
        disposables.clear()
    }
}