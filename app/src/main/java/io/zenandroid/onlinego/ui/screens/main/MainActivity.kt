package io.zenandroid.onlinego.ui.screens.main

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.core.os.bundleOf
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.data.model.ogs.Size
import io.zenandroid.onlinego.data.model.ogs.Speed
import io.zenandroid.onlinego.databinding.ActivityMainBinding
import io.zenandroid.onlinego.notifications.SynchronizeGamesWork
import io.zenandroid.onlinego.ui.screens.game.GAME_HEIGHT
import io.zenandroid.onlinego.ui.screens.game.GAME_ID
import io.zenandroid.onlinego.ui.screens.game.GAME_WIDTH
import io.zenandroid.onlinego.ui.screens.login.FacebookLoginCallbackActivity
import io.zenandroid.onlinego.ui.screens.newchallenge.ChallengeParams
import io.zenandroid.onlinego.ui.screens.newchallenge.NewAutomatchChallengeBottomSheet
import io.zenandroid.onlinego.ui.screens.newchallenge.NewChallengeBottomSheet
import io.zenandroid.onlinego.ui.views.BoardView
import io.zenandroid.onlinego.utils.*
import org.koin.android.ext.android.get


class MainActivity : AppCompatActivity(), MainContract.View {
    companion object {
        var isInForeground = false
        val TAG = MainActivity::class.java.simpleName
    }

    private val analytics = OnlineGoApplication.instance.analytics

    private lateinit var binding: ActivityMainBinding

    private val presenter: MainPresenter by lazy { MainPresenter(this, get(), get(), get(), get()) }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            findNavController(R.id.fragment_container).navigateUp()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    @ExperimentalAnimationApi
    @ExperimentalMaterialApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowInsetsControllerCompat(window, binding.root).isAppearanceLightStatusBars = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_NO

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
        binding.bottomNavigation.setupWithNavController(navHostFragment.navController)
        navHostFragment.navController.addOnDestinationChangedListener { _, destination, arguments ->
            binding.apply {
                bottomNavigation.apply {
                    showIf(
                        destination.id in arrayOf(
                            R.id.myGames,
                            R.id.learn,
                            R.id.settings
                        ) || (destination.id == R.id.stats && arguments?.isEmpty != false)
                    )
                    setOnNavigationItemReselectedListener { }
                }
                newChallengeView.showIf(destination.id == R.id.myGames)
            }
        }

        createNotificationChannel()
        scheduleNotificationJob()

        binding.newChallengeView.apply {
            showFab().subscribe()
            onAutomatchClicked = this@MainActivity::onAutoMatchSearch
            onOnlineCustomClicked = this@MainActivity::onCustomGameSearch
        }

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

    @SuppressLint("NewApi")
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            notificationManager.createNotificationChannels(
                    listOf(
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

    override fun onResume() {
        presenter.subscribe()

        isInForeground = true
        super.onResume()
    }

    override fun showLogin() {
        findNavController(R.id.fragment_container).apply {
            if(currentDestination?.id != R.id.onboardingFragment) {
                navigate(R.id.onboardingFragment)
            }
        }
    }

    override fun showMyGames() {
        findNavController(R.id.fragment_container).apply {
            if(currentDestination?.id != R.id.myGames) {
                navigate(R.id.myGames)
            }
        }
    }

    override fun onPause() {
        super.onPause()

        presenter.unsubscribe()
        isInForeground = false
    }

    override fun navigateToGameScreen(game: Game) {
        findNavController(R.id.fragment_container)
                .navigate(
                        R.id.gameFragment,
                        bundleOf(
                            GAME_ID to game.id,
                            GAME_WIDTH to game.width,
                            GAME_HEIGHT to game.height,
                        ),
                        NavOptions.Builder()
                                .setLaunchSingleTop(true)
                                .build()
                )
    }

    override fun showError(msg: String?) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    fun onAutoMatchSearch() {
        NewAutomatchChallengeBottomSheet(this) { speed: Speed, sizes: List<Size> ->
            val params = Bundle().apply {
                putString("SPEED", speed.toString())
                putString("SIZE", sizes.joinToString { it.toString() })
            }
            analytics.logEvent("new_game_search", params)
            presenter.onStartSearch(sizes, speed)
        }.show()
    }

    fun onNavigateToSupport() {
        findNavController(R.id.fragment_container).navigate(R.id.supporterFragment)
    }

    fun onCustomGameSearch() {
        NewChallengeBottomSheet().show(supportFragmentManager, "BOTTOM_SHEET")
    }

    fun onNewChallengeSearchClicked(challengeParams: ChallengeParams) {
        analytics.logEvent("bot_challenge", null)
        presenter.onNewBotChallenge(challengeParams)
    }
}
