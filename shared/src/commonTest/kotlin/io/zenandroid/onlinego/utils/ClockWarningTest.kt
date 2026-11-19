package io.zenandroid.onlinego.utils

import io.zenandroid.onlinego.data.model.local.Clock
import io.zenandroid.onlinego.data.model.local.Time
import io.zenandroid.onlinego.data.ogs.TimeControl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClockWarningTest {
  private val start = 1_000_000L
  private val clock = Clock(
    lastMove = start,
    receivedAt = start,
    whiteTimeSimple = null,
    whiteTime = null,
    blackTimeSimple = null,
    blackTime = null,
    startMode = false,
  )

  private fun remaining(time: Time?, elapsed: Long = 0, active: Boolean = true, paused: Long? = null) =
    computeTimeLeft(start + elapsed, clock, null, time, active, paused)

  @Test
  fun absoluteWarnsOnlyInTheLastTenSeconds() {
    assertFalse(remaining(Time(11.0)).timeoutWarning)
    assertTrue(remaining(Time(11.0), elapsed = 1000).timeoutWarning)
    assertTrue(remaining(Time(11.0), elapsed = 10_999).timeoutWarning)
    assertFalse(remaining(Time(11.0), elapsed = 11_000).timeoutWarning)
  }

  @Test
  fun fischerIncrementIsNotGrantedUntilAStoneIsPlayed() {
    val result = computeTimeLeft(
      serverTime = start + 1000,
      clock = clock,
      playerTimeSimple = null,
      playerTime = Time(11.0),
      currentPlayer = true,
      pausedSince = null,
      timeControl = TimeControl(time_control = "fischer", time_increment = 30),
    )
    assertEquals(ClockPeriod.Fischer(30_000), result.period)
    assertEquals(10_000, result.timeUntilTimeout)
    assertTrue(result.timeoutWarning)
  }

  @Test
  fun simpleTimerUsesTheExpirationTimestamp() {
    val result = computeTimeLeft(start + 1000, clock, start + 11_000, null, true, null)
    assertEquals(10_000, result.timeUntilTimeout)
    assertTrue(result.timeoutWarning)
  }

  @Test
  fun byoyomiMainTimeAndSparePeriodsDoNotCauseAnEarlyWarning() {
    val mainTime = remaining(Time(5.0, periods = 3, period_time = 30.0))
    assertEquals(95_000, mainTime.timeUntilTimeout)
    assertFalse(mainTime.timeoutWarning)
    val sparePeriod = remaining(Time(0.0, periods = 2, period_time = 30.0), elapsed = 21_000)
    assertEquals(9_000, sparePeriod.timeLeft)
    assertEquals(39_000, sparePeriod.timeUntilTimeout)
    assertFalse(sparePeriod.timeoutWarning)
  }

  @Test
  fun byoyomiWarnsInTheFinalPeriodIncludingAfterLocalPeriodRollover() {
    val time = Time(0.0, periods = 3, period_time = 30.0)
    assertFalse(remaining(time, elapsed = 79_999).timeoutWarning)
    val lastTen = remaining(time, elapsed = 80_000)
    assertEquals(10_000, lastTen.timeUntilTimeout)
    assertTrue(lastTen.timeoutWarning)
    assertFalse(remaining(time, elapsed = 90_000).timeoutWarning)
  }

  @Test
  fun shortByoyomiPeriodsAreIncludedInTheActualTimeToTimeout() {
    assertTrue(remaining(Time(0.0, periods = 2, period_time = 5.0)).timeoutWarning)
    assertFalse(remaining(Time(0.0, periods = 3, period_time = 5.0)).timeoutWarning)
  }

  @Test
  fun canadianMainTimeDoesNotWarnUntilTheBlockAlsoRunsOut() {
    val time = Time(5.0, block_time = 30.0, moves_left = 10)
    assertFalse(remaining(time).timeoutWarning)
    assertFalse(remaining(time, elapsed = 24_999).timeoutWarning)
    assertTrue(remaining(time, elapsed = 25_000).timeoutWarning)
    assertFalse(remaining(time, elapsed = 35_000).timeoutWarning)
  }

  @Test
  fun inactiveAndPausedClocksDoNotLoseTime() {
    assertEquals(9_000, remaining(Time(9.0), elapsed = 60_000, active = false).timeUntilTimeout)
    assertEquals(8_000, remaining(Time(9.0), elapsed = 60_000, paused = start + 1000).timeUntilTimeout)
  }

  @Test
  fun untimedGamesNeverWarn() {
    assertFalse(remaining(null).timeoutWarning)
    assertEquals(Long.MAX_VALUE, remaining(null).timeUntilTimeout)
  }
}
