package io.zenandroid.onlinego

import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.AppCompatImageView
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import butterknife.BindView
import butterknife.ButterKnife
import io.reactivex.android.schedulers.AndroidSchedulers
import io.zenandroid.onlinego.game.GameFragment
import io.zenandroid.onlinego.model.ogs.Game
import io.zenandroid.onlinego.mygames.MyGamesFragment
import io.zenandroid.onlinego.ogs.ActiveGameService
import io.zenandroid.onlinego.spectate.SpectateFragment


class MainActivity : AppCompatActivity() {

    @BindView(R.id.bottom_navigation) lateinit var bottomNavigation: BottomNavigationView
    @BindView(R.id.badge) lateinit var badge: TextView

    private val spectateFragment = SpectateFragment()
    private val myGamesFragment = MyGamesFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        ButterKnife.bind(this)

        setSupportActionBar(findViewById(R.id.toolbar))

        bottomNavigation.setOnNavigationItemSelectedListener(this::selectItem)
        bottomNavigation.selectedItemId = R.id.navigation_my_games

    }

    override fun onResume() {
        super.onResume()

        ActiveGameService.myMoveCountObservable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ myMoveCount ->
                    val button = findViewById<AppCompatImageView>(R.id.notifications)
                    if(myMoveCount == 0) {
                        button.isEnabled = false
                        button.animate().alpha(.33f)
                        badge.animate().alpha(0f)
                    } else {
                        button.isEnabled = true
                        button.animate().alpha(1f)
                        badge.text = myMoveCount.toString()
                        badge.animate().alpha(1f)
                    }
                })
    }

    private fun selectItem(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.navigation_spectate -> {
                supportFragmentManager.beginTransaction().replace(R.id.fragment_container, spectateFragment).commit()
                true
            }
            R.id.navigation_challenges -> {
                Toast.makeText(this, "Not implemented yet", Toast.LENGTH_LONG).show()
                false
            }
            R.id.navigation_my_games -> {
                supportFragmentManager.beginTransaction().replace(R.id.fragment_container, myGamesFragment).commit()
                true
            }
            else -> false
        }

    }

    fun navigateToGameScreen(game: Game) {
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container, GameFragment.createFragment(game)).commit()
    }
}
