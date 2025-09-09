package io.zenandroid.onlinego.data.repositories

import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import io.zenandroid.onlinego.data.ogs.OGSWebSocketService
import io.zenandroid.onlinego.utils.addToDisposable
import io.zenandroid.onlinego.utils.recordException
import org.json.JSONObject

class ServerNotificationsRepository(
  private val socketService: OGSWebSocketService
) : SocketConnectedRepository {
  private val subscriptions = CompositeDisposable()
  private val notificationsHash = hashMapOf<String, JSONObject>()
  private val notificationsSubject = PublishSubject.create<JSONObject>()

  override fun onSocketConnected() {
    socketService.connectToServerNotifications()
      .subscribe(this::onNewNotification, ::recordException)
      .addToDisposable(subscriptions)
  }

  override fun onSocketDisconnected() {
    notificationsHash.clear()
    subscriptions.clear()
  }

  private fun onNewNotification(notification: JSONObject) {
    if ((notification["type"] as? String) == "delete") {
      (notification["id"] as? String)?.let {
        notificationsHash.remove(it)
      }
    } else {
      (notification["id"] as? String)?.let {
        notificationsHash[it] = notification
        notificationsSubject.onNext(notification)
        acknowledgeNotification(notification)
      }
    }
  }

  fun notificationsObservable() =
    notificationsSubject.hide().startWith(notificationsHash.values)

  fun acknowledgeNotification(notification: JSONObject) {
    (notification["id"] as? String)?.let {
      socketService.deleteNotification(it)
    }
  }
}