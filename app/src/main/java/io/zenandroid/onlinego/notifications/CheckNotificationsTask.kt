package io.zenandroid.onlinego.notifications
import android.content.Context
import android.util.Log
import androidx.work.ListenableWorker
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.zenandroid.onlinego.data.db.GameDao
import io.zenandroid.onlinego.data.model.local.GameNotification
import io.zenandroid.onlinego.data.repositories.ActiveGamesRepository
import io.zenandroid.onlinego.data.repositories.ChallengesRepository
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import io.zenandroid.onlinego.ui.screens.main.MainActivity
import io.zenandroid.onlinego.utils.NotificationUtils
import io.zenandroid.onlinego.utils.recordException
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import org.koin.core.context.GlobalContext
import retrofit2.HttpException
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
    FirebaseCrashlytics.getInstance().log("I/$TAG: Checking for notifications")
    return try {
      notifyGames()
      notifyChallenges()
      ListenableWorker.Result.success()
    } catch (e: Exception) {
      when {
        (e as? HttpException)?.code() in arrayOf(401, 403) -> {
          FirebaseCrashlytics.getInstance()
            .log("E/$TAG: Unauthorized when checking for notifications")
          recordException(e)
          FirebaseCrashlytics.getInstance()
            .setCustomKey("AUTO_LOGOUT", System.currentTimeMillis())
          NotificationUtils.notifyLogout(context)
          userSessionRepository.logOut()
          ListenableWorker.Result.failure()
        }
        e is SocketTimeoutException || e is ConnectException || e is UnknownHostException -> {
          FirebaseCrashlytics.getInstance()
            .log("E/$TAG: Can't connect when checking for notifications")
          ListenableWorker.Result.failure()
        }
        else -> {
          FirebaseCrashlytics.getInstance()
            .log("E/$TAG: Error when checking for notifications")
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
    Log.v(TAG, "Got ${activeGames.size} games")
    if (!(supressWhenInForeground && MainActivity.isInForeground)) {
      Log.v(TAG, "Updating game notification")
      NotificationUtils.notifyGames(context, activeGames, gameNotifications, userId)
    }
    val newNotifications = activeGames.map { GameNotification(it.id, it.moves, it.phase) }
    if (newNotifications != gameNotifications) {
      gameDao.replaceGameNotifications(newNotifications)
    }
  }

  private suspend fun notifyChallenges() {
    val userId = userSessionRepository.userId.filterNotNull().first()
    challengesRepository.refreshChallenges()
    val challenges = challengesRepository.monitorChallenges().first()
    val challengeNotifications = gameDao.getChallengeNotifications().first()
    Log.v(TAG, "Updating challenges notification")
    if (!(supressWhenInForeground && MainActivity.isInForeground)) {
      Log.v(TAG, "Updating challenges notification")
      NotificationUtils.notifyChallenges(context, challenges, challengeNotifications, userId)
      gameDao.replaceChallengeNotifications(challenges.map {
        io.zenandroid.onlinego.data.model.local.ChallengeNotification(it.id)
      })
    }
  }
}
