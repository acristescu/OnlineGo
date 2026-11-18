package io.zenandroid.onlinego.notifications

import co.touchlab.kermit.Logger
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.zenandroid.onlinego.data.repositories.LoginStatus
import io.zenandroid.onlinego.data.repositories.PushRegistrationRepository
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import io.zenandroid.onlinego.ui.screens.main.MainActivity
import io.zenandroid.onlinego.utils.NotificationUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.context.GlobalContext
import kotlin.time.Duration.Companion.seconds

private const val TAG = "OgsMessagingService"
private val REFRESHED_TYPES = setOf("yourMove", "timecop", "gameEnded", "challenge")
private val REFRESH_TIMEOUT = 8.seconds

class OgsMessagingService : FirebaseMessagingService() {
  private val koin get() = GlobalContext.get()

  override fun onNewToken(token: String) {
    koin.get<CoroutineScope>().launch {
      koin.get<PushRegistrationRepository>().register()
    }
  }

  override fun onMessageReceived(message: RemoteMessage) {
    val type = message.data["type"]
    Logger.i("Push received, type=$type", tag = TAG)
    runBlocking {
      if (type in REFRESHED_TYPES) {
        refreshGamesAndChallenges()
      } else {
        showServerNotification(message)
      }
    }
  }

  private suspend fun refreshGamesAndChallenges() {
    if (MainActivity.isInForeground) return
    if (koin.get<UserSessionRepository>().loginStatus.first() == LoginStatus.LoggedOut) return
    withTimeoutOrNull(REFRESH_TIMEOUT) { CheckNotificationsTask().doWork() }
      ?: Logger.w("Refresh after push timed out", tag = TAG)
  }

  private suspend fun showServerNotification(message: RemoteMessage) {
    val title = message.data["title"] ?: return
    val tag = message.data["tag"] ?: message.data["notification_id"] ?: message.messageId ?: title
    koin.get<NotificationUtils>().notifyServerNotification(title, message.data["body"], tag)
  }
}
