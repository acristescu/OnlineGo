package io.zenandroid.onlinego

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.BitmapFactory
import android.preference.PreferenceManager
import android.support.v4.app.NotificationCompat
import com.firebase.jobdispatcher.JobParameters
import com.firebase.jobdispatcher.JobService
import io.zenandroid.onlinego.login.LoginActivity
import io.zenandroid.onlinego.model.ogs.Game
import io.zenandroid.onlinego.ogs.OGSService
import io.zenandroid.onlinego.ogs.OGSServiceImpl
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Created by alex on 24/11/2017.
 */
class NotificationsService : JobService() {
    companion object {
        const val HASH_KEY = "LAST_NOTIFICATION_HASH"
        const val NOTIFICATION_ID = 0

        fun cancelNotification(context: Context) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(NOTIFICATION_ID)
        }

        fun updateNotification(context: Context, games: List<Game>, connection: OGSService) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (games.isNotEmpty()) {
                val style = NotificationCompat.InboxStyle()
                        .setSummaryText("Your move in ${games.size} game(s)")

                val sha = MessageDigest.getInstance("SHA-1")
                games.forEach {
                    var opponent = (it.black as? Map<*, *>)?.get("username")
                    if (connection.uiConfig?.user?.id == it.blackId) {
                        opponent = (it.white as? Map<*, *>)?.get("username")
                    }
                    style.addLine("vs $opponent")
                    sha.update(it.id.toString().toByteArray())
                    sha.update(it.move_number.toString().toByteArray())
                }
                val hash = sha.digest()

                val lastNotificationHash = PreferenceManager.getDefaultSharedPreferences(context)
                        .getString(HASH_KEY, null)?.toByteArray(Charsets.ISO_8859_1)


                if(lastNotificationHash?.contentEquals(hash) != true) {
                    PreferenceManager.getDefaultSharedPreferences(context).edit().putString(HASH_KEY, hash.toString(Charsets.ISO_8859_1)).apply()

                    val notification = NotificationCompat.Builder(context, "active_games")
                            .setContentTitle("Your move in ${games.size} game${if(games.size > 1) "s" else ""}")
                            .setSmallIcon(R.mipmap.ic_board_black_white)
                            .setColor(context.resources.getColor(R.color.colorText))
                            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_board_black_white))
                            .setStyle(style)
                            .setContentIntent(PendingIntent.getActivity(context, 0, LoginActivity.getIntent(context), PendingIntent.FLAG_UPDATE_CURRENT))
                            .setAutoCancel(true)
                            .setVibrate(longArrayOf(100, -1))
                            .build()


                    notificationManager.notify(NOTIFICATION_ID, notification)
                }
            } else {
                notificationManager.cancel(NOTIFICATION_ID)
            }
        }
    }

    override fun onStartJob(job: JobParameters): Boolean {
        // Do some work here

        if(MainActivity.isInForeground) {
            return false
        }
        val connection = OGSServiceImpl()
        connection.loginWithToken()
                .andThen(connection.connectToNotifications())
                .take(10, TimeUnit.SECONDS)
                .filter { it.player_to_move == connection.uiConfig?.user?.id }
                .toList()
                .subscribe({
                    if (!MainActivity.isInForeground) {
                        updateNotification(this, it, connection)
                    }
                    connection.disconnect()
                    jobFinished(job, false)
                }, { e ->
                    println(e)
                    jobFinished(job, true)
                })
        return true // Answers the question: "Is there still work going on?"
    }

    override fun onStopJob(job: JobParameters): Boolean {
        return false // Answers the question: "Should this job be retried?"
    }

}