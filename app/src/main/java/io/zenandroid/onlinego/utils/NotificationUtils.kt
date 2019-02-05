package io.zenandroid.onlinego.utils

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v4.content.res.ResourcesCompat
import android.view.View
import android.widget.RemoteViews
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.gamelogic.RulesManager
import io.zenandroid.onlinego.login.LoginActivity
import io.zenandroid.onlinego.model.local.Game
import io.zenandroid.onlinego.model.local.GameNotification
import io.zenandroid.onlinego.model.ogs.Phase
import io.zenandroid.onlinego.views.BoardView


/**
 * Created by alex on 07/03/2018.
 */
class NotificationUtils {
    companion object {
        const val NOTIFICATION_ID = 0
        val TAG = NotificationUtils::class.java.simpleName

        fun cancelNotification(context: Context) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancelAll()
        }

        private fun supportsNotificationGrouping() =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.N

        fun updateNotification(context: Context, games: List<Game>, notifications: List<GameNotification>, userId: Long) {
            val newGames = games.filter { game ->
                notifications.find { it.gameId == game.id } == null
            }
            val finishedGames = notifications.filter { gameNotification ->
                games.find { it.id == gameNotification.gameId } == null
            }
            val gamesThatChanged = games.filter { game ->
                notifications.find {
                    it.gameId == game.id && (it.moves != game.moves || it.phase != game.phase)
                } != null
            }

            val gamesToNotify = (newGames + gamesThatChanged).filter { game ->
                when {
                    game.phase == Phase.PLAY -> game.playerToMoveId == userId
                    game.phase == Phase.STONE_REMOVAL -> {
                        val myRemovedStones = if(userId == game.whitePlayer.id) game.whitePlayer.acceptedStones else game.blackPlayer.acceptedStones
                        game.removedStones != myRemovedStones
                    }
                    else -> false
                }
            }

            if (gamesToNotify.isNotEmpty()) {
                when {
                    supportsNotificationGrouping() -> {
                        notifyIndividual(context, gamesToNotify, userId)
                        notifySummary(context, gamesToNotify, userId)
                    }
                    gamesToNotify.size == 1 -> notifyIndividual(context, gamesToNotify, userId)
                    else -> notifySummary(context, gamesToNotify, userId)
                }
            }

            val newNotifications = games.map { GameNotification(it.id, it.moves, it.phase) }
            if(newNotifications != notifications) {
                OnlineGoApplication.instance.db.gameDao().replaceGameNotifications(newNotifications)
            }
        }

        private fun View.convertToContentBitmap(): Bitmap {
            val screenWidth = context.resources.displayMetrics.widthPixels
            val widthSpec = View.MeasureSpec.makeMeasureSpec(screenWidth, View.MeasureSpec.AT_MOST)
            val heightSpec = View.MeasureSpec.makeMeasureSpec((256 * context.resources.displayMetrics.density).toInt(), View.MeasureSpec.AT_MOST)
            measure(widthSpec, heightSpec)
            layout(0, 0, measuredWidth, measuredHeight)
            val r = Bitmap.createBitmap(measuredHeight, measuredHeight, Bitmap.Config.ARGB_8888)
            r.eraseColor(Color.TRANSPARENT)
            val canvas = Canvas(r)
//            canvas.translate(measuredHeight/2f, 0f)
            draw(canvas)
            return r
        }

        private fun View.convertToIconBitmap(): Bitmap {
            val width = context.resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width)
            val height = context.resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_height)
            measure(width, height)
            layout(0, 0, measuredWidth, measuredHeight)
            val r = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            r.eraseColor(Color.TRANSPARENT)
            val canvas = Canvas(r)
            canvas.translate((width - measuredWidth)/2f, (height - measuredHeight)/2f)
            draw(canvas)
            return r
        }

        private fun notifyIndividual(context: Context, games: List<Game>, userId: Long?) {
            val board = BoardView(context)
            games.forEach {
                context.resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width)
                val notificationIntent = Intent(context, LoginActivity::class.java)
                notificationIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                notificationIntent.putExtra("GAME_ID", it.id)
                val pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, FLAG_UPDATE_CURRENT)

                val opponent = if (userId == it.blackPlayer.id) it.whitePlayer.username else it.blackPlayer.username
                val message = if(it.phase == Phase.STONE_REMOVAL) "Stone removal phase" else "Your turn"
                val remoteView = RemoteViews(context.packageName, R.layout.notification_board)

                board.boardSize = it.height
                board.position = RulesManager.replay(it, computeTerritory = false)
                remoteView.setImageViewBitmap(R.id.notification_bitmap, board.convertToContentBitmap())
                val notification =
                        NotificationCompat.Builder(context, "active_games")
                                .setContentTitle(opponent)
                                .setContentText(message)
                                .setContentIntent(pendingIntent)
                                .setVibrate(arrayOf(0L, 200L, 0L, 200L).toLongArray())
                                .setLargeIcon(board.convertToIconBitmap())
                                .setSmallIcon(R.drawable.ic_notification_go_board)
                                .setColor(ResourcesCompat.getColor(context.resources, R.color.colorTextSecondary, null))
                                .setGroup("GAME_NOTIFICATIONS")
                                .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
                                .setStyle(
                                        NotificationCompat.DecoratedCustomViewStyle()
                                )
                                .setCustomBigContentView(remoteView)
                                .setAutoCancel(true)
                                .build()

                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(it.id.toInt(), notification)
            }
        }

        private fun notifySummary(context: Context, games: List<Game>, userId: Long?) {
            val notificationIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)!!
            notificationIntent.`package` = null
            notificationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            val pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, FLAG_UPDATE_CURRENT)

            val notification =
                    NotificationCompat.Builder(context, "active_games")
                            .setContentTitle("Your turn in ${games.size} games")
                            .setContentText("Your turn in ${games.size} games")
                            .setAutoCancel(true)
                            .setContentIntent(pendingIntent)
                            .setSmallIcon(R.drawable.ic_notification_go_board)
                            .setColor(ResourcesCompat.getColor(context.resources, R.color.colorTextSecondary, null))
                            .setGroupSummary(true)
                            .setGroup("GAME_NOTIFICATIONS")
                            .setInboxStyle(games, userId)
                            .build()

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
        }

        private fun NotificationCompat.Builder.setInboxStyle(games: List<Game>, userId: Long?): NotificationCompat.Builder {
            val inboxStyle = NotificationCompat.InboxStyle()
            games.forEach {
                val opponent = if (userId == it.blackPlayer.id) it.whitePlayer.username else it.blackPlayer.username
                inboxStyle.addLine("vs $opponent")
            }
            setStyle(inboxStyle)
            return this
        }
    }
}