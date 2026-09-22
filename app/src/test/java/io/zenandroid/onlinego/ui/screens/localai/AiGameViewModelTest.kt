package io.zenandroid.onlinego.ui.screens.localai

import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.data.model.katago.MoveInfo
import io.zenandroid.onlinego.data.model.katago.RootInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class AiGameViewModelTest {

  // A tiny 3x3 board keeps the humanPolicy arrays (boardWidth*boardHeight + 1 for pass)
  // small and their GTP coordinates easy to hand-verify:
  // index 0="A3" 1="B3" 2="C3" 3="A2" 4="B2" 5="C2" 6="A1" 7="B1" 8="C1" 9=PASS
  private val boardWidth = 3
  private val boardHeight = 3

  private fun moveInfo(move: String, visits: Int = 0, winrate: Float = 0.5f) = MoveInfo(
    move = move,
    visits = visits,
    winrate = winrate,
    scoreStdev = 0f,
    scoreLead = 0f,
    scoreSelfplay = 0f,
    prior = 0f,
    utility = 0f,
    lcb = 0f,
    utilityLcb = 0f,
    order = 0,
    pv = emptyList(),
    pvVisits = null,
  )

  private fun select(
    humanPolicy: List<Float>,
    random: Random,
    moveInfos: List<MoveInfo> = emptyList(),
    rootInfo: RootInfo = RootInfo(winrate = 0.5f),
  ) = selectHumanMove(
    humanPolicy = humanPolicy,
    moveInfos = moveInfos,
    rootInfo = rootInfo,
    boardWidth = boardWidth,
    boardHeight = boardHeight,
    random = random,
  )

  @Test
  fun `selectHumanMove only ever picks the single legal point when everything else is illegal`() {
    // Only index 2 ("C3") is legal; everything else, including pass, is marked -1.
    val humanPolicy = listOf(-1f, -1f, 0.5f, -1f, -1f, -1f, -1f, -1f, -1f, -1f)

    repeat(20) {
      val selected = select(humanPolicy, random = Random(it))
      assertEquals("C3", selected.move)
    }
  }

  @Test
  fun `selectHumanMove samples proportional to policy weight`() {
    // Two legal points: "C3" (index 2) at 90% weight, "B1" (index 7) at 10%.
    val humanPolicy = listOf(-1f, -1f, 0.9f, -1f, -1f, -1f, -1f, 0.1f, -1f, -1f)
    val random = Random(42)

    val counts = (1..2000).map { select(humanPolicy, random = random).move }
      .groupingBy { it }
      .eachCount()

    val dominantCount = counts["C3"] ?: 0
    assertTrue(
      "Expected the 90% weight move to dominate, but counts were $counts",
      dominantCount in 1700..1900
    )
  }

  @Test
  fun `selectHumanMove resolves the pass index to a PASS move`() {
    // Only the trailing (boardWidth*boardHeight) index - pass - is legal.
    val humanPolicy = listOf(-1f, -1f, -1f, -1f, -1f, -1f, -1f, -1f, -1f, 1f)

    val selected = select(humanPolicy, random = Random(1))
    assertEquals("PASS", selected.move)
  }

  @Test
  fun `selectHumanMove plays pass outright when the real search ranks it top, regardless of humanPolicy weight`() {
    // humanPolicy heavily favors "C3" (index 2); pass gets no weight at all here, yet the
    // real search's own top-ranked candidate (moveInfos[0]) already being pass must win.
    val humanPolicy = listOf(-1f, -1f, 1f, -1f, -1f, -1f, -1f, -1f, -1f, -1f)
    val moveInfos = listOf(
      moveInfo("pass", visits = 11, winrate = 0.00002f),
      moveInfo("C3", visits = 4, winrate = 0.00002f),
    )

    repeat(20) {
      val selected = select(humanPolicy, random = Random(it), moveInfos = moveInfos)
      assertEquals("pass", selected.move)
      assertEquals(11, selected.visits)
    }
  }

  @Test
  fun `selectHumanMove reuses a matching moveInfos entry's real stats when the search also explored it`() {
    val humanPolicy = listOf(-1f, -1f, 1f, -1f, -1f, -1f, -1f, -1f, -1f, -1f)
    val moveInfos = listOf(moveInfo("C3", visits = 37, winrate = 0.62f))

    val selected = select(humanPolicy, random = Random(1), moveInfos = moveInfos)

    assertEquals("C3", selected.move)
    assertEquals(37, selected.visits)
    assertEquals(0.62f, selected.winrate, 0.0001f)
  }

  @Test
  fun `selectHumanMove synthesizes a fallback MoveInfo from rootInfo when the search never explored the sampled point`() {
    val humanPolicy = listOf(-1f, -1f, 1f, -1f, -1f, -1f, -1f, -1f, -1f, -1f)
    val rootInfo = RootInfo(
      winrate = 0.42f,
      scoreLead = 3.5f,
      scoreStdev = 1.2f,
      scoreSelfplay = 2.1f,
      utility = 0.15f,
    )

    // moveInfos only covers a different point, so "C3" (the sampled one) isn't in there.
    val selected = select(
      humanPolicy,
      random = Random(1),
      moveInfos = listOf(moveInfo("B1", visits = 5)),
      rootInfo = rootInfo,
    )

    assertEquals("C3", selected.move)
    assertEquals(0, selected.visits)
    assertEquals(-1, selected.order)
    assertEquals(0.42f, selected.winrate, 0.0001f)
    assertEquals(3.5f, selected.scoreLead, 0.0001f)
    assertEquals(1.2f, selected.scoreStdev, 0.0001f)
    assertEquals(2.1f, selected.scoreSelfplay, 0.0001f)
    assertEquals(0.15f, selected.utility, 0.0001f)
    assertTrue(selected.pv.isEmpty())
    assertEquals(null, selected.pvVisits)
  }

  @Test
  fun `selectHumanMove throws when every point in humanPolicy is illegal`() {
    val humanPolicy = List(10) { -1f }

    assertThrows(IllegalStateException::class.java) {
      select(humanPolicy, random = Random(1))
    }
  }

  @Test
  fun `selectBestMove always returns the first candidate regardless of the rest of the list`() {
    val moveInfos = listOf(
      moveInfo("D4", visits = 5, winrate = 0.3f),
      moveInfo("Q16", visits = 500, winrate = 0.9f),
      moveInfo("A1", visits = 1, winrate = 0.1f),
    )

    assertEquals("D4", selectBestMove(moveInfos).move)
  }

  @Test
  fun `selectBestMove plays pass outright when it is the first candidate, with no special-casing`() {
    val moveInfos = listOf(
      moveInfo("pass", visits = 1),
      moveInfo("D4", visits = 1000),
    )

    assertEquals("pass", selectBestMove(moveInfos).move)
  }

  @Test
  fun `orientedWinrateForEngine is unchanged when the engine plays white`() {
    assertEquals(0.9f, orientedWinrateForEngine(0.9f, enginePlaysBlack = false), 0.0001f)
  }

  @Test
  fun `orientedWinrateForEngine is flipped when the engine plays black`() {
    assertEquals(0.1f, orientedWinrateForEngine(0.9f, enginePlaysBlack = true), 0.0001f)
  }

  @Test
  fun `hopelessPassMove returns null when winrate is at or above the threshold, even with pass present`() {
    val moveInfos = listOf(moveInfo("pass", visits = 1), moveInfo("D4", visits = 2))

    assertEquals(null, hopelessPassMove(engineWinrate = 0.01f, moveInfos))
    assertEquals(null, hopelessPassMove(engineWinrate = 0.5f, moveInfos))
  }

  @Test
  fun `hopelessPassMove returns null when winrate is hopeless but no pass candidate exists`() {
    val moveInfos = listOf(moveInfo("D4", visits = 2), moveInfo("Q16", visits = 1))

    assertEquals(null, hopelessPassMove(engineWinrate = 0.001f, moveInfos))
  }

  @Test
  fun `hopelessPassMove returns the pass candidate when winrate is hopeless, even if pass isn't first`() {
    val moveInfos = listOf(
      moveInfo("D4", visits = 2, winrate = 0.0f),
      moveInfo("Q16", visits = 2, winrate = 0.0f),
      moveInfo("pass", visits = 1, winrate = 0.0f),
    )

    val selected = hopelessPassMove(engineWinrate = 0.001f, moveInfos)

    assertEquals("pass", selected?.move)
  }

  @Test
  fun `isGameReady is true only once the engine has started and restore is no longer pending`() {
    assertFalse(AiGameState(engineStarted = false, stateRestorePending = true).isGameReady)
    assertFalse(AiGameState(engineStarted = true, stateRestorePending = true).isGameReady)
    assertFalse(AiGameState(engineStarted = false, stateRestorePending = false).isGameReady)
    assertTrue(AiGameState(engineStarted = true, stateRestorePending = false).isGameReady)
  }

  @Test
  fun `isEnginesTurn matches nextToMove against which color the engine plays`() {
    val blackToMove = Position(boardWidth = 9, boardHeight = 9, nextToMove = StoneType.BLACK)
    val whiteToMove = Position(boardWidth = 9, boardHeight = 9, nextToMove = StoneType.WHITE)

    assertTrue(isEnginesTurn(blackToMove, enginePlaysBlack = true))
    assertFalse(isEnginesTurn(blackToMove, enginePlaysBlack = false))
    assertTrue(isEnginesTurn(whiteToMove, enginePlaysBlack = false))
    assertFalse(isEnginesTurn(whiteToMove, enginePlaysBlack = true))
  }
}
