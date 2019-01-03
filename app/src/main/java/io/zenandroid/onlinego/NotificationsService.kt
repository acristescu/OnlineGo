package io.zenandroid.onlinego

import android.util.Log
import com.crashlytics.android.Crashlytics
import com.firebase.jobdispatcher.JobParameters
import com.firebase.jobdispatcher.JobService
import io.reactivex.disposables.CompositeDisposable
import io.zenandroid.onlinego.extensions.addToDisposable
import io.zenandroid.onlinego.main.MainActivity
import io.zenandroid.onlinego.ogs.OGSServiceImpl
import io.zenandroid.onlinego.utils.NotificationUtils.Companion.updateNotification
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException

/**
 * Created by alex on 24/11/2017.
 */
class NotificationsService : JobService() {

    private val subscriptions = CompositeDisposable()
    val TAG = NotificationsService::class.java.simpleName

    override fun onStartJob(job: JobParameters): Boolean {
        Log.v(TAG, "Started checking for active games")
        if(!OGSServiceImpl.isLoggedIn()) {
            Log.v(TAG, "Not logged in, giving up")
            return false
        }
        if(MainActivity.isInForeground) {
            Log.v(TAG, "App is in foreground, giving up")
            return false
        }
        val connection = OGSServiceImpl
        connection.fetchActiveGames()
                .map { it.filter { it.json?.clock?.current_player == connection.uiConfig?.user?.id } }
                .map { it.sortedWith(compareBy { it.id }) }
                .subscribe({
                    Log.v(TAG, "Got ${it.size} games")
                    if (!MainActivity.isInForeground) {
                        Log.v(TAG, "Updating notification")
                        updateNotification(this, it, connection.uiConfig?.user?.id)
                    }
                    jobFinished(job, false)
                }, { e ->
                    val needsReschedule: Boolean
                    when {
                        (e as? HttpException)?.code() == 403 -> {
                            needsReschedule = false
                            Crashlytics.log(Log.ERROR, TAG, "403 Error when checking for notifications")
                            Crashlytics.logException(e)
                        }
                        e is SocketTimeoutException || e is ConnectException -> {
                            needsReschedule = false
                            Crashlytics.log(Log.ERROR, TAG, "Can't connect when checking for notifications")
                        }
                        else -> {
                            needsReschedule = true
                            Crashlytics.log(Log.ERROR, TAG, "Error when checking for notifications")
                            Crashlytics.logException(e)
                        }
                    }

                    jobFinished(job, needsReschedule)
                }).addToDisposable(subscriptions)
        return true // Answers the question: "Is there still work going on?"
    }

    override fun onStopJob(job: JobParameters): Boolean {
        Crashlytics.log(Log.DEBUG, TAG, "onStopJob called, system wants to kill us")
        subscriptions.clear()
        return true // Answers the question: "Should this job be retried?"
    }

}