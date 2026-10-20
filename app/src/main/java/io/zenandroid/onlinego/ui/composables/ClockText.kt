package io.zenandroid.onlinego.ui.composables

import androidx.compose.runtime.Composable
import io.zenandroid.onlinego.resources.Res
import io.zenandroid.onlinego.resources.clock_byoyomi_periods
import io.zenandroid.onlinego.resources.clock_canadian_period
import io.zenandroid.onlinego.resources.clock_days
import io.zenandroid.onlinego.resources.clock_days_hours
import io.zenandroid.onlinego.resources.clock_fischer_increment
import io.zenandroid.onlinego.resources.clock_hours
import io.zenandroid.onlinego.resources.clock_hours_minutes
import io.zenandroid.onlinego.resources.clock_minutes_seconds
import io.zenandroid.onlinego.resources.clock_seconds
import io.zenandroid.onlinego.resources.clock_tenths
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import io.zenandroid.onlinego.utils.ClockFace
import io.zenandroid.onlinego.utils.ClockPeriod
import io.zenandroid.onlinego.utils.clockFace

@Composable
fun clockText(millis: Long): String = when (val face = clockFace(millis)) {
  ClockFace.Unlimited -> "∞"
  is ClockFace.Days -> pluralStringResource(Res.plurals.clock_days, face.days.toInt(), face.days)
  is ClockFace.DaysHours -> stringResource(Res.string.clock_days_hours, face.days, face.hours)
  is ClockFace.Hours -> stringResource(Res.string.clock_hours, face.hours)
  is ClockFace.HoursMinutes -> stringResource(
    Res.string.clock_hours_minutes,
    face.hours,
    face.minutes.twoDigits()
  )

  is ClockFace.MinutesSeconds -> stringResource(
    Res.string.clock_minutes_seconds,
    face.minutes,
    face.seconds.twoDigits()
  )

  is ClockFace.Seconds -> stringResource(Res.string.clock_seconds, face.seconds.twoDigits())
  is ClockFace.Tenths -> stringResource(
    Res.string.clock_tenths,
    formatOneDecimal(face.tenths / 10.0)
  )
}

private fun Long.twoDigits(): String = toString().padStart(2, '0')

@Composable
fun clockPeriodText(period: ClockPeriod): String = when (period) {
  is ClockPeriod.Canadian -> stringResource(
    Res.string.clock_canadian_period,
    clockText(period.blockMillis),
    period.movesLeft
  )

  is ClockPeriod.ByoYomi -> stringResource(
    Res.string.clock_byoyomi_periods,
    period.periodsLeft,
    clockText(period.periodMillis)
  )

  is ClockPeriod.Fischer -> stringResource(
    Res.string.clock_fischer_increment,
    clockText(period.incrementMillis)
  )
}
