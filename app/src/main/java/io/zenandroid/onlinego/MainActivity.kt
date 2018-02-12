package io.zenandroid.onlinego

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.design.widget.FloatingActionButton
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
import io.zenandroid.onlinego.extensions.*
import io.zenandroid.onlinego.game.GameFragment
import io.zenandroid.onlinego.gamelogic.Util
import io.zenandroid.onlinego.model.ogs.Game
import io.zenandroid.onlinego.mygames.MyGamesFragment
import io.zenandroid.onlinego.ogs.ActiveGameService
import io.zenandroid.onlinego.spectate.ChallengesFragment
import io.zenandroid.onlinego.spectate.SpectateFragment


class MainActivity : AppCompatActivity() {

    companion object {
        var isInForeground = false
    }

    private enum class FabMenuState {
        OFF, SPEED, SIZE
    }

    @BindView(R.id.bottom_navigation) lateinit var bottomNavigation: BottomNavigationView
    @BindView(R.id.badge) lateinit var badge: TextView
    @BindView(R.id.notifications) lateinit var notificationsButton: ImageView
    @BindView(R.id.fab) lateinit var fab: FloatingActionButton
    @BindView(R.id.fade_out_mask) lateinit var fadeOutMask: View
    @BindView(R.id.long_fab) lateinit var longFab: FloatingActionButton
    @BindView(R.id.long_label) lateinit var longLabel: TextView
    @BindView(R.id.normal_fab) lateinit var normalFab: FloatingActionButton
    @BindView(R.id.normal_label) lateinit var normalLabel: TextView
    @BindView(R.id.blitz_fab) lateinit var blitzFab: FloatingActionButton
    @BindView(R.id.blitz_label) lateinit var blitzLabel: TextView
    @BindView(R.id.small_fab) lateinit var smallFab: FloatingActionButton
    @BindView(R.id.small_label) lateinit var smallLabel: TextView
    @BindView(R.id.medium_fab) lateinit var mediumFab: FloatingActionButton
    @BindView(R.id.medium_label) lateinit var mediumLabel: TextView
    @BindView(R.id.large_fab) lateinit var largeFab: FloatingActionButton
    @BindView(R.id.large_label) lateinit var largeLabel: TextView

    private val spectateFragment = SpectateFragment()
    private val myGamesFragment = MyGamesFragment()
    private val challengesFragment = ChallengesFragment()

    private lateinit var lastSelectedItem: MenuItem
    private var fabMiniSize: Float = 0f
    private var menuState = FabMenuState.OFF


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
        fabMiniSize = resources.getDimension(R.dimen.fab_mini_with_margin)
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

        ActiveGameService.myMoveCountObservable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { myMoveCount ->
                    if(myMoveCount == 0) {
                        notificationsButton.isEnabled = false
                        notificationsButton.animate().alpha(.33f)
                        badge.animate().alpha(0f)
                    } else {
                        notificationsButton.isEnabled = true
                        notificationsButton.animate().alpha(1f)
                        badge.text = myMoveCount.toString()
                        badge.animate().alpha(1f)
                    }
                }
        isInForeground = true
    }

    override fun onPause() {
        super.onPause()
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
            fab.visibility = View.VISIBLE
            fab.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
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

    fun navigateToGameScreen(game: Game) {
        bottomNavigation.animate()
                .translationY(bottomNavigation.height.toFloat())
                .alpha(.33f)
                .withEndAction({
                    bottomNavigation.visibility = View.GONE
                })
        fab.animate()
                .alpha(0f)
                .scaleX(0f)
                .scaleY(0f)
                .withEndAction({
                    fab.visibility = View.GONE
                    supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, GameFragment.createFragment(game), "game")
                            .commit()
                })

    }

    @OnClick(R.id.fab)
    fun onFabClicked() {
        menuState = when(menuState) {
            FabMenuState.OFF -> {
                showSpeedMenu().subscribe()
                FabMenuState.SPEED
            }
            FabMenuState.SPEED -> {
                hideSpeedMenu().subscribe()
                FabMenuState.OFF
            }
            FabMenuState.SIZE -> {
                hideSizeMenu().subscribe()
                FabMenuState.OFF
            }
        }
        fadeOutMask.showIf(menuState != FabMenuState.OFF)
    }

    private fun showSpeedMenu() = Completable.mergeArray(
            fab.rotate(45f),
            longFab.slideIn(fabMiniSize).andThen(longLabel.fadeIn()),
            normalFab.slideIn(2 * fabMiniSize).andThen(normalLabel.fadeIn()),
            blitzFab.slideIn(3 * fabMiniSize).andThen(blitzLabel.fadeIn())
    )

    private fun showSizeMenu() = Completable.mergeArray(
            fab.rotate(45f),
            largeFab.slideIn(fabMiniSize).andThen(largeLabel.fadeIn()),
            mediumFab.slideIn(2 * fabMiniSize).andThen(mediumLabel.fadeIn()),
            smallFab.slideIn(3 * fabMiniSize).andThen(smallLabel.fadeIn())
    )

    private fun hideSpeedMenu() = Completable.mergeArray(
            fab.rotate(0f),
            longLabel.fadeOut().andThen(longFab.slideOut(fabMiniSize)),
            normalLabel.fadeOut().andThen(normalFab.slideOut(2 * fabMiniSize)),
            blitzLabel.fadeOut().andThen(blitzFab.slideOut(3 * fabMiniSize))
    )

    private fun hideSizeMenu() = Completable.mergeArray(
            fab.rotate(0f),
            largeLabel.fadeOut().andThen(largeFab.slideOut(fabMiniSize)),
            mediumLabel.fadeOut().andThen(mediumFab.slideOut(2 * fabMiniSize)),
            smallLabel.fadeOut().andThen(smallFab.slideOut(3 * fabMiniSize))
    )

    @OnClick(R.id.blitz_fab, R.id.long_fab, R.id.normal_fab)
    fun onSpeedClicked() {
        hideSpeedMenu()
                .andThen(showSizeMenu())
                .subscribe { menuState = FabMenuState.SIZE }
    }

    override fun onBackPressed() {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        when(fragment) {
            is GameFragment -> selectItem(lastSelectedItem)
            else -> super.onBackPressed()
        }
    }
}
