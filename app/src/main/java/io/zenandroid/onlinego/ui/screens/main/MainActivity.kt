package io.zenandroid.onlinego.ui.screens.main

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import com.google.android.gms.common.wrappers.Wrappers.packageManager
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.data.model.ogs.Size
import io.zenandroid.onlinego.data.model.ogs.Speed
import io.zenandroid.onlinego.notifications.SynchronizeGamesWork
import io.zenandroid.onlinego.ui.screens.login.FacebookLoginCallbackActivity
import io.zenandroid.onlinego.data.model.ogs.ChallengeParams
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import io.zenandroid.onlinego.ui.views.BoardView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get


class MainActivity : ComponentActivity(), MainContract.View {
    companion object {
        var isInForeground = false
    }

    private val userSessionRepository: UserSessionRepository = get()

    private var requestPermissionLauncher: ActivityResultLauncher<String>? = null

    private val presenter: MainPresenter by lazy { MainPresenter(this, get(), get(), get(), get()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OnlineGoApp(isLoggedIn = userSessionRepository.isLoggedIn())
        }

//        Handler(Looper.getMainLooper()).post {
//            ViewCompat.getWindowInsetsController(binding.root)?.isAppearanceLightStatusBars =
//                resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_NO
//        }

        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
        createNotificationChannel()
        scheduleNotificationJob()

        packageManager.setComponentEnabledSetting(
            ComponentName(this, FacebookLoginCallbackActivity::class.java),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )

        BoardView.preloadResources(resources)
    }

    private fun scheduleNotificationJob() {
        SynchronizeGamesWork.schedule()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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
                    NotificationChannel("active_correspondence_games", "Correspondence Games", NotificationManager.IMPORTANCE_LOW).apply {
                        group = "your_turn"
                        enableLights(true)
                        lightColor = Color.WHITE
                        enableVibration(true)
                        vibrationPattern = longArrayOf(0, 200, 0, 200)

                    },
                    NotificationChannel("active_live_games", "Live Games", NotificationManager.IMPORTANCE_LOW).apply {
                        group = "your_turn"
                        enableLights(true)
                        lightColor = Color.WHITE
                        enableVibration(true)
                        vibrationPattern = longArrayOf(0, 200, 0, 200)

                    },
                    NotificationChannel("active_blitz_games", "Blitz Games", NotificationManager.IMPORTANCE_LOW).apply {
                        group = "your_turn"
                        enableLights(true)
                        lightColor = Color.WHITE
                        enableVibration(true)
                        vibrationPattern = longArrayOf(0, 200, 0, 200)

                    },
                    NotificationChannel("active_games", "Your Turn", NotificationManager.IMPORTANCE_LOW).apply {
                        enableLights(true)
                        lightColor = Color.WHITE
                        enableVibration(true)
                        vibrationPattern = longArrayOf(0, 200, 0, 200)
                    },
                    NotificationChannel("challenges", "Challenges", NotificationManager.IMPORTANCE_LOW).apply {
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

    override fun onDestroy() {
        super.onDestroy()
        requestPermissionLauncher = null
    }

    override fun askForNotificationsPermission(delayed: Boolean) {
        CoroutineScope(Dispatchers.Main).launch {
            if(delayed) {
                delay(5000)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissionLauncher?.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    override fun onResume() {
        presenter.subscribe()

        isInForeground = true
        super.onResume()
    }

    override fun showLogin() {
//        findNavController(R.id.fragment_container).apply {
//            if(currentDestination?.id != R.id.onboardingFragment) {
//                navigate(R.id.onboardingFragment)
//            }
//        }
    }

    override fun onPause() {
        super.onPause()

        presenter.unsubscribe()
        isInForeground = false
    }
}
