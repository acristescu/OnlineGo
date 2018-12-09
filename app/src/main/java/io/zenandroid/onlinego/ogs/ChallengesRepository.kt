package io.zenandroid.onlinego.ogs

import android.util.Log
import com.crashlytics.android.Crashlytics
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.extensions.addToDisposable
import io.zenandroid.onlinego.model.local.Challenge
import io.zenandroid.onlinego.model.ogs.OGSChallenge

object ChallengesRepository {

    private val ogs = OGSServiceImpl.instance
    private val dao = OnlineGoApplication.instance.db.gameDao()
    private val disposables = CompositeDisposable()
    private val TAG = ChallengesRepository.javaClass.simpleName

    public fun subscribe() {
        fetchChallenges()
        ogs.connectToUIPushes()
                .filter { it.event == "challenge-list-updated" }
                .subscribe(
                        { fetchChallenges() },
                        { Crashlytics.logException(it) }
                ).addToDisposable(disposables)
    }

    private fun fetchChallenges() {
        Log.i(TAG, "Fetching challenges")
        ogs.fetchChallenges()
                .subscribe(this::storeChallenges, Crashlytics::logException)
                .addToDisposable(disposables)
    }

    private fun storeChallenges(challenges: List<OGSChallenge>) {
        dao.replaceAllChallenges(challenges.map { Challenge.fromOGSChallenge (it) })
    }

    public fun monitorChallenges(): Flowable<List<Challenge>> =
            dao.getChallenges()

    public fun unsubscribe() {
        disposables.clear()
    }
}