package io.zenandroid.onlinego.ui.screens.mygames

import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.model.local.Game
import org.junit.Assert.assertEquals
import org.junit.Test

class GameOutcomeFormatterTest {

  private val baseGame = Game.sampleData().copy(
    blackPlayer = Game.sampleData().blackPlayer.copy(id = 100L),
    whitePlayer = Game.sampleData().whitePlayer.copy(id = 200L),
  )

  private val stringMap = mapOf(
    R.string.mygames_outcome_cancelled to "Cancelled",
    R.string.mygames_outcome_lost to "Lost",
    R.string.mygames_outcome_won to "Won",
    R.string.mygames_outcome_black_won to "Black won",
    R.string.mygames_outcome_white_won to "White won",
    R.string.mygames_outcome_lost_with_reason to "Lost %1\$s",
    R.string.mygames_outcome_won_with_reason to "Won %1\$s",
    R.string.mygames_outcome_black_won_with_reason to "Black won %1\$s",
    R.string.mygames_outcome_white_won_with_reason to "White won %1\$s",
    R.string.mygames_outcome_reason_resignation to "by resignation",
    R.string.mygames_outcome_reason_points to "by %1\$s points",
    R.string.mygames_outcome_reason_timeout to "by timeout",
    R.string.mygames_outcome_reason_disconnection to "by disconnection",
    R.string.mygames_outcome_reason_other to "by %1\$s",
  )

  private fun format(game: Game, userId: Long?): String =
    formatGameOutcome(game, userId) { resId, args ->
      val template = stringMap[resId] ?: error("Missing resource for $resId")
      if (args.isEmpty()) template else template.format(*args)
    }

  @Test
  fun testAnnulledGameSaysCancelledRegardlessOfOutcome() {
    val game1 =
      baseGame.copy(blackLost = true, whiteLost = false, outcome = "Resignation", annulled = true)
    assertEquals("Cancelled", format(game1, 100L))

    val game2 =
      baseGame.copy(blackLost = false, whiteLost = true, outcome = "34 points", annulled = true)
    assertEquals("Cancelled", format(game2, 100L))

    val game3 = baseGame.copy(blackLost = false, whiteLost = true, outcome = null, annulled = true)
    assertEquals("Cancelled", format(game3, null))
  }

  @Test
  fun testCancellation() {
    val game = baseGame.copy(outcome = "Cancellation")
    assertEquals("Cancelled", format(game, 100L))
    assertEquals("Cancelled", format(game, null))
  }

  @Test
  fun testResignation() {
    // User is Black and lost
    val game1 = baseGame.copy(blackLost = true, whiteLost = false, outcome = "Resignation")
    assertEquals("Lost by resignation", format(game1, 100L))

    // User is White and lost
    val game2 = baseGame.copy(blackLost = false, whiteLost = true, outcome = "Resignation")
    assertEquals("Lost by resignation", format(game2, 200L))

    // User is Black and won
    assertEquals("Won by resignation", format(game2, 100L))

    // User is White and won
    assertEquals("Won by resignation", format(game1, 200L))

    // Spectator: White lost -> Black won
    assertEquals("Black won by resignation", format(game2, 999L))

    // Spectator: Black lost -> White won
    assertEquals("White won by resignation", format(game1, 999L))

    // Case-insensitivity and whitespace
    val gameLower = baseGame.copy(blackLost = true, whiteLost = false, outcome = "  resignation  ")
    assertEquals("Lost by resignation", format(gameLower, 100L))
  }

  @Test
  fun testPoints() {
    // Integer points
    val game1 = baseGame.copy(blackLost = true, whiteLost = false, outcome = "34 points")
    assertEquals("Lost by 34 points", format(game1, 100L))

    // Fractional points
    val game2 = baseGame.copy(blackLost = false, whiteLost = true, outcome = "8.5 points")
    assertEquals("Won by 8.5 points", format(game2, 100L))

    // 0.5 points
    val game3 = baseGame.copy(blackLost = false, whiteLost = true, outcome = "0.5 points")
    assertEquals("Lost by 0.5 points", format(game3, 200L))

    // Singular point
    val game4 = baseGame.copy(blackLost = true, whiteLost = false, outcome = "1 point")
    assertEquals("Won by 1 points", format(game4, 200L))

    // Spectator: White lost -> Black won
    assertEquals("Black won by 8.5 points", format(game2, null))

    // Spectator: Black lost -> White won
    assertEquals("White won by 34 points", format(game1, null))
  }

  @Test
  fun testTimeout() {
    val game1 = baseGame.copy(blackLost = true, whiteLost = false, outcome = "Timeout")
    assertEquals("Lost by timeout", format(game1, 100L))

    val game2 = baseGame.copy(blackLost = false, whiteLost = true, outcome = "Timeout")
    assertEquals("Won by timeout", format(game2, 100L))

    // Spectator
    assertEquals("Black won by timeout", format(game2, null))
    assertEquals("White won by timeout", format(game1, null))

    // Case-insensitivity and whitespace
    val gameUpper = baseGame.copy(blackLost = true, whiteLost = false, outcome = " TIMEOUT ")
    assertEquals("Lost by timeout", format(gameUpper, 100L))
  }

  @Test
  fun testDisconnection() {
    val game1 = baseGame.copy(blackLost = true, whiteLost = false, outcome = "Disconnection")
    assertEquals("Lost by disconnection", format(game1, 100L))

    val game2 = baseGame.copy(blackLost = false, whiteLost = true, outcome = "Disconnection")
    assertEquals("Won by disconnection", format(game2, 100L))

    val res3 = format(game2, null)
    assertEquals("Black won by disconnection", res3)

    val res4 = format(game1, null)
    assertEquals("White won by disconnection", res4)
  }

  @Test
  fun testUnknownOutcomeFallback() {
    val game1 = baseGame.copy(blackLost = true, whiteLost = false, outcome = "Moderator Decision")
    assertEquals("Lost by Moderator Decision", format(game1, 100L))

    val game2 = baseGame.copy(blackLost = false, whiteLost = true, outcome = "Special Rule")
    assertEquals("Won by Special Rule", format(game2, 100L))

    assertEquals("Black won by Special Rule", format(game2, null))
    assertEquals("White won by Moderator Decision", format(game1, null))
  }

  @Test
  fun testBlankOutcomeFallback() {
    val game1 = baseGame.copy(blackLost = true, whiteLost = false, outcome = "")
    assertEquals("Lost", format(game1, 100L))

    val game2 = baseGame.copy(blackLost = false, whiteLost = true, outcome = null)
    assertEquals("Won", format(game2, 100L))

    assertEquals("Black won", format(game2, null))
    assertEquals("White won", format(game1, null))
  }

  @Test
  fun testDirectResIdVerification() {
    val recordedCalls = mutableListOf<Pair<Int, List<Any>>>()
    val game = baseGame.copy(blackLost = true, whiteLost = false, outcome = "8.5 points")
    formatGameOutcome(game, 100L) { resId, args ->
      recordedCalls.add(resId to args.toList())
      "stub"
    }

    assertEquals(2, recordedCalls.size)
    assertEquals(R.string.mygames_outcome_reason_points, recordedCalls[0].first)
    assertEquals(listOf("8.5"), recordedCalls[0].second)
    assertEquals(R.string.mygames_outcome_lost_with_reason, recordedCalls[1].first)
    assertEquals(listOf("stub"), recordedCalls[1].second)
  }
}
