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
import java.util.concurrent.atomic.AtomicLong

class ClockDriftRepository(
        private val socketService: OGSWebSocketService
) : SocketConnectedRepository {
    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var drift = AtomicLong(0L)
    private var latency = AtomicLong(0L)

    val serverTime: Long
        get() = System.currentTimeMillis() - drift.get() + latency.get()

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
            "client" - System.currentTimeMillis()
            "drift" - drift.get()
            "latecy" - latency.get()
        }
    }

    private fun onError(t: Throwable) {
      Logger.e(t.message.orEmpty(), t, "ClockDriftRepository")
      CrashReporter.recordException(t)
    }

    private fun onPong(pong: NetPong) {
        if(pong.client != null && pong.server != null) {
            val now = System.currentTimeMillis()
            val newLatency = now - pong.client
            val newDrift = now - newLatency / 2 - pong.server
            latency = AtomicLong(newLatency)
            drift = AtomicLong(newDrift)

          Logger.v(tag = "ClockDriftRepository") { "latency=$latency drift=$drift" }
        } else {
          Logger.w("Got pong with invalid payload $pong", tag = "ClockDriftRepository")
        }
    }
}