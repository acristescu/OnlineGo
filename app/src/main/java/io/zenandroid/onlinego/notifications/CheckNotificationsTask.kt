package io.zenandroid.onlinego.notifications

import android.content.Context
import android.util.Log
import androidx.work.ListenableWorker
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.zenandroid.onlinego.data.db.GameDao
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.data.model.local.GameNotification
import io.zenandroid.onlinego.data.model.local.GameNotificationWithDetails
import io.zenandroid.onlinego.data.repositories.ActiveGamesRepository
import io.zenandroid.onlinego.data.repositories.ChallengesRepository
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import io.zenandroid.onlinego.ui.screens.main.MainActivity
import io.zenandroid.onlinego.utils.NotificationUtils
import org.koin.core.context.GlobalContext
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException


private const val TAG = "CheckNotificationsTask"
class CheckNotificationsTask(val context: Context, val supressWhenInForeground: Boolean = true) {

    private val gameDao: GameDao = GlobalContext.get().get()
    private val userSessionRepository: UserSessionRepository = GlobalContext.get().get()
    private val activeGamesRepository: ActiveGamesRepository = GlobalContext.get().get()
    private val challengesRepository: ChallengesRepository = GlobalContext.get().get()

    fun doWork() =
            Completable
                    .mergeArray(
                            notifyGames(),
                            notifyChallenges()
                    ).toSingleDefault(ListenableWorker.Result.success())
                    .onErrorReturn { e ->
                        when {
                            (e as? HttpException)?.code() in arrayOf(401, 403) -> {
                                FirebaseCrashlytics.getInstance().log("E/$TAG: Unauthorized when checking for notifications")
                                FirebaseCrashlytics.getInstance().recordException(e)
                                FirebaseCrashlytics.getInstance().setCustomKey("AUTO_LOGOUT", System.currentTimeMillis())
                                NotificationUtils.notifyLogout(context)
                                userSessionRepository.logOut()
                                return@onErrorReturn ListenableWorker.Result.failure()
                            }
                            e is SocketTimeoutException || e is ConnectException -> {
                                FirebaseCrashlytics.getInstance().log("E/$TAG: Can't connect when checking for notifications")
                                return@onErrorReturn ListenableWorker.Result.failure()
                            }
                            else -> {
                                FirebaseCrashlytics.getInstance().log("E/$TAG: Error when checking for notifications")
                                FirebaseCrashlytics.getInstance().recordException(e)
                                return@onErrorReturn ListenableWorker.Result.retry()
                            }
                        }
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
                        if (!(supressWhenInForeground && MainActivity.isInForeground)) {
                            Log.v(TAG, "Updating game notification")
                            NotificationUtils.notifyGames(context, it.first, it.second, userSessionRepository.userId!!)
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
                        if (!(supressWhenInForeground && MainActivity.isInForeground)) {
                            Log.v(TAG, "Updating challenges notification")
                            NotificationUtils.notifyChallenges(context, it, userSessionRepository.userId!!)
                        }
                    }.ignoreElement()

}