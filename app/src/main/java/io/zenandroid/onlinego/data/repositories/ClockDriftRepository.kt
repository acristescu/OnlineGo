package io.zenandroid.onlinego.data.repositories

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.data.model.ogs.NetPong
import io.zenandroid.onlinego.data.ogs.OGSWebSocketService
import io.zenandroid.onlinego.utils.recordException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

class ClockDriftRepository(
        private val socketService: OGSWebSocketService
) : SocketConnectedRepository {
    private val subscriptions = CompositeDisposable()
    private var drift = AtomicLong(0L)
    private var latency = AtomicLong(0L)

    val serverTime: Long
        get() = System.currentTimeMillis() - drift.get() + latency.get()

    override fun onSocketConnected() {
        subscriptions += Observable.interval(10, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .subscribe { doPing() }
        subscriptions += socketService.listenToNetPongEvents()
                .subscribeOn(Schedulers.io())
                .doOnError(this::onError)
                .retry()
                .subscribe(this::onPong)
    }

    override fun onSocketDisconnected() {
        subscriptions.clear()
    }

    private fun doPing() {
        socketService.emit("net/ping") {
            "client" - System.currentTimeMillis()
            "drift" - drift.get()
            "latecy" - latency.get()
        }
    }

    private fun onError(t: Throwable) {
        Log.e(this::class.java.canonicalName, t.message, t)
        recordException(t)
    }

    private fun onPong(pong: NetPong) {
        if(pong.client != null && pong.server != null) {
            val now = System.currentTimeMillis()
            val newLatency = now - pong.client
            val newDrift = now - newLatency / 2 - pong.server
            latency = AtomicLong(newLatency)
            drift = AtomicLong(newDrift)

            Log.v(this::class.java.canonicalName, "latency=$latency drift=$drift")
        } else {
            FirebaseCrashlytics.getInstance().log("W/ClockDriftRepository: Got pong with invalid payload $pong")
        }
    }
}