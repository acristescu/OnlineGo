package io.zenandroid.onlinego.data.db

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.local.Clock
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.data.model.local.Player
import io.zenandroid.onlinego.data.model.local.Puzzle
import io.zenandroid.onlinego.data.model.ogs.MoveTree
import io.zenandroid.onlinego.data.model.ogs.OGSPuzzle
import io.zenandroid.onlinego.data.model.ogs.Phase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DatabaseTest {

  private val database = Room.inMemoryDatabaseBuilder<Database>()
    .setDriver(BundledSQLiteDriver())
    .build()

  @AfterTest
  fun tearDown() {
    database.close()
  }

  @Test
  fun gameTransactionsReadModifyWrite() = runTest {
    val dao = database.gameDao()
    dao.insertAllGames(listOf(game))

    dao.addMoveToGame(game.id, moveNumber = 3, move = Cell(2, 2))
    dao.insertAllGames(
      listOf(
        game.copy(
          blackPlayer = game.blackPlayer.copy(country = null),
          moves = emptyList(),
          phase = Phase.FINISHED,
        )
      )
    )
    dao.updateClock(game.id, playerToMoveId = BLACK_ID, clock = clock.copy(lastMove = 2))

    val stored = dao.monitorGame(game.id).first()
    assertEquals(listOf(Cell(0, 0), Cell(-1, -1), Cell(2, 2)), stored.moves)
    assertEquals("ro", stored.blackPlayer.country)
    assertEquals(Phase.FINISHED, stored.phase)
    assertEquals(BLACK_ID, stored.playerToMoveId)
    assertEquals(2, stored.clock?.lastMove)
    assertNull(stored.undoRequested)
  }

  @Test
  fun puzzleMoveTreeRoundTrips() = runTest {
    val moveTree = MoveTree(
      branches = listOf(MoveTree(x = 3, y = 4, correct_answer = true, text = "ok"))
    )
    val puzzle = Puzzle(
      id = 7,
      order = null,
      owner = null,
      name = "p",
      created = null,
      modified = null,
      puzzle = OGSPuzzle.PuzzleData(move_tree = moveTree),
      private = null,
      width = 9,
      height = 9,
      type = null,
      has_solution = null,
      rating = 0f,
      rating_count = 0,
      rank = 0,
      collection = null,
      view_count = 0,
      solved_count = 0,
      attempt_count = 0,
    )
    database.puzzleDao().insertPuzzles(listOf(puzzle))

    assertEquals(moveTree, database.puzzleDao().getPuzzle(7).first()?.puzzle?.move_tree)
  }

  internal companion object {
    const val BLACK_ID = 2L

    val clock = Clock(
      lastMove = 1,
      receivedAt = 1,
      whiteTimeSimple = null,
      whiteTime = null,
      blackTimeSimple = null,
      blackTime = null,
      startMode = null,
    )

    val game = Game(
      id = 1,
      width = 9,
      height = 9,
      playerToMoveId = 1,
      outcome = null,
      whiteLost = null,
      blackLost = null,
      initialState = null,
      whiteGoesFirst = false,
      moves = listOf(Cell(0, 0)),
      removedStones = null,
      whiteScore = null,
      blackScore = null,
      whitePlayer = Player(id = 1, username = "white", country = "de"),
      blackPlayer = Player(id = BLACK_ID, username = "black", country = "ro"),
      clock = clock,
      phase = Phase.PLAY,
      komi = 6.5f,
      ended = null,
      freeHandicapPlacement = null,
      handicap = null,
      undoRequested = 1,
      undoRequestedBy = 1,
      undoRequestedMoveCount = 1,
      scoreAGAHandicap = null,
      scoreHandicap = null,
      scorePasses = null,
      scorePrisoners = null,
      scoreStones = null,
      scoreTerritory = null,
      scoreTerritoryInSeki = null,
      name = null,
      rules = null,
      ranked = null,
      disableAnalysis = null,
      timeControl = null,
    )
  }
}
