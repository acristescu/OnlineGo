package io.zenandroid.onlinego.utils

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.res.ResourcesCompat
import android.view.View
import android.widget.RemoteViews
import androidx.navigation.NavDeepLinkBuilder
import java.util.Locale
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.gamelogic.RulesManager
import io.zenandroid.onlinego.data.model.local.Challenge
import io.zenandroid.onlinego.data.model.local.ChallengeNotification
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.data.model.local.GameNotificationWithDetails
import io.zenandroid.onlinego.data.model.ogs.Phase
import io.zenandroid.onlinego.ui.screens.main.MainActivity
import io.zenandroid.onlinego.ui.views.BoardView

/**
 * Created by alex on 07/03/2018.
 */
class NotificationUtils {
    companion object {
        const val NOTIFICATION_ID = 0
        val TAG = NotificationUtils::class.java.simpleName

        fun cancelNotification() {
            val notificationManager = OnlineGoApplication.instance.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancelAll()
        }

        fun cancelNotification(id: Int) {
            val notificationManager = OnlineGoApplication.instance.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(id)
        }

        private fun supportsNotificationGrouping() =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.N

        fun notifyGames(context: Context, games: List<Game>, lastNotifications: List<GameNotificationWithDetails>, userId: Long) {
            val newGames = games.filter { game ->
                lastNotifications.find { it.notification.gameId == game.id } == null
            }
            val finishedGames = lastNotifications
                    .filter { gameNotification ->
                        games.find { it.id == gameNotification.notification.gameId } == null
                    }.filter { it.games.isNotEmpty() }
                    .map { it.games[0] }

            val gamesThatChanged = games.filter { game ->
                lastNotifications.find {
                    it.notification.gameId == game.id && (it.notification.moves != game.moves || it.notification.phase != game.phase)
                } != null
            }

            val gamesToNotify = (newGames + gamesThatChanged + finishedGames).filter { game ->
                when {
                    game.phase == Phase.PLAY -> game.playerToMoveId == userId
                    game.phase == Phase.STONE_REMOVAL -> {
                        val myRemovedStones = if(userId == game.whitePlayer.id) game.whitePlayer.acceptedStones else game.blackPlayer.acceptedStones
                        game.removedStones != myRemovedStones
                    }
                    game.phase == Phase.FINISHED -> true
                    else -> false
                }
            }

            val gamesToClear = (newGames + gamesThatChanged).filter { game ->
                when {
                    lastNotifications.find {
                        it.notification.gameId == game.id
                    } == null -> false

                    game.phase == Phase.FINISHED -> false
                    game.phase == Phase.PLAY && game.playerToMoveId != userId -> true
                    game.phase == Phase.STONE_REMOVAL -> {
                        val myRemovedStones = if(userId == game.whitePlayer.id) game.whitePlayer.acceptedStones else game.blackPlayer.acceptedStones
                        game.removedStones == myRemovedStones
                    }
                    else -> false
                }
            }

            for (game in gamesToClear) {
                cancelNotification(game.id.toInt())
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
        }

        fun notifyChallenges(context: Context, challenges: List<Challenge>, previousNotifications: List<ChallengeNotification>, userId: Long) {
            val notificationIntent = Intent(context, MainActivity::class.java)
            notificationIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            val pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE)

            challenges
                    .filter { it.challenger?.id != userId }
                    .filter { previousNotifications.find { previous -> it.id == previous.id } == null }
                    .forEach {
                        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.notify(it.id.toInt(),
                                NotificationCompat.Builder(context, "challenges")
                                        .setContentTitle("A new challenge!")
                                        .setContentText("${it.challenger?.username} has issued a challenge")
                                        .setContentIntent(pendingIntent)
                                        .setVibrate(arrayOf(0L, 200L, 0L, 200L).toLongArray())
                                        .setSmallIcon(R.drawable.ic_notification_go_board)
                                        .setColor(ResourcesCompat.getColor(context.resources, R.color.colorTextSecondary, null))
                                        .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
                                        .setStyle(NotificationCompat.BigTextStyle())
                                        .setAutoCancel(true)
                                        .build()
                        )
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
            val board = BoardView(context).apply {
                animationEnabled = false
            }
            games.forEach {
                context.resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width)
                //TODO: FIXME!!!
                val pendingIntent = NavDeepLinkBuilder(context)
                    .setComponentName(MainActivity::class.java)
//                    .setGraph(R.navigation.graph)
//                    .setDestination(R.id.gameFragment)
//                    .setArguments(bundleOf(
//                        GAME_ID to it.id,
//                        GAME_WIDTH to it.width,
//                        GAME_HEIGHT to it.height
//                    ))
                    .createPendingIntent()

                val opponent = if (userId == it.blackPlayer.id) it.whitePlayer.username else it.blackPlayer.username
                val message = when (it.phase) {
                    Phase.FINISHED -> {
                        val outcome = when {
                            it.outcome == "Cancellation" -> "Cancelled"
                            userId == it.blackPlayer.id ->
                                if (it.blackLost == true) "Lost by ${it.outcome}"
                                else "Won by ${it.outcome}"

                            userId == it.whitePlayer.id ->
                                if (it.whiteLost == true) "Lost by ${it.outcome}"
                                else "Won by ${it.outcome}"

                            it.whiteLost == true ->
                                "Black won by ${it.outcome}"

                            else ->
                                "White won by ${it.outcome}"
                        }
                        "Game ended - $outcome"
                    }

                    Phase.PLAY -> "Your turn"
                    Phase.STONE_REMOVAL -> "Stone removal phase"
                    else -> "${it.phase} Requires your attention"
                }
                val category = when (it.timeControl?.speed?.lowercase(Locale.ROOT)) {
                    "correspondence" -> "active_correspondence_games"
                    "live" -> "active_live_games"
                    "blitz" -> "active_blitz_games"
                    else -> "active_games"
                }
                val timeLimit = System.currentTimeMillis() + timeLeftForCurrentPlayer(it)
                val remoteView = RemoteViews(context.packageName, R.layout.notification_board)

                board.boardWidth = it.width
                board.boardHeight = it.height
                board.position = RulesManager.replay(it, computeTerritory = false)
                remoteView.setImageViewBitmap(R.id.notification_bitmap, board.convertToContentBitmap())
                val notification =
                    NotificationCompat.Builder(context, category)
                        .setContentTitle(opponent)
                        .setContentText(message)
                        .setContentIntent(pendingIntent)
                        .setVibrate(arrayOf(0L, 200L, 0L, 200L).toLongArray())
                        .setLargeIcon(board.convertToIconBitmap())
                        .setSmallIcon(R.drawable.ic_notification_go_board)
                        .setColor(ResourcesCompat.getColor(context.resources, R.color.colorTextSecondary, null))
                        .setGroup("GAME_NOTIFICATIONS")
                        .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
                        .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                        .setCustomBigContentView(remoteView)
                        .apply {
                            if (it.phase == Phase.PLAY && it.timeControl?.speed != "correspondence" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                setChronometerCountDown(true)
                                    .setUsesChronometer(true)
                                    .setShowWhen(true)
                                    .setWhen(timeLimit)
                                    .setOngoing(true)
                            } else {
                                setAutoCancel(true)
                            }
                        }
                        .build()

                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(it.id.toInt(), notification)
            }
        }

        private fun notifySummary(context: Context, games: List<Game>, userId: Long?) {
            val notificationIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)!!
            notificationIntent.`package` = null
            notificationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            val pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE)

            val notification =
                    NotificationCompat.Builder(context, "active_games")
                            .setContentTitle("${games.size} games require your attention")
                            .setContentText("${games.size} games require your attention")
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

        fun notifyLogout(context: Context) {
            val notificationIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)!!
            notificationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            val pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE)

            val notification =
                    NotificationCompat.Builder(context, "logout")
                            .setContentTitle("Please log in again")
                            .setContentText("You have been logged out of the app. This usually happens because you changed your password. Please log in again to re-enable notifications")
                            .setContentIntent(pendingIntent)
                            .setSmallIcon(R.drawable.ic_notification_go_board)
                            .setColor(ResourcesCompat.getColor(context.resources, R.color.colorTextSecondary, null))
                            .setStyle(NotificationCompat.BigTextStyle())
                            .setAutoCancel(true)
                            .build()

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(0, notification)
        }
    }
}
