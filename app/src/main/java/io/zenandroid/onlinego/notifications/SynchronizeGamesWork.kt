package io.zenandroid.onlinego.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import co.touchlab.kermit.Logger
import io.zenandroid.onlinego.data.repositories.LoginStatus
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import io.zenandroid.onlinego.ui.screens.main.MainActivity
import kotlinx.coroutines.flow.first
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
    Logger.i("Started checking for active games", tag = TAG)
    val loggedIn = userSessionRepository.loginStatus.first()
    if (loggedIn == LoginStatus.LoggedOut) {
      Logger.v(tag = TAG) { "Not logged in, giving up" }
      return Result.failure()
    }
    if (MainActivity.isInForeground) {
      Logger.v(tag = TAG) { "App is in foreground, giving up" }
      return Result.success()
    }
    return task.doWork()
  }
}
