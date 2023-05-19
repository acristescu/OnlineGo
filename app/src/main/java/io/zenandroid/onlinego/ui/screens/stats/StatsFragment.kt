package io.zenandroid.onlinego.ui.screens.stats

import android.annotation.SuppressLint
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.MotionEvent
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
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.core.content.res.ResourcesCompat
import androidx.core.text.bold
import androidx.core.text.color
import androidx.core.text.scale
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.github.mikephil.charting.charts.BarLineChartBase
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.ChartTouchListener.*
import com.github.mikephil.charting.listener.OnChartGestureListener
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.utils.EntryXComparator
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.model.local.HistoryItem
import io.zenandroid.onlinego.data.model.ogs.OGSPlayer
import io.zenandroid.onlinego.databinding.FragmentStatsBinding
import io.zenandroid.onlinego.gamelogic.Util
import io.zenandroid.onlinego.ui.screens.game.composables.BoxWithImage
import io.zenandroid.onlinego.ui.screens.game.composables.shimmer
import io.zenandroid.onlinego.ui.screens.stats.StatsViewModel.Filter
import io.zenandroid.onlinego.ui.screens.stats.StatsViewModel.StatsState
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme
import io.zenandroid.onlinego.utils.analyticsReportScreen
import io.zenandroid.onlinego.utils.convertCountryCodeToEmojiFlag
import io.zenandroid.onlinego.utils.egfToRank
import io.zenandroid.onlinego.utils.formatRank
import io.zenandroid.onlinego.utils.processGravatarURL
import io.zenandroid.onlinego.utils.rememberStateWithLifecycle
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
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
  private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
  private val shortFormat = DateTimeFormatter.ofPattern("d MMM uuuu")
  private lateinit var binding: FragmentStatsBinding

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ) = ComposeView(requireContext()).apply {
    setContent {
      OnlineGoTheme {
        val state by rememberStateWithLifecycle(viewModel.state)

        StatsScreen(state, viewModel::onFilterChanged)
      }
    }
  }

  override fun onResume() {
    super.onResume()
    analyticsReportScreen("Stats")
  }

  private fun render(state: StatsViewModel.StatsState) {
    state.playerDetails?.let { fillPlayerDetails(it) }
    state.mostFacedOpponent?.let {
      mostFacedOpponent(
        it, state.mostFacedGameCount!!, state.mostFacedWon!!
      )
    }
    state.highestWin?.let { fillHighestWin(it, state.winningGame!!) }
    state.highestRank?.let {
      binding.playerProfile.highestRankView.text = state.highestRank
      binding.playerProfile.highestRankDateView.text = state.highestRankDate
    }
    fillRankGraph(state.chartData)
    fillOutcomePieChart(state.lostCount ?: 0, state.wonCount ?: 0)
    state.streak?.let {
      binding.winningStreak.text = "$it game(s)"
      binding.winningStreakDetails.text = "${state.startDate} to ${state.endDate}"
    }
    state.last10Games?.let { fillCurrentForm(it) }
  }

  private fun fillPlayerDetails(playerDetails: OGSPlayer) {
    binding.playerProfile.apply {
      nameView.text = playerDetails.username
      playerDetails.country?.let {
        flagView.text = convertCountryCodeToEmojiFlag(it)
      }
      playerDetails.icon?.let {
        Glide.with(this@StatsFragment).load(processGravatarURL(it, iconView.width)).transition(
            DrawableTransitionOptions.withCrossFade(
              DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()
            )
          ).apply(RequestOptions().centerCrop().placeholder(R.drawable.ic_person_outline))
          .into(iconView)
      }

      val glicko = playerDetails.ratings?.overall?.rating?.toInt()
      val deviation = playerDetails.ratings?.overall?.deviation?.toInt()
      rankView.text = formatRank(
        egfToRank(playerDetails.ratings?.overall?.rating), playerDetails.ratings?.overall?.deviation
      )
      glickoView.text = "$glicko ± $deviation"
    }
  }

  private fun mostFacedOpponent(playerDetails: OGSPlayer, total: Int, won: Int) {
    binding.apply {
      mostFacedOpponentView.text = playerDetails.username
      opponentRankView.text = "(${
        formatRank(
          egfToRank(playerDetails.ratings?.overall?.rating),
          playerDetails.ratings?.overall?.deviation
        )
      })"
      playerDetails.icon?.let {
        Glide.with(this@StatsFragment).load(processGravatarURL(it, opponentIconView.width))
          .transition(
            DrawableTransitionOptions.withCrossFade(
              DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()
            )
          ).apply(RequestOptions().centerCrop().placeholder(R.drawable.ic_person_outline))
          .apply(RequestOptions().circleCrop().diskCacheStrategy(DiskCacheStrategy.RESOURCE))
          .into(opponentIconView)
      }
      mostFacedOpponentDetailsView.text = "$total games ($won wins, ${total - won} losses)"
    }
  }

  private fun fillHighestWin(playerDetails: OGSPlayer, winningGame: HistoryItem) {
    binding.apply {
      val rank = formatRank(
        egfToRank(playerDetails.ratings?.overall?.rating), playerDetails.ratings?.overall?.rating
      )

      highestWinOpponent.text = playerDetails.username
      highestWinRank.text = "($rank)"
      playerDetails.icon?.let {
        Glide.with(this@StatsFragment).load(processGravatarURL(it, highestWinIconView.width))
          .transition(
            DrawableTransitionOptions.withCrossFade(
              DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()
            )
          ).apply(RequestOptions().centerCrop().placeholder(R.drawable.ic_person_outline))
          .apply(RequestOptions().circleCrop().diskCacheStrategy(DiskCacheStrategy.RESOURCE))
          .into(highestWinIconView)
      }
      highestWinDetails.text = dateFormat.format(Date(winningGame.ended * 1000))
    }
  }

  private fun fillRankGraph(entries: List<Entry>) {
    if (entries.isNotEmpty()) {
      Collections.sort(entries, EntryXComparator())
      val rankDataSet = LineDataSet(entries, "Games").apply {
        setDrawIcons(false)
        lineWidth = 1.3f
        highLightColor = android.graphics.Color.GRAY
        highlightLineWidth = 0.7f
        enableDashedHighlightLine(7f, 2f, 0f)
        setDrawCircles(false)
        setDrawValues(false)
        color = ResourcesCompat.getColor(resources, R.color.rankGraphLine, context?.theme)
        mode = LineDataSet.Mode.LINEAR
        setDrawFilled(true)
        fillDrawable = GradientDrawable(
          GradientDrawable.Orientation.TOP_BOTTOM, arrayOf(
            ResourcesCompat.getColor(resources, R.color.color_type_info, context?.theme),
            android.graphics.Color.TRANSPARENT
          ).toIntArray()
        )
      }
      binding.rankGraph.data = LineData(rankDataSet)
    }
    binding.rankGraph.apply {
      xAxis.apply {
        position = XAxis.XAxisPosition.BOTTOM
        valueFormatter = DayAxisValueFormatter(binding.rankGraph)
        setDrawAxisLine(false)
        setDrawLabels(false)
        textColor = ResourcesCompat.getColor(resources, R.color.colorText, context?.theme)
        setDrawGridLines(false)
      }

      axisLeft.apply {
        setDrawGridLines(false)
        textColor = ResourcesCompat.getColor(resources, R.color.colorText, context?.theme)
        setDrawLabels(false)
        setDrawAxisLine(false)
      }

      axisRight.apply {
        setDrawGridLines(false)
        setDrawLabels(false)
        setDrawAxisLine(false)
        valueFormatter = object : ValueFormatter() {
          override fun getAxisLabel(value: Float, axis: AxisBase?): String {
            return formatRank(egfToRank(value.toDouble()))
          }
        }
      }

      val onChartValueSelectedListener = object : OnChartValueSelectedListener {
        override fun onValueSelected(e: Entry?, h: Highlight?) {
          if (e == null) {
            onNothingSelected()
          } else {
            binding.chartDetails.text = SpannableStringBuilder().bold {
                append(
                  "${e?.y?.toInt()} ELO (${
                    formatRank(
                      egfToRank(e.y.toDouble()), longFormat = true
                    )
                  })"
                )
              }.append(" on ${formatDate(e.x.toLong())}")
          }
        }

        override fun onNothingSelected() {
          if (entries.isNotEmpty()) {
            val delta = (entries.last().y - entries.first().y).toInt()
            val color = ResourcesCompat.getColor(
              resources, if (delta < 0) R.color.chartLost else R.color.chartWon, context?.theme
            )
            val arrow = if (delta < 0) "⬇" else "⬆"
            binding.chartDetails.text = SpannableStringBuilder().color(color) {
                bold { append("$arrow ${abs(entries.last().y - entries.first().y).toInt()} ELO") }
              }.append(" since ${formatDate(entries.first().x.toLong())}")
          }
        }
      }
      onChartValueSelectedListener.onNothingSelected()

      setOnChartValueSelectedListener(onChartValueSelectedListener)

      onChartGestureListener = object : OnChartGestureListener {
        override fun onChartGestureStart(me: MotionEvent, lastPerformedGesture: ChartGesture) {
          data?.setDrawValues(false)
        }

        override fun onChartGestureEnd(me: MotionEvent, lastPerformedGesture: ChartGesture) {
          data?.setDrawValues(false)

          onChartValueSelectedListener.onNothingSelected()
          highlightValues(null)
        }

        override fun onChartLongPressed(me: MotionEvent) {}
        override fun onChartDoubleTapped(me: MotionEvent) {}
        override fun onChartSingleTapped(me: MotionEvent) {}
        override fun onChartFling(
          me1: MotionEvent, me2: MotionEvent, velocityX: Float, velocityY: Float
        ) {
        }

        override fun onChartScale(me: MotionEvent, scaleX: Float, scaleY: Float) {}
        override fun onChartTranslate(me: MotionEvent, dX: Float, dY: Float) {}
      }

      setViewPortOffsets(0f, 0f, 0f, 0f)

      if (entries.isNotEmpty()) {
        axisLeft.axisMinimum = entries.minOf { it.y } * .85f
        axisRight.axisMinimum = entries.minOf { it.y } * .85f
      }

      description.isEnabled = false
      setDrawMarkers(false)
      setNoDataText("No ranked games on record")
      setNoDataTextColor(
        ResourcesCompat.getColor(
          resources, R.color.colorActionableText, context?.theme
        )
      )
      legend.isEnabled = false
      isDoubleTapToZoomEnabled = false

      animateY(250)
      invalidate()
      notifyDataSetChanged()
    }
  }

  private fun formatDate(secondsSinceEpoch: Long): String {
    return shortFormat.format(
      LocalDateTime.ofEpochSecond(
        secondsSinceEpoch, 0, OffsetDateTime.now().offset
      )
    )
  }

  private fun fillOutcomePieChart(lostCount: Int, wonCount: Int) {
    if (wonCount + lostCount != 0) {
      val lostEntry = PieEntry(lostCount.toFloat(), "Lost")
      val wonEntry = PieEntry(wonCount.toFloat(), "Won")
      val dataSet = PieDataSet(listOf(lostEntry, wonEntry), "Games").apply {
        setColors(intArrayOf(R.color.chartLost, R.color.chartWon), context)
        setDrawValues(false)
      }

      binding.chart.data = PieData(dataSet)

      val gamesWon = wonEntry.value.toInt()
      val gamesLost = lostEntry.value.toInt()
      val gamesWonPercent = (gamesWon * 100 / (gamesWon + gamesLost).toFloat())
      val gamesLostPercent = 100 - gamesWonPercent

      val gamesWonString = String.format("%.1f", gamesWonPercent)
      val gamesLostString = String.format("%.1f", gamesLostPercent)

      val wonColor = ResourcesCompat.getColor(resources, R.color.chartWon, context?.theme)
      val lostColor = ResourcesCompat.getColor(resources, R.color.chartLost, context?.theme)

      val centerText = SpannableStringBuilder().color(wonColor) {
          scale(1.8f) { bold { append(gamesWon.toString()) } }
          append(" $gamesWonString%")
        }.append('\n').color(lostColor) {
          scale(1.8f) { bold { append(gamesLost.toString()) } }
          append(" $gamesLostString%")
        }

      binding.chart.centerText = centerText
    }
    binding.chart.apply {
      offsetLeftAndRight(0)
      setTransparentCircleAlpha(0)
      holeRadius = 60f
      setHoleColor(ResourcesCompat.getColor(resources, R.color.colorTextBackground, context?.theme))
      setTouchEnabled(false)
      legend.isEnabled = false
      description.isEnabled = false
      setDrawEntryLabels(false)
      setDrawMarkers(false)
      setNoDataText("No ranked games on record")
      setNoDataTextColor(
        ResourcesCompat.getColor(
          resources, R.color.colorActionableText, context?.theme
        )
      )

      animateX(250)

      invalidate()
    }
  }

  private fun fillCurrentForm(lastGames: List<HistoryItem>) {
    val currentFormSpan = SpannableStringBuilder()
    val wonColor = ResourcesCompat.getColor(resources, R.color.chartWon, context?.theme)
    val lostColor = ResourcesCompat.getColor(resources, R.color.chartLost, context?.theme)
    lastGames.forEach {
      if (it.won) {
        currentFormSpan.color(wonColor) { append("W") }
      } else {
        currentFormSpan.color(lostColor) { append("L") }
      }
    }
    binding.currentForm.text = currentFormSpan
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

@Composable private fun StatsScreen(state: StatsState, onFilterChanged: (Filter) -> Unit) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colors.background)
      .verticalScroll(rememberScrollState())
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
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = state.allGames?.total?.toString() ?: "",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally),
          )
          Text(
            text = "ranked games",
            modifier = Modifier.align(Alignment.CenterHorizontally),
            style = TextStyle(
              fontWeight = FontWeight.Normal, fontSize = 12.sp, letterSpacing = 0.4.sp
            )
          )
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = state.allGames?.winRate?.toString() ?: "",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally),
          )
          Text(
            text = "win rate",
            modifier = Modifier.align(Alignment.CenterHorizontally),
            style = TextStyle(
              fontWeight = FontWeight.Normal, fontSize = 12.sp, letterSpacing = 0.4.sp
            )
          )
        }
      }
      Row(modifier = Modifier.padding(top = 8.dp)) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "3456",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally),
          )
          Text(
            text = "ranked games",
            modifier = Modifier.align(Alignment.CenterHorizontally),
            style = TextStyle(
              fontWeight = FontWeight.Normal, fontSize = 12.sp, letterSpacing = 0.4.sp
            )
          )
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "89%",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally),
          )
          Text(
            text = "long time control",
            modifier = Modifier.align(Alignment.CenterHorizontally),
            style = TextStyle(
              fontWeight = FontWeight.Normal, fontSize = 12.sp, letterSpacing = 0.4.sp
            )
          )
        }
      }
    }
    StatsSurface(title = "Rating over time") {
      ChartWrapper(
        chartData = state.chartData,
        filter = state.filter,
        onFilterChanged = onFilterChanged,
      )
    }
    StatsSurface(title = "Games played by board size") {
      Row(modifier = Modifier.padding(18.dp)) {
        StatsChart(
          values = listOf(state.smallBoard?.total ?: 0, state.mediumBoard?.total ?: 0, state.largeBoard?.total ?: 0).map { it.toFloat() },
          topText = state.allGames?.total?.toString() ?: "",
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
            textMiddle = state.smallBoard?.total?.toString() ?: "",
            value = state.smallBoard?.totalRatio ?: 0f,
            color = COLORS[0],
          )
          StatsBar(
            text = "13×13",
            textMiddle = state.mediumBoard?.total?.toString() ?: "",
            value = state.mediumBoard?.totalRatio ?: 0f,
            color = COLORS[1],
          )
          StatsBar(
            text = "19×19",
            textMiddle = state.largeBoard?.total?.toString() ?: "",
            value = state.largeBoard?.totalRatio ?: 0f,
            color = COLORS[2],
          )
        }
      }
    }
    StatsSurface(title = "Games played by time controls") {
      Row(modifier = Modifier.padding(18.dp)) {
        StatsChart(
          values = listOf(1456f, 4657f, 464f),
          topText = "3456",
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
            textMiddle = " 342",
            value = 27.3f,
            color = COLORS[0],
          )
          StatsBar(
            text = "Live",
            textMiddle = "1442",
            value = 67.3f,
            color = COLORS[1],
          )
          StatsBar(
            text = "Corresp.",
            textMiddle = "  42",
            value = 7.3f,
            color = COLORS[2],
          )
        }
      }
    }
    StatsSurface(title = "Win ratio by Board Size") {
      Column(
        modifier = Modifier.padding(16.dp)
      ) {
        GainLossStatsBar(
          text = "All games",
          secondaryText = "Wins: ${state.allGames?.won} Losses: ${state.allGames?.lost}",
          value = state.allGames?.winRate?.minus(state.allGames.lossRate)?.times(50) ?: 0f,
          modifier = Modifier
        )
        GainLossStatsBar(
          text = "9×9",
          secondaryText = "Wins: ${state.smallBoard?.won} Losses: ${state.smallBoard?.lost}",
          value = state.smallBoard?.winRate?.minus(state.smallBoard.lossRate)?.times(50) ?: 0f,
          modifier = Modifier
            .padding(top = 24.dp)
        )
        GainLossStatsBar(
          text = "13×13",
          secondaryText = "Wins: ${state.mediumBoard?.won} Losses: ${state.mediumBoard?.lost}",
          value = state.mediumBoard?.winRate?.minus(state.mediumBoard.lossRate)?.times(50) ?: 0f,
          modifier = Modifier
            .padding(top = 24.dp)
        )
        GainLossStatsBar(
          text = "19×19",
          secondaryText = "Wins: ${state.largeBoard?.won} Losses: ${state.largeBoard?.lost}",
          value = state.largeBoard?.winRate?.minus(state.largeBoard.lossRate)?.times(50) ?: 0f,
          modifier = Modifier
            .padding(top = 24.dp)
        )
      }
    }
    StatsSurface(title = "Win ratio by Time Controls") {
      Column(
        modifier = Modifier.padding(16.dp)
      ) {
        GainLossStatsBar(
          text = "Blitz",
          secondaryText = "Wins: 34 Losses: 435",
          value = 0.3f,
        )
        GainLossStatsBar(
          text = "Live",
          secondaryText = "Wins: 34 Losses: 435",
          value = 0f,
          modifier = Modifier
            .padding(top = 24.dp)
        )
        GainLossStatsBar(
          text = "Correspondence",
          secondaryText = "Wins: 34 Losses: 435",
          value = -1.3f,
          modifier = Modifier
            .padding(top = 24.dp)
        )
      }
    }
    StatsSurface(title = "Win ratio by Colour") {
      Column(
        modifier = Modifier.padding(16.dp)
      ) {
        GainLossStatsBar(
          text = "Black",
          secondaryText = "Wins: 34 Losses: 435",
          value = 1.3f,
        )
        GainLossStatsBar(
          text = "White",
          secondaryText = "Wins: 34 Losses: 435",
          value = 2.3f,
          modifier = Modifier
            .padding(top = 24.dp)
        )
      }
    }
    Spacer(modifier = Modifier.height(20.dp))
  }
}

@Composable private fun GainLossStatsBar(
  modifier: Modifier = Modifier,
  text: String,
  secondaryText: String,
  value: Float,
) {
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
          text = secondaryText,
          style = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            letterSpacing = 0.4.sp
          ),
          modifier = Modifier.padding(bottom = 4.dp),
        )
      }
      Text(
        text = "${"%+.1f".format(value)}%",
        textAlign = TextAlign.End,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 16.sp,
        color = if (value >= 0) COLORS[1] else COLORS[0],
        modifier = Modifier
          .weight(1f)
          .align(Alignment.CenterVertically),
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
  value: Float,
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
      Text(
        text = "${(value * 100).toInt()}%",
        fontSize = 14.sp,
        textAlign = TextAlign.End,
        modifier = Modifier.weight(1f),
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
          .fillMaxWidth(value)
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
  content: @Composable () -> Unit,
) {
  Column {
    Text(
      text = title,
      fontSize = 14.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colors.onBackground,
      modifier = modifier.padding(start = 8.dp, top = 16.dp)
    )
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
      text = topText ?: "",
      fontSize = 16.sp,
      fontWeight = FontWeight.Medium,
      modifier = Modifier
        .padding(top = 38.dp)
        .align(Alignment.TopCenter),
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

@Preview @Composable private fun PreviewGainLossBar() {
  OnlineGoTheme {
    Surface(
      shape = MaterialTheme.shapes.medium,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
      GainLossStatsBar(
        text = "All games",
        secondaryText = "Wins: 345 Losses: 234",
        value = -10.1f,
        modifier = Modifier.padding(8.dp)
      )
    }
  }
}

@Composable @Preview private fun Preview() {
  OnlineGoTheme {
    StatsScreen(StatsState.Initial.copy(
      chartData = listOf(
        Entry(1f, 1f),
        Entry(2f, 5f),
        Entry(3f, 16f),
        Entry(4f, 12f),
      )
    ), onFilterChanged = { })
  }
}
