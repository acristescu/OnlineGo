package io.zenandroid.onlinego.data.repositories

import com.crashlytics.android.Crashlytics
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.utils.addToDisposable
import io.zenandroid.onlinego.data.model.local.Player
import io.zenandroid.onlinego.data.model.ogs.OGSPlayer
import io.zenandroid.onlinego.data.ogs.OGSServiceImpl

object BotsRepository {

    private val subscriptions = CompositeDisposable()
    var bots = listOf<Player>()
        private set

    internal fun subscribe() {
        OGSServiceImpl.connectToBots()
                .subscribeOn(Schedulers.io())
                .subscribe(this::storeBots) { Crashlytics.logException(it) }
                .addToDisposable(subscriptions)
    }

    private fun storeBots(newBots: List<OGSPlayer>) {
        bots = newBots.map { Player.fromOGSPlayer(it) }
    }

    internal fun unsubscribe() {
        subscriptions.clear()
    }
}