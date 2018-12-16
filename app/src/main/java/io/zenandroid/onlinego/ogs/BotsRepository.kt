package io.zenandroid.onlinego.ogs

import com.crashlytics.android.Crashlytics
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.extensions.addToDisposable
import io.zenandroid.onlinego.model.ogs.Bot

object BotsRepository {

    private val subscriptions = CompositeDisposable()
    var bots = listOf<Bot>()
        private set

    internal fun subscribe() {
        OGSServiceImpl.instance.connectToBots()
                .subscribeOn(Schedulers.io())
                .subscribe(this::storeBots) { Crashlytics.logException(it) }
                .addToDisposable(subscriptions)
    }

    private fun storeBots(newBots: List<Bot>) {
        bots = newBots
    }

    internal fun unsubscribe() {
        subscriptions.clear()
    }
}