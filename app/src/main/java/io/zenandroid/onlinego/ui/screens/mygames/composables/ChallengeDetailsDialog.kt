package io.zenandroid.onlinego.ui.screens.mygames.composables

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.model.local.Challenge
import io.zenandroid.onlinego.data.model.local.Player
import io.zenandroid.onlinego.ui.screens.mygames.ChallengeDialogStatus
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme
import io.zenandroid.onlinego.utils.processGravatarURL

@Composable
fun ChallengeDetailsDialog(
    onChallengeAccepted: (Challenge) -> Unit,
    onChallengeDeclined: (Challenge) -> Unit,
    onDialogDismissed: () -> Unit,
    status: ChallengeDialogStatus,
) {
    BackHandler { onDialogDismissed() }
    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color(0x88000000))
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) { onDialogDismissed() }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(vertical = 80.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { }
                .fillMaxWidth(.9f)
                .fillMaxHeight()
                .align(Alignment.Center)
                .shadow(4.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(100.dp))
            Text(
                text = status.name ?: "",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 14.dp, bottom = 8.dp)
            )

            Text(
                text = status.rank,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "You received a challenge!",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            status.details.forEach { (name, value) ->
                StatsRow(title = name, value = value)
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth(),
            ) {
                TextButton (onClick = { onChallengeAccepted(status.challenge) }) {
                    Text("Accept")
                }
                TextButton(onClick = { onChallengeDeclined(status.challenge) }) {
                    Text("Decline")
                }
            }
        }
        Image(
            painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(processGravatarURL(status.imageURL, LocalDensity.current.run { 124.dp.roundToPx() }))
                    .crossfade(true)
                    .placeholder(R.drawable.ic_person_filled_with_background)
                    .error(R.mipmap.placeholder)
                    .build()
            ),
            contentDescription = "Avatar",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 29.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(4.dp)
                .size(124.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { }
        )
    }
}

@Composable
private fun StatsRow(title: String, value: String) {
    Row {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun StatsRow(title: String, value: Int) {
    StatsRow(title = title, value = value.toString())
}

@Composable
private fun StatsRowDualValue(title: String, value1: Int, value2: String) {
    val v2 = value2.padStart(8, ' ')
    StatsRow(title = title, value = value1.toString() + v2)
}


@Preview(showBackground = true)
@Composable
fun PreviewPlayerDetailsDialog() {
    OnlineGoTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            ChallengeDetailsDialog(
                onChallengeAccepted = { _ -> },
                onChallengeDeclined = { _ -> },
                onDialogDismissed = {},
                status = ChallengeDialogStatus(
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
                    imageURL = null,
                    rank = "9k (1410)",
                    name = "Bula",
                    details = listOf(
                        "Size" to "19x19"
                    )
                )
            )
        }
    }
}
