package io.zenandroid.onlinego.ui.screens.mygames

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnLifecycleDestroyed
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import com.awesomedialog.blennersilva.awesomedialoglibrary.AwesomeInfoDialog
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.ui.screens.main.MainActivity
import io.zenandroid.onlinego.data.model.local.Challenge
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.data.model.ogs.OGSAutomatch
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import io.zenandroid.onlinego.databinding.FragmentMygamesBinding
import io.zenandroid.onlinego.ui.items.*
import io.zenandroid.onlinego.ui.screens.game.GAME_ID
import io.zenandroid.onlinego.ui.screens.game.GAME_SIZE
import io.zenandroid.onlinego.ui.screens.whatsnew.WhatsNewDialog
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme
import io.zenandroid.onlinego.ui.theme.salmon
import io.zenandroid.onlinego.ui.theme.shapes
import io.zenandroid.onlinego.utils.showIf
import org.koin.android.ext.android.get

/**
 * Created by alex on 05/11/2017.
 */
class MyGamesFragment : Fragment(), MyGamesContract.View {
    override fun showLoginScreen() {
        (activity as? MainActivity)?.showLogin()
    }

    private val groupAdapter = GameListGroupAdapter(get<UserSessionRepository>().userId)

    private val whatsNewDialog: WhatsNewDialog by lazy { WhatsNewDialog() }

    private lateinit var presenter: MyGamesContract.Presenter
    private var analytics = OnlineGoApplication.instance.analytics

    private lateinit var binding: FragmentMygamesBinding

    private var lastReportedGameCount = -1

    override val needsMoreOlderGames by lazy {
        groupAdapter.olderGamesAdapter.needsMoreDataObservable
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentMygamesBinding.inflate(inflater, container, false)
        binding.tutorialView.apply {
            setViewCompositionStrategy(
                DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                OnlineGoTheme {
                    TutorialItem(percentage = 73, tutorial = "Advanced > Snapback")
                }
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.gamesRecycler.layoutManager = LinearLayoutManager(context)
        (binding.gamesRecycler.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        binding.gamesRecycler.adapter = groupAdapter
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
                    view.findNavController().navigate(R.id.action_myGamesFragment_to_aiGameFragment)
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
        if(parentFragmentManager.findFragmentByTag("WHATS_NEW") == null) {
            whatsNewDialog.show(parentFragmentManager, "WHATS_NEW")
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
        view?.findNavController()?.navigate(R.id.action_myGamesFragment_to_gameFragment, bundleOf(GAME_ID to game.id, GAME_SIZE to game.width))
    }

    override fun onResume() {
        super.onResume()
        analytics.setCurrentScreen(requireActivity(), javaClass.simpleName, null)
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
        binding.progressBar.showIf(loading)
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


@Composable
fun TutorialItem(percentage: Int, tutorial: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$percentage %",
            fontWeight = FontWeight.Black,
            color = salmon,
            modifier = Modifier
                .align(CenterVertically)
                .padding(24.dp)
        )
        Surface(
            color = salmon,
            shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .padding(start = 12.dp)
        ) {
            Row {
                Canvas(
                    modifier = Modifier
                        .size(25.dp, 50.dp)
                        .align(CenterVertically)) {
                    drawArc(
                        color = Color.White,
                        alpha = .25f,
                        startAngle = 90f,
                        sweepAngle = -180f,
                        useCenter = false,
                        topLeft = Offset(-size.width, 0f),
                        size = Size(size.width * 2, size.height),
                        style = Stroke(width = 24.dp.value)
                    )
                    drawArc(
                        color = Color.White,
                        startAngle = 90f,
                        sweepAngle = -180f * .73f,
                        useCenter = false,
                        topLeft = Offset(-size.width, 0f),
                        size = Size(size.width * 2, size.height),
                        style = Stroke(
                            width = 24.dp.value,
                            cap = StrokeCap.Round
                        )
                    )
                }
                Column {
                    Text(
                        text = "Learn to play",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(start = 70.dp, top = 20.dp)
                    )
                    Text(
                        text = tutorial,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 70.dp, top = 18.dp)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun Preview() {
    OnlineGoTheme {
        TutorialItem(percentage = 73, tutorial = "Basics > Capturing")
    }
}