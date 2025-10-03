package io.zenandroid.onlinego.ui.screens.mygames

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.model.local.Challenge
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.data.model.local.Player
import io.zenandroid.onlinego.data.model.ogs.OGSAutomatch
import io.zenandroid.onlinego.data.model.ogs.SizeSpeedOption
import io.zenandroid.onlinego.ui.screens.automatch.NewAutomatchChallengeBottomSheet
import io.zenandroid.onlinego.ui.screens.mygames.composables.AutomatchItem
import io.zenandroid.onlinego.ui.screens.mygames.composables.ChallengeDetailsDialog
import io.zenandroid.onlinego.ui.screens.mygames.composables.ChallengeItem
import io.zenandroid.onlinego.ui.screens.mygames.composables.HistoricGameLazyRow
import io.zenandroid.onlinego.ui.screens.mygames.composables.HomeScreenHeader
import io.zenandroid.onlinego.ui.screens.mygames.composables.MyTurnCarousel
import io.zenandroid.onlinego.ui.screens.mygames.composables.NewGameButtonsRow
import io.zenandroid.onlinego.ui.screens.mygames.composables.SenteCard
import io.zenandroid.onlinego.ui.screens.mygames.composables.SmallGameItem
import io.zenandroid.onlinego.ui.screens.mygames.composables.TutorialItem
import io.zenandroid.onlinego.ui.screens.newchallenge.NewChallengeBottomSheet
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme
import io.zenandroid.onlinego.utils.WhatsNewUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel

@Composable
fun MyGamesScreen(
  viewModel: MyGamesViewModel = koinViewModel(),
  onScreenReady: () -> Unit,
  onNavigateToGame: (Game) -> Unit,
  onNavigateToAIGame: () -> Unit,
  onNavigateToFaceToFace: () -> Unit,
  onNavigateToSupporter: () -> Unit,
  onNavigateToLogin: () -> Unit,
  onNavigateToSignUp: () -> Unit,
) {
  val state by viewModel.state.collectAsStateWithLifecycle()

  // We want to hold off dismissing the splash screen until we have all the data we need to display
  var timerExpired by remember { mutableStateOf(false) }
  val screenReady by remember {
    derivedStateOf {
      timerExpired || state.userIsLoggedOut || (state.hasReceivedChallenges && state.hasReceivedAutomatches && state.hasReceivedActiveGames && state.hasReceivedRecentGames && state.hasReceivedHistoricGames)
    }
  }


  LaunchedEffect(screenReady) {
    withContext(Dispatchers.Default) {
      if (screenReady) {
        onScreenReady()
      }
    }
  }

  LaunchedEffect(screenReady) {
    if (!screenReady) {
      delay(2000)
      timerExpired = true
      FirebaseCrashlytics.getInstance().log("Splash screen timed out")
    }
  }

  val lifecycleOwner = LocalLifecycleOwner.current

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        viewModel.onAction(Action.ViewResumed)
      }
    }

    lifecycleOwner.lifecycle.addObserver(observer)

    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  if (screenReady) {
    MyGamesContent(
      state,
      viewModel::onAction,
      onNavigateToAIGame,
      onNavigateToFaceToFace,
      onNavigateToLogin,
      onNavigateToSignUp
    )
  }

  if (state.alertDialogText != null) {
    AlertDialog(
      title = { state.alertDialogTitle?.let { Text(it) } },
      text = { state.alertDialogText?.let { Text(it) } },
      confirmButton = {
        Button(onClick = { viewModel.onAction(Action.DismissAlertDialog) }) {
          Text("OK")
        }
      },
      onDismissRequest = { viewModel.onAction(Action.DismissAlertDialog) }
    )
  }
  state.gameNavigationPending?.let {
    LaunchedEffect(it) {
      onNavigateToGame(it)
      viewModel.onAction(Action.GameNavigationConsumed)
    }
  }
  if (state.whatsNewDialogVisible) {
    AlertDialog(
      onDismissRequest = { viewModel.onAction(Action.DismissWhatsNewDialog) },
      dismissButton = {
        TextButton(onClick = { viewModel.onAction(Action.DismissWhatsNewDialog) }) {
          Text("OK")
        }
      },
      confirmButton = {
        TextButton(onClick = onNavigateToSupporter) {
          Text("SUPPORT")
        }
      },
      text = { Text(WhatsNewUtils.whatsNewTextAnnotated) }
    )
  }
  state.challengeDetailsStatus?.let {
    ChallengeDetailsDialog(
      onChallengeAccepted = { viewModel.onAction(Action.ChallengeAccepted(it)) },
      onChallengeDeclined = { viewModel.onAction(Action.ChallengeDeclined(it)) },
      onDialogDismissed = { viewModel.onAction(Action.ChallengeDialogDismissed) },
      status = it,
    )
  }
  state.warning?.let {
    var secondsLeft by remember { mutableIntStateOf(if (it.severity == "acknowledgement") 0 else 5) }
    val buttonEnabled = secondsLeft == 0
    LaunchedEffect(Unit) {
      while (secondsLeft > 0) {
        delay(1000)
        secondsLeft--
      }
      secondsLeft = 0
    }
    AlertDialog(
      onDismissRequest = { },
      title = { Text("OGS Moderator ${it.severity}") },
      text = {
        Text(
          if (it.text.isNullOrEmpty()) it.message_id ?: "Empty message" else it.text
        )
      },
      confirmButton = {
        Button(
          onClick = { viewModel.onAction(Action.WarningAcknowledged) },
          enabled = buttonEnabled
        ) {
          Text(
            if (buttonEnabled) "Acknowledge"
            else "Acknowledge (${secondsLeft}s)"
          )
        }
      }
    )
  }
}

@ExperimentalFoundationApi
@ExperimentalComposeUiApi
@Composable
fun MyGamesContent(
  state: MyGamesState,
  onAction: (Action) -> Unit,
  onNavigateToAIGame: () -> Unit,
  onNavigateToFaceToFace: () -> Unit,
  onNavigateToLogin: () -> Unit,
  onNavigateToSignUp: () -> Unit,
) {
  var newChallengeBottomSheetVisible by remember { mutableStateOf(false) }
  var newAutomatchChallengeBottomSheetVisible by remember { mutableStateOf(false) }
  val listState = rememberLazyListState()
  LazyColumn(
    state = listState,
    modifier = Modifier
      .fillMaxHeight()
      .background(MaterialTheme.colorScheme.surface)
  ) {
    item("HomeScreenHeader") {
      HomeScreenHeader(
        image = state.userImageURL,
        mainText = state.headerMainText,
        subText = state.headerSubText,
        offline = !state.online,
      )
    }
    if (state.tutorialVisible) {
      item("TutorialItem") {
        TutorialItem(
          percentage = state.tutorialPercentage ?: 0,
          tutorial = state.tutorialTitle ?: ""
        )
      }
    }
    items(items = state.automatches, key = { it.uuid }) {
      AutomatchItem(it, onAction)
    }
    if (state.myTurnGames.isNotEmpty()) {
      if (state.myTurnGames.size > 10) {
        item("Your turn") {
          Header("Your turn")
        }
        items(items = state.myTurnGames, key = { "myturn/${it.id}" }) {
          SmallGameItem(game = it, state.userId, onAction = onAction)
        }
      } else {
        item("MyTurnCarousel") {
          MyTurnCarousel(state.myTurnGames, state.userId, onAction)
        }
      }
    }

    if (state.challenges.isNotEmpty()) {
      item(key = "Challenges") {
        Header("Challenges")
      }
    }

    items(items = state.challenges, key = { "challenge/${it.id}" }) {
      ChallengeItem(it, state.userId, onAction)
    }

    item("NewGameButtons") {
      NewGameButtonsRow(
        playOnlineEnabled = state.playOnlineEnabled,
        customGameEnabled = state.customGameEnabled,
        modifier = Modifier.padding(top = 10.dp),
        onCustomGame = { newChallengeBottomSheetVisible = true },
        onPlayAgainstAI = onNavigateToAIGame,
        onFaceToFace = onNavigateToFaceToFace,
        onPlayOnline = { newAutomatchChallengeBottomSheetVisible = true },
      )
    }
    if (state.loginPromptVisible) {
      item("LoggedOut") {
        LoggedOutItem(
          onNavigateToLogin = onNavigateToLogin,
          onNavigateToSignUp = onNavigateToSignUp
        )
      }
    }

    if (state.opponentTurnGames.isNotEmpty()) {
      item("Opponent's turn") {
        Header("Opponent's turn")
      }
    }
    items(items = state.opponentTurnGames, key = { "opponent/${it.id}" }) {
      SmallGameItem(it, state.userId, onAction)
    }

    if (state.recentGames.isNotEmpty()) {
      item("Recently finished") {
        Header("Recently finished")
      }
    }
    items(items = state.recentGames, key = { "recent/${it.id}" }) {
      SmallGameItem(game = it, state.userId, onAction = onAction)
    }

    if (state.historicGames.isNotEmpty()) {
      item("Older games") {
        Header("Older games")
      }
      item("HistoricGameLazyRow") {
        HistoricGameLazyRow(
          state.historicGames,
          state.userId,
          state.loadedAllHistoricGames,
          onAction
        )
      }
    }
    item("spacer1") {
      Spacer(modifier = Modifier.height(8.dp))
    }
  }
  if (newChallengeBottomSheetVisible) {
    NewChallengeBottomSheet(
      onDismiss = { newChallengeBottomSheetVisible = false },
      onNewChallengeSearchClicked = {
        onAction(Action.NewChallengeSearchClicked(it))
        newChallengeBottomSheetVisible = false
      }
    )
  }
  if (newAutomatchChallengeBottomSheetVisible) {
    NewAutomatchChallengeBottomSheet(
      onDismiss = { newAutomatchChallengeBottomSheetVisible = false },
      onAutomatchSearchClicked = { speeds, sizes ->
        onAction(Action.NewAutomatchSearch(speeds, sizes))
        newAutomatchChallengeBottomSheetVisible = false
      }
    )
  }
}

@Composable
fun LoggedOutItem(
  onNavigateToLogin: () -> Unit,
  onNavigateToSignUp: () -> Unit,
) {
  SenteCard(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 24.dp, vertical = 16.dp)
  ) {
    Column {
      Image(
        painter = painterResource(id = R.drawable.logo_ogs),
        contentDescription = null,
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 32.dp)
      )
      Text(
        text = "Welcome to Sente!",
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier
          .padding(horizontal = 16.dp)
          .fillMaxWidth()
      )
      Text(
        text = "This app is an open source, community supported frontend for the OGS (online-go.com) website. You can explore the tutorials and play against the AI, but most features require a free OGS account.",
        fontSize = 16.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
          .padding(horizontal = 16.dp, vertical = 8.dp)
          .fillMaxWidth()
      )
      Button(
        onClick = onNavigateToLogin,
        modifier = Modifier
          .padding(horizontal = 16.dp, vertical = 8.dp)
          .fillMaxWidth(),
      ) {
        Text(
          text = "Log in to OGS",
          fontSize = 16.sp
        )
      }
      Button(
        onClick = onNavigateToSignUp,
        modifier = Modifier
          .padding(horizontal = 16.dp, vertical = 8.dp)
          .fillMaxWidth(),
      ) {
        Text(
          text = "Sign up to OGS",
          fontSize = 16.sp
        )
      }
      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

@Composable
private fun Header(text: String) {
  Text(
    text = text,
    fontSize = 12.sp,
    color = MaterialTheme.colorScheme.onBackground,
    fontWeight = FontWeight.Medium,
    modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp)
  )
}

@ExperimentalFoundationApi
@ExperimentalComposeUiApi
@Preview
@Composable
private fun Preview() {
  OnlineGoTheme(darkTheme = false) {
    Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
      MyGamesContent(
        MyGamesState(
          userId = 0L,
          headerMainText = "Hi MrAlex!",
          tutorialVisible = true,
          tutorialPercentage = 23,
          tutorialTitle = "Basics > How to capture",
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
                ui_class = null,
                historicRating = null,
                deviation = null
              ),
              challenged = Player(
                id = 1L,
                username = "Somebody",
                rating = null,
                acceptedStones = null,
                country = null,
                icon = null,
                ui_class = null,
                historicRating = null,
                deviation = null
              ),
              rules = "japanese",
              handicap = 0,
              gameId = 123L,
              disabledAnalysis = true,
              height = 19,
              width = 19,
              ranked = true,
              speed = "correspondence",
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
                ui_class = null,
                historicRating = null,
                deviation = null,
              ),
              challenged = Player(
                id = 0L,
                username = "Me",
                rating = null,
                acceptedStones = null,
                country = null,
                icon = null,
                ui_class = null,
                historicRating = null,
                deviation = null,
              ),
              rules = "japanese",
              handicap = 0,
              gameId = 123L,
              disabledAnalysis = true,
              height = 19,
              width = 19,
              ranked = true,
              speed = "correspondence",
            ),
          ),
        ),
        {},
        {},
        {},
        {},
        {},
      )
    }
  }
}

@Preview
@Composable
private fun PreviewLoggedOut() {
  OnlineGoTheme(darkTheme = false) {
    Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
      MyGamesContent(
        MyGamesState(
          userId = 0L,
          headerMainText = "Hi MrAlex!",
          headerSubText = "You are logged out",
          online = false,
          loginPromptVisible = true,
          playOnlineEnabled = false,
          customGameEnabled = false,
        ),
        {},
        {},
        {},
        {},
        {},
      )
    }
  }
}
