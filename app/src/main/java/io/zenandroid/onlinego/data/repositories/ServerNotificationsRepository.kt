package io.zenandroid.onlinego.data.repositories

import io.reactivex.subjects.PublishSubject
import io.zenandroid.onlinego.data.ogs.OGSWebSocketService
import io.zenandroid.onlinego.utils.recordException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONObject

class ServerNotificationsRepository(
  private val socketService: OGSWebSocketService
) : SocketConnectedRepository {
  private var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val notificationsHash = hashMapOf<String, JSONObject>()
  private val notificationsSubject = PublishSubject.create<JSONObject>()

  override fun onSocketConnected() {
    scope.launch {
      try {
        socketService.connectToServerNotifications().collect { onNewNotification(it) }
      } catch (e: Exception) {
        recordException(e)
      }
    }
  }

  override fun onSocketDisconnected() {
    notificationsHash.clear()
    scope.cancel()
    scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  }

  private suspend fun onNewNotification(notification: JSONObject) {
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

  suspend fun acknowledgeNotification(notification: JSONObject) {
    (notification["id"] as? String)?.let {
      socketService.deleteNotification(it)
    }
  }
}