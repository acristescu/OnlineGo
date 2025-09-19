package io.zenandroid.onlinego.notifications

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.RxWorker
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.reactivex.Single
import io.zenandroid.onlinego.data.repositories.LoginStatus
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import io.zenandroid.onlinego.ui.screens.main.MainActivity
import org.koin.core.context.GlobalContext.get
import java.util.concurrent.TimeUnit

private const val NOT_CHARGING_PERIOD_MINUTES = 30L
private const val CHARGING_PERIOD_MINUTES = 4L

private const val NOT_CHARGING_WORK_NAME = "poll_active_games"
private const val CHARGING_WORK_NAME = "poll_active_games_charging"
private const val PERIODIC_WORK_NAME = "periodic_work"

class SynchronizeGamesWork(val context: Context, params: WorkerParameters) :
  RxWorker(context, params) {

  companion object {
    fun schedule(context: Context) {
//      scheduleCharging(context)
//      scheduleNotCharging(context)
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

//    private fun scheduleNotCharging(context: Context) {
//      val constraints = Constraints.Builder()
//        .setRequiredNetworkType(NetworkType.CONNECTED)
//        .build()
//      val request = OneTimeWorkRequestBuilder<SynchronizeGamesWork>()
//        .setInitialDelay(NOT_CHARGING_PERIOD_MINUTES, TimeUnit.MINUTES)
//        .setConstraints(constraints)
//        .build()
//      WorkManager.getInstance(context)
//        .enqueueUniqueWork(NOT_CHARGING_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
//    }
//
//    private fun scheduleCharging(context: Context) {
//      val constraints = Constraints.Builder()
//        .setRequiredNetworkType(NetworkType.CONNECTED)
//        .setRequiresCharging(true)
//        .build()
//      val request = OneTimeWorkRequestBuilder<SynchronizeGamesWork>()
//        .setInitialDelay(CHARGING_PERIOD_MINUTES, TimeUnit.MINUTES)
//        .setConstraints(constraints)
//        .build()
//      WorkManager.getInstance(context)
//        .enqueueUniqueWork(CHARGING_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
//    }
  }

  private val TAG = SynchronizeGamesWork::class.java.simpleName

  private val task = CheckNotificationsTask(context)

  private val userSessionRepository: UserSessionRepository = get().get()

  override fun createWork(): Single<Result> {
    FirebaseCrashlytics.getInstance().log("I/$TAG: Started checking for active games")
    return userSessionRepository.loggedInObservable.flatMapSingle { loggedIn ->
      if (loggedIn == LoginStatus.LoggedOut) {
        Log.v(TAG, "Not logged in, giving up")
        return@flatMapSingle Single.just(Result.failure())
      }
      if (MainActivity.isInForeground) {
        Log.v(TAG, "App is in foreground, giving up")
        return@flatMapSingle Single.just(Result.success())
          //.reschedule()
      }
      return@flatMapSingle task.doWork()
        //.reschedule()
    }.firstOrError()
  }

//  private fun <T : Any> Single<T>.reschedule(): Single<T> {
//    return this.doFinally {
//      try {
//        FirebaseCrashlytics.getInstance().log("I/$TAG: Enqueue work")
//        schedule(context)
//      } catch (e: Exception) {
//        Log.e(TAG, e.message, e)
//        recordException(e)
//      }
//    }
//  }
}
