package io.zenandroid.onlinego.main

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.firebase.jobdispatcher.*
import com.firebase.jobdispatcher.Constraint.ON_ANY_NETWORK
import io.reactivex.Completable
import io.zenandroid.onlinego.NotificationsService
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.extensions.fadeIn
import io.zenandroid.onlinego.extensions.fadeOut
import io.zenandroid.onlinego.game.GameFragment
import io.zenandroid.onlinego.model.ogs.Game
import io.zenandroid.onlinego.mygames.MyGamesFragment
import io.zenandroid.onlinego.newchallenge.NewChallengeView
import io.zenandroid.onlinego.ogs.ActiveGameRepository
import io.zenandroid.onlinego.spectate.ChallengesFragment
import io.zenandroid.onlinego.spectate.SpectateFragment
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

    private val spectateFragment = SpectateFragment()
    private val myGamesFragment = MyGamesFragment()
    private val challengesFragment = ChallengesFragment()

    val activeGameRepository: ActiveGameRepository by lazy { ActiveGameRepository() }

    private lateinit var lastSelectedItem: MenuItem

    private val presenter: MainPresenter by lazy { MainPresenter(this, activeGameRepository) }

    override var subtitle: CharSequence?
        get() = supportActionBar?.subtitle
        set(value) { supportActionBar?.subtitle = value }

    override var mainTitle: CharSequence?
        get() = title
        set(value) { title = value }

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

    override fun updateNotification(sortedMyTurnGames: List<Game>) {
        NotificationUtils.updateNotification(this, sortedMyTurnGames, MainActivity.userId)
    }

    override fun cancelNotification() {
        NotificationUtils.cancelNotification(this)
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

    override fun onResume() {
        presenter.subscribe()

        isInForeground = true
        super.onResume()
    }

    override fun onPause() {
        super.onPause()

        presenter.unsubscribe()
        isInForeground = false
    }

    @OnClick(R.id.notifications)
    fun onNotificationClicked() {
        presenter.onNotificationClicked()
    }

    private fun selectItem(item: MenuItem): Boolean {
        lastSelectedItem = item
        return when(item.itemId) {
            R.id.navigation_spectate -> {
                supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, spectateFragment)
                        .runOnCommit (this::ensureNavigationVisible)
                        .commit()
                true
            }
            R.id.navigation_challenges -> {
                supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, challengesFragment)
                        .runOnCommit (this::ensureNavigationVisible)
                        .commit()
                true
            }
            R.id.navigation_my_games -> {
                supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, myGamesFragment)
                        .runOnCommit (this::ensureNavigationVisible)
                        .commit()
                true
            }
            else -> false
        }
    }

    private fun ensureNavigationVisible() {
        if(bottomNavigation.visibility != View.VISIBLE) {
            bottomNavigation.fadeIn().subscribe()
            newChallengeView.fadeIn().subscribe()
        }
    }

    override fun navigateToGameScreen(game: Game) {
        Completable.mergeArray(
                bottomNavigation.fadeOut(),
                newChallengeView.fadeOut()
        ).subscribe {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, GameFragment.createFragment(game), "game")
                    .commit()
        }
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
