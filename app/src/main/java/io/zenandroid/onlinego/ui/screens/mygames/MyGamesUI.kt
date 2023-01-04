package io.zenandroid.onlinego.ui.screens.mygames

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.pager.*
import io.zenandroid.onlinego.data.model.BoardTheme
import io.zenandroid.onlinego.data.model.local.Challenge
import io.zenandroid.onlinego.data.model.local.Player
import io.zenandroid.onlinego.data.model.ogs.OGSAutomatch
import io.zenandroid.onlinego.data.model.ogs.SizeSpeedOption
import io.zenandroid.onlinego.ui.screens.mygames.composables.*
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme


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
        item {
            HomeScreenHeader(
                image = state.userImageURL,
                mainText = state.headerMainText,
                subText = state.headerSubText
            )
        }
        if(state.tutorialVisible) {
            item {
                TutorialItem(percentage = state.tutorialPercentage ?: 0, tutorial = state.tutorialTitle ?: "")
            }
        }
        items(items = state.automatches) {
            AutomatchItem(it, onAction)
        }
        if(state.myTurnGames.isNotEmpty()) {
            if(state.myTurnGames.size > 10) {
                item {
                    Header("Your turn")
                }
                items (items = state.myTurnGames) {
                    SmallGameItem(game = it, boardTheme = state.boardTheme, state.userId, onAction = onAction)
                }
            } else {
                item {
                    MyTurnCarousel(state.myTurnGames, boardTheme = state.boardTheme, state.userId, onAction)
                }
            }
        }

        if(state.challenges.isNotEmpty()) {
            item {
                Header("Challenges")
            }
        }

        items(items = state.challenges) {
            ChallengeItem(it, state.userId, onAction)
        }

        item {
            NewGameButtonsRow(modifier = Modifier.padding(top = 10.dp), onAction)
        }

        if(state.opponentTurnGames.isNotEmpty()) {
            item {
                Header("Opponent's turn")
            }
        }
        items (items = state.opponentTurnGames) {
            SmallGameItem(it, boardTheme = state.boardTheme, state.userId, onAction)
        }

        if(state.recentGames.isNotEmpty()) {
            item {
                Header("Recently finished")
            }
        }
        items (items = state.recentGames) {
            SmallGameItem(game = it, boardTheme = state.boardTheme, state.userId, onAction = onAction)
        }

        if(state.historicGames.isNotEmpty()) {
            item {
                Header("Older games")
            }
            item {
                HistoricGameLazyRow(state.historicGames, boardTheme = state.boardTheme, state.userId, state.loadedAllHistoricGames, onAction)
            }
        }
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun Header(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        color = MaterialTheme.colors.onBackground,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp)
    )
}

@ExperimentalFoundationApi
@ExperimentalPagerApi
@ExperimentalComposeUiApi
@Preview
@Composable
private fun Preview() {
    OnlineGoTheme (darkTheme = false) {
        Box(modifier = Modifier.background(MaterialTheme.colors.background)) {
            MyGamesScreen(MyGamesState(
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
                            ui_class = null,
                            historicRating = null,
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
                            )
                    ),
                ),
                boardTheme = BoardTheme.WOOD,
            )) {}
        }
    }
}