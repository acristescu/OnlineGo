package io.zenandroid.onlinego

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
import com.firebase.jobdispatcher.*
import com.firebase.jobdispatcher.Constraint.ON_ANY_NETWORK
import io.reactivex.android.schedulers.AndroidSchedulers
import io.zenandroid.onlinego.game.GameFragment
import io.zenandroid.onlinego.model.ogs.Game
import io.zenandroid.onlinego.mygames.MyGamesFragment
import io.zenandroid.onlinego.ogs.ActiveGameService
import io.zenandroid.onlinego.spectate.ChallengesFragment
import io.zenandroid.onlinego.spectate.SpectateFragment


class MainActivity : AppCompatActivity() {

    companion object {
        var isInForeground = false
    }

    @BindView(R.id.bottom_navigation) lateinit var bottomNavigation: BottomNavigationView
    @BindView(R.id.badge) lateinit var badge: TextView
    @BindView(R.id.notifications) lateinit var notificationsButton: ImageView
    @BindView(R.id.fab) lateinit var fab: FloatingActionButton

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
        val dispatcher = FirebaseJobDispatcher(GooglePlayDriver(this))
        val job = dispatcher.newJobBuilder()
                .setLifetime(Lifetime.FOREVER)
                .setRecurring(true)
                .setTag("poller")
                .setTrigger(Trigger.executionWindow(1200, 7200))
                .setReplaceCurrent(true)
                .setRetryStrategy(RetryStrategy.DEFAULT_LINEAR)
                .setService(NotificationsService::class.java)
                .setConstraints(ON_ANY_NETWORK)
                .build()
        dispatcher.mustSchedule(job)
    }

    override fun onResume() {
        super.onResume()

        ActiveGameService.myMoveCountObservable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ myMoveCount ->
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
                })
        isInForeground = true
    }

    override fun onPause() {
        super.onPause()
        isInForeground = false
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

    override fun onBackPressed() {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        when(fragment) {
            is GameFragment -> selectItem(lastSelectedItem)
            else -> super.onBackPressed()
        }
    }
}
