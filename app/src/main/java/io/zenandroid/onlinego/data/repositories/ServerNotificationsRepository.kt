package io.zenandroid.onlinego.data.repositories

import com.crashlytics.android.Crashlytics
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import io.zenandroid.onlinego.utils.addToDisposable
import io.zenandroid.onlinego.data.ogs.OGSServiceImpl
import org.json.JSONObject

object ServerNotificationsRepository {
    private val subscriptions = CompositeDisposable()
    private val ogs = OGSServiceImpl
    private val notificationsHash = hashMapOf<String, JSONObject>()
    private val notificationsSubject = PublishSubject.create<JSONObject>()

    fun subscribe() {
        OGSServiceImpl.connectToServerNotifications()
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
            OGSServiceImpl.deleteNotification(it)
        }
    }
}