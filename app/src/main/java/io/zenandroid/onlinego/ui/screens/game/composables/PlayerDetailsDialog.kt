package io.zenandroid.onlinego.ui.screens.game.composables

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.placeholder.shimmer
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.model.local.HistoryItem
import io.zenandroid.onlinego.data.model.local.Player
import io.zenandroid.onlinego.data.model.local.UserStats
import io.zenandroid.onlinego.data.model.local.WinLossStats
import io.zenandroid.onlinego.data.model.ogs.VersusStats
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme
import io.zenandroid.onlinego.usecases.RepoResult
import io.zenandroid.onlinego.usecases.RepoResult.Success
import io.zenandroid.onlinego.utils.egfToRank
import io.zenandroid.onlinego.utils.formatRank
import io.zenandroid.onlinego.utils.getPercentile
import io.zenandroid.onlinego.utils.processGravatarURL
import kotlin.math.roundToInt

@Composable
fun BoxWithImage(
    imageURL: String?,
    modifier: Modifier = Modifier,
    contentWidthPct: Float = .9f,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(top = 51.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { }
                .fillMaxWidth(contentWidthPct)
                .fillMaxHeight()
                .align(Alignment.Center)
                .background(
                    color = MaterialTheme.colors.surface,
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(16.dp)
                .padding(top = 45.dp)
        ) {
            CompositionLocalProvider(
                LocalContentColor provides MaterialTheme.colors.onSurface,
            ) {
                content()
            }
        }
        Image(
            painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(processGravatarURL(imageURL, LocalDensity.current.run { 124.dp.roundToPx() }))
                    .crossfade(true)
                    .placeholder(R.drawable.ic_person_filled_with_background)
                    .error(R.mipmap.placeholder)
                    .build()
            ),
            contentDescription = "Avatar",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colors.surface)
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
fun PlayerDetailsDialog(
    onDialogDismissed: () -> Unit,
    player: Player,
    stats: RepoResult<UserStats>,
    versusStats: RepoResult<VersusStats>,
    versusStatsHidden: Boolean,
) {
    BackHandler { onDialogDismissed() }
    BoxWithImage(
        imageURL = player.icon,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x88000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDialogDismissed() }
            .padding(vertical = 50.dp)
    ) {
        Spacer(modifier = Modifier.height(55.dp))
        Text(
            text = player.username,
            style = MaterialTheme.typography.h1,
            color = MaterialTheme.colors.onSurface,
            modifier = Modifier.padding(top = 14.dp, bottom = 8.dp)
        )

        val rank = formatRank(egfToRank(player.rating), player.deviation, true)
        val rating = player.rating?.toInt()?.toString() ?: ""

        Text(
            text = "$rank ($rating)",
            color = MaterialTheme.colors.onSurface,
            style = MaterialTheme.typography.body2,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (stats is Success && stats.data.highestRating != null) {
            val highestRatingFloat = stats.data.highestRating
            val highestRank =
                formatRank(egfToRank(highestRatingFloat.toDouble()), longFormat = true)
            val highestRating = highestRatingFloat.toInt().toString()
            StatsRow(title = "Highest rank", value = "$highestRank ($highestRating)")
        } else {
            StatsRow(title = "Highest rank", value = "            ", true)
        }

        player.rating?.let {
            StatsRow(title = "Percentile", value = getPercentile(it))
        }

        if (!versusStatsHidden) {
            val versusData = if (versusStats is Success) versusStats.data else VersusStats.EMPTY
            val total = versusData.wins + versusData.losses
            val wonRatio =
                if (total == 0) 0 else (versusData.wins.toFloat() / total * 100).roundToInt()
            val lossRatio =
                if (total == 0) 0 else (versusData.losses.toFloat() / total * 100).roundToInt()

            Text(
                text = "Played vs you",
                color = MaterialTheme.colors.onSurface,
                style = MaterialTheme.typography.body2,
                modifier = Modifier
                    .padding(vertical = 16.dp)
            )
            StatsRowDualValue(
                title = "You Won",
                value1 = versusData.wins,
                value2 = "${wonRatio}%",
                loading = versusStats !is Success,
            )
            StatsRowDualValue(
                title = "They Won",
                value1 = versusData.losses,
                value2 = "${lossRatio}%",
                loading = versusStats !is Success,
            )
            StatsRowDualValue(
                title = "Total",
                value1 = versusData.wins + versusData.losses,
                value2 = if (total == 0) "0%" else "100%",
                loading = versusStats !is Success,
            )
        }

        val statsData = if (stats is Success) stats.data else UserStats.EMPTY
        val total = statsData.wonCount + statsData.lostCount
        Text(
            text = "Ranked Games",
            color = MaterialTheme.colors.onSurface,
            style = MaterialTheme.typography.body2,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        val wonRatio =
            if (total == 0) 100 else (statsData.wonCount.toFloat() / total * 100).roundToInt()
        val lossRatio =
            if (total == 0) 100 else (statsData.lostCount.toFloat() / total * 100).roundToInt()
        StatsRowDualValue(
            title = "Wins",
            value1 = statsData.wonCount,
            value2 = "${wonRatio}%",
            loading = stats !is Success,
        )
        StatsRowDualValue(
            title = "Losses",
            value1 = statsData.lostCount,
            value2 = "${lossRatio}%",
            loading = stats !is Success,
        )
        StatsRowDualValue(
            title = "Total",
            value1 = statsData.wonCount + statsData.lostCount,
            value2 = "100%",
            loading = stats !is Success,
        )
    }
}

fun Modifier.shimmer(visible: Boolean): Modifier = composed {
    placeholder(
        visible = visible,
        shape = RoundedCornerShape(4.dp),
        highlight = PlaceholderHighlight.shimmer(
            highlightColor = MaterialTheme.colors.surface.copy(alpha = .9f),
        ),
        color = MaterialTheme.colors.onBackground.copy(alpha = 0.1f),
    )
}

@Composable
private fun StatsRow(title: String, value: String, loading: Boolean = false) {
    Row(modifier = Modifier.padding(vertical = 1.dp)) {
        Text(
            text = title,
            color = MaterialTheme.colors.onSurface,
            style = MaterialTheme.typography.body2,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = value,
            color = MaterialTheme.colors.onSurface,
            style = MaterialTheme.typography.body2.copy(fontFamily = FontFamily.Monospace),
            modifier = Modifier.shimmer(loading),
        )
    }
}

@Composable
private fun StatsRow(title: String, value: Int, loading: Boolean = false) {
    StatsRow(title = title, value = value.toString(), loading)
}

@Composable
private fun StatsRowDualValue(title: String, value1: Int, value2: String, loading: Boolean = false) {
    val v2 = value2.padStart(8, ' ')
    StatsRow(title = title, value = value1.toString() + v2, loading)
}


@Preview(showBackground = true)
@Composable
fun PreviewPlayerDetailsDialog() {
    OnlineGoTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            PlayerDetailsDialog(
                onDialogDismissed = {},
                player = Player(
                    id = 111L,
                    username = "Bula Bistrucizatorul",
                    rating = 1234.0,
                    historicRating = 1200.0,
                    country = "UK",
                    icon = null,
                    acceptedStones = null,
                    ui_class = null,
                    deviation = null
                ),
                stats = Success(UserStats(
                    highestRating = 1512.0f,
                    highestRatingTimestamp = 34564325L,
                    chartData1M = emptyList(),
                    chartData3M = emptyList(),
                    chartData1Y = emptyList(),
                    chartData5Y = emptyList(),
                    chartDataAll = emptyList(),
                    chartData20G = emptyList(),
                    chartData100G = emptyList(),
                    chartDataAllG = emptyList(),
                    wonCount = 789,
                    lostCount = 453,
                    bestStreak = 45,
                    bestStreakStart = 23452345L,
                    bestStreakEnd = 23456345L,
                    mostFacedId = 2345L,
                    mostFacedGameCount = 234,
                    mostFacedWon = 23,
                    highestWin = HistoryItem(ended = 100L, gameId = 2345L, playedBlack = true, handicap = 3, rating = 2345f, 60f, 60f, 43456L, 2342f, 60f, true, "", false, "Resignation", "overall", 0),
                    last10Games = emptyList(),
                    allGames = WinLossStats(1456, 1f, 234, 1245, .36f, .64f),
                    smallBoard = WinLossStats(1456, .23f, 234, 1245, .36f, .64f),
                    mediumBoard = WinLossStats(1456, .34f, 234, 1245, .36f, .64f),
                    largeBoard = WinLossStats(1456, .43f, 234, 1245, .36f, .64f),
                    blitz = WinLossStats(1456, .43f, 234, 1245, .36f, .64f),
                    live = WinLossStats(1456, .43f, 234, 1245, .36f, .64f),
                    asBlack = WinLossStats(1456, .43f, 234, 1245, .36f, .64f),
                    asWhite = WinLossStats(1456, .43f, 234, 1245, .36f, .64f),
                    correspondence = WinLossStats(1456, .43f, 234, 1245, .36f, .64f),
                )),
                versusStats = Success(VersusStats(
                    draws = 0,
                    history = emptyList(),
                    losses = 10,
                    wins = 40,
                )),
                versusStatsHidden = false,
            )
        }
    }
}
