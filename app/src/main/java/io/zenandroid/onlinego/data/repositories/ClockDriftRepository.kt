package io.zenandroid.onlinego.data.repositories

import co.touchlab.kermit.Logger
import io.zenandroid.onlinego.data.model.ogs.NetPong
import io.zenandroid.onlinego.data.ogs.OGSWebSocketService
import io.zenandroid.onlinego.utils.CrashReporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.launch
import kotlin.concurrent.Volatile
import kotlin.time.Clock

class ClockDriftRepository(
        private val socketService: OGSWebSocketService
) : SocketConnectedRepository {
    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile
    private var drift = 0L
    @Volatile
    private var latency = 0L

    val serverTime: Long
        get() = Clock.System.now().toEpochMilliseconds() - drift + latency

    override fun onSocketConnected() {
        scope.launch {
            while (true) {
                delay(10_000)
                doPing()
            }
        }
        scope.launch {
            socketService.listenToNetPongEvents()
                .retry { onError(it); true }
                .collect { onPong(it) }
        }
    }

    override fun onSocketDisconnected() {
        scope.cancel()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    private fun doPing() {
        socketService.emit("net/ping") {
            "client" - Clock.System.now().toEpochMilliseconds()
            "drift" - drift
            "latecy" - latency
        }
    }

    private fun onError(t: Throwable) {
      Logger.e(t.message.orEmpty(), t, "ClockDriftRepository")
      CrashReporter.recordException(t)
    }

    private fun onPong(pong: NetPong) {
        if(pong.client != null && pong.server != null) {
            val now = Clock.System.now().toEpochMilliseconds()
            val newLatency = now - pong.client
            val newDrift = now - newLatency / 2 - pong.server
            latency = newLatency
            drift = newDrift

          Logger.v(tag = "ClockDriftRepository") { "latency=$latency drift=$drift" }
        } else {
          Logger.w("Got pong with invalid payload $pong", tag = "ClockDriftRepository")
        }
    }
}