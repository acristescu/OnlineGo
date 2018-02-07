package io.zenandroid.onlinego

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.BitmapFactory
import android.support.v4.app.NotificationCompat
import com.firebase.jobdispatcher.JobParameters
import com.firebase.jobdispatcher.JobService
import io.zenandroid.onlinego.login.LoginActivity
import io.zenandroid.onlinego.model.ogs.Game
import io.zenandroid.onlinego.ogs.OGSServiceImpl
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Created by alex on 24/11/2017.
 */
class NotificationsService : JobService() {
    companion object {
        var lastNotificationHash = byteArrayOf()
    }
    override fun onStartJob(job: JobParameters): Boolean {
        // Do some work here

        if(MainActivity.isInForeground) {
            return false
        }
        val games = mutableListOf<Game>()
        val connection = OGSServiceImpl()
        connection.loginWithToken()
                .andThen(connection.connectToNotifications())
                .take(10, TimeUnit.SECONDS)
                .filter { it.player_to_move == connection.uiConfig?.user?.id }
                .subscribe({ game ->
                    games.add(game)
                }, { e ->
                    println(e)
                }, {
                    if (!MainActivity.isInForeground) {
                        if (games.size > 0) {
                            val style = NotificationCompat.InboxStyle()
                                    .setSummaryText("Your move in ${games.size} game(s)")

                            val sha = MessageDigest.getInstance("SHA-1")
                            games.forEach {
                                var opponent = (it.black as? Map<*, *>)?.get("username")
                                if (connection.uiConfig?.user?.id == it.blackId) {
                                    opponent = (it.white as? Map<*, *>)?.get("username")
                                }
                                style.addLine("vs $opponent")
                                sha.update(it.toString().toByteArray())
                            }
                            val hash = sha.digest()

                            if(!lastNotificationHash.contentEquals(hash)) {
                                lastNotificationHash = hash

                                val notification = NotificationCompat.Builder(this, "active_games")
                                        .setContentTitle("Your move in ${games.size} game(s)")
                                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                                        .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_launcher_foreground))
                                        .setStyle(style)
                                        .setContentIntent(PendingIntent.getActivity(this, 0, LoginActivity.getIntent(this), PendingIntent.FLAG_UPDATE_CURRENT))
                                        .setAutoCancel(true)
                                        .setVibrate(longArrayOf(100, -1))
                                        .build()

                                (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                                        .notify(0, notification)
                            }
                        }
                    }
                    connection.disconnect()
                    jobFinished(job, false)
                })
        return true // Answers the question: "Is there still work going on?"
    }

    override fun onStopJob(job: JobParameters): Boolean {
        return false // Answers the question: "Should this job be retried?"
    }
}