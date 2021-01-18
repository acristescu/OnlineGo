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
import android.view.View
import android.widget.Toast
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.Completable
import io.reactivex.Observable
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.ui.screens.game.GameFragment
import io.zenandroid.onlinego.ui.screens.joseki.JosekiExplorerFragment
import io.zenandroid.onlinego.ui.screens.learn.LearnFragment
import io.zenandroid.onlinego.ui.screens.login.LoginActivity
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.data.model.local.Tutorial
import io.zenandroid.onlinego.data.model.ogs.OGSGame
import io.zenandroid.onlinego.data.model.ogs.Size
import io.zenandroid.onlinego.data.model.ogs.Speed
import io.zenandroid.onlinego.ui.screens.mygames.MyGamesFragment
import io.zenandroid.onlinego.ui.screens.newchallenge.NewAutomatchChallengeBottomSheet
import io.zenandroid.onlinego.ui.screens.newchallenge.NewChallengeBottomSheet
import io.zenandroid.onlinego.notifications.SynchronizeGamesWork
import io.zenandroid.onlinego.data.repositories.SettingsRepository
import io.zenandroid.onlinego.databinding.ActivityMainBinding
import io.zenandroid.onlinego.gamelogic.Util
import io.zenandroid.onlinego.ui.screens.stats.StatsFragment
import io.zenandroid.onlinego.ui.items.statuschips.Chip
import io.zenandroid.onlinego.ui.items.statuschips.ChipAdapter
import io.zenandroid.onlinego.ui.screens.localai.AiGameFragment
import io.zenandroid.onlinego.ui.screens.settings.SettingsFragment
import io.zenandroid.onlinego.ui.screens.supporter.SupporterFragment
import io.zenandroid.onlinego.ui.screens.tutorial.TutorialFragment
import io.zenandroid.onlinego.ui.views.BoardView
import io.zenandroid.onlinego.utils.*
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject


class MainActivity : AppCompatActivity(), MainContract.View {
    companion object {
        var isInForeground = false
        val TAG = MainActivity::class.java.simpleName
    }

    private val myGamesFragment = MyGamesFragment()
    @ExperimentalAnimationApi
    @ExperimentalMaterialApi
    private val learnFragment = LearnFragment()
    private val settingsFragment = SettingsFragment()
    private val statsFragment = StatsFragment.createFragment(Util.getCurrentUserId()!!)

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

    fun setChipsVisible(visible: Boolean) {
        binding.chipList.showIf(visible)
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
        when (item.itemId) {
            android.R.id.home -> {
                if(supportFragmentManager.backStackEntryCount > 0) {
                    goBackOneScreen()
                } else {
                    onBackPressed()
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun goBackOneScreen() {
        do {
            supportFragmentManager.popBackStackImmediate()

            // Game fragments are added to the stack each time we tap on the notification button,
            // so we keep unstacking them until we reach another kind of fragment.
        } while (getCurrentFragment() is GameFragment)
    }

    private fun getCurrentFragment(): Fragment? {
        return supportFragmentManager.findFragmentById(R.id.fragment_container)
    }

    @ExperimentalAnimationApi
    @ExperimentalMaterialApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(findViewById(R.id.toolbar))

        binding.bottomNavigation.selectedItemId = R.id.navigation_my_games
        supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out,
                        R.anim.fade_in, R.anim.fade_out)
                .replace(R.id.fragment_container, myGamesFragment)
                .commitAllowingStateLoss()

        binding.bottomNavigation.setOnNavigationItemSelectedListener(this::selectItem)
        supportFragmentManager.addOnBackStackChangedListener {
            binding.bottomNavigation.setOnNavigationItemSelectedListener(null)
            when(supportFragmentManager.fragments.lastOrNull()) {
                is MyGamesFragment -> {
                    binding.bottomNavigation.selectedItemId = R.id.navigation_my_games
                    ensureNavigationVisible()
                    setToolbarVisible(true)
                }
                is LearnFragment -> {
                    binding.bottomNavigation.selectedItemId = R.id.navigation_learn
                    ensureNavigationVisible()
                    setToolbarVisible(true)
                }
                statsFragment -> {
                    binding.bottomNavigation.selectedItemId = R.id.navigation_stats
                    ensureNavigationVisible()
                    setToolbarVisible(true)
                }
                is SettingsFragment -> {
                    binding.bottomNavigation.selectedItemId = R.id.navigation_settings
                    ensureNavigationVisible()
                    setToolbarVisible(false)
                }
            }
            binding.bottomNavigation.setOnNavigationItemSelectedListener(this::selectItem)
        }
        if(intent.hasExtra("GAME_ID")) {
            presenter.navigateToGameScreenById(intent.getLongExtra("GAME_ID", 0))
        }

        createNotificationChannel()
        scheduleNotificationJob()

        binding.chipList.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        binding.chipList.adapter = chipAdapter

        binding.bottomNavigation.disableShiftMode()
        binding.bottomNavigation.getOrCreateBadge(R.id.navigation_learn).apply {
            isVisible = !PersistenceManager.visitedJosekiExplorer
            backgroundColor = ResourcesCompat.getColor(resources, R.color.colorAccent, theme)
        }
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

    @ExperimentalAnimationApi
    @ExperimentalMaterialApi
    private fun selectItem(item: MenuItem): Boolean {
        if(item.itemId == binding.bottomNavigation.selectedItemId) {
            return true
        }
        return when(item.itemId) {
            R.id.navigation_my_games -> {
                setToolbarVisible(true)
                ensureNavigationVisible()
                supportFragmentManager.beginTransaction()
                        .setCustomAnimations(R.anim.fade_in, R.anim.fade_out,
                                R.anim.fade_in, R.anim.fade_out)
                        .replace(R.id.fragment_container, myGamesFragment)
                        .addToBackStack(null)
                        .commitAllowingStateLoss()
                true
            }
            R.id.navigation_learn -> {
                setToolbarVisible(true)
                ensureNavigationVisible()
                supportFragmentManager.beginTransaction()
                        .setCustomAnimations(R.anim.fade_in, R.anim.fade_out,
                                R.anim.fade_in, R.anim.fade_out)
                        .replace(R.id.fragment_container, learnFragment)
                        .addToBackStack(null)
                        .commitAllowingStateLoss()
                true
            }
            R.id.navigation_settings -> {
                setToolbarVisible(false)
                binding.bottomNavigation.visibility = View.VISIBLE
                binding.newChallengeView.fadeOut().subscribe()
                supportFragmentManager.beginTransaction()
                        .setCustomAnimations(R.anim.fade_in, R.anim.fade_out,
                                R.anim.fade_in, R.anim.fade_out)
                        .replace(R.id.fragment_container, settingsFragment)
                        .addToBackStack(null)
                        .commitAllowingStateLoss()
                true
            }
            R.id.navigation_stats -> {
                setToolbarVisible(true)
                ensureNavigationVisible()
                supportFragmentManager.beginTransaction()
                        .setCustomAnimations(R.anim.fade_in, R.anim.fade_out,
                                R.anim.fade_in, R.anim.fade_out)
                        .replace(R.id.fragment_container, statsFragment)
                        .addToBackStack(null)
                        .commitAllowingStateLoss()
                true
            }
            else -> {
                Toast.makeText(this, "Not implemented yet!", Toast.LENGTH_SHORT).show()
                false
            }
        }
    }

    private fun setToolbarVisible(visible: Boolean) {
        binding.toolbar.showIf(visible)
    }

    fun ensureNavigationVisible() {
        if(binding.bottomNavigation.visibility != View.VISIBLE) {
            binding.bottomNavigation.visibility = View.VISIBLE
            binding.bottomNavigation.getOrCreateBadge(R.id.navigation_learn).isVisible = !PersistenceManager.visitedJosekiExplorer
            Completable.mergeArray(
                    binding.newChallengeView.fadeIn(),
                    binding.newChallengeView.showFab()
            ).subscribe()
        }
    }

    override fun navigateToGameScreen(game: Game) {
        setToolbarVisible(true)
        binding.bottomNavigation.visibility = View.GONE
        binding.newChallengeView.fadeOut().subscribe()
        supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out,
                        R.anim.fade_in, R.anim.fade_out)
                .addToBackStack(null)
                .replace(R.id.fragment_container, GameFragment.createFragment(game), "game")
                .commitAllowingStateLoss()
    }

    override fun navigateToStatsScreen(id: Long) {
        supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out,
                        R.anim.fade_in, R.anim.fade_out)
                .addToBackStack(null)
                .replace(R.id.fragment_container, StatsFragment.createFragment(id))
                .commitAllowingStateLoss()
    }

    override fun showError(msg: String?) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    override fun onBackPressed() {
        val fragment = getCurrentFragment()
        when {
            fragment is JosekiExplorerFragment && fragment.canHandleBack() -> fragment.onBackPressed()
            binding.newChallengeView.subMenuVisible -> binding.newChallengeView.toggleSubMenu()
            supportFragmentManager.backStackEntryCount > 0 -> goBackOneScreen()

            else -> super.onBackPressed()
        }
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

    fun navigateToJosekiExplorer() {
        setToolbarVisible(true)
        binding.bottomNavigation.visibility = View.GONE
        binding.newChallengeView.fadeOut().subscribe()
        supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out,
                        R.anim.fade_in, R.anim.fade_out)
                .replace(R.id.fragment_container, JosekiExplorerFragment())
                .addToBackStack(null)
                .commitAllowingStateLoss()
    }

    fun navigateToTutorialScreen(tutorial: Tutorial) {
        setToolbarVisible(false)
        binding.bottomNavigation.visibility = View.GONE
        binding.newChallengeView.fadeOut().subscribe()
        supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out,
                        R.anim.fade_in, R.anim.fade_out)
                .replace(R.id.fragment_container, TutorialFragment.newInstance(tutorial.name))
                .addToBackStack(null)
                .commitAllowingStateLoss()
    }

    fun onLocalAIClicked() {
        setToolbarVisible(true)
        binding.bottomNavigation.visibility = View.GONE
        binding.newChallengeView.fadeOut().subscribe()
        supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out,
                        R.anim.fade_in, R.anim.fade_out)
                .replace(R.id.fragment_container, AiGameFragment())
                .addToBackStack(null)
                .commitAllowingStateLoss()
    }

    fun navigateToSupporterScreen() {
        setToolbarVisible(false)
        binding.bottomNavigation.visibility = View.GONE
        binding.newChallengeView.fadeOut().subscribe()
        supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out,
                        R.anim.fade_in, R.anim.fade_out)
                .replace(R.id.fragment_container, SupporterFragment())
                .addToBackStack(null)
                .commitAllowingStateLoss()
    }
}
