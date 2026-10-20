package io.zenandroid.onlinego.ui.screens.stats

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM
import android.text.format.DateUtils
import android.view.MotionEvent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import io.zenandroid.onlinego.resources.Res
import io.zenandroid.onlinego.resources.stats_filter_1m
import io.zenandroid.onlinego.resources.stats_filter_1y
import io.zenandroid.onlinego.resources.stats_filter_3m
import io.zenandroid.onlinego.resources.stats_filter_5y
import io.zenandroid.onlinego.resources.stats_filter_all
import io.zenandroid.onlinego.resources.stats_filter_last_100
import io.zenandroid.onlinego.resources.stats_filter_last_20
import io.zenandroid.onlinego.resources.stats_no_ranked_games
import io.zenandroid.onlinego.resources.stats_tooltip_at_game
import io.zenandroid.onlinego.resources.stats_tooltip_delta_elo
import io.zenandroid.onlinego.resources.stats_tooltip_elo_rank
import io.zenandroid.onlinego.resources.stats_tooltip_on_date
import io.zenandroid.onlinego.resources.stats_tooltip_since_date
import io.zenandroid.onlinego.resources.stats_tooltip_since_game
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import com.github.mikephil.charting.charts.BarLineChartBase
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.LineDataSet.Mode.LINEAR
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.ChartTouchListener.ChartGesture
import com.github.mikephil.charting.listener.OnChartGestureListener
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.utils.EntryXComparator
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.R.color
import io.zenandroid.onlinego.R.drawable
import io.zenandroid.onlinego.ui.screens.stats.StatsViewModel.Filter
import io.zenandroid.onlinego.ui.screens.stats.StatsViewModel.Filter.ALL
import io.zenandroid.onlinego.ui.screens.stats.StatsViewModel.Filter.ALL_GAMES
import io.zenandroid.onlinego.ui.screens.stats.StatsViewModel.Filter.FIVE_YEARS
import io.zenandroid.onlinego.ui.screens.stats.StatsViewModel.Filter.HUNDRED_GAMES
import io.zenandroid.onlinego.ui.screens.stats.StatsViewModel.Filter.ONE_MONTH
import io.zenandroid.onlinego.ui.screens.stats.StatsViewModel.Filter.ONE_YEAR
import io.zenandroid.onlinego.ui.screens.stats.StatsViewModel.Filter.THREE_MONTHS
import io.zenandroid.onlinego.ui.screens.stats.StatsViewModel.Filter.TWENTY_GAMES
import io.zenandroid.onlinego.ui.theme.LocalThemeSettings
import io.zenandroid.onlinego.utils.egfToRank
import io.zenandroid.onlinego.utils.formatRank
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

@SuppressLint("ClickableViewAccessibility")
@Composable
fun ChartWrapper(
  chartData: List<Pair<Float, Float>>,
  filter: Filter,
  collapseTimeByGame: Boolean,
  onFilterChanged: (Filter) -> Unit,
  disableScroll: (Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  Column(modifier = modifier) {
    var tooltip by remember { mutableStateOf<ChartTooltip?>(null) }
    val noRankedGames = stringResource(Res.string.stats_no_ranked_games)
    Text(
      text = tooltip?.text(collapseTimeByGame) ?: AnnotatedString(""),
      fontSize = 12.sp,
      modifier = Modifier.padding(start = 8.dp, top = 4.dp)
    )
    if (chartData.isEmpty()) {
      Image(
        modifier = Modifier
          .fillMaxWidth()
          .height(200.dp)
          .alpha(alpha = if (LocalThemeSettings.current.isDarkTheme) 0.6f else 0.15f),
        painter = painterResource(id = drawable.chart_placeholder),
        contentScale = ContentScale.FillBounds,
        contentDescription = null
      )
    } else {
      AndroidView(
        factory = { context ->
          LineChart(context).apply {
            val chart = this
            xAxis.apply {
              position = BOTTOM
              valueFormatter = DayAxisValueFormatter(chart)
              setDrawAxisLine(false)
              setDrawLabels(false)
              textColor = ResourcesCompat.getColor(resources, color.colorText, context.theme)
              setDrawGridLines(false)
            }

            axisLeft.apply {
              setDrawGridLines(false)
              textColor = ResourcesCompat.getColor(resources, color.colorText, context.theme)
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

            ChartValueSelectedListener.chart = this
            ChartValueSelectedListener.onTooltipChanged = { tooltip = it }

            val onChartValueSelectedListener = ChartValueSelectedListener
            ChartValueSelectedListener.onNothingSelected()

            setOnChartValueSelectedListener(onChartValueSelectedListener)

            onChartGestureListener = object : OnChartGestureListener {
              override fun onChartGestureStart(
                me: MotionEvent,
                lastPerformedGesture: ChartGesture
              ) {
                data?.setDrawValues(false)
              }

              override fun onChartGestureEnd(
                me: MotionEvent,
                lastPerformedGesture: ChartGesture
              ) {
                data?.setDrawValues(false)

                onChartValueSelectedListener.onNothingSelected()
                highlightValues(null)
              }

              override fun onChartLongPressed(me: MotionEvent) {}
              override fun onChartDoubleTapped(me: MotionEvent) {}
              override fun onChartSingleTapped(me: MotionEvent) {}
              override fun onChartFling(
                me1: MotionEvent,
                me2: MotionEvent,
                velocityX: Float,
                velocityY: Float
              ) {
              }

              override fun onChartScale(me: MotionEvent, scaleX: Float, scaleY: Float) {}
              override fun onChartTranslate(me: MotionEvent, dX: Float, dY: Float) {}
            }

            setViewPortOffsets(0f, 0f, 0f, 0f)

            description.isEnabled = false
            setDrawMarkers(false)
            setNoDataText(noRankedGames)
            setNoDataTextColor(
              ResourcesCompat.getColor(
                resources,
                color.colorActionableText,
                context.theme
              )
            )
            legend.isEnabled = false
            isDoubleTapToZoomEnabled = false

            animateY(250)
            invalidate()
            notifyDataSetChanged()
          }
        },
        update = {
          val entries = chartData.map { Entry(it.first, it.second) }.sortedWith(EntryXComparator())
          val rankDataSet = LineDataSet(entries, "Games").apply {
            setDrawIcons(false)
            lineWidth = 1.3f
            highLightColor = android.graphics.Color.GRAY
            highlightLineWidth = 0.7f
            enableDashedHighlightLine(7f, 2f, 0f)
            setDrawCircles(false)
            setDrawValues(false)
            color =
              ResourcesCompat.getColor(it.resources, R.color.rankGraphLine, it.context?.theme)
            mode = LINEAR
            setDrawFilled(true)
            fillDrawable = GradientDrawable(
              TOP_BOTTOM,
              arrayOf(
                ResourcesCompat.getColor(
                  it.resources,
                  R.color.rankGraphLine,
                  it.context?.theme
                ),
                android.graphics.Color.TRANSPARENT
              ).toIntArray()
            )
          }
          if (entries.isNotEmpty()) {
            it.axisLeft.axisMinimum = entries.minOf { it.y } * .85f
            it.axisRight.axisMinimum = entries.minOf { it.y } * .85f
          }
          it.data = LineData(rankDataSet)
          if (!ChartValueSelectedListener.isDragging) {
            ChartValueSelectedListener.onNothingSelected()
          }
          it.invalidate()
          onFilterChanged(filter)
        },
        modifier = Modifier
          .fillMaxWidth()
          .height(200.dp)
          .pointerInput(Unit) {
            awaitPointerEventScope {
              while (true) {
                val event = awaitPointerEvent()
                if (event.type in arrayOf(PointerEventType.Release, PointerEventType.Exit)) {
                  disableScroll(false)
                } else {
                  disableScroll(true)
                }
              }
            }
          }
      )
    }
    TimeRangeTabs(
      filter = filter,
      collapseTimeByGame = collapseTimeByGame,
      onFilterChanged = onFilterChanged
    )
  }
}

private fun formatDate(context: Context, secondsSinceEpoch: Long): String =
  DateUtils.formatDateTime(
    context,
    secondsSinceEpoch * 1000,
    DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_MONTH
  )

@Composable
private fun TimeRangeTabs(
  filter: Filter,
  collapseTimeByGame: Boolean,
  onFilterChanged: (Filter) -> Unit
) {
  val divider = 5
  val tabs = Filter.entries.run {
    if (collapseTimeByGame) drop(divider)
    else take(divider)
  }
  val index = (Filter.entries.indexOf(filter) % divider)
    .coerceAtMost(tabs.size - 1)
  if (!tabs.contains(filter)) SideEffect {
    onFilterChanged(tabs[index])
  }
  TabRow(
    selectedTabIndex = index,
    contentColor = MaterialTheme.colorScheme.primary,
    containerColor = Color.Transparent,
  ) {
    tabs.forEach { range ->
      Tab (
        selected = range == filter,
        onClick = { onFilterChanged(range) },
        text = { Text(text = stringResource(range.labelResId())) },
        selectedContentColor = MaterialTheme.colorScheme.primary,
        unselectedContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
      )
    }
  }
}

private fun Filter.labelResId() =
  when (this) {
    ONE_MONTH -> Res.string.stats_filter_1m
    THREE_MONTHS -> Res.string.stats_filter_3m
    ONE_YEAR -> Res.string.stats_filter_1y
    FIVE_YEARS -> Res.string.stats_filter_5y
    ALL -> Res.string.stats_filter_all
    TWENTY_GAMES -> Res.string.stats_filter_last_20
    HUNDRED_GAMES -> Res.string.stats_filter_last_100
    ALL_GAMES -> Res.string.stats_filter_all
  }

private sealed interface ChartTooltip {
  data class Selected(val rating: Int, val rank: String, val x: Float) : ChartTooltip
  data class Summary(val delta: Int, val sinceX: Float) : ChartTooltip
}

@Composable
private fun ChartTooltip.text(byGame: Boolean): AnnotatedString {
  val context = LocalContext.current
  return when (this) {
    is ChartTooltip.Selected -> {
      val ratingText = stringResource(Res.string.stats_tooltip_elo_rank, rating, rank)
      val positionText =
        if (byGame) stringResource(Res.string.stats_tooltip_at_game, x.toInt() + 1)
        else stringResource(Res.string.stats_tooltip_on_date, formatDate(context, x.toLong()))
      buildAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(ratingText) }
        append(" $positionText")
      }
    }

    is ChartTooltip.Summary -> {
      val color = if (delta < 0) Color(0xFFC63A38) else Color(0xFF90b06e)
      val arrow = if (delta < 0) "⬇" else "⬆"
      val deltaText = stringResource(Res.string.stats_tooltip_delta_elo, delta)
      val sinceText =
        if (byGame) stringResource(Res.string.stats_tooltip_since_game, sinceX.toInt() + 1)
        else stringResource(
          Res.string.stats_tooltip_since_date,
          formatDate(context, sinceX.toLong())
        )
      buildAnnotatedString {
        withStyle(
          SpanStyle(
            color = color,
            fontWeight = FontWeight.Bold
          )
        ) { append("$arrow $deltaText") }
        append(" $sinceText")
      }
    }
  }
}

private object ChartValueSelectedListener : OnChartValueSelectedListener {
  var chart: LineChart? = null
  var onTooltipChanged: (ChartTooltip) -> Unit = {}
  var isDragging = false
    private set

  override fun onValueSelected(e: Entry?, h: Highlight?) {
    if (e == null || chart == null) {
      onNothingSelected()
    } else {
      isDragging = true
      onTooltipChanged(
        ChartTooltip.Selected(
          rating = e.y.toInt(),
          rank = formatRank(egfToRank(e.y.toDouble()), longFormat = true),
          x = e.x,
        )
      )
    }
  }

  override fun onNothingSelected() {
    isDragging = false
    val chart = chart ?: return
    if ((chart.data?.entryCount ?: 0) != 0) {
      val entries = (chart.data as LineData).dataSets[0]
      val last = entries.getEntryForXValue(entries.xMax, 0f)
      val first = entries.getEntryForXValue(entries.xMin, 0f)
      onTooltipChanged(ChartTooltip.Summary(delta = (last.y - first.y).toInt(), sinceX = first.x))
    }
  }
}

class DayAxisValueFormatter(private val chart: BarLineChartBase<*>) : ValueFormatter() {

  private val yearFormat = LocalDate.Format { year() }
  private val monthFormat = LocalDate.Format {
    monthName(MonthNames.ENGLISH_ABBREVIATED)
    char('\'')
    yearTwoDigits(baseYear = 2000)
  }
  private val dayFormat = LocalDate.Format {
    day()
    char(' ')
    monthName(MonthNames.ENGLISH_ABBREVIATED)
  }

  override fun getFormattedValue(secondsSinceEpoch: Float): String? {
    val date = Instant.fromEpochSeconds(secondsSinceEpoch.toLong())
      .toLocalDateTime(TimeZone.currentSystemDefault())
      .date
    return when {
      chart.visibleXRange > 189_216_000 -> date.format(yearFormat) // 6 years
      chart.visibleXRange > 15_780_000 -> date.format(monthFormat) // 6 months
      else -> date.format(dayFormat)
    }
  }
}



