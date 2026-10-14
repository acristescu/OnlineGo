package io.zenandroid.onlinego.data.ogs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val HEADER =
  "ended\tgame_id\tplayed_black\thandicap\trating\tdeviation\tvolatility\topponent_id\t" +
      "opponent_rating\topponent_deviation\toutcome\textra\tannulled\tresult"

private const val NEWEST =
  "1532206900\t13701699\t1\t0\t1503.28\t190.92\t0.060005\t196869\t1661.61\t61.82\t0\tnull\t0\tTimeout"

private const val OLDEST =
  "1469392610\t5906460\t1\t0\t1480.44\t330.96\t0.060000\t165311\t2107.30\t67.55\t0\tnull\t0\tTimeout"

private const val INITIAL_RATING =
  "1469306210\t0\t0\t0\t1500.00\t350.00\t0.060000\t0\t0.00\t0.00\t-1\t" +
      "{\"special\":\"initial rating\"}\t0\t"

private val BODY = "$HEADER\n$NEWEST\n$OLDEST\n$INITIAL_RATING\n"

class Glicko2HistoryConverterTest {

  @Test
  fun `keeps every real game, including the oldest`() {
    val history = parseGlicko2History(BODY).history

    assertEquals(2, history.size)
    assertEquals(13701699L, history[0].gameId)
    assertEquals(5906460L, history[1].gameId)
  }

  @Test
  fun `drops the synthetic initial rating row`() {
    val history = parseGlicko2History(BODY).history

    assertTrue(history.none { it.gameId == 0L })
    assertTrue(history.none { it.extra.contains("initial rating") })
  }

  @Test
  fun `parses every column of a row`() {
    val game = parseGlicko2History(BODY).history.first()

    assertEquals(1532206900L, game.ended)
    assertEquals(13701699L, game.gameId)
    assertEquals(true, game.playedBlack)
    assertEquals(0, game.handicap)
    assertEquals(1503.28f, game.rating, 0.001f)
    assertEquals(190.92f, game.deviation, 0.001f)
    assertEquals(0.060005f, game.volatility, 0.0000001f)
    assertEquals(196869L, game.opponentId)
    assertEquals(1661.61f, game.opponentRating, 0.001f)
    assertEquals(61.82f, game.opponentDeviation, 0.001f)
    assertEquals(false, game.won)
    assertEquals("null", game.extra)
    assertEquals(false, game.annulled)
    assertEquals("Timeout", game.result)
  }

  @Test
  fun `tolerates a body with no trailing newline`() {
    assertEquals(2, parseGlicko2History(BODY.trimEnd('\n')).history.size)
  }

  @Test
  fun `returns nothing for a body that is only a header`() {
    assertEquals(0, parseGlicko2History("$HEADER\n").history.size)
  }
}
