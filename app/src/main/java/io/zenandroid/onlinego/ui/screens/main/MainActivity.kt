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
import androidx.core.content.res.ResourcesCompat
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
import io.zenandroid.onlinego.data.model.ogs.OGSGame
import io.zenandroid.onlinego.data.model.ogs.Size
import io.zenandroid.onlinego.data.model.ogs.Speed
import io.zenandroid.onlinego.ui.screens.mygames.MyGamesFragment
import io.zenandroid.onlinego.ui.screens.newchallenge.NewAutomatchChallengeBottomSheet
import io.zenandroid.onlinego.ui.screens.newchallenge.NewChallengeBottomSheet
import io.zenandroid.onlinego.notifications.SynchronizeGamesWork
import io.zenandroid.onlinego.ui.screens.settings.SettingsFragment
import io.zenandroid.onlinego.data.repositories.SettingsRepository
import io.zenandroid.onlinego.gamelogic.Util
import io.zenandroid.onlinego.ui.screens.stats.StatsFragment
import io.zenandroid.onlinego.ui.items.statuschips.Chip
import io.zenandroid.onlinego.ui.items.statuschips.ChipAdapter
import io.zenandroid.onlinego.ui.screens.localai.AiGameFragment
import io.zenandroid.onlinego.ui.views.BoardView
import io.zenandroid.onlinego.utils.*
import kotlinx.android.synthetic.main.activity_main.*
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import java.util.*


class MainActivity : AppCompatActivity(), MainContract.View {
    companion object {
        var isInForeground = false
        val TAG = MainActivity::class.java.simpleName
    }

    private val myGamesFragment = MyGamesFragment()
    private val learnFragment = LearnFragment()
    private val settingsFragment = SettingsFragment()
    private val statsFragment = StatsFragment.createFragment(Util.getCurrentUserId()!!)

    private val analytics = OnlineGoApplication.instance.analytics
    private val chipAdapter = ChipAdapter()
    private var unreadCount = 0

    private val settingsRepository: SettingsRepository by inject()

    val chatClicks: Observable<Any> by lazy { RxView.clicks(chatButton) }

    private val locationsStack = Stack<MenuItem>()
    private var currentItem: MenuItem? = null

    private val presenter: MainPresenter by lazy { MainPresenter(this, get(), get(), get(), get(), get()) }

    var loading: Boolean = false
        set(value) {
            field = value
            progressBar.showIf(value)
        }

    var mainTitle: CharSequence? = null
        set(value) {
            field = value
            titleView.text = value
        }

    fun setLogoVisible(visible: Boolean) {
        titleView.showIf(!visible)
        logo.showIf(visible)
    }

    fun setChipsVisible(visible: Boolean) {
        chipList.showIf(visible)
    }

    fun setChatButtonVisible(visible: Boolean) {
        chatButton.showIf(visible)
        chatBadge.showIf(visible && unreadCount != 0)
    }

    override var notificationsButtonEnabled: Boolean
        get() = notificationsButton.isEnabled
        set(value) {
            notificationsButton.isEnabled = value
            val targetAlpha = if(value) 1f else .33f
            notificationsButton.animate().alpha(targetAlpha)
        }
    override var notificationsBadgeVisible: Boolean
        get() { return badge.alpha == 1f }
        set(value) { badge.animate().alpha(if(value) 1f else 0f) }

    override var notificationsBadgeCount: String? = null
        set(value) { badge.text = value }

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
                if(locationsStack.isNotEmpty()) {
                    selectItem(locationsStack.pop(), false)
                } else {
                    onBackPressed()
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        setSupportActionBar(findViewById(R.id.toolbar))

        bottomNavigation.setOnNavigationItemSelectedListener(this::selectItem)
        if(intent.hasExtra("GAME_ID")) {
            presenter.navigateToGameScreenById(intent.getLongExtra("GAME_ID", 0))
        }
        bottomNavigation.selectedItemId = R.id.navigation_my_games

        createNotificationChannel()
        scheduleNotificationJob()

        chipList.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        chipList.adapter = chipAdapter

        bottomNavigation.disableShiftMode()
        bottomNavigation.getOrCreateBadge(R.id.navigation_learn).apply {
            isVisible = !PersistenceManager.visitedJosekiExplorer
            backgroundColor = ResourcesCompat.getColor(resources, R.color.colorAccent, theme)
        }
        notificationsButton.setOnClickListener { onNotificationClicked() }

        newChallengeView.apply {
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

    private fun selectItem(item: MenuItem): Boolean =
        selectItem(item, true)

    private fun selectItem(item: MenuItem, addToStack: Boolean): Boolean {
        if(currentItem == item) {
            return true
        }
        if(addToStack) {
            currentItem?.let { locationsStack.add(it) }
        }
        currentItem = item
        return when(item.itemId) {
            R.id.navigation_my_games -> {
                bottomNavigation.selectedItemId = item.itemId
                supportFragmentManager.beginTransaction()
                        .setCustomAnimations(R.anim.fade_in, R.anim.fade_out,
                                R.anim.fade_in, R.anim.fade_out)
                        .replace(R.id.fragment_container, myGamesFragment)
                        .runOnCommit (this::ensureNavigationVisible)
                        .commitAllowingStateLoss()
                true
            }
            R.id.navigation_learn -> {
                bottomNavigation.selectedItemId = item.itemId
                supportFragmentManager.beginTransaction()
                        .setCustomAnimations(R.anim.fade_in, R.anim.fade_out,
                                R.anim.fade_in, R.anim.fade_out)
                        .replace(R.id.fragment_container, learnFragment)
                        .runOnCommit (this::ensureNavigationVisible)
                        .commitAllowingStateLoss()
                true
            }
            R.id.navigation_settings -> {
                bottomNavigation.selectedItemId = item.itemId
                supportFragmentManager.beginTransaction()
                        .setCustomAnimations(R.anim.fade_in, R.anim.fade_out,
                                R.anim.fade_in, R.anim.fade_out)
                        .replace(R.id.fragment_container, settingsFragment)
                        .runOnCommit (this::ensureNavigationVisible)
                        .commitAllowingStateLoss()
                true
            }
            R.id.navigation_stats -> {
                bottomNavigation.selectedItemId = item.itemId
                supportFragmentManager.beginTransaction()
                        .setCustomAnimations(R.anim.fade_in, R.anim.fade_out,
                                R.anim.fade_in, R.anim.fade_out)
                        .replace(R.id.fragment_container, statsFragment)
                        .runOnCommit (this::ensureNavigationVisible)
                        .commitAllowingStateLoss()
                true
            }
            else -> {
                Toast.makeText(this, "Not implemented yet!", Toast.LENGTH_SHORT).show()
                false
            }
        }
    }

    private fun ensureNavigationVisible() {
        if(bottomNavigation.visibility != View.VISIBLE) {
            bottomNavigation.visibility = View.VISIBLE
            bottomNavigation.getOrCreateBadge(R.id.navigation_learn).isVisible = !PersistenceManager.visitedJosekiExplorer
            Completable.mergeArray(
                    newChallengeView.fadeIn(),
                    newChallengeView.showFab()
            ).subscribe()
        }
    }

    override fun navigateToGameScreen(game: Game) {
        currentItem?.let (locationsStack::push)
        currentItem = null
        bottomNavigation.visibility = View.GONE
        newChallengeView.fadeOut().subscribe()
        supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out,
                        R.anim.fade_in, R.anim.fade_out)
                .addToBackStack(null)
                .replace(R.id.fragment_container, GameFragment.createFragment(game), "game")
                .commitAllowingStateLoss()
    }

    override fun navigateToStatsScreen(id: Long) {
        currentItem?.let(locationsStack::push)
        currentItem = null
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
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        when {
            fragment is JosekiExplorerFragment && fragment.canHandleBack() -> fragment.onBackPressed()
            newChallengeView.subMenuVisible -> newChallengeView.toggleSubMenu()
            fragment is StatsFragment && currentItem == null -> supportFragmentManager.popBackStackImmediate()

            fragment is MyGamesFragment || locationsStack.empty() -> super.onBackPressed()

            else -> selectItem(locationsStack.pop(), false)
        }
    }

    fun setNewMessagesCount(count: Int) {
        if(count == 0) {
            if(unreadCount != 0) {
                chatBadge.fadeOut().subscribe()
            }
        } else {
            if(unreadCount == 0) {
                chatBadge.fadeIn().subscribe()
            }
            chatBadge.text = count.toString()
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
        currentItem?.let (locationsStack::push)
        currentItem = null
        bottomNavigation.visibility = View.GONE
        newChallengeView.fadeOut().subscribe()
        supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out,
                        R.anim.fade_in, R.anim.fade_out)
                .replace(R.id.fragment_container, JosekiExplorerFragment(), "game")
                .commitAllowingStateLoss()
    }

    fun onLocalAIClicked() {
        currentItem?.let (locationsStack::push)
        currentItem = null
        bottomNavigation.visibility = View.GONE
        newChallengeView.fadeOut().subscribe()
        supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out,
                        R.anim.fade_in, R.anim.fade_out)
                .replace(R.id.fragment_container, AiGameFragment(), "localai")
                .commitAllowingStateLoss()
    }
}
