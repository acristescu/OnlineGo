package io.zenandroid.onlinego.data.repositories

import io.zenandroid.onlinego.data.db.GameDao
import io.zenandroid.onlinego.data.model.local.Game
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.Answers
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class GameDaoRefreshTest {
  private val dao: GameDao = mock(defaultAnswer = Answers.CALLS_REAL_METHODS)

  @Test
  fun unchangedOverviewDoesNotRewriteExistingGames() = runTest {
    val game = Game.sampleData()
    whenever(dao.getGameList(listOf(game.id))).thenReturn(listOf(game))
    dao.insertAllGames(listOf(game))
    verify(dao, never()).updateGames(any())
  }

  @Test
  fun incompleteUnchangedOverviewPreservesCachedDetailsWithoutRewriting() = runTest {
    val game = Game.sampleData()
    whenever(dao.getGameList(listOf(game.id))).thenReturn(listOf(game))
    dao.insertAllGames(listOf(game.copy(
      whitePlayer = game.whitePlayer.copy(country = null),
      blackPlayer = game.blackPlayer.copy(country = null),
      moves = emptyList(),
    )))
    verify(dao, never()).updateGames(any())
  }

  @Test
  fun changedGamesAreStillWritten() = runTest {
    val game = Game.sampleData()
    whenever(dao.getGameList(listOf(game.id))).thenReturn(listOf(game))
    val changed = game.copy(playerToMoveId = game.blackPlayer.id)
    dao.insertAllGames(listOf(changed))
    verify(dao).updateGames(listOf(changed))
  }
}
