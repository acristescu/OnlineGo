package io.zenandroid.onlinego.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClockTickTest {
  private val tick = ClockTick()
  private val turn = 1_000_000L to 1L

  private fun update(millis: Long, running: Boolean = true, move: Pair<Long, Long?> = turn) =
    tick.update(move, PlayerClock(millis), running)

  @Test
  fun ticksAtSecondBoundariesOnlyOnce() {
    assertFalse(update(10_100))
    assertTrue(update(10_000))
    assertFalse(update(9_999))
    assertFalse(update(9_050))
    assertTrue(update(9_000))
    assertFalse(update(8_999))
    for (second in 8 downTo 1) assertTrue(update(second * 1000L))
    assertFalse(update(0))
    assertFalse(update(-100))
  }

  @Test
  fun openingOrResumingAGameDoesNotPlayAnOffBeatTick() {
    assertFalse(update(8_500))
    assertTrue(update(8_000))
    tick.reset()
    assertFalse(update(5_200))
    assertTrue(update(5_000))
  }

  @Test
  fun disabledAndPausedClocksStaySilent() {
    assertFalse(update(10_100))
    assertFalse(update(10_000, running = false))
    assertFalse(update(9_500))
    assertTrue(update(9_000))
    assertFalse(update(8_000, running = false))
  }

  @Test
  fun aNewTurnOrAddedTimeDoesNotProduceASpuriousTick() {
    assertFalse(update(5_500))
    assertFalse(update(4_000, move = 1_001_000L to 2L))
    assertFalse(update(20_000, move = 1_001_000L to 2L))
    assertFalse(update(19_000, move = 1_001_000L to 2L))
  }

  @Test
  fun delayedUpdatesNeverPlayABurstOfCatchUpTicks() {
    assertFalse(update(10_100))
    assertTrue(update(7_990))
    assertFalse(update(7_980))
  }

  @Test
  fun schedulingAlignsWithRemainingSecondsAndTenthsWithoutDrift() {
    assertEquals(1, clockUpdateDelay(10_001))
    assertEquals(1000, clockUpdateDelay(10_000))
    assertEquals(999, clockUpdateDelay(9_999))
    assertEquals(1, clockUpdateDelay(1001))
    assertEquals(100, clockUpdateDelay(1000))
    assertEquals(37, clockUpdateDelay(937))
    assertEquals(1000, clockUpdateDelay(0))
    assertEquals(1000, clockUpdateDelay(Long.MAX_VALUE))
  }
}
