package io.zenandroid.onlinego.main

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
import android.support.design.widget.BottomNavigationView
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.firebase.jobdispatcher.*
import com.firebase.jobdispatcher.Constraint.ON_ANY_NETWORK
import io.reactivex.Completable
import io.zenandroid.onlinego.NotificationsService
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.extensions.disableShiftMode
import io.zenandroid.onlinego.extensions.fadeIn
import io.zenandroid.onlinego.extensions.fadeOut
import io.zenandroid.onlinego.extensions.showIf
import io.zenandroid.onlinego.game.GameFragment
import io.zenandroid.onlinego.learn.LearnFragment
import io.zenandroid.onlinego.login.LoginActivity
import io.zenandroid.onlinego.model.local.Game
import io.zenandroid.onlinego.model.ogs.OGSGame
import io.zenandroid.onlinego.mygames.MyGamesFragment
import io.zenandroid.onlinego.newchallenge.NewChallengeView
import io.zenandroid.onlinego.ogs.ActiveGameRepository
import io.zenandroid.onlinego.settings.SettingsFragment
import io.zenandroid.onlinego.stats.StatsFragment
import io.zenandroid.onlinego.statuschips.Chip
import io.zenandroid.onlinego.statuschips.ChipAdapter
import io.zenandroid.onlinego.utils.NotificationUtils


class MainActivity : AppCompatActivity(), MainContract.View {
    companion object {
        var isInForeground = false
        var userId: Long? = null
        val TAG = MainActivity::class.java.simpleName
    }

    @BindView(R.id.bottom_navigation) lateinit var bottomNavigation: BottomNavigationView
    @BindView(R.id.badge) lateinit var badge: TextView
    @BindView(R.id.notifications) lateinit var notificationsButton: ImageView
    @BindView(R.id.new_challenge) lateinit var newChallengeView: NewChallengeView
    @BindView(R.id.progress_bar) lateinit var progressBar: ProgressBar
    @BindView(R.id.title) lateinit var titleView: TextView
    @BindView(R.id.chipList) lateinit var chipList: RecyclerView

    private val myGamesFragment = MyGamesFragment()
    private val learnFragment = LearnFragment()
    private val settingsFragment = SettingsFragment()
    private val statsFragment = StatsFragment()

    private val analytics = OnlineGoApplication.instance.analytics
    private val chipAdapter = ChipAdapter()

    val activeGameRepository: ActiveGameRepository by lazy { ActiveGameRepository() }

    private lateinit var lastSelectedItem: MenuItem

    private val presenter: MainPresenter by lazy { MainPresenter(this, activeGameRepository) }

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
        val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            v.vibrate(20)
        }
    }

    override fun updateNotification(sortedMyTurnGames: List<OGSGame>) {
        NotificationUtils.updateNotification(this, sortedMyTurnGames, MainActivity.userId)
    }

    override fun cancelNotification() {
        NotificationUtils.cancelNotification(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        ButterKnife.bind(this)

        setSupportActionBar(findViewById(R.id.toolbar))

        bottomNavigation.setOnNavigationItemSelectedListener(this::selectItem)
        bottomNavigation.selectedItemId = R.id.navigation_my_games

        createNotificationChannel()
        scheduleNotificationJob()

        chipList.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        chipList.adapter = chipAdapter

        bottomNavigation.disableShiftMode()

        newChallengeView.showFab().subscribe()
    }

    private fun scheduleNotificationJob() {
        val dispatcher = FirebaseJobDispatcher(GooglePlayDriver(this))
        val job = dispatcher.newJobBuilder()
                .setLifetime(Lifetime.FOREVER)
                .setRecurring(true)
                .setTag("poller")
                .setTrigger(Trigger.executionWindow(100, 600))
                .setReplaceCurrent(true)
                .setRetryStrategy(RetryStrategy.DEFAULT_LINEAR)
                .setService(NotificationsService::class.java)
                .setConstraints(ON_ANY_NETWORK)
                .build()
        dispatcher.mustSchedule(job)
    }

    @SuppressLint("NewApi")
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channelId = "active_games"
            val channelName = "Active Games"
            val importance = NotificationManager.IMPORTANCE_LOW
            val notificationChannel = NotificationChannel(channelId, channelName, importance)
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.WHITE
            notificationChannel.enableVibration(true)
            notificationChannel.vibrationPattern = longArrayOf(0, 200, 0, 200)
            notificationManager.createNotificationChannel(notificationChannel)
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
    }

    override fun onPause() {
        super.onPause()

        presenter.unsubscribe()
        isInForeground = false
    }

    @OnClick(R.id.notifications)
    fun onNotificationClicked() {
        analytics.logEvent("my_move_clicked", null)
        presenter.onNotificationClicked()
    }

    private fun selectItem(item: MenuItem): Boolean {
        lastSelectedItem = item
        return when(item.itemId) {
            R.id.navigation_my_games -> {
                supportFragmentManager.beginTransaction()
                        .setCustomAnimations(R.anim.fade_in, R.anim.fade_out,
                                R.anim.fade_in, R.anim.fade_out)
                        .replace(R.id.fragment_container, myGamesFragment)
                        .runOnCommit (this::ensureNavigationVisible)
                        .commit()
                true
            }
            R.id.navigation_learn -> {
                supportFragmentManager.beginTransaction()
                        .setCustomAnimations(R.anim.fade_in, R.anim.fade_out,
                                R.anim.fade_in, R.anim.fade_out)
                        .replace(R.id.fragment_container, learnFragment)
                        .runOnCommit (this::ensureNavigationVisible)
                        .commit()
                true
            }
            R.id.navigation_settings -> {
                supportFragmentManager.beginTransaction()
                        .setCustomAnimations(R.anim.fade_in, R.anim.fade_out,
                                R.anim.fade_in, R.anim.fade_out)
                        .replace(R.id.fragment_container, settingsFragment)
                        .runOnCommit (this::ensureNavigationVisible)
                        .commit()
                true
            }
            R.id.navigation_stats -> {
                supportFragmentManager.beginTransaction()
                        .setCustomAnimations(R.anim.fade_in, R.anim.fade_out,
                                R.anim.fade_in, R.anim.fade_out)
                        .replace(R.id.fragment_container, statsFragment)
                        .runOnCommit (this::ensureNavigationVisible)
                        .commit()
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
            Completable.mergeArray(
                    newChallengeView.fadeIn(),
                    newChallengeView.showFab()
            ).subscribe()
        }
    }

    override fun navigateToGameScreen(game: Game) {
        bottomNavigation.visibility = View.GONE
        newChallengeView.fadeOut().subscribe()
        supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out,
                        R.anim.fade_in, R.anim.fade_out)
                .replace(R.id.fragment_container, GameFragment.createFragment(game), "game")
                .commitAllowingStateLoss()
    }

    override fun showError(msg: String?) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    override fun onBackPressed() {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        when(fragment) {
            is GameFragment -> selectItem(lastSelectedItem)
            else -> super.onBackPressed()
        }
    }

    fun navigateToGameScreenById(gameId: Long) {
        presenter.navigateToGameScreenById(gameId)
    }
}
