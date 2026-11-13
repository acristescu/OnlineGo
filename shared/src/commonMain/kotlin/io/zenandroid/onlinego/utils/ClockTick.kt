package io.zenandroid.onlinego.utils

internal class ClockTick {
  private var previousTurn: Pair<Long, Long?>? = null
  private var previousSecond: Long? = null

  fun update(turn: Pair<Long, Long?>, clock: PlayerClock, running: Boolean): Boolean {
    val second = if (running && clock.timeUntilTimeout > 0 && clock.timeUntilTimeout != Long.MAX_VALUE) {
      (clock.timeUntilTimeout - 1) / 1000 + 1
    } else null
    val tick = turn == previousTurn && second != null && second in 1..10 &&
      previousSecond?.let { second < it } == true
    previousTurn = turn
    previousSecond = second
    return tick
  }

  fun reset() {
    previousTurn = null
    previousSecond = null
  }
}

internal fun clockUpdateDelay(timeLeft: Long): Long {
  if (timeLeft <= 0 || timeLeft == Long.MAX_VALUE) return 1_000L
  val resolution = when (timeLeft) {
    in 1..2_000 -> 100L
    in 2_001 until 3_600_000 -> 1_000L
    in 3_600_000 until 24 * 3_600_000 -> 60_000L
    else -> 12 * 60_000L
  }
  return (timeLeft - 1) % resolution + 1
}
