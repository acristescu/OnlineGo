package io.zenandroid.onlinego.mygames

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.SimpleItemAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.awesomedialog.blennersilva.awesomedialoglibrary.AwesomeInfoDialog
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.main.MainActivity
import io.zenandroid.onlinego.model.local.Challenge
import io.zenandroid.onlinego.model.local.Game
import io.zenandroid.onlinego.model.ogs.OGSAutomatch
import io.zenandroid.onlinego.ogs.AutomatchRepository
import io.zenandroid.onlinego.ogs.ChallengesRepository
import io.zenandroid.onlinego.ogs.NotificationsRepository
import io.zenandroid.onlinego.reusable.ActiveGameItem
import io.zenandroid.onlinego.reusable.AutomatchItem
import io.zenandroid.onlinego.reusable.ChallengeItem
import io.zenandroid.onlinego.reusable.FinishedGameItem
import kotlinx.android.synthetic.main.fragment_mygames.*

/**
 * Created by alex on 05/11/2017.
 */
class MyGamesFragment : Fragment(), MyGamesContract.View {
    private val groupAdapter = GameListGroupAdapter()

    private lateinit var presenter: MyGamesContract.Presenter
    private var analytics = OnlineGoApplication.instance.analytics

    private var lastReportedGameCount = -1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.fragment_mygames, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        gamesRecycler.layoutManager = LinearLayoutManager(context)
        (gamesRecycler.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        gamesRecycler.adapter = groupAdapter
        groupAdapter.setOnItemClickListener { item, _ ->
            when (item) {
                is ActiveGameItem -> presenter.onGameSelected(item.game)
                is FinishedGameItem -> presenter.onGameSelected(item.game)
                is NewGameItem.AutoMatch -> (activity as MainActivity).onAutoMatchSearch()
                is NewGameItem.OnlineBot -> (activity as MainActivity).onOnlineBotSearch()
            }
        }

        presenter = MyGamesPresenter(
                this,
                analytics,
                (activity as MainActivity).activeGameRepository,
                ChallengesRepository,
                AutomatchRepository,
                NotificationsRepository
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