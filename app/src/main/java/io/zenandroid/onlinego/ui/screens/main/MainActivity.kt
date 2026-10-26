package io.zenandroid.onlinego.ui.screens.main

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import co.touchlab.kermit.Logger
import io.zenandroid.onlinego.BuildConfig
import io.zenandroid.onlinego.notifications.SynchronizeGamesWork
import io.zenandroid.onlinego.playstore.PlayStoreService
import io.zenandroid.onlinego.resources.Res
import io.zenandroid.onlinego.resources.notification_channel_blitz_games
import io.zenandroid.onlinego.resources.notification_channel_challenges
import io.zenandroid.onlinego.resources.notification_channel_correspondence_games
import io.zenandroid.onlinego.resources.notification_channel_group_your_turn
import io.zenandroid.onlinego.resources.notification_channel_live_games
import io.zenandroid.onlinego.resources.notification_channel_logout
import io.zenandroid.onlinego.resources.notification_channel_your_turn
import io.zenandroid.onlinego.utils.AppLanguageSetting
import io.zenandroid.onlinego.utils.AppLocaleManager
import io.zenandroid.onlinego.utils.NotificationUtils
import io.zenandroid.onlinego.utils.ReviewPromptManager
import io.zenandroid.onlinego.utils.ReviewRequester
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.koin.android.ext.android.inject


class MainActivity : ComponentActivity() {
  companion object {
    var isInForeground = false
  }

  private val reviewPromptManager: ReviewPromptManager by inject()

  private val appLanguageSetting: AppLanguageSetting by inject()

  private val notificationUtils: NotificationUtils by inject()

  private val playStoreService: PlayStoreService by inject()

  /** Applies the language the user picked in the settings. See [AppLocaleManager]. */
  override fun attachBaseContext(newBase: Context) {
    super.attachBaseContext(AppLocaleManager.wrap(newBase))
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    intent?.data?.let { Logger.i("Deep link: $it", tag = "MainActivity") }
    enableEdgeToEdge()
    if (BuildConfig.DEBUG) {
      StrictMode.setThreadPolicy(
        StrictMode.ThreadPolicy.Builder()
          .detectAll()
          // .penaltyDeath() // Makes the app crash when a violation occurs
          .penaltyLog()     // Logs violations to Logcat
//          .penaltyDialog()  // Shows a dialog (can be annoying but effective)
          .penaltyFlashScreen() // Flashes the screen
          .build()
      )

      StrictMode.setVmPolicy(
        StrictMode.VmPolicy.Builder()
          .detectLeakedSqlLiteObjects()
          .detectLeakedClosableObjects()
          .penaltyLog()
          // .penaltyDeath()
          .build()
      )
    }

    Logger.i("onCreate() intentURL=${intent.data}", tag = "MainActivity")

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
      lifecycleScope.launch {
        appLanguageSetting.language.drop(1).collect {
          if (resources.configuration.locales[0].language != it.localeTag) recreate()
        }
      }
    }

    val reviewRequester = ReviewRequester { reviewPromptManager.requestReview(this) }

    setContent {
      OnlineGoRoot(
        versionCode = BuildConfig.VERSION_CODE,
        reviewRequester = reviewRequester,
        onForceReviewPrompt = { reviewPromptManager.forceShowReviewPrompt(this@MainActivity) },
        onTestNotification = notificationUtils::notifyGameForDebug,
        onLaunchBillingFlow = { product, oldPurchase ->
          playStoreService.launchBillingFlow(this@MainActivity, product, oldPurchase)
        },
        onFinish = ::finish,
      )
    }

    lifecycleScope.launch(Dispatchers.IO) {
      createNotificationChannel()
      scheduleNotificationJob()
    }
  }

  private fun scheduleNotificationJob() {
    SynchronizeGamesWork.schedule(applicationContext)
  }

  private suspend fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val notificationManager =
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

      listOf(
        NotificationChannelGroup("correspondence", "Correspondence"),
        NotificationChannelGroup("live", "Live"),
        NotificationChannelGroup("blitz", "Blitz"),
      )
        .map { it.id }
        .map {
          try {
            notificationManager.deleteNotificationChannelGroup(it)
          } catch (_: Exception) {
            // a bug in Oreo where deleting a non-existant notification channel group throws an NPE
          }
        }

      notificationManager.createNotificationChannelGroup(
        NotificationChannelGroup(
          "your_turn",
          getString(Res.string.notification_channel_group_your_turn)
        )
      )

      notificationManager.createNotificationChannels(
        listOf(
          NotificationChannel(
            "active_correspondence_games",
            getString(Res.string.notification_channel_correspondence_games),
            NotificationManager.IMPORTANCE_LOW
          ).apply {
            group = "your_turn"
            enableLights(true)
            lightColor = Color.WHITE
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 200, 0, 200)

          },
          NotificationChannel(
            "active_live_games",
            getString(Res.string.notification_channel_live_games),
            NotificationManager.IMPORTANCE_LOW
          ).apply {
            group = "your_turn"
            enableLights(true)
            lightColor = Color.WHITE
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 200, 0, 200)

          },
          NotificationChannel(
            "active_blitz_games",
            getString(Res.string.notification_channel_blitz_games),
            NotificationManager.IMPORTANCE_LOW
          ).apply {
            group = "your_turn"
            enableLights(true)
            lightColor = Color.WHITE
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 200, 0, 200)

          },
          NotificationChannel(
            "active_games",
            getString(Res.string.notification_channel_your_turn),
            NotificationManager.IMPORTANCE_LOW
          ).apply {
            enableLights(true)
            lightColor = Color.WHITE
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 200, 0, 200)
          },
          NotificationChannel(
            "challenges",
            getString(Res.string.notification_channel_challenges),
            NotificationManager.IMPORTANCE_LOW
          ).apply {
            enableLights(true)
            lightColor = Color.WHITE
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 200, 0, 200)

          },
          NotificationChannel(
            "logout",
            getString(Res.string.notification_channel_logout),
            NotificationManager.IMPORTANCE_LOW
          ).apply {
            enableLights(false)
            enableVibration(false)
          }
        )
      )
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    Logger.i("onNewIntent() intentURL=${intent.data}", tag = "MainActivity")
  }

  override fun onResume() {
    Logger.i("onResume()", tag = "MainActivity")
    isInForeground = true
    super.onResume()
  }

  override fun onPause() {
    Logger.i("onPause()", tag = "MainActivity")
    super.onPause()
    isInForeground = false
    Logger.i("onPause() DONE", tag = "MainActivity")
  }

  override fun onWindowFocusChanged(hasFocus: Boolean) {
    super.onWindowFocusChanged(hasFocus)
    Logger.i("onWindowFocusChanged: hasFocus=$hasFocus", tag = "MainActivity")
  }

  private var firstTouchLogged = false
  override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
    if (!firstTouchLogged) {
      firstTouchLogged = true
      Logger.i(
        "First touch event: ${ev?.action} at ${System.currentTimeMillis()} " +
            " hasWindowFocus=${hasWindowFocus()}",
        tag = "MainActivity"
      )
    }
    return super.dispatchTouchEvent(ev)
  }
}
