package io.zenandroid.onlinego

import android.util.Log
import com.firebase.jobdispatcher.JobParameters
import com.firebase.jobdispatcher.JobService
import io.zenandroid.onlinego.main.MainActivity
import io.zenandroid.onlinego.ogs.OGSServiceImpl
import io.zenandroid.onlinego.utils.NotificationUtils.Companion.updateNotification

/**
 * Created by alex on 24/11/2017.
 */
class NotificationsService : JobService() {
    companion object {
        val TAG = NotificationsService::class.java.simpleName
    }

    override fun onStartJob(job: JobParameters): Boolean {
        Log.v(TAG, "Started checking for active games")
        if(MainActivity.isInForeground) {
            Log.v(TAG, "App is in foreground, giving up")
            return false
        }
        val connection = OGSServiceImpl()
        connection.loginWithToken()
                .andThen(connection.fetchActiveGames())
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
                    Log.e(TAG, "Error when checking for notifications", e)
                    jobFinished(job, true)
                })
        return true // Answers the question: "Is there still work going on?"
    }

    override fun onStopJob(job: JobParameters): Boolean {
        Log.d(TAG, "onStopJob called, system wants to kill us")
        return true // Answers the question: "Should this job be retried?"
    }

}