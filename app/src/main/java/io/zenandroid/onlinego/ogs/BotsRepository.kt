package io.zenandroid.onlinego.ogs

import com.crashlytics.android.Crashlytics
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.extensions.addToDisposable
import io.zenandroid.onlinego.model.local.Player
import io.zenandroid.onlinego.model.ogs.OGSPlayer

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