package io.zenandroid.onlinego.notifications
import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.zenandroid.onlinego.data.repositories.LoginStatus
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import io.zenandroid.onlinego.ui.screens.main.MainActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.rx2.asFlow
import org.koin.core.context.GlobalContext.get
import java.util.concurrent.TimeUnit
private const val NOT_CHARGING_PERIOD_MINUTES = 30L
private const val CHARGING_PERIOD_MINUTES = 4L
private const val NOT_CHARGING_WORK_NAME = "poll_active_games"
private const val CHARGING_WORK_NAME = "poll_active_games_charging"
private const val PERIODIC_WORK_NAME = "periodic_work"
class SynchronizeGamesWork(val context: Context, params: WorkerParameters) :
  CoroutineWorker(context, params) {
  companion object {
    fun schedule(context: Context) {
      WorkManager.getInstance(context).cancelUniqueWork(NOT_CHARGING_WORK_NAME)
      WorkManager.getInstance(context).cancelUniqueWork(CHARGING_WORK_NAME)
      val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()
      val request = PeriodicWorkRequestBuilder<SynchronizeGamesWork>(15, TimeUnit.MINUTES)
        .setConstraints(constraints)
        .build()
      WorkManager.getInstance(context)
        .enqueueUniquePeriodicWork(PERIODIC_WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }
  }
  private val TAG = SynchronizeGamesWork::class.java.simpleName
  private val task = CheckNotificationsTask(context)
  private val userSessionRepository: UserSessionRepository = get().get()
  override suspend fun doWork(): Result {
    FirebaseCrashlytics.getInstance().log("I/$TAG: Started checking for active games")
    val loggedIn = userSessionRepository.loggedInObservable.asFlow().first()
    if (loggedIn == LoginStatus.LoggedOut) {
      Log.v(TAG, "Not logged in, giving up")
      return Result.failure()
    }
    if (MainActivity.isInForeground) {
      Log.v(TAG, "App is in foreground, giving up")
      return Result.success()
    }
    return task.doWork()
  }
}
