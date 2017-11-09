package io.zenandroid.onlinego

import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.AppCompatImageView
import android.view.MenuItem
import android.widget.Toast
import butterknife.BindView
import butterknife.ButterKnife
import io.reactivex.android.schedulers.AndroidSchedulers
import io.zenandroid.onlinego.ogs.ActiveGameService
import io.zenandroid.onlinego.spectate.SpectateFragment


class MainActivity : AppCompatActivity() {

    @BindView(R.id.bottom_navigation) lateinit var bottomNavigation: BottomNavigationView

    private val spectateFragment = SpectateFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        ButterKnife.bind(this)

        setSupportActionBar(findViewById(R.id.toolbar))

        bottomNavigation.setOnNavigationItemSelectedListener { selectItem(it) }
        bottomNavigation.selectedItemId = R.id.navigation_spectate

        ActiveGameService.myMoveCountSubject
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ myMoveCount ->
            val button = findViewById<AppCompatImageView>(R.id.notifications)
            button.isEnabled = myMoveCount > 0
            button.animate().alpha(if(myMoveCount == 0) .33f else 1f)
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
                Toast.makeText(this, "Not implemented yet", Toast.LENGTH_LONG).show()
                false
            }
            else -> false
        }

    }
}
