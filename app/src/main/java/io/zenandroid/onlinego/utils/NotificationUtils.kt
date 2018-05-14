package io.zenandroid.onlinego.utils

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationCompat.BADGE_ICON_NONE
import android.support.v4.content.ContextCompat
import android.support.v4.content.res.ResourcesCompat
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.model.ogs.Game
import java.security.MessageDigest
import android.graphics.Bitmap
import android.graphics.Canvas


/**
 * Created by alex on 07/03/2018.
 */
class NotificationUtils {
    companion object {
        const val HASH_KEY = "LAST_NOTIFICATION_HASH"
        const val COUNT_KEY = "LAST_NOTIFICATION_GAME_COUNT"
        const val NOTIFICATION_ID = 0
        val TAG = NotificationUtils::class.java.simpleName

        fun cancelNotification(context: Context) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(NOTIFICATION_ID)
        }

        fun updateNotification(context: Context, games: List<Game>, userId: Long?) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val lastNotificationCount = prefs.getInt(COUNT_KEY, 0)
            val lastNotificationHash = prefs.getString(HASH_KEY, null)?.toByteArray(Charsets.ISO_8859_1)
            prefs.edit().putInt(COUNT_KEY, games.size).apply()

            if (games.isNotEmpty()) {
                val style = NotificationCompat.InboxStyle()
                        .setSummaryText("Your move in ${games.size} game(s)")

                val sha = MessageDigest.getInstance("SHA-1")
                games.forEach {
                    var opponent = (it.black as? Map<*, *>)?.get("username")
                    if (userId == it.blackId) {
                        opponent = (it.white as? Map<*, *>)?.get("username")
                    }
                    style.addLine("vs $opponent")
                    sha.update(it.id.toString().toByteArray())
                    sha.update(it.move_number.toString().toByteArray())
                }
                val hash = sha.digest()

                if(lastNotificationHash?.contentEquals(hash) != true ) {
                    prefs.edit().putString(HASH_KEY, hash.toString(Charsets.ISO_8859_1)).apply()

                    val notificationIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)!!
                    notificationIntent.`package` = null
                    notificationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                    val pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, FLAG_UPDATE_CURRENT)

                    val vibrationPattern =
                            if(games.size > lastNotificationCount) longArrayOf(0, 200)
                            else longArrayOf(0, 10)
                    val drawable = ContextCompat.getDrawable(context, R.drawable.ic_board_transparent)!!
                    val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)
                    val notification = NotificationCompat.Builder(context, "active_games")
                            .setContentTitle("Your move in ${games.size} game${if(games.size > 1) "s" else ""}")
                            .setSmallIcon(R.drawable.ic_notification_go_board)
                            .setColor(ResourcesCompat.getColor(context.resources, R.color.colorTextSecondary, null))
                            .setBadgeIconType(BADGE_ICON_NONE)
                            .setLargeIcon(bitmap)
                            .setStyle(style)
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(true)
                            .setVibrate(vibrationPattern)
                            .build()


                    notificationManager.notify(NOTIFICATION_ID, notification)
                }
            } else {
                notificationManager.cancel(NOTIFICATION_ID)
            }
        }
    }
}