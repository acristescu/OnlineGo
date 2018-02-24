package io.zenandroid.onlinego

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
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.firebase.jobdispatcher.*
import com.firebase.jobdispatcher.Constraint.ON_ANY_NETWORK
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.extensions.fadeIn
import io.zenandroid.onlinego.extensions.fadeOut
import io.zenandroid.onlinego.game.GameFragment
import io.zenandroid.onlinego.gamelogic.Util
import io.zenandroid.onlinego.model.ogs.Game
import io.zenandroid.onlinego.mygames.MyGamesFragment
import io.zenandroid.onlinego.newchallenge.NewChallengeView
import io.zenandroid.onlinego.ogs.ActiveGameService
import io.zenandroid.onlinego.ogs.OGSServiceImpl
import io.zenandroid.onlinego.spectate.ChallengesFragment
import io.zenandroid.onlinego.spectate.SpectateFragment


class MainActivity : AppCompatActivity() {

    companion object {
        var isInForeground = false
    }

    @BindView(R.id.bottom_navigation) lateinit var bottomNavigation: BottomNavigationView
    @BindView(R.id.badge) lateinit var badge: TextView
    @BindView(R.id.notifications) lateinit var notificationsButton: ImageView
    @BindView(R.id.new_challenge) lateinit var newChallengeView: NewChallengeView

    private val spectateFragment = SpectateFragment()
    private val myGamesFragment = MyGamesFragment()
    private val challengesFragment = ChallengesFragment()

    private lateinit var lastSelectedItem: MenuItem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        ButterKnife.bind(this)

        setSupportActionBar(findViewById(R.id.toolbar))

        bottomNavigation.setOnNavigationItemSelectedListener(this::selectItem)
        bottomNavigation.selectedItemId = R.id.navigation_my_games

        createNotificationChannel()

        val dispatcher = FirebaseJobDispatcher(GooglePlayDriver(this))
        val job = dispatcher.newJobBuilder()
                .setLifetime(Lifetime.FOREVER)
                .setRecurring(true)
                .setTag("poller")
                .setTrigger(Trigger.executionWindow(300, 600))
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
            notificationChannel.lightColor = Color.RED
            notificationChannel.enableVibration(true)
            notificationChannel.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    override fun onResume() {
        super.onResume()
        title = "OnlineGo"
        supportActionBar?.subtitle = "beta ${BuildConfig.VERSION_CODE}"

        newChallengeView.onResume()

        ActiveGameService.myMoveCountObservable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { myMoveCount ->
                    if(myMoveCount == 0) {
                        notificationsButton.isEnabled = false
                        notificationsButton.animate().alpha(.33f)
                        badge.animate().alpha(0f)
                        NotificationsService.cancelNotification(this)
                    } else {
                        notificationsButton.isEnabled = true
                        notificationsButton.animate().alpha(1f)
                        badge.text = myMoveCount.toString()
                        badge.animate().alpha(1f)
                        NotificationsService.updateNotification(this, ActiveGameService.activeGamesList, OGSServiceImpl.instance)
                    }
                }
        isInForeground = true
    }

    override fun onPause() {
        super.onPause()
        newChallengeView.onPause()
        isInForeground = false
    }

    @OnClick(R.id.notifications)
    fun onNotificationsClicked() {
        ActiveGameService.activeGamesObservable
                .filter { Util.isMyTurn(it) }
                .firstElement()
                .subscribe(this@MainActivity::navigateToGameScreen)
    }

    private fun selectItem(item: MenuItem): Boolean {
        lastSelectedItem = item
        if(bottomNavigation.visibility != View.VISIBLE) {
            bottomNavigation.visibility = View.VISIBLE
            bottomNavigation.animate()
                    .translationY(0f)
                    .alpha(1f)
            newChallengeView.fadeIn().subscribe()
        }
        return when(item.itemId) {
            R.id.navigation_spectate -> {
                supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, spectateFragment)
                        .commit()
                true
            }
            R.id.navigation_challenges -> {
                supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, challengesFragment)
                        .commit()
                true
            }
            R.id.navigation_my_games -> {
                supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, myGamesFragment)
                        .commit()
                true
            }
            else -> false
        }

    }

    fun navigateToGameScreenById(gameId: Long) {
        OGSServiceImpl.instance.fetchGame(gameId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::navigateToGameScreen)
    }

    fun navigateToGameScreen(game: Game) {
        Completable.mergeArray(
                bottomNavigation.fadeOut(),
                newChallengeView.fadeOut()
        ).subscribe {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, GameFragment.createFragment(game), "game")
                    .commit()
        }
    }

    override fun onBackPressed() {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        when(fragment) {
            is GameFragment -> selectItem(lastSelectedItem)
            else -> super.onBackPressed()
        }
    }
}
