package io.zenandroid.onlinego.ui.screens.mygames

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import android.view.View
import com.awesomedialog.blennersilva.awesomedialoglibrary.AwesomeInfoDialog
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.ui.screens.login.LoginActivity
import io.zenandroid.onlinego.ui.screens.main.MainActivity
import io.zenandroid.onlinego.data.model.local.Challenge
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.data.model.ogs.OGSAutomatch
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import io.zenandroid.onlinego.ui.items.*
import io.zenandroid.onlinego.ui.screens.whatsnew.WhatsNewDialog
import kotlinx.android.synthetic.main.fragment_mygames.*
import org.koin.android.ext.android.get

/**
 * Created by alex on 05/11/2017.
 */
class MyGamesFragment : Fragment(R.layout.fragment_mygames), MyGamesContract.View {
    override fun showLoginScreen() {
        startActivity(Intent(context, LoginActivity::class.java))
        activity?.finish()
    }

    private val groupAdapter = GameListGroupAdapter(get<UserSessionRepository>().userId)

    private val whatsNewDialog: WhatsNewDialog by lazy { WhatsNewDialog() }

    private lateinit var presenter: MyGamesContract.Presenter
    private var analytics = OnlineGoApplication.instance.analytics

    private var lastReportedGameCount = -1

    override val needsMoreOlderGames by lazy {
        groupAdapter.olderGamesAdapter.needsMoreDataObservable
    }

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
                is NewGameItem.LocalAI -> {
                    analytics.logEvent("localai_item_clicked", null)
                    (activity as MainActivity).onLocalAIClicked()
                }
            }
        }
        groupAdapter.olderGamesAdapter.setOnItemClickListener { item, _ ->
            if(item is HistoricGameItem) {
                presenter.onGameSelected(item.game)
            }
        }

        presenter = MyGamesPresenter(this, analytics, get(), get(), get(), get(), get(), get(), get(), get())
    }

    override fun showWhatsNewDialog() {
        if(fragmentManager?.findFragmentByTag("WHATS_NEW") == null) {
            whatsNewDialog.show(fragmentManager!!, "WHATS_NEW")
        }
    }

    override fun setLoadedAllHistoricGames(loadedLastPage: Boolean) {
        groupAdapter.olderGamesAdapter.loadedLastPage = loadedLastPage
    }

    override fun setLoadingMoreHistoricGames(loading: Boolean) {
        groupAdapter.olderGamesAdapter.loading = loading
    }

    override fun showMessage(title: String, message: String) {
        AwesomeInfoDialog(context)
                .setTitle(title)
                .setMessage(message)
                .setDialogBodyBackgroundColor(R.color.colorOffWhite)
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

    override fun setRecentGames(games: List<Game>) {
        groupAdapter.setRecentGames(games)
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

    override fun appendHistoricGames(games: List<Game>) {
        if(games.isNotEmpty()) {
            groupAdapter.historicGamesvisible = true
            groupAdapter.olderGamesAdapter.appendData(games)
        }
    }

    override fun isHistoricGamesSectionEmpty() =
        groupAdapter.olderGamesAdapter.isEmpty()
}