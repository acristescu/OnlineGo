package io.zenandroid.onlinego.ui.screens.mygames

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.zenandroid.onlinego.data.model.BoardTheme
import io.zenandroid.onlinego.data.model.local.Challenge
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.data.model.local.Player
import io.zenandroid.onlinego.data.model.ogs.OGSAutomatch
import io.zenandroid.onlinego.data.model.ogs.SizeSpeedOption
import io.zenandroid.onlinego.ui.screens.mygames.composables.AutomatchItem
import io.zenandroid.onlinego.ui.screens.mygames.composables.ChallengeDetailsDialog
import io.zenandroid.onlinego.ui.screens.mygames.composables.ChallengeItem
import io.zenandroid.onlinego.ui.screens.mygames.composables.HistoricGameLazyRow
import io.zenandroid.onlinego.ui.screens.mygames.composables.HomeScreenHeader
import io.zenandroid.onlinego.ui.screens.mygames.composables.MyTurnCarousel
import io.zenandroid.onlinego.ui.screens.mygames.composables.NewGameButtonsRow
import io.zenandroid.onlinego.ui.screens.mygames.composables.SmallGameItem
import io.zenandroid.onlinego.ui.screens.mygames.composables.TutorialItem
import io.zenandroid.onlinego.ui.screens.newchallenge.NewChallengeBottomSheet
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme
import io.zenandroid.onlinego.utils.WhatsNewUtils
import io.zenandroid.onlinego.utils.rememberStateWithLifecycle
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

@Composable
fun MyGamesScreen(
  onNavigateToGame: (Game) -> Unit,
  onNavigateToAIGame: () -> Unit,
) {
  val viewModel: MyGamesViewModel = koinViewModel()
  val state by rememberStateWithLifecycle(viewModel.state)

  MyGamesContent(state, viewModel::onAction, onNavigateToAIGame)

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
        TextButton(onClick = { viewModel.onAction(Action.SupportClicked) }) {
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

//private fun onAction(action: Action) {
//  when (action) {
//    Action.PlayAgainstAI -> {
//      analytics.logEvent("localai_item_clicked", null)
//      view?.findNavController()?.apply {
//        if (currentDestination?.id == R.id.myGames) {
//          navigate(R.id.action_myGamesFragment_to_aiGameFragment)
//        }
//      }
//    }
//
//    Action.FaceToFace -> {
//      analytics.logEvent("face2face_item_clicked", null)
//      view?.findNavController()?.apply {
//        if (currentDestination?.id == R.id.myGames) {
//          navigate(R.id.action_myGamesFragment_to_faceToFaceFragment)
//        }
//      }
//    }
//
//    Action.PlayOnline -> {
//      analytics.logEvent("automatch_item_clicked", null)
//      (activity as MainActivity).onAutoMatchSearch()
//    }
//
//    Action.SupportClicked -> {
//      analytics.logEvent("support_whats_new_clicked", null)
//      (activity as MainActivity).onNavigateToSupport()
//    }
//
//    is GameSelected -> {
//      val game = action.game
//      analytics.logEvent("clicked_game", Bundle().apply {
//        putLong("GAME_ID", game.id)
//        putBoolean("ACTIVE_GAME", game.ended == null)
//      })
//      navigateToGameScreen(game)
//    }
//
//    else -> viewModel.onAction(action)
//  }
//}

@ExperimentalFoundationApi
@ExperimentalComposeUiApi
@Composable
fun MyGamesContent(
  state: MyGamesState,
  onAction: (Action) -> Unit,
  onNavigateToAIGame: () -> Unit,
) {
  var newChallengeBottomSheetVisible by remember { mutableStateOf(false) }
  val listState = rememberLazyListState()
  LazyColumn(
    state = listState,
    modifier = Modifier
      .fillMaxHeight()
      .background(MaterialTheme.colorScheme.surface)
  ) {
    item {
      HomeScreenHeader(
        image = state.userImageURL,
        mainText = state.headerMainText,
        subText = state.headerSubText,
        offline = !state.online,
      )
    }
    if (state.tutorialVisible) {
      item {
        TutorialItem(
          percentage = state.tutorialPercentage ?: 0,
          tutorial = state.tutorialTitle ?: ""
        )
      }
    }
    items(items = state.automatches) {
      AutomatchItem(it, onAction)
    }
    if (state.myTurnGames.isNotEmpty()) {
      if (state.myTurnGames.size > 10) {
        item {
          Header("Your turn")
        }
        items(items = state.myTurnGames) {
          SmallGameItem(game = it, boardTheme = state.boardTheme, state.userId, onAction = onAction)
        }
      } else {
        item {
          MyTurnCarousel(state.myTurnGames, boardTheme = state.boardTheme, state.userId, onAction)
        }
      }
    }

    if (state.challenges.isNotEmpty()) {
      item(key = "Challenges") {
        Header("Challenges")
      }
    }

    items(items = state.challenges) {
      ChallengeItem(it, state.userId, onAction)
    }

    item {
      NewGameButtonsRow(
        modifier = Modifier.padding(top = 10.dp),
        onCustomGame = { newChallengeBottomSheetVisible = true },
        onPlayAgainstAI = onNavigateToAIGame,
        onFaceToFace = { },
        onPlayOnline = { },
      )
    }

    if (state.opponentTurnGames.isNotEmpty()) {
      item {
        Header("Opponent's turn")
      }
    }
    items(items = state.opponentTurnGames) {
      SmallGameItem(it, boardTheme = state.boardTheme, state.userId, onAction)
    }

    if (state.recentGames.isNotEmpty()) {
      item {
        Header("Recently finished")
      }
    }
    items(items = state.recentGames) {
      SmallGameItem(game = it, boardTheme = state.boardTheme, state.userId, onAction = onAction)
    }

    if (state.historicGames.isNotEmpty()) {
      item {
        Header("Older games")
      }
      item {
        HistoricGameLazyRow(
          state.historicGames,
          boardTheme = state.boardTheme,
          state.userId,
          state.loadedAllHistoricGames,
          onAction
        )
      }
    }
    item {
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
          boardTheme = BoardTheme.WOOD,
        ),
        {},
        {},
      )
    }
  }
}
