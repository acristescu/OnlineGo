package io.zenandroid.onlinego.mygames

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.awesomedialog.blennersilva.awesomedialoglibrary.AwesomeInfoDialog
import io.reactivex.android.schedulers.AndroidSchedulers
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.login.LoginActivity
import io.zenandroid.onlinego.main.MainActivity
import io.zenandroid.onlinego.model.local.Challenge
import io.zenandroid.onlinego.model.local.Game
import io.zenandroid.onlinego.model.ogs.OGSAutomatch
import io.zenandroid.onlinego.ogs.*
import io.zenandroid.onlinego.reusable.ActiveGameItem
import io.zenandroid.onlinego.reusable.AutomatchItem
import io.zenandroid.onlinego.reusable.ChallengeItem
import io.zenandroid.onlinego.reusable.FinishedGameItem
import kotlinx.android.synthetic.main.fragment_mygames.*

/**
 * Created by alex on 05/11/2017.
 */
class MyGamesFragment : Fragment(R.layout.fragment_mygames), MyGamesContract.View {
    override fun showLoginScreen() {
        startActivity(Intent(context, LoginActivity::class.java))
        activity?.finish()
    }

    private val groupAdapter = GameListGroupAdapter()

    private lateinit var presenter: MyGamesContract.Presenter
    private var analytics = OnlineGoApplication.instance.analytics

    private var lastReportedGameCount = -1


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        gamesRecycler.layoutManager = LinearLayoutManager(context)
        (gamesRecycler.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        gamesRecycler.adapter = groupAdapter
        groupAdapter.setOnItemClickListener { item, _ ->
            when (item) {
                is ActiveGameItem -> presenter.onGameSelected(item.game)
                is FinishedGameItem -> presenter.onGameSelected(item.game)
                is NewGameItem.AutoMatch -> {
                    analytics.logEvent("automatch_item_clicked", null)
                    (activity as MainActivity).onAutoMatchSearch()
                }
                is NewGameItem.Custom -> {
                    analytics.logEvent("friend_item_clicked", null)
                    (activity as MainActivity).onCustomGameSearch()
                }
            }
        }

        presenter = MyGamesPresenter(
                this,
                analytics,
                (activity as MainActivity).activeGameRepository,
                ChallengesRepository,
                AutomatchRepository,
                ServerNotificationsRepository
        )
    }

    override fun showMessage(title: String, message: String) {
        AwesomeInfoDialog(context)
                .setTitle(title)
                .setMessage(message)
                .setColoredCircle(R.color.colorPrimary)
                .setDialogIconAndColor(R.drawable.ic_dialog_info, R.color.white)
                .setCancelable(true)
                .setPositiveButtonText("OK")
                .setPositiveButtonbackgroundColor(R.color.colorPrimary)
                .setPositiveButtonTextColor(R.color.white)
                .setPositiveButtonClick {  }
                .show()
    }

    override fun setChallenges(challenges: List<Challenge>) {
        groupAdapter.setChallenges(challenges.map {
            ChallengeItem(it, presenter::onChallengeCancelled, presenter::onChallengeAccepted, presenter::onChallengeDeclined)
        })
    }

    override fun setAutomatches(automatches: List<OGSAutomatch>) {
        groupAdapter.setAutomatches(automatches.map {
            AutomatchItem(it, presenter::onAutomatchCancelled)
        })
    }

    override fun navigateToGameScreen(game: Game) {
        (activity as MainActivity).navigateToGameScreen(game)
    }

    override fun onResume() {
        super.onResume()
        analytics.setCurrentScreen(activity!!, javaClass.simpleName, null)
        (activity as? AppCompatActivity)?.supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(false)
        }

        (activity as? MainActivity)?.apply {
            setLogoVisible(true)
            setChipsVisible(false)
            setChatButtonVisible(false)
        }
        presenter.subscribe()
    }

    override fun setHistoricGames(games: List<Game>) {
        groupAdapter.setHistoricGames(games)
    }

    override fun onPause() {
        super.onPause()
        presenter.unsubscribe()
    }

    override fun setGames(games: List<Game>) {
        if (lastReportedGameCount != games.size) {
            analytics.logEvent("active_games", Bundle().apply { putInt("GAME_COUNT", games.size) })
            lastReportedGameCount = games.size
        }
        groupAdapter.setGames(games)
    }

    override fun setLoading(loading: Boolean) {
        (activity as? MainActivity)?.loading = loading
    }

}