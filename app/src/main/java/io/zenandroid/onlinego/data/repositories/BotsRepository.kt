package io.zenandroid.onlinego.data.repositories

import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.data.model.local.Player
import io.zenandroid.onlinego.data.model.ogs.OGSPlayer
import io.zenandroid.onlinego.data.ogs.OGSWebSocketService
import io.zenandroid.onlinego.utils.addToDisposable
import io.zenandroid.onlinego.utils.recordException

class BotsRepository(
        private val socketService: OGSWebSocketService
): SocketConnectedRepository {

    private val subscriptions = CompositeDisposable()
    var bots = listOf<Player>()
        private set

    override fun onSocketConnected() {
        socketService.connectToBots()
                .subscribeOn(Schedulers.io())
                .subscribe(this::storeBots, :: recordException)
                .addToDisposable(subscriptions)
    }

    private fun storeBots(newBots: List<OGSPlayer>) {
        bots = newBots.map { Player.fromOGSPlayer(it) }
    }

    override fun onSocketDisconnected() {
        subscriptions.clear()
    }
}