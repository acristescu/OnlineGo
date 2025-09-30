package io.zenandroid.onlinego.ui.screens.main

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.vectorResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.util.Consumer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.zenandroid.onlinego.BuildConfig
import io.zenandroid.onlinego.data.model.BoardTheme
import io.zenandroid.onlinego.notifications.SynchronizeGamesWork
import io.zenandroid.onlinego.ui.screens.login.FacebookLoginCallbackActivity
import io.zenandroid.onlinego.ui.theme.LocalPreloadedImages
import io.zenandroid.onlinego.ui.theme.LocalThemeSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.viewmodel.ext.android.viewModel


class MainActivity : ComponentActivity() {
  companion object {
    var isInForeground = false
  }

  private val viewModel: MainActivityViewModel by viewModel()

  override fun onCreate(savedInstanceState: Bundle?) {
    val splashScreen = installSplashScreen()
    super.onCreate(savedInstanceState)
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

    FirebaseCrashlytics.getInstance().log("MainActivity.onCreate() intentURL=${intent.data}")

    var themeSettings by mutableStateOf(
      ThemeSettings(
        isDarkTheme = resources.configuration.isSystemInDarkTheme,
        boardTheme = BoardTheme.WOOD,
        dynamicColors = true,
        showCoordinates = true
      )
    )
    var preloadedImages by mutableStateOf(PreloadedImages(null, null, null))

    if (intent?.action == Intent.ACTION_VIEW && intent?.data != null) {
      //
      // If the app was launched from a link, just dismiss the splash screen immediately
      //
      viewModel.onScreenReady()
    }

    lifecycleScope.launch {
      lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        combine(
          isSystemInDarkTheme(),
          viewModel.state,
        ) { systemDark, state ->
          ThemeSettings(
            isDarkTheme = when (state.appTheme) {
              "System Default" -> systemDark
              "Light" -> false
              "Dark" -> true
              else -> systemDark
            },
            boardTheme = state.boardTheme ?: BoardTheme.WOOD,
            dynamicColors = true,
            showCoordinates = state.showCoordinates,
          )
        }
          .onEach { themeSettings = it }
          .map { it.isDarkTheme }
          .distinctUntilChanged()
          .collect { darkTheme ->
            // Turn off the decor fitting system windows, which allows us to handle insets,
            // including IME animations, and go edge-to-edge.
            // This is the same parameters as the default enableEdgeToEdge call, but we manually
            // resolve whether or not to show dark theme using uiState, since it can be different
            // than the configuration's dark theme value based on the user preference.
            enableEdgeToEdge(
              statusBarStyle = SystemBarStyle.auto(
                lightScrim = Color.TRANSPARENT,
                darkScrim = Color.TRANSPARENT,
              ) { darkTheme },
              navigationBarStyle = SystemBarStyle.auto(
                lightScrim = lightScrim,
                darkScrim = darkScrim,
              ) { darkTheme },
            )
          }
      }
    }

//    splashScreen.setKeepOnScreenCondition {
//      val keep = !viewModel.state.value.isLoaded && !hasWindowFocus()
//      if (!keep) {
//        FirebaseCrashlytics.getInstance().log(
//          "Splash dismissed at ${System.currentTimeMillis()}"
//        )
//        Log.d("MainActivity", "Splash dismissed at ${System.currentTimeMillis()}")
//      }
//      keep
//    }

    setContent {
      val state by viewModel.state.collectAsStateWithLifecycle()
      LaunchedEffect(themeSettings.isDarkTheme, themeSettings.boardTheme) {
        preloadedImages = preloadImages(themeSettings)
      }

      state.hasCompletedOnboarding?.let { hasCompletedOnboarding ->
        CompositionLocalProvider(
          LocalThemeSettings provides themeSettings,
          LocalPreloadedImages provides preloadedImages,
        ) {
          OnlineGoApp(
            onAppReady = { viewModel.onScreenReady() },
            darkTheme = themeSettings.isDarkTheme,
            isLoggedIn = state.isLoggedIn == true,
            hasCompletedOnboarding = hasCompletedOnboarding,
          )
        }
      }
    }

    lifecycleScope.launch(Dispatchers.IO) {
      createNotificationChannel()
      scheduleNotificationJob()
    }

    packageManager.setComponentEnabledSetting(
      ComponentName(this, FacebookLoginCallbackActivity::class.java),
      PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
      PackageManager.DONT_KILL_APP
    )
  }

  private suspend fun preloadImages(themeSettings: ThemeSettings): PreloadedImages {
    return withContext(Dispatchers.IO) {
      val background =
        if (themeSettings.isDarkTheme) themeSettings.boardTheme.backgroundImageDarkMode else themeSettings.boardTheme.backgroundImage
      return@withContext PreloadedImages(
        background = background?.let {
          ImageBitmap.imageResource(resources, id = it)
        },
        whiteStone = ImageVector.vectorResource(
          theme,
          resources,
          themeSettings.boardTheme.whiteStone
        ),
        blackStone = ImageVector.vectorResource(
          theme,
          resources,
          themeSettings.boardTheme.blackStone
        )
      )
    }
  }

  private fun scheduleNotificationJob() {
    SynchronizeGamesWork.schedule(applicationContext)
  }

  private fun createNotificationChannel() {
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
        NotificationChannelGroup("your_turn", "Your Turn")
      )

      notificationManager.createNotificationChannels(
        listOf(
          NotificationChannel(
            "active_correspondence_games",
            "Correspondence Games",
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
            "Live Games",
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
            "Blitz Games",
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
            "Your Turn",
            NotificationManager.IMPORTANCE_LOW
          ).apply {
            enableLights(true)
            lightColor = Color.WHITE
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 200, 0, 200)
          },
          NotificationChannel(
            "challenges",
            "Challenges",
            NotificationManager.IMPORTANCE_LOW
          ).apply {
            enableLights(true)
            lightColor = Color.WHITE
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 200, 0, 200)

          },
          NotificationChannel("logout", "Logout", NotificationManager.IMPORTANCE_LOW).apply {
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
    FirebaseCrashlytics.getInstance().log("MainActivity.onNewIntent() intentURL=${intent.data}")
  }

  override fun onResume() {
    FirebaseCrashlytics.getInstance().log("MainActivity.onResume()")
    viewModel.onResume()
    isInForeground = true
    super.onResume()
  }

  override fun onPause() {
    FirebaseCrashlytics.getInstance().log("MainActivity.onPause()")
    super.onPause()
    viewModel.onPause()
    isInForeground = false
    FirebaseCrashlytics.getInstance().log("MainActivity.onPause() DONE")
  }

  override fun onWindowFocusChanged(hasFocus: Boolean) {
    super.onWindowFocusChanged(hasFocus)
    Log.d("MainActivity", "onWindowFocusChanged: hasFocus=$hasFocus")
    FirebaseCrashlytics.getInstance().log("MainActivity.onWindowFocusChanged: hasFocus=$hasFocus")
  }

  private var firstTouchLogged = false
  override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
    if (!firstTouchLogged) {
      firstTouchLogged = true
      Log.d(
        "MainActivity", "First touch event: ${ev?.action} at ${System.currentTimeMillis()} " +
            " hasWindowFocus=${hasWindowFocus()}"
      )
      FirebaseCrashlytics.getInstance().log(
        "First touch event: ${ev?.action} at ${System.currentTimeMillis()} " +
            " hasWindowFocus=${hasWindowFocus()}"
      )
    }
    return super.dispatchTouchEvent(ev)
  }
}

/**
 * Convenience wrapper for dark mode checking
 */
val Configuration.isSystemInDarkTheme
  get() = (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

/**
 * Registers listener for configuration changes to retrieve whether system is in dark theme or not.
 * Immediately upon subscribing, it sends the current value and then registers listener for changes.
 */
fun ComponentActivity.isSystemInDarkTheme() = callbackFlow {
  channel.trySend(resources.configuration.isSystemInDarkTheme)

  val listener = Consumer<Configuration> {
    channel.trySend(it.isSystemInDarkTheme)
  }

  addOnConfigurationChangedListener(listener)

  awaitClose { removeOnConfigurationChangedListener(listener) }
}
  .distinctUntilChanged()
  .conflate()

data class ThemeSettings(
  val isDarkTheme: Boolean,
  val boardTheme: BoardTheme,
  val dynamicColors: Boolean,
  val showCoordinates: Boolean,
)

data class PreloadedImages(
  val background: ImageBitmap?,
  val whiteStone: ImageVector?,
  val blackStone: ImageVector?,
)

/**
 * The default light scrim, as defined by androidx and the platform:
 * https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:activity/activity/src/main/java/androidx/activity/EdgeToEdge.kt;l=35-38;drc=27e7d52e8604a080133e8b842db10c89b4482598
 */
private val lightScrim = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)

/**
 * The default dark scrim, as defined by androidx and the platform:
 * https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:activity/activity/src/main/java/androidx/activity/EdgeToEdge.kt;l=40-44;drc=27e7d52e8604a080133e8b842db10c89b4482598
 */
private val darkScrim = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
