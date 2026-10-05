package io.zenandroid.onlinego.ui.screens.game

import android.content.res.Resources
import androidx.annotation.PluralsRes
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.ogs.TimeControl

fun timeControlDescription(resources: Resources, timeControl: TimeControl): String {

  val system = timeControl.system ?: timeControl.time_control
  var desc = when (system) {
    "simple" -> resources.getString(
      R.string.time_control_simple,
      formatSeconds(resources, timeControl.per_move)
    )

    "fischer" -> resources.getString(
      R.string.time_control_fischer,
      formatSeconds(resources, timeControl.initial_time),
      formatSeconds(resources, timeControl.time_increment),
      formatSeconds(resources, timeControl.max_time)
    )

    "byoyomi" -> {
      val periods = timeControl.periods ?: 0
      resources.getQuantityString(
        R.plurals.time_control_byoyomi,
        periods,
        formatSeconds(resources, timeControl.main_time),
        periods,
        formatSeconds(resources, timeControl.period_time)
      )
    }

    "canadian" -> {
      val stones = timeControl.stones_per_period ?: 0
      resources.getQuantityString(
        R.plurals.time_control_canadian,
        stones,
        formatSeconds(resources, timeControl.main_time),
        formatSeconds(resources, timeControl.period_time),
        stones
      )
    }

    "absolute" -> resources.getString(
      R.string.time_control_absolute,
      formatSeconds(resources, timeControl.total_time)
    )

    "none" -> resources.getString(R.string.time_control_none)
    else -> resources.getString(R.string.time_control_unknown)
  }

  if (timeControl.pause_on_weekends == true) {
    desc += resources.getString(R.string.time_control_pauses_on_weekends)
  }

  return desc
}

private fun formatSeconds(resources: Resources, seconds: Int?): String {
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
      weeks > 0 -> resources.duration(
        R.plurals.duration_weeks,
        weeks,
        R.plurals.duration_days,
        days
      )

      days > 0 -> resources.duration(
        R.plurals.duration_days,
        days,
        R.plurals.duration_hours,
        hours
      )

      hours > 0 -> resources.duration(
        R.plurals.duration_hours,
        hours,
        R.plurals.duration_minutes,
        minutes
      )

      minutes > 0 -> resources.duration(
        R.plurals.duration_minutes,
        minutes,
        R.plurals.duration_seconds,
        s.toLong()
      )

      else -> resources.durationUnit(R.plurals.duration_seconds, s.toLong())
    }
  }
  return resources.getString(R.string.duration_unknown)
}

private fun Resources.durationUnit(@PluralsRes unit: Int, value: Long): String =
  getQuantityString(unit, value.toInt(), value)

/** Formats [value] of [unit], appending [remainderValue] of [remainderUnit] when it is not zero. */
private fun Resources.duration(
  @PluralsRes unit: Int,
  value: Long,
  @PluralsRes remainderUnit: Int,
  remainderValue: Long,
): String {
  val head = durationUnit(unit, value)
  return if (remainderValue > 0) {
    getString(R.string.duration_two_units, head, durationUnit(remainderUnit, remainderValue))
  } else head
}
