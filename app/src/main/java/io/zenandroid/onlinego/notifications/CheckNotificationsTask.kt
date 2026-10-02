package io.zenandroid.onlinego.notifications

import android.content.Context
import androidx.work.ListenableWorker
import co.touchlab.kermit.Logger
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.zenandroid.onlinego.data.db.GameDao
import io.zenandroid.onlinego.data.model.local.GameNotification
import io.zenandroid.onlinego.data.ogs.httpStatusCode
import io.zenandroid.onlinego.data.repositories.ActiveGamesRepository
import io.zenandroid.onlinego.data.repositories.ChallengesRepository
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import io.zenandroid.onlinego.ui.screens.main.MainActivity
import io.zenandroid.onlinego.utils.NotificationUtils
import io.zenandroid.onlinego.utils.recordException
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import org.koin.core.context.GlobalContext
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

private const val TAG = "CheckNotificationsTask"
class CheckNotificationsTask(val context: Context, val supressWhenInForeground: Boolean = true) {
  private val gameDao: GameDao = GlobalContext.get().get()
  private val userSessionRepository: UserSessionRepository = GlobalContext.get().get()
  private val activeGamesRepository: ActiveGamesRepository = GlobalContext.get().get()
  private val challengesRepository: ChallengesRepository = GlobalContext.get().get()
  suspend fun doWork(): ListenableWorker.Result {
    Logger.i("Checking for notifications", tag = TAG)
    return try {
      notifyGames()
      notifyChallenges()
      ListenableWorker.Result.success()
    } catch (e: Exception) {
      when {
        e.httpStatusCode in arrayOf(401, 403) -> {
          Logger.e("Unauthorized when checking for notifications", tag = TAG)
          recordException(e)
          FirebaseCrashlytics.getInstance()
            .setCustomKey("AUTO_LOGOUT", System.currentTimeMillis())
          NotificationUtils.notifyLogout(context)
          userSessionRepository.logOut()
          ListenableWorker.Result.failure()
        }
        e is SocketTimeoutException || e is ConnectException || e is UnknownHostException -> {
          Logger.e("Can't connect when checking for notifications", tag = TAG)
          ListenableWorker.Result.failure()
        }
        else -> {
          Logger.e("Error when checking for notifications", tag = TAG)
          recordException(e)
          ListenableWorker.Result.retry()
        }
      }
    }
  }

  private suspend fun notifyGames() {
    val userId = userSessionRepository.userId.filterNotNull().first()
    activeGamesRepository.refreshActiveGames()
    val activeGames = activeGamesRepository.monitorActiveGames().first()
    val gameNotifications = gameDao.getGameNotifications().first()
    Logger.v(tag = TAG) { "Got ${activeGames.size} games" }
    if (!(supressWhenInForeground && MainActivity.isInForeground)) {
      Logger.v(tag = TAG) { "Updating game notification" }
      NotificationUtils.notifyGames(context, activeGames, gameNotifications, userId)
    }
    val newNotifications = activeGames.map { GameNotification(it.id, it.moves, it.phase) }
    if (newNotifications != gameNotifications.map { it.notification }) {
      gameDao.replaceGameNotifications(newNotifications)
    }
  }

  private suspend fun notifyChallenges() {
    val userId = userSessionRepository.userId.filterNotNull().first()
    challengesRepository.refreshChallenges()
    val challenges = challengesRepository.monitorChallenges().first()
    val challengeNotifications = gameDao.getChallengeNotifications().first()
    Logger.v(tag = TAG) { "Updating challenges notification" }
    if (!(supressWhenInForeground && MainActivity.isInForeground)) {
      Logger.v(tag = TAG) { "Updating challenges notification" }
      NotificationUtils.notifyChallenges(context, challenges, challengeNotifications, userId)
      gameDao.replaceChallengeNotifications(challenges.map {
        io.zenandroid.onlinego.data.model.local.ChallengeNotification(it.id)
      })
    }
  }
}
