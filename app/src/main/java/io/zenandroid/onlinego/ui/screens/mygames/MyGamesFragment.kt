package io.zenandroid.onlinego.ui.screens.mygames

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Alignment.Companion.End
import androidx.compose.ui.Alignment.Companion.TopEnd
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnLifecycleDestroyed
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import com.awesomedialog.blennersilva.awesomedialoglibrary.AwesomeInfoDialog
import com.google.accompanist.pager.*
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.ui.screens.main.MainActivity
import io.zenandroid.onlinego.data.model.local.Challenge
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.data.model.local.Player
import io.zenandroid.onlinego.data.model.local.isPaused
import io.zenandroid.onlinego.data.model.ogs.OGSAutomatch
import io.zenandroid.onlinego.data.model.ogs.SizeSpeedOption
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import io.zenandroid.onlinego.databinding.FragmentMygamesBinding
import io.zenandroid.onlinego.gamelogic.Util
import io.zenandroid.onlinego.ui.composables.Board
import io.zenandroid.onlinego.ui.items.*
import io.zenandroid.onlinego.ui.screens.game.GAME_ID
import io.zenandroid.onlinego.ui.screens.game.GAME_SIZE
import io.zenandroid.onlinego.ui.screens.mygames.Action.GameSelected
import io.zenandroid.onlinego.ui.screens.whatsnew.WhatsNewDialog
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme
import io.zenandroid.onlinego.ui.theme.background
import io.zenandroid.onlinego.ui.theme.salmon
import io.zenandroid.onlinego.ui.theme.shapes
import io.zenandroid.onlinego.utils.computeTimeLeft
import io.zenandroid.onlinego.utils.showIf
import org.koin.android.ext.android.get
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.math.absoluteValue

/**
 * Created by alex on 05/11/2017.
 */
class MyGamesFragment : Fragment(), MyGamesContract.View {

    private val viewModel: MyGamesViewModel by viewModel()
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

    @ExperimentalFoundationApi
    @ExperimentalPagerApi
    @ExperimentalComposeUiApi
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentMygamesBinding.inflate(inflater, container, false)
        binding.tutorialView.apply {
            setViewCompositionStrategy(
                DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                OnlineGoTheme {
                    val state by viewModel.state.observeAsState(MyGamesState(userId = 0L))

                    if(state.errorMessage != null) {
                        LaunchedEffect(state.errorMessage) {
                            Toast.makeText(context, state.errorMessage, Toast.LENGTH_LONG).show()
                        }
                    }
                    MyGamesScreen(state, ::onAction)
                }
            }
        }
        return binding.root
    }

    private fun onAction(action: Action) {
        when(action) {
            Action.CustomGame -> {
                analytics.logEvent("friend_item_clicked", null)
                (activity as MainActivity).onCustomGameSearch()
            }
            Action.PlayOffline -> {
                analytics.logEvent("localai_item_clicked", null)
                view?.findNavController()?.navigate(R.id.action_myGamesFragment_to_aiGameFragment)
            }
            Action.PlayOnline -> {
                analytics.logEvent("automatch_item_clicked", null)
                (activity as MainActivity).onAutoMatchSearch()
            }
            is GameSelected -> {
                val game = action.game
                analytics.logEvent("clicked_game", Bundle().apply {
                    putLong("GAME_ID", game.id)
                    putBoolean("ACTIVE_GAME", game.ended == null)
                })
                navigateToGameScreen(game)
            }
            else -> viewModel.onAction(action)
        }
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

        viewModel.setChallenges(challenges)
    }

    override fun setAutomatches(automatches: List<OGSAutomatch>) {
        groupAdapter.setAutomatches(automatches.map {
            AutomatchItem(it, presenter::onAutomatchCancelled)
        })

        viewModel.setAutomatches(automatches)
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

        viewModel.setRecentGames(games)
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

        viewModel.setGames(games)
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
                        fontWeight = Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(start = 70.dp, top = 20.dp)
                    )
                    Text(
                        text = tutorial,
                        color = Color.White,
                        fontWeight = Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 70.dp, top = 18.dp)
                    )
                }
            }
        }
    }
}

sealed class Action {
    object PlayOnline: Action()
    object CustomGame: Action()
    object PlayOffline: Action()
    class GameSelected(val game: Game): Action()
    class ChallengeCancelled(val challenge: Challenge): Action()
    class ChallengeAccepted(val challenge: Challenge): Action()
    class ChallengeDeclined(val challenge: Challenge): Action()
    class AutomatchCancelled(val automatch: OGSAutomatch): Action()
    class LoadMoreHistoricGames(val game: Game?): Action()
}

@Composable
fun NewGameButtonsRow(modifier: Modifier = Modifier, onAction: (Action) -> Unit) {
    Row (
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = modifier.fillMaxWidth()
    ) {
        NewGameButton(img = R.drawable.ic_person_filled, text = "Play\nOnline") { onAction(Action.PlayOnline) }
        NewGameButton(img = R.drawable.ic_challenge, text = "Custom\nGame") { onAction(Action.CustomGame) }
        NewGameButton(img = R.drawable.ic_robot, text = "Play\nOffline") { onAction(Action.PlayOffline) }
    }
}

@Composable
fun NewGameButton(@DrawableRes img: Int, text: String, onClick: () -> Unit) {
    Column(modifier = Modifier
        .clickable { onClick.invoke() }
        .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Image(
            painter = painterResource(id = img),
            contentDescription = null,
            colorFilter = ColorFilter.tint(Color.White),
            modifier = Modifier
                .align(CenterHorizontally)
                .background(color = salmon, shape = CircleShape)
                .padding(16.dp)
        )
        Text(
            text = text,
            textAlign = TextAlign.Center,
            fontSize = 12.sp,
            modifier = Modifier
                .align(CenterHorizontally)
                .padding(top = 4.dp)
        )
    }
}

@ExperimentalFoundationApi
@ExperimentalPagerApi
@ExperimentalComposeUiApi
@Composable
fun MyGamesScreen(state: MyGamesState, onAction: (Action) -> Unit) {
    val listState = rememberLazyListState()
    LazyColumn (
        state = listState,
        modifier = Modifier.fillMaxHeight()
    ) {
//        item {
//            TutorialItem(percentage = 73, tutorial = "Basics > Capturing")
//        }
        items(items = state.automatches) {
            AutomatchItemCompose(it, onAction)
        }
        if(state.myTurnGames.isNotEmpty()) {
            item {
                MyTurnCarousel(state.myTurnGames, state.userId, onAction)
            }
        }

        if(state.challenges.isNotEmpty()) {
            item {
                Text(
                    text = "Challenges",
                    color = Color(0xFF757575),
                    fontSize = 12.sp,
                    fontWeight = Bold,
                    modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp)
                )
            }
        }

        items(items = state.challenges) {
            ChallengeItemCompose(it, state.userId, onAction)
        }

        item {
            NewGameButtonsRow(modifier = Modifier.padding(top = 10.dp), onAction)
        }

        if(state.opponentTurnGames.isNotEmpty()) {
            item {
                Text(
                    text = "Opponent's turn",
                    color = Color(0xFF757575),
                    fontSize = 12.sp,
                    fontWeight = Bold,
                    modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp)
                )
            }
        }
        items (items = state.opponentTurnGames) {
            OpponentsTurnGameItem(it, state.userId, onAction)
        }

        if(state.recentGames.isNotEmpty()) {
            item {
                Text(
                    text = "Recently finished",
                    color = Color(0xFF757575),
                    fontSize = 12.sp,
                    fontWeight = Bold,
                    modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp)
                )
            }
        }
        items (items = state.recentGames) {
            OpponentsTurnGameItem(game = it, state.userId, onAction = onAction)
        }

        if(state.historicGames.isNotEmpty()) {
            item {
                Text(
                    text = "Older games",
                    color = Color(0xFF757575),
                    fontSize = 12.sp,
                    fontWeight = Bold,
                    modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp)
                )
            }
            item {
                LazyRow {
                    items(state.historicGames) { game ->
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .size(width = 105.dp, height = 140.dp)
                                .padding(horizontal = 8.dp)
                        ) {
                            Column {
                                Board(
                                    boardSize = game.width,
                                    position = game.position,
                                    drawCoordinates = false,
                                    interactive = false,
                                    drawShadow = false,
                                    fadeInLastMove = false,
                                    fadeOutRemovedStones = false,
                                    modifier = Modifier
                                        .clip(MaterialTheme.shapes.large)
                                        .padding(15.dp)
                                )
                                val opponent =
                                    when (state.userId) {
                                        game.blackPlayer.id -> game.whitePlayer
                                        game.whitePlayer.id -> game.blackPlayer
                                        else -> null
                                    }

                                Row (modifier = Modifier.padding(top = 8.dp)) {
                                    Text(
                                        text = opponent?.username ?: "Unknown",
                                        style = TextStyle.Default.copy(
                                            color = Color(0xFF757575),
                                            fontSize = 14.sp,
                                            fontWeight = Bold
                                        )
                                    )
                                    val circleColor = if (opponent?.id == game.blackPlayer.id) Color(0xFF757575) else Color.White
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 2.dp, start = 8.dp)
                                            .background(Color(0xFF757575), shape = CircleShape)
                                            .padding(all = 1.dp) // width of the line of the empty circle
                                            .background(color = circleColor, shape = CircleShape)
                                            .size(8.dp) // size of the middle circle
                                            .align(CenterVertically)
                                    )
                                }
                                val outcome = when {
                                    game.outcome == "Cancellation" -> "Cancelled"
                                    state.userId == game.blackPlayer.id ->
                                        if (game.blackLost == true) "Lost by ${game.outcome}"
                                        else "Won by ${game.outcome}"
                                    state.userId == game.whitePlayer.id ->
                                        if (game.whiteLost == true) "Lost by ${game.outcome}"
                                        else "Won by ${game.outcome}"
                                    game.whiteLost == true ->
                                        "Black won by ${game.outcome}"
                                    else ->
                                        "White won by ${game.outcome}"
                                }
                                Text(
                                    text = outcome,
                                    style = TextStyle.Default.copy(
                                        color = Color(0xFF757575),
                                        fontSize = 12.sp,
                                    ),
                                )
                            }
                        }
                    }
                    if(!state.loadedAllHistoricGames) {
                        item {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier
                                    .size(width = 105.dp, height = 140.dp)
                                    .padding(horizontal = 8.dp)
                            ) {
                                Log.e("****", "recomposing")
                                LaunchedEffect(state.historicGames) {
                                    Log.e("****", "load more")
                                    onAction(Action.LoadMoreHistoricGames(state.historicGames.lastOrNull()))
                                }
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AutomatchItemCompose(automatch: OGSAutomatch, onAction: (Action) -> Unit) {
    Surface (
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Column (modifier = Modifier.padding(horizontal = 16.dp)){
            Text(
                text = "Searching for a game",
                fontSize = 14.sp,
                fontWeight = Bold,
                color = Color(0xFF757575),
                modifier = Modifier
                    .align(CenterHorizontally)
                    .padding(top = 16.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            TextButton(
                onClick = { onAction(Action.AutomatchCancelled(automatch)) },
                modifier = Modifier
                    .align(CenterHorizontally)
                    .padding(vertical = 8.dp)
            ) {
                Text("Cancel")
            }
        }
    }
}

@Composable
fun ChallengeItemCompose(challenge: Challenge, userId: Long, onAction: (Action) -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .height(90.dp)
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row {
            Image(
                painter = painterResource(id = R.drawable.ic_person_filled),
                contentDescription = null,
                colorFilter = ColorFilter.tint(colorResource(id = R.color.colorAccent)),
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f)
                    .padding(top = 10.dp, bottom = 10.dp, start = 15.dp)
            )

            Column(modifier = Modifier.padding(top = 10.dp)) {
                val text = if(challenge.challenger?.id == userId) {
                    "You are challenging ${challenge.challenged?.username}"
                } else {
                    "${challenge.challenger?.username} is challenging you"
                }

                Text(
                    text = text,
                    fontSize = 14.sp,
                    fontWeight = Bold,
                    color = Color(0xFF757575),
                    modifier = Modifier.align(CenterHorizontally)
                )
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    if(challenge.challenger?.id == userId) {
                        TextButton(onClick = { onAction(Action.ChallengeCancelled(challenge)) }) {
                            Text("Cancel")
                        }
                    } else {
                        TextButton(onClick = { onAction(Action.ChallengeAccepted(challenge)) }) {
                            Text("Accept")
                        }
                        TextButton(onClick = { onAction(Action.ChallengeDeclined(challenge)) }) {
                            Text("Decline")
                        }
                    }
                }
            }
        }
    }
}

@ExperimentalComposeUiApi
@Composable
fun OpponentsTurnGameItem(game: Game, userId: Long, onAction: (Action) -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .height(110.dp)
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        val opponent =
            when (userId) {
                game.blackPlayer.id -> game.whitePlayer
                game.whitePlayer.id -> game.blackPlayer
                else -> null
            }
        Row(modifier = Modifier.clickable { onAction(GameSelected(game)) }) {
            Board(
                boardSize = game.width,
                position = game.position,
                drawCoordinates = false,
                interactive = false,
                drawShadow = false,
                fadeInLastMove = false,
                fadeOutRemovedStones = false,
                modifier = Modifier
                    .align(CenterVertically)
                    .padding(horizontal = 10.dp, vertical = 10.dp)
                    .clip(MaterialTheme.shapes.small)
            )
            Column {
                Row (modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = opponent?.username ?: "Unknown",
                        style = TextStyle.Default.copy(
                            color = Color(0xFF757575),
                            fontSize = 14.sp,
                            fontWeight = Bold
                        )
                    )
                    val circleColor = if (opponent?.id == game.blackPlayer.id) Color(0xFF757575) else Color.White
                    Box(
                        modifier = Modifier
                            .padding(top = 2.dp, start = 8.dp)
                            .background(Color(0xFF757575), shape = CircleShape)
                            .padding(all = 1.dp) // width of the line of the empty circle
                            .background(color = circleColor, shape = CircleShape)
                            .size(8.dp) // size of the middle circle
                            .align(CenterVertically)
                    )
                }
                if(game.blackLost != true && game.whiteLost != true) {
                    Row(modifier = Modifier.padding(top = 4.dp)) {
                        Text(
                            text = calculateTimer(game),
                            style = TextStyle.Default.copy(
                                color = Color(0xFF757575),
                                fontSize = 12.sp,
                            ),
                        )
                        if (game.pauseControl.isPaused()) {
                            Text(
                                text = "  ·  paused",
                                style = TextStyle.Default.copy(
                                    color = Color(0xFF757575),
                                    fontSize = 12.sp,
                                ),
                            )
                        }
                    }
                } else {
                    val outcome = when {
                        game.outcome == "Cancellation" -> "Cancelled"
                        userId == game.blackPlayer.id ->
                            if (game.blackLost == true) "Lost by ${game.outcome}"
                            else "Won by ${game.outcome}"
                        userId == game.whitePlayer.id ->
                            if (game.whiteLost == true) "Lost by ${game.outcome}"
                            else "Won by ${game.outcome}"
                        game.whiteLost == true ->
                            "Black won by ${game.outcome}"
                        else ->
                            "White won by ${game.outcome}"
                    }
                    Text(
                        text = outcome,
                        style = TextStyle.Default.copy(
                            color = Color(0xFF757575),
                            fontSize = 12.sp,
                        ),
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                if(game.messagesCount != null && game.messagesCount != 0) {
                    ChatIndicator(
                        chatCount = game.messagesCount,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
        }
    }
}


@Composable
fun ChatIndicator(chatCount: Int, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(40.dp)) {
        Image(
            painter = painterResource(id = R.drawable.ic_chat_bubble),
            colorFilter = ColorFilter.tint(Color(0xFF757575)),
            contentDescription = null,
            modifier = Modifier.align(Center)
        )
        Text(
            text = chatCount.toString(),
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .size(18.dp)
                .background(color = MaterialTheme.colors.primary, shape = CircleShape)
                .align(TopEnd)
        )
    }
}


@ExperimentalPagerApi
@ExperimentalComposeUiApi
@Composable
fun MyTurnCarousel(games: List<Game>, userId: Long, onAction: (Action) -> Unit) {
    Column {
        val pagerState = rememberPagerState(pageCount = games.size)
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
        ) { page ->
            val game = games[page]
            val opponent =
                when (userId) {
                    game.blackPlayer.id -> game.whitePlayer
                    game.whitePlayer.id -> game.blackPlayer
                    else -> null
                }
            Box(modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    val pageOffset = calculateCurrentOffsetForPage(page).absoluteValue

                    lerp(
                        start = 0.25f,
                        stop = 1f,
                        fraction = 1f - pageOffset.coerceIn(0f, 1f)
                    ).also { scale ->
                        scaleX = scale
                        scaleY = scale
                    }

                    alpha = lerp(
                        start = 0.25f,
                        stop = 1f,
                        fraction = 1f - pageOffset.coerceIn(0f, 1f)
                    )
                }
            ) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .fillMaxWidth(.75f)
                        .align(Center)

                ) {
                    Column(modifier = Modifier
                        .clickable {
                            onAction(GameSelected(game))
                        }
                        .padding(
                            vertical = 16.dp,
                            horizontal = 24.dp
                        )) {
                        Board(
                            boardSize = game.width,
                            position = game.position,
                            drawCoordinates = false,
                            interactive = false,
                            fadeInLastMove = false,
                            fadeOutRemovedStones = false,
                            modifier = Modifier.clip(MaterialTheme.shapes.large)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row {
                            Text(
                                text = opponent?.username ?: "Unknown",
                                style = TextStyle.Default.copy(
                                    color = Color(0xFF757575),
                                    fontSize = 14.sp,
                                    fontWeight = Bold
                                )
                            )
                            val circleColor = if (opponent?.id == game.blackPlayer.id) Color(0xFF757575) else Color.White
                            Box(
                                modifier = Modifier
                                    .padding(top = 2.dp, start = 8.dp)
                                    .background(Color(0xFF757575), shape = CircleShape)
                                    .padding(all = 1.dp) // width of the line of the empty circle
                                    .background(color = circleColor, shape = CircleShape)
                                    .size(8.dp) // size of the middle circle
                                    .align(CenterVertically)
                            )
                        }
                        Row {
                            Text(
                                text = calculateTimer(game),
                                style = TextStyle.Default.copy(
                                    color = Color(0xFF757575),
                                    fontSize = 12.sp,
                                )
                            )
                            if (game.pauseControl.isPaused()) {
                                Text(
                                    text = "  ·  paused",
                                    style = TextStyle.Default.copy(
                                        color = Color(0xFF757575),
                                        fontSize = 12.sp,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
        HorizontalPagerIndicator(
            pagerState = pagerState,
            modifier = Modifier
                .align(CenterHorizontally)
                .padding(16.dp)
        )
    }
}

private fun calculateTimer(game: Game): String {
    val currentPlayer = when (game.playerToMoveId) {
        game.blackPlayer.id -> game.blackPlayer
        game.whitePlayer.id -> game.whitePlayer
        else -> null
    }
    val timerDetails = game.clock?.let {
        if (currentPlayer?.id == game.blackPlayer.id)
            computeTimeLeft(it, it.blackTimeSimple, it.blackTime, true, game.pausedSince)
        else
            computeTimeLeft(it, it.whiteTimeSimple, it.whiteTime, true, game.pausedSince)
    }
    return timerDetails?.firstLine ?: ""
}

@ExperimentalFoundationApi
@ExperimentalPagerApi
@ExperimentalComposeUiApi
@Preview
@Composable
fun Preview() {
    OnlineGoTheme {
        Box(modifier = Modifier.background(Color(0xFFF2F4F7))) {
            MyGamesScreen(MyGamesState(
                userId = 0L,
                automatches = listOf(
                    OGSAutomatch(
                        uuid = "aaa",
                        game_id = null,
                        size_speed_options = listOf(
                            SizeSpeedOption("9x9", "blitz"),
                        )
                    )
                ),
                challenges = listOf(
                    Challenge(
                        id = 0L,
                        challenger = Player(
                            id = 0L,
                            username = "Me",
                            rating = null,
                            acceptedStones = null,
                            country = null,
                            icon = null,
                            ui_class = null
                        ),
                        challenged = Player(
                            id = 1L,
                            username = "Somebody",
                            rating = null,
                            acceptedStones = null,
                            country = null,
                            icon = null,
                            ui_class = null
                        )
                    ),
                    Challenge(
                        id = 1L,
                        challenger = Player(
                            id = 1L,
                            username = "Somebody",
                            rating = null,
                            acceptedStones = null,
                            country = null,
                            icon = null,
                            ui_class = null
                        ),
                        challenged = Player(
                            id = 0L,
                            username = "Me",
                            rating = null,
                            acceptedStones = null,
                            country = null,
                            icon = null,
                            ui_class = null
                        )
                    ),
                )
            )) {}
        }
    }
}