package io.zenandroid.onlinego.notifications

import androidx.work.ListenableWorker
import co.touchlab.kermit.Logger
import io.zenandroid.onlinego.data.db.GameDao
import io.zenandroid.onlinego.data.model.local.GameNotification
import io.zenandroid.onlinego.data.ogs.httpStatusCode
import io.zenandroid.onlinego.data.repositories.ActiveGamesRepository
import io.zenandroid.onlinego.data.repositories.ChallengesRepository
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import io.zenandroid.onlinego.ui.screens.main.MainActivity
import io.zenandroid.onlinego.utils.CrashReporter
import io.zenandroid.onlinego.utils.NotificationUtils
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import org.koin.core.context.GlobalContext
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

private const val TAG = "CheckNotificationsTask"

class CheckNotificationsTask(val supressWhenInForeground: Boolean = true) {
  private val gameDao: GameDao = GlobalContext.get().get()
  private val userSessionRepository: UserSessionRepository = GlobalContext.get().get()
  private val activeGamesRepository: ActiveGamesRepository = GlobalContext.get().get()
  private val challengesRepository: ChallengesRepository = GlobalContext.get().get()
  private val notificationUtils: NotificationUtils = GlobalContext.get().get()
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
          CrashReporter.recordException(e)
          CrashReporter.setCustomKey("AUTO_LOGOUT", System.currentTimeMillis())
          notificationUtils.notifyLogout()
          userSessionRepository.logOut()
          ListenableWorker.Result.failure()
        }
        e is SocketTimeoutException || e is ConnectException || e is UnknownHostException -> {
          Logger.e("Can't connect when checking for notifications", tag = TAG)
          ListenableWorker.Result.failure()
        }
        else -> {
          Logger.e("Error when checking for notifications", tag = TAG)
          CrashReporter.recordException(e)
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
      notificationUtils.notifyGames(activeGames, gameNotifications, userId)
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
      notificationUtils.notifyChallenges(challenges, challengeNotifications, userId)
      gameDao.replaceChallengeNotifications(challenges.map {
        io.zenandroid.onlinego.data.model.local.ChallengeNotification(it.id)
      })
    }
  }
}
