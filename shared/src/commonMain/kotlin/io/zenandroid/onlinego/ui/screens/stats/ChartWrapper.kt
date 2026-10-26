package io.zenandroid.onlinego.ui.screens.stats

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.zenandroid.onlinego.resources.Res
import io.zenandroid.onlinego.resources.chart_placeholder
import io.zenandroid.onlinego.resources.stats_filter_1m
import io.zenandroid.onlinego.resources.stats_filter_1y
import io.zenandroid.onlinego.resources.stats_filter_3m
import io.zenandroid.onlinego.resources.stats_filter_5y
import io.zenandroid.onlinego.resources.stats_filter_all
import io.zenandroid.onlinego.resources.stats_filter_last_100
import io.zenandroid.onlinego.resources.stats_filter_last_20
import io.zenandroid.onlinego.resources.stats_tooltip_at_game
import io.zenandroid.onlinego.resources.stats_tooltip_delta_elo
import io.zenandroid.onlinego.resources.stats_tooltip_elo_rank
import io.zenandroid.onlinego.resources.stats_tooltip_on_date
import io.zenandroid.onlinego.resources.stats_tooltip_since_date
import io.zenandroid.onlinego.resources.stats_tooltip_since_game
import io.zenandroid.onlinego.ui.composables.formatMediumDate
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
import io.zenandroid.onlinego.ui.theme.nightBlue
import io.zenandroid.onlinego.ui.theme.salmon
import io.zenandroid.onlinego.utils.egfToRank
import io.zenandroid.onlinego.utils.formatRank
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun ChartWrapper(
  chartData: List<Pair<Float, Float>>,
  filter: Filter,
  collapseTimeByGame: Boolean,
  onFilterChanged: (Filter) -> Unit,
  modifier: Modifier = Modifier
) {
  Column(modifier = modifier) {
    val points = remember(chartData) { chartData.sortedBy { it.first } }
    var selected by remember(points) { mutableStateOf<Int?>(null) }
    val tooltip = selected?.let { points[it].toSelectedTooltip() } ?: points.toSummaryTooltip()
    Text(
      text = tooltip?.text(collapseTimeByGame) ?: AnnotatedString(""),
      fontSize = 12.sp,
      modifier = Modifier.padding(start = 8.dp, top = 4.dp)
    )
    if (points.isEmpty()) {
      Image(
        modifier = Modifier
          .fillMaxWidth()
          .height(200.dp)
          .alpha(alpha = if (LocalThemeSettings.current.isDarkTheme) 0.6f else 0.15f),
        painter = painterResource(Res.drawable.chart_placeholder),
        contentScale = ContentScale.FillBounds,
        contentDescription = null
      )
    } else {
      RankChart(
        points = points,
        selected = selected,
        onSelectedChanged = { selected = it },
        modifier = Modifier
          .fillMaxWidth()
          .height(200.dp)
      )
    }
    TimeRangeTabs(
      filter = filter,
      collapseTimeByGame = collapseTimeByGame,
      onFilterChanged = onFilterChanged
    )
  }
}

@Composable
private fun RankChart(
  points: List<Pair<Float, Float>>,
  selected: Int?,
  onSelectedChanged: (Int?) -> Unit,
  modifier: Modifier = Modifier
) {
  val lineColor =
    if (LocalThemeSettings.current.isDarkTheme) nightBlue else salmon.copy(alpha = 0.4f)
  val rise = remember { Animatable(0f) }
  LaunchedEffect(Unit) { rise.animateTo(1f, tween(durationMillis = 250, easing = LinearEasing)) }
  val xs = remember(points) { points.map { it.first } }
  val minX = xs.first()
  val maxX = xs.last()
  val minY = points.minOf { it.second } * .85f
  val maxY = points.maxOf { it.second }.let { it + (it - minY) * .1f }

  Canvas(
    modifier = modifier.pointerInput(points) {
      fun indexAt(x: Float): Int {
        val value = if (maxX == minX) minX else minX + x.coerceIn(
          0f,
          size.width.toFloat()
        ) / size.width * (maxX - minX)
        return xs.nearestIndexOf(value)
      }
      awaitEachGesture {
        val down = awaitFirstDown()
        down.consume()
        onSelectedChanged(indexAt(down.position.x))
        do {
          val event = awaitPointerEvent()
          event.changes.forEach { it.consume() }
          event.changes.firstOrNull { it.pressed }
            ?.let { onSelectedChanged(indexAt(it.position.x)) }
        } while (event.changes.any { it.pressed })
        onSelectedChanged(null)
      }
    }
  ) {
    fun xOf(x: Float) =
      if (maxX == minX) size.width / 2 else (x - minX) / (maxX - minX) * size.width

    fun yOf(y: Float) = size.height - (y - minY) / (maxY - minY) * size.height * rise.value

    val line = Path()
    points.forEachIndexed { i, (x, y) ->
      if (i == 0) line.moveTo(xOf(x), yOf(y)) else line.lineTo(xOf(x), yOf(y))
    }
    val fill = Path().apply {
      addPath(line)
      lineTo(xOf(maxX), size.height)
      lineTo(xOf(minX), size.height)
      close()
    }
    drawPath(fill, Brush.verticalGradient(listOf(lineColor, Color.Transparent)))
    drawPath(line, lineColor, style = Stroke(width = 1.3.dp.toPx()))

    selected?.let { points[it] }?.let { (x, y) ->
      val highlight = PathEffect.dashPathEffect(floatArrayOf(7f, 2f))
      val width = 0.7.dp.toPx()
      drawLine(
        Color.Gray,
        Offset(xOf(x), 0f),
        Offset(xOf(x), size.height),
        width,
        pathEffect = highlight
      )
      drawLine(
        Color.Gray,
        Offset(0f, yOf(y)),
        Offset(size.width, yOf(y)),
        width,
        pathEffect = highlight
      )
    }
  }
}

internal fun List<Float>.nearestIndexOf(value: Float): Int {
  val found = binarySearch(value)
  if (found >= 0) return found
  val after = -found - 1
  return when {
    after == 0 -> 0
    after == size -> lastIndex
    value - this[after - 1] <= this[after] - value -> after - 1
    else -> after
  }
}

private fun Pair<Float, Float>.toSelectedTooltip() =
  ChartTooltip.Selected(
    rating = second.toInt(),
    rank = formatRank(egfToRank(second.toDouble()), longFormat = true),
    x = first,
  )

private fun List<Pair<Float, Float>>.toSummaryTooltip() =
  if (isEmpty()) null
  else ChartTooltip.Summary(
    delta = (last().second - first().second).toInt(),
    sinceX = first().first
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
  return when (this) {
    is ChartTooltip.Selected -> {
      val ratingText = stringResource(Res.string.stats_tooltip_elo_rank, rating, rank)
      val positionText =
        if (byGame) stringResource(Res.string.stats_tooltip_at_game, x.toInt() + 1)
        else stringResource(Res.string.stats_tooltip_on_date, formatMediumDate(x.toLong()))
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
          formatMediumDate(sinceX.toLong())
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
