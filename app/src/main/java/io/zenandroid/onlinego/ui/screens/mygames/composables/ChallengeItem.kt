package io.zenandroid.onlinego.ui.screens.mygames.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.zenandroid.onlinego.data.model.local.Challenge
import io.zenandroid.onlinego.data.model.local.Player
import io.zenandroid.onlinego.ui.screens.mygames.Action
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme

@Composable
fun ChallengeItem(challenge: Challenge, userId: Long, onAction: (Action) -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .height(90.dp)
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(top = 10.dp)) {
            val text = if(challenge.challenger?.id == userId) {
                "You are challenging ${challenge.challenged?.username}"
            } else {
                "${challenge.challenger?.username} is challenging you"
            }

            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
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
                    TextButton(onClick = { onAction(Action.ChallengeSeeDetails(challenge)) }) {
                        Text("See Details")
                    }
                    TextButton(onClick = { onAction(Action.ChallengeDeclined(challenge)) }) {
                        Text("Decline")
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    OnlineGoTheme {
        ChallengeItem(
            challenge = Challenge(
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
                    deviation = null,
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
            userId = 0L,
            onAction = {}
        )
    }
}

@Preview
@Composable
private fun Preview1() {
    OnlineGoTheme {
        ChallengeItem(
            challenge = Challenge(
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
            userId = 0L,
            onAction = {}
        )
    }
}