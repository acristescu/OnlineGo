package io.zenandroid.onlinego.data.repositories

import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import io.zenandroid.onlinego.data.ogs.OGSWebSocketService
import io.zenandroid.onlinego.utils.addToDisposable
import org.json.JSONObject

class ServerNotificationsRepository(
        private val socketService: OGSWebSocketService
): SocketConnectedRepository {
    private val subscriptions = CompositeDisposable()
    private val notificationsHash = hashMapOf<String, JSONObject>()
    private val notificationsSubject = PublishSubject.create<JSONObject>()

    override fun onSocketConnected() {
        socketService.connectToServerNotifications()
                .subscribe(this::onNewNotification, this::onError)
                .addToDisposable(subscriptions)
    }

    override fun onSocketDisconnected() {
        notificationsHash.clear()
        subscriptions.clear()
    }

    private fun onNewNotification(notification: JSONObject) {
        (notification["id"] as? String)?.let {
            notificationsHash[it] = notification
            notificationsSubject.onNext(notification)
        }
    }

    private fun onError(t: Throwable) {
        FirebaseCrashlytics.getInstance().recordException(t)
    }

    fun notificationsObservable() =
            notificationsSubject.hide().startWith(notificationsHash.values)

    fun acknowledgeNotification(notification: JSONObject) {
        (notification["id"] as? String)?.let {
            socketService.deleteNotification(it)
        }
    }
}