package io.zenandroid.onlinego.ui.screens.game

import androidx.compose.runtime.Composable
import io.zenandroid.onlinego.data.ogs.TimeControl
import io.zenandroid.onlinego.resources.Res
import io.zenandroid.onlinego.resources.duration_days
import io.zenandroid.onlinego.resources.duration_hours
import io.zenandroid.onlinego.resources.duration_minutes
import io.zenandroid.onlinego.resources.duration_seconds
import io.zenandroid.onlinego.resources.duration_two_units
import io.zenandroid.onlinego.resources.duration_weeks
import io.zenandroid.onlinego.resources.time_control_absolute
import io.zenandroid.onlinego.resources.time_control_byoyomi
import io.zenandroid.onlinego.resources.time_control_canadian
import io.zenandroid.onlinego.resources.time_control_fischer
import io.zenandroid.onlinego.resources.time_control_none
import io.zenandroid.onlinego.resources.time_control_pauses_on_weekends
import io.zenandroid.onlinego.resources.time_control_simple
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun timeControlDescription(timeControl: TimeControl): String {

  val system = timeControl.system ?: timeControl.time_control
  var desc = when (system) {
    "simple" -> stringResource(
      Res.string.time_control_simple,
      formatSeconds(timeControl.per_move)
    )

    "fischer" -> stringResource(
      Res.string.time_control_fischer,
      formatSeconds(timeControl.initial_time),
      formatSeconds(timeControl.time_increment),
      formatSeconds(timeControl.max_time)
    )

    "byoyomi" -> {
      val periods = timeControl.periods ?: 0
      pluralStringResource(
        Res.plurals.time_control_byoyomi,
        periods,
        formatSeconds(timeControl.main_time),
        periods,
        formatSeconds(timeControl.period_time)
      )
    }

    "canadian" -> {
      val stones = timeControl.stones_per_period ?: 0
      pluralStringResource(
        Res.plurals.time_control_canadian,
        stones,
        formatSeconds(timeControl.main_time),
        formatSeconds(timeControl.period_time),
        stones
      )
    }

    "absolute" -> stringResource(
      Res.string.time_control_absolute,
      formatSeconds(timeControl.total_time)
    )

    "none" -> stringResource(Res.string.time_control_none)
    else -> "?"
  }

  if (timeControl.pause_on_weekends == true) {
    desc += " " + stringResource(Res.string.time_control_pauses_on_weekends)
  }

  return desc
}

@Composable
private fun formatSeconds(seconds: Int?): String {
  seconds?.let {
    var s = it.toDouble()
    val weeks = (s / (86400 * 7)).toLong()
    s -= weeks.toInt() * 86400 * 7
    val days = (s / 86400).toLong()
    s -= days * 86400
    val hours = (s / 3600).toLong()
    s -= hours * 3600
    val minutes = (s / 60).toLong()
    s -= minutes * 60

    return when {
      weeks > 0 -> duration(
        Res.plurals.duration_weeks,
        weeks,
        Res.plurals.duration_days,
        days
      )

      days > 0 -> duration(
        Res.plurals.duration_days,
        days,
        Res.plurals.duration_hours,
        hours
      )

      hours > 0 -> duration(
        Res.plurals.duration_hours,
        hours,
        Res.plurals.duration_minutes,
        minutes
      )

      minutes > 0 -> duration(
        Res.plurals.duration_minutes,
        minutes,
        Res.plurals.duration_seconds,
        s.toLong()
      )

      else -> durationUnit(Res.plurals.duration_seconds, s.toLong())
    }
  }
  return "?"
}

@Composable
private fun durationUnit(unit: PluralStringResource, value: Long): String =
  pluralStringResource(unit, value.toInt(), value)

/** Formats [value] of [unit], appending [remainderValue] of [remainderUnit] when it is not zero. */
@Composable
private fun duration(
  unit: PluralStringResource,
  value: Long,
  remainderUnit: PluralStringResource,
  remainderValue: Long,
): String {
  val head = durationUnit(unit, value)
  return if (remainderValue > 0) {
    stringResource(Res.string.duration_two_units, head, durationUnit(remainderUnit, remainderValue))
  } else head
}
