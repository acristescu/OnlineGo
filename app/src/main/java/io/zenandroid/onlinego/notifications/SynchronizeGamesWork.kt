package io.zenandroid.onlinego.notifications

import android.content.Context
import android.util.Log
import androidx.work.*
import com.crashlytics.android.Crashlytics
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.zenandroid.onlinego.data.db.GameDao
import io.zenandroid.onlinego.ui.screens.main.MainActivity
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.data.model.local.GameNotification
import io.zenandroid.onlinego.data.model.local.GameNotificationWithDetails
import io.zenandroid.onlinego.data.repositories.ActiveGamesRepository
import io.zenandroid.onlinego.data.repositories.ChallengesRepository
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import io.zenandroid.onlinego.utils.NotificationUtils
import io.zenandroid.onlinego.utils.NotificationUtils.Companion.notifyGames
import io.zenandroid.onlinego.utils.NotificationUtils.Companion.notifyChallenges
import org.koin.core.context.KoinContextHandler.get
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

private const val NOT_CHARGING_PERIOD_MINUTES = 30L
private const val CHARGING_PERIOD_MINUTES = 4L

private const val NOT_CHARGING_WORK_NAME = "poll_active_games"
private const val CHARGING_WORK_NAME = "poll_active_games_charging"

class SynchronizeGamesWork(val context: Context, params: WorkerParameters) : RxWorker(context, params) {

    companion object {
        fun schedule() {
            scheduleCharging()
            scheduleNotCharging()
        }

        fun unschedule() {
            WorkManager.getInstance().cancelUniqueWork(NOT_CHARGING_WORK_NAME)
            WorkManager.getInstance().cancelUniqueWork(CHARGING_WORK_NAME)
        }

        private fun scheduleNotCharging() {
            val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            val request = OneTimeWorkRequestBuilder<SynchronizeGamesWork>()
                    .setInitialDelay(NOT_CHARGING_PERIOD_MINUTES, TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    .build()
            WorkManager.getInstance()
                    .enqueueUniqueWork(NOT_CHARGING_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }

        private fun scheduleCharging() {
            val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresCharging(true)
                    .build()
            val request = OneTimeWorkRequestBuilder<SynchronizeGamesWork>()
                    .setInitialDelay(CHARGING_PERIOD_MINUTES, TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    .build()
            WorkManager.getInstance()
                    .enqueueUniqueWork(CHARGING_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }

    private val TAG = SynchronizeGamesWork::class.java.simpleName

    private val gameDao: GameDao = get().get()
    private val userSessionRepository: UserSessionRepository = get().get()
    private val activeGamesRepository: ActiveGamesRepository = get().get()
    private val challengesRepository: ChallengesRepository = get().get()

    override fun createWork(): Single<Result> {
        Crashlytics.log(Log.INFO, TAG, "Started checking for active games")
        if (!userSessionRepository.isLoggedIn()) {
            Log.v(TAG, "Not logged in, giving up")
            return Single.just(Result.failure())
        }
        if (MainActivity.isInForeground) {
            Log.v(TAG, "App is in foreground, giving up")
            return Single.just(Result.success()).reschedule()
        }
        return Completable
                .mergeArray(
                        notifyGames(),
                        notifyChallenges()
                ).toSingleDefault(Result.success())
                .onErrorReturn { e ->
                    when {
                        (e as? HttpException)?.code() in arrayOf(401, 403) -> {
                            Crashlytics.log(Log.ERROR, TAG, "Unauthorized when checking for notifications")
                            Crashlytics.logException(e)
                            Crashlytics.setLong("AUTO_LOGOUT", System.currentTimeMillis())
                            NotificationUtils.notifyLogout(context)
                            userSessionRepository.logOut()
                            return@onErrorReturn Result.failure()
                        }
                        e is SocketTimeoutException || e is ConnectException -> {
                            Crashlytics.log(Log.ERROR, TAG, "Can't connect when checking for notifications")
                            return@onErrorReturn Result.failure()
                        }
                        else -> {
                            Crashlytics.log(Log.ERROR, TAG, "Error when checking for notifications")
                            Crashlytics.logException(e)
                            return@onErrorReturn Result.retry()
                        }
                    }
                }.reschedule()
    }

    private fun notifyGames() : Completable =
            activeGamesRepository
                    .refreshActiveGames()
                    .andThen(Single.zip(
                            activeGamesRepository.monitorActiveGames().firstOrError(),
                            gameDao.getGameNotifications().firstOrError(),
                            BiFunction { a: List<Game>, b: List<GameNotificationWithDetails> -> Pair(a, b) }
                    )).doOnSuccess {
                        Log.v(TAG, "Got ${it.first.size} games")
                        if (!MainActivity.isInForeground) {
                            Log.v(TAG, "Updating game notification")
                            notifyGames(context, it.first, it.second, userSessionRepository.userId!!)
                        }

                        val newNotifications = it.first.map { GameNotification(it.id, it.moves, it.phase) }
                        if(newNotifications != it.second) {
                            gameDao.replaceGameNotifications(newNotifications)
                        }
                    }.ignoreElement()

    private fun notifyChallenges() : Completable =
            challengesRepository
                    .refreshChallenges()
                    .andThen(challengesRepository.monitorChallenges().firstOrError())
                    .doOnSuccess {
                        Log.v(TAG, "Updating challenges notification")
                        if (!MainActivity.isInForeground) {
                            Log.v(TAG, "Updating challenges notification")
                            notifyChallenges(context, it, userSessionRepository.userId!!)
                        }
                    }.ignoreElement()

    private fun <T : Any> Single<T>.reschedule(): Single<T> {
        return this.doFinally {
            try {
                Crashlytics.log(Log.INFO, TAG, "Enqueue work")
                schedule()
            } catch (e: Exception) {
                Log.e(TAG, e.message, e)
                Crashlytics.logException(e)
            }
        }
    }
}