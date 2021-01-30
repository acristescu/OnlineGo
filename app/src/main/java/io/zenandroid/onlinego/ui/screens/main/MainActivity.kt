package io.zenandroid.onlinego.ui.screens.main

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.MenuItem
import android.widget.Toast
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.core.os.bundleOf
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.Observable
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.ui.screens.login.LoginActivity
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.data.model.ogs.OGSGame
import io.zenandroid.onlinego.data.model.ogs.Size
import io.zenandroid.onlinego.data.model.ogs.Speed
import io.zenandroid.onlinego.ui.screens.newchallenge.NewAutomatchChallengeBottomSheet
import io.zenandroid.onlinego.ui.screens.newchallenge.NewChallengeBottomSheet
import io.zenandroid.onlinego.notifications.SynchronizeGamesWork
import io.zenandroid.onlinego.data.repositories.SettingsRepository
import io.zenandroid.onlinego.databinding.ActivityMainBinding
import io.zenandroid.onlinego.ui.items.statuschips.Chip
import io.zenandroid.onlinego.ui.items.statuschips.ChipAdapter
import io.zenandroid.onlinego.ui.screens.game.GAME_ID
import io.zenandroid.onlinego.ui.screens.game.GAME_SIZE
import io.zenandroid.onlinego.ui.views.BoardView
import io.zenandroid.onlinego.utils.*
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject


class MainActivity : AppCompatActivity(), MainContract.View {
    companion object {
        var isInForeground = false
        val TAG = MainActivity::class.java.simpleName
    }

    private val analytics = OnlineGoApplication.instance.analytics
    private val chipAdapter = ChipAdapter()
    private var unreadCount = 0

    private lateinit var binding: ActivityMainBinding
    private val settingsRepository: SettingsRepository by inject()

    val chatClicks: Observable<Any> by lazy { RxView.clicks(binding.chatButton) }

    private val presenter: MainPresenter by lazy { MainPresenter(this, get(), get(), get(), get(), get()) }

    var loading: Boolean = false
        set(value) {
            field = value
            binding.progressBar.showIf(value)
        }

    var mainTitle: CharSequence? = null
        set(value) {
            field = value
            binding.titleView.text = value
        }

    fun setLogoVisible(visible: Boolean) {
        binding.titleView.showIf(!visible)
        binding.logo.showIf(visible)
    }

    fun setChatButtonVisible(visible: Boolean) {
        binding.chatButton.showIf(visible)
        binding.chatBadge.showIf(visible && unreadCount != 0)
    }

    override var notificationsButtonEnabled: Boolean
        get() = binding.notificationsButton.isEnabled
        set(value) {
            binding.notificationsButton.isEnabled = value
            val targetAlpha = if(value) 1f else .33f
            binding.notificationsButton.animate().alpha(targetAlpha)
        }
    override var notificationsBadgeVisible: Boolean
        get() { return binding.badge.alpha == 1f }
        set(value) { binding.badge.animate().alpha(if(value) 1f else 0f) }

    override var notificationsBadgeCount: String? = null
        set(value) { binding.badge.text = value }

    override fun vibrate() {
        if(!settingsRepository.vibrate) {
            return
        }
        val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            v.vibrate(20)
        }
    }

    override fun updateNotification(sortedMyTurnGames: List<OGSGame>) {
//        NotificationUtils.updateNotification(this, sortedMyTurnGames, MainActivity.userId)
    }

    override fun cancelNotification() {
        NotificationUtils.cancelNotification(this)
    }

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

        setSupportActionBar(binding.toolbar)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
        binding.bottomNavigation.setupWithNavController(navHostFragment.navController)
        navHostFragment.navController.addOnDestinationChangedListener { _, destination, arguments ->
            binding.apply {
                bottomNavigation.showIf(destination.id in arrayOf(R.id.myGames, R.id.learn, R.id.settings) || (destination.id == R.id.stats && arguments?.isEmpty != false))
                newChallengeView.showIf(destination.id == R.id.myGames)
                toolbar.showIf(destination.id in arrayOf(R.id.myGames, R.id.learn, R.id.stats, R.id.gameFragment, R.id.aiGameFragment, R.id.josekiExplorerFragment))
                chipList.showIf(destination.id == R.id.gameFragment)
            }
            setChatButtonVisible(destination.id == R.id.gameFragment)
        }

        createNotificationChannel()
        scheduleNotificationJob()

        binding.chipList.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        binding.chipList.adapter = chipAdapter

        binding.bottomNavigation.disableShiftMode()
        binding.notificationsButton.setOnClickListener { onNotificationClicked() }

        binding.newChallengeView.apply {
            showFab().subscribe()
            onAutomatchClicked = this@MainActivity::onAutoMatchSearch
            onOnlineCustomClicked = this@MainActivity::onCustomGameSearch
        }

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

    fun setChips(chips: List<Chip>) = chipAdapter.update(chips)

    override fun onResume() {
        presenter.subscribe()

        isInForeground = true
        super.onResume()
    }

    override fun showLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    override fun onPause() {
        super.onPause()

        presenter.unsubscribe()
        isInForeground = false
    }

    private fun onNotificationClicked() {
        analytics.logEvent("my_move_clicked", null)
        presenter.onNotificationClicked()
    }

    override fun navigateToGameScreen(game: Game) {
        findNavController(R.id.fragment_container)
                .navigate(
                        R.id.gameFragment,
                        bundleOf(GAME_ID to game.id, GAME_SIZE to game.width),
                        NavOptions.Builder()
                                .setLaunchSingleTop(true)
                                .build()
                )
    }

    override fun showError(msg: String?) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    fun setNewMessagesCount(count: Int) {
        if(count == 0) {
            if(unreadCount != 0) {
                binding.chatBadge.fadeOut().subscribe()
            }
        } else {
            if(unreadCount == 0) {
                binding.chatBadge.fadeIn().subscribe()
            }
            binding.chatBadge.text = count.toString()
        }

        unreadCount = count
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

    fun onCustomGameSearch() {
        NewChallengeBottomSheet(this) {
            analytics.logEvent("bot_challenge", null)
            presenter.onNewBotChallenge(it)
        }.show(supportFragmentManager, "BOTTOM_SHEET")
    }

}
