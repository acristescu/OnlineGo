package io.zenandroid.onlinego.gamelogic

import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.data.model.local.InitialState
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class PositionCacheTest {
  @Test
  fun clockChatAndPlayerUpdatesReuseTheExistingPosition() = runTest {
    val game = Game.sampleData()
    val original = RulesManager.replay(game)
    val updated = game.copy(
      messagesCount = 5,
      clock = game.clock?.copy(receivedAt = 999L),
      whitePlayer = game.whitePlayer.copy(username = "Updated name"),
    )
    assertSame(original, RulesManager.replay(updated))
  }

  @Test
  fun moveChangesAndUndoInvalidateThePositionEvenAtTheSameMoveCount() = runTest {
    val game = Game.sampleData()
    val original = RulesManager.replay(game)
    val replaced = RulesManager.replay(game.copy(moves = listOf(Cell(4, 4), Cell(13, 13))))
    assertNotSame(original, replaced)
    assertEquals(setOf(Cell(4, 4)), replaced.whiteStones)
    val undone = RulesManager.replay(game.copy(moves = game.moves!!.dropLast(1)))
    assertNotSame(original, undone)
    assertEquals(1, undone.whiteStones.size + undone.blackStones.size)
  }

  @Test
  fun initialStateRemovedStonesAndKomiInvalidateCachedPositions() = runTest {
    val game = Game.sampleData()
    val original = RulesManager.replay(game)
    assertNotSame(original, RulesManager.replay(game.copy(initialState = InitialState(black = "aa"))))
    assertNotSame(original, RulesManager.replay(game.copy(removedStones = "dd")))
    val updatedKomi = RulesManager.replay(game.copy(komi = 7.5f))
    assertNotSame(original, updatedKomi)
    assertEquals(7.5f, updatedKomi.komi)
  }

  @Test
  fun scoringRulesInvalidateTerritoryCalculations() = runTest {
    val game = Game.sampleData().copy(scoreStones = false)
    val original = RulesManager.replay(game, computeTerritory = true)
    val updated = RulesManager.replay(game.copy(scoreStones = true), computeTerritory = true)
    assertNotSame(original, updated)
  }
}
