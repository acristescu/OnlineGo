package io.zenandroid.onlinego.ui.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.utils.ClockFace
import io.zenandroid.onlinego.utils.ClockPeriod
import io.zenandroid.onlinego.utils.clockFace

@Composable
fun clockText(millis: Long): String = when (val face = clockFace(millis)) {
  ClockFace.Unlimited -> "∞"
  is ClockFace.Days -> pluralStringResource(R.plurals.clock_days, face.days.toInt(), face.days)
  is ClockFace.DaysHours -> stringResource(R.string.clock_days_hours, face.days, face.hours)
  is ClockFace.Hours -> stringResource(R.string.clock_hours, face.hours)
  is ClockFace.HoursMinutes -> stringResource(
    R.string.clock_hours_minutes,
    face.hours,
    face.minutes
  )

  is ClockFace.MinutesSeconds -> stringResource(
    R.string.clock_minutes_seconds,
    face.minutes,
    face.seconds
  )

  is ClockFace.Seconds -> stringResource(R.string.clock_seconds, face.seconds)
  is ClockFace.Tenths -> stringResource(R.string.clock_tenths, face.tenths / 10.0)
}

@Composable
fun clockPeriodText(period: ClockPeriod): String = when (period) {
  is ClockPeriod.Canadian -> stringResource(
    R.string.clock_canadian_period,
    clockText(period.blockMillis),
    period.movesLeft
  )

  is ClockPeriod.ByoYomi -> stringResource(
    R.string.clock_byoyomi_periods,
    period.periodsLeft,
    clockText(period.periodMillis)
  )

  is ClockPeriod.Fischer -> stringResource(
    R.string.clock_fischer_increment,
    clockText(period.incrementMillis)
  )
}
