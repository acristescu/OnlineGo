package io.zenandroid.onlinego.ogs

import com.crashlytics.android.Crashlytics
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import io.zenandroid.onlinego.extensions.addToDisposable
import org.json.JSONObject

object NotificationsRepository {
    private val subscriptions = CompositeDisposable()
    private val ogs = OGSServiceImpl
    private val notificationsHash = hashMapOf<String, JSONObject>()
    private val notificationsSubject = PublishSubject.create<JSONObject>()

    fun subscribe() {
        ogs.connectToServerNotifications()
                .subscribe(this::onNewNotification, this::onError)
                .addToDisposable(subscriptions)
    }

    fun unsubscribe() {
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
        Crashlytics.logException(t)
    }

    fun notificationsObservable() =
            notificationsSubject.hide().startWith(notificationsHash.values)

    fun acknowledgeNotification(notification: JSONObject) {
        (notification["id"] as? String)?.let {
            ogs.deleteNotification(it)
        }
    }
}