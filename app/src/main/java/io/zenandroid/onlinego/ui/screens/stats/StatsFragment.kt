package io.zenandroid.onlinego.ui.screens.stats

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.BarLineChartBase
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.listener.ChartTouchListener.*
import io.zenandroid.onlinego.data.model.local.WinLossStats
import io.zenandroid.onlinego.gamelogic.Util
import io.zenandroid.onlinego.ui.screens.game.composables.BoxWithImage
import io.zenandroid.onlinego.ui.screens.game.composables.shimmer
import io.zenandroid.onlinego.ui.screens.stats.StatsViewModel.Filter
import io.zenandroid.onlinego.ui.screens.stats.StatsViewModel.StatsState
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme
import io.zenandroid.onlinego.utils.analyticsReportScreen
import io.zenandroid.onlinego.utils.egfToRank
import io.zenandroid.onlinego.utils.formatRank
import io.zenandroid.onlinego.utils.rememberStateWithLifecycle
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

const val PLAYER_ID = "PLAYER_ID"

@SuppressLint("SetTextI18n")
class StatsFragment : Fragment() {

  private val viewModel: StatsViewModel by viewModel {
    parametersOf(
      arguments?.getLong(PLAYER_ID) ?: Util.getCurrentUserId()!!
    )
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ) = ComposeView(requireContext()).apply {
    setContent {
      OnlineGoTheme {
        val state by rememberStateWithLifecycle(viewModel.state)

        StatsScreen(state, viewModel::onFilterChanged, viewModel::onGraphChanged)
      }
    }
  }

  override fun onResume() {
    super.onResume()
    analyticsReportScreen("Stats")
  }
}

class DayAxisValueFormatter(private val chart: BarLineChartBase<*>) : ValueFormatter() {

  private val yearFormatter = SimpleDateFormat("yyyy", Locale.US)
  private val monthFormatter = SimpleDateFormat("MMM''yy", Locale.US)
  private val dayFormatter = SimpleDateFormat("dd MMM", Locale.US)

  override fun getFormattedValue(secondsSinceEpoch: Float): String? {
    return when {
      chart.visibleXRange > 189_216_000 -> { // 6 years
        yearFormatter.format(Date(secondsSinceEpoch.toLong() * 1000)).toString()
      }

      chart.visibleXRange > 15_780_000 -> { // 6 months
        monthFormatter.format(Date(secondsSinceEpoch.toLong() * 1000)).toString()
      }

      else -> {
        dayFormatter.format(Date(secondsSinceEpoch.toLong() * 1000)).toString()
      }
    }
  }
}

@Composable private fun StatsScreen(state: StatsState, onFilterChanged: (Filter) -> Unit, onGraphChanged: () -> Unit) {
  var scrollEnabled by remember { mutableStateOf(true) }
  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colors.background)
      .verticalScroll(
        state = rememberScrollState(),
        enabled = scrollEnabled,
      )
  ) {
    BoxWithImage(
      imageURL = state.playerDetails?.icon,
      contentWidthPct = 1f,
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 24.dp, start = 8.dp, end = 8.dp)
    ) {
      Spacer(modifier = Modifier.height(15.dp))
      Text(
        text = state.playerDetails?.username ?: "              ",
        style = MaterialTheme.typography.h1,
        color = MaterialTheme.colors.onSurface,
        modifier = Modifier
          .padding(top = 14.dp, bottom = 8.dp)
          .shimmer(state.playerDetails?.username == null)
      )

      val rank = formatRank(
        egfToRank(state.playerDetails?.ratings?.overall?.rating),
        state.playerDetails?.ratings?.overall?.deviation,
        true
      )
      val rating = state.playerDetails?.ratings?.overall?.rating?.let { "%.0f".format(it) } ?: ""

      Text(
        text = "$rank · ELO $rating",
        color = MaterialTheme.colors.onSurface,
        style = MaterialTheme.typography.body2,
        modifier = Modifier
          .padding(bottom = 16.dp)
          .shimmer(state.playerDetails?.ratings?.overall?.rating == null)
      )

      Row {
        StatsGridCell(
          name = "ranked games",
          value = state.allGames?.total?.toString(),
          modifier = Modifier.weight(1f)
        )
        StatsGridCell(
          name = "win rate",
          value = state.allGames?.winRate?.let { "${"%.1f".format(it * 100f)}%" },
          modifier = Modifier.weight(1f)
        )
      }
      Row(modifier = Modifier.padding(top = 4.dp)) {
        StatsGridCell(
          name = "current streak",
          value = state.currentStreak,
          modifier = Modifier.weight(1f)
        )
        StatsGridCell(
          name = "recent results",
          value = state.recentResults,
          modifier = Modifier.weight(1f)
        )
      }
    }
    StatsSurface(
      title = "Rating over ${if (state.collapseTimeByGame == true) "games" else "time"}",
      key = {
        Switch(
          checked = state.collapseTimeByGame == true,
          onCheckedChange = { onGraphChanged() },
          modifier = Modifier
              .padding(end = 10.dp)
        )
      }
    ) {
      ChartWrapper(
        chartData = state.chartData,
        filter = state.filter,
        collapseTimeByGame = state.collapseTimeByGame == true,
        disableScroll = { scrollEnabled = !it },
        onFilterChanged = onFilterChanged,
      )
    }
    StatsSurface(title = "Games played by board size") {
      Row(modifier = Modifier.padding(18.dp)) {
        StatsChart(
          values = listOf(
            state.smallBoard?.total ?: 0,
            state.mediumBoard?.total ?: 0,
            state.largeBoard?.total ?: 0
          ).map { it.toFloat() },
          topText = state.allGames?.total?.toString(),
          bottomText = "Played",
        )
        Column(
          verticalArrangement = Arrangement.SpaceEvenly,
          modifier = Modifier
            .padding(start = 18.dp)
            .height(128.dp)
        ) {
          StatsBar(
            text = "9×9",
            textMiddle = state.smallBoard?.total?.toString(),
            value = state.smallBoard?.totalRatio,
            color = COLORS[0],
          )
          StatsBar(
            text = "13×13",
            textMiddle = state.mediumBoard?.total?.toString(),
            value = state.mediumBoard?.totalRatio,
            color = COLORS[1],
          )
          StatsBar(
            text = "19×19",
            textMiddle = state.largeBoard?.total?.toString(),
            value = state.largeBoard?.totalRatio,
            color = COLORS[2],
          )
        }
      }
    }
    StatsSurface(title = "Games played by time controls") {
      Row(modifier = Modifier.padding(18.dp)) {
        StatsChart(
          values = listOf(
            state.blitz?.total ?: 0,
            state.live?.total ?: 0,
            state.correspondence?.total ?: 0
          ).map { it.toFloat() },
          topText = state.allGames?.total?.toString(),
          bottomText = "Played",
        )
        Column(
          verticalArrangement = Arrangement.SpaceEvenly,
          modifier = Modifier
            .padding(start = 18.dp)
            .height(128.dp)
        ) {
          StatsBar(
            text = "Blitz",
            textMiddle = state.blitz?.total?.toString(),
            value = state.blitz?.totalRatio,
            color = COLORS[0],
          )
          StatsBar(
            text = "Live",
            textMiddle = state.live?.total?.toString(),
            value = state.live?.totalRatio,
            color = COLORS[1],
          )
          StatsBar(
            text = "Corresp.",
            textMiddle = state.correspondence?.total?.toString(),
            value = state.correspondence?.totalRatio,
            color = COLORS[2],
          )
        }
      }
    }
    StatsSurface(title = "Win ratio by Board Size") {
      Column(
        modifier = Modifier.padding(16.dp)
      ) {
        WinLossStatsBar(
          text = "All games",
          stats = state.allGames,
          modifier = Modifier
        )
        WinLossStatsBar(
          text = "9×9",
          stats = state.smallBoard,
          modifier = Modifier
            .padding(top = 24.dp)
        )
        WinLossStatsBar(
          text = "13×13",
          stats = state.mediumBoard,
          modifier = Modifier
            .padding(top = 24.dp)
        )
        WinLossStatsBar(
          text = "19×19",
          stats = state.largeBoard,
          modifier = Modifier
            .padding(top = 24.dp)
        )
      }
    }
    StatsSurface(title = "Win ratio by Time Controls") {
      Column(
        modifier = Modifier.padding(16.dp)
      ) {
        WinLossStatsBar(
          text = "Blitz",
          stats = state.blitz,
        )
        WinLossStatsBar(
          text = "Live",
          stats = state.live,
          modifier = Modifier
            .padding(top = 24.dp)
        )
        WinLossStatsBar(
          text = "Correspondence",
          stats = state.correspondence,
          modifier = Modifier
            .padding(top = 24.dp)
        )
      }
    }
    StatsSurface(title = "Win ratio by colour") {
      Column(
        modifier = Modifier.padding(16.dp)
      ) {
        WinLossStatsBar(
          text = "Black",
          stats = state.asBlack,
        )
        WinLossStatsBar(
          text = "White",
          stats = state.asWhite,
          modifier = Modifier
            .padding(top = 24.dp)
        )
      }
    }
    Spacer(modifier = Modifier.height(20.dp))
  }
}

@Composable
private fun StatsGridCell(
  name: String,
  value: String?,
  modifier: Modifier = Modifier
) {
  Column(modifier = modifier) {
    Text(
      text = value ?: " ".repeat(10),
      fontWeight = FontWeight.Bold,
      modifier = Modifier
        .align(Alignment.CenterHorizontally)
        .shimmer(value == null),
    )
    Text(
      text = name,
      modifier = Modifier.align(Alignment.CenterHorizontally),
      style = TextStyle(
        fontWeight = FontWeight.Normal, fontSize = 12.sp, letterSpacing = 0.4.sp
      )
    )
  }
}

@Composable
private fun WinLossStatsBar(
  modifier: Modifier = Modifier,
  text: String,
  stats: WinLossStats?,
) {
  val value = if (stats == null) 0f else (stats.winRate - stats.lossRate) * 50f
  Column(modifier = modifier) {
    Row {
      Column {
        Text(
          text = text,
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(bottom = 4.dp),
        )
        Text(
          text = stats?.let { "${stats.won} won · ${stats.lost} lost" } ?: " ".repeat(10),
          style = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            letterSpacing = 0.4.sp
          ),
          modifier = Modifier
            .padding(bottom = 4.dp)
            .shimmer(stats == null),
        )
      }
      Spacer(modifier = Modifier.weight(1f))
      Text(
        text = stats?.let { "${"%+.1f".format(value)}%" } ?: " ".repeat(10),
        textAlign = TextAlign.End,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        color = MaterialTheme.colors.onSurface,
        modifier = Modifier
          .align(Alignment.CenterVertically)
          .shimmer(stats == null),
      )
    }
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(12.dp)
        .clip(RoundedCornerShape(6.dp))
        .background(MaterialTheme.colors.onSurface.copy(alpha = 0.15f))
    ) {
      Row {
        val normalized = (abs(value) / 20f).coerceIn(0.05f, 1f)
        if (normalized < 1f) Spacer(modifier = Modifier.weight(1f - normalized))
        Box(
          modifier = Modifier
            .weight(normalized)
            .height(12.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (value >= 0) Color.Transparent else COLORS[0])
        )
        Box(
          modifier = Modifier
            .weight(normalized)
            .height(12.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (value >= 0) COLORS[1] else Color.Transparent)
        )
        if (normalized < 1f) Spacer(modifier = Modifier.weight(1f - normalized))
      }
    }
  }
}

@Composable private fun StatsBar(
  modifier: Modifier = Modifier,
  text: String,
  textMiddle: String? = null,
  value: Float?,
  color: Color,
) {
  Column(modifier = modifier) {
    Row {
      Text(
        text = text, fontSize = 14.sp, modifier = Modifier.weight(1f)
      )
      textMiddle?.let {
        Text(
          text = it,
          fontSize = 14.sp,
          fontFamily = FontFamily.Monospace,
        )
      }
      Spacer(modifier = Modifier.weight(1f))
      Text(
        text = value?.let { "${(it * 100).toInt()}%" } ?: " ".repeat(5),
        fontSize = 14.sp,
        textAlign = TextAlign.End,
        modifier = Modifier
          .padding(bottom = 2.dp)
          .shimmer(value == null),
      )
    }
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(12.dp)
        .clip(RoundedCornerShape(6.dp))
        .background(color.copy(alpha = 0.2f))
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth(value ?: 0f)
          .height(12.dp)
          .clip(RoundedCornerShape(6.dp))
          .background(color)
      )
    }
  }
}

@Composable private fun StatsSurface(
  title: String,
  modifier: Modifier = Modifier,
  key: @Composable () -> Unit = {},
  content: @Composable () -> Unit,
) {
  Column {
    Row {
      Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colors.onBackground,
        modifier = modifier.padding(start = 8.dp, top = 16.dp)
            .weight(1f)
      )
      key()
    }
    Surface(
      shape = MaterialTheme.shapes.medium,
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 8.dp, end = 8.dp, top = 8.dp)
    ) {
      content()
    }
  }
}

private val COLORS = listOf(
  Color(0xFFED7861), Color(0xFF85CBD9), Color(0xFFFFCC00)
)
private val COLORS_DARK = listOf(
  Color(0xFFC63A38), Color(0xFF06B9A2), Color(0xFFFFCC00)
)

@Composable private fun StatsChart(
  modifier: Modifier = Modifier,
  values: List<Float>,
  topText: String? = null,
  bottomText: String? = null,
) {
  Box(modifier = modifier.size(128.dp)) {
    Canvas(
      modifier = Modifier
        .fillMaxSize()
        .padding(5.dp)
    ) {
      var currentAngle = 0f
      values.forEachIndexed { i, value ->
        val sweepAngle = value / values.sum() * 360f
        drawArc(
          color = COLORS[i],
          startAngle = currentAngle,
          sweepAngle = (sweepAngle - 13f).coerceAtLeast(0f),
          useCenter = false,
          style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round),
        )
        currentAngle += sweepAngle
      }
    }
    Text(
      text = topText ?: " ".repeat(6),
      fontSize = 16.sp,
      fontWeight = FontWeight.Medium,
      modifier = Modifier
        .padding(top = 38.dp)
        .align(Alignment.TopCenter)
        .shimmer(topText == null),
    )
    Text(
      text = bottomText ?: "",
      style = TextStyle(
        fontWeight = FontWeight.Normal, fontSize = 12.sp, letterSpacing = 0.4.sp
      ),
      modifier = Modifier
        .padding(top = 69.dp)
        .align(Alignment.TopCenter),
    )
  }
}

@Preview
@Composable
private fun PreviewGainLossBar() {
  OnlineGoTheme {
    Surface(
      shape = MaterialTheme.shapes.medium,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
      WinLossStatsBar(
        text = "All games",
        stats = WinLossStats(0, 0f, 100, 200, .33f, 0f),
        modifier = Modifier.padding(8.dp)
      )
    }
  }
}

@Composable
@Preview
private fun Preview() {
  OnlineGoTheme {
    StatsScreen(StatsState.Initial.copy(
      chartData = listOf(
        Entry(1f, 1f),
        Entry(2f, 5f),
        Entry(3f, 16f),
        Entry(4f, 12f),
      )
    ), onFilterChanged = { }, onGraphChanged = { })
  }
}
