package io.zenandroid.onlinego.data.repositories

import io.zenandroid.onlinego.data.model.local.Player
import io.zenandroid.onlinego.data.model.ogs.OGSPlayer
import io.zenandroid.onlinego.data.ogs.OGSWebSocketService
import io.zenandroid.onlinego.utils.recordException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class BotsRepository(
        private val socketService: OGSWebSocketService
): SocketConnectedRepository {

    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    var bots = listOf<Player>()
        private set

    override fun onSocketConnected() {
        scope.launch {
            try {
                socketService.connectToBots().collect { storeBots(it) }
            } catch (e: Exception) {
                recordException(e)
            }
        }
    }

    private fun storeBots(newBots: List<OGSPlayer>) {
        bots = newBots.map { Player.fromOGSPlayer(it) }
    }

    override fun onSocketDisconnected() {
        scope.cancel()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}