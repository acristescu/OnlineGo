package io.zenandroid.onlinego.ui.screens.localai

import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.katago.MoveInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class AiGameViewModelTest {

  private val boardHeight = 19

  private fun moveInfo(move: String, visits: Int) = MoveInfo(
    move = move,
    visits = visits,
    winrate = 0.5f,
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
    moveInfos: List<MoveInfo>,
    temperature: Float,
    random: Random,
    lastMove: Cell? = null,
    localitySigma: Float? = null,
  ) = selectAiMove(
    moveInfos = moveInfos,
    temperature = temperature,
    boardHeight = boardHeight,
    lastMove = lastMove,
    localitySigma = localitySigma,
    random = random,
  )

  @Test
  fun `temperature zero always plays the top move`() {
    val moveInfos = listOf(
      moveInfo("D4", visits = 5),
      moveInfo("Q16", visits = 500),
      moveInfo("A1", visits = 1),
    )

    repeat(20) {
      val selected = select(moveInfos, temperature = 0f, random = Random(it))
      assertEquals("D4", selected.move)
    }
  }

  @Test
  fun `a dominant move is still picked almost always at high temperature`() {
    val moveInfos = listOf(
      moveInfo("saveGroup", visits = 100_000),
      moveInfo("blunder1", visits = 1),
      moveInfo("blunder2", visits = 1),
    )
    val random = Random(42)

    val counts = (1..1000).map { select(moveInfos, temperature = 1.5f, random = random).move }
      .groupingBy { it }
      .eachCount()

    val dominantCount = counts["saveGroup"] ?: 0
    assertTrue(
      "Expected the dominant move to be picked almost always, but counts were $counts",
      dominantCount > 990
    )
  }

  @Test
  fun `comparable candidates get genuine variety at temperature one`() {
    val moveInfos = listOf(
      moveInfo("moveA", visits = 100),
      moveInfo("moveB", visits = 100),
      moveInfo("moveC", visits = 100),
    )
    val random = Random(7)

    val counts = (1..3000).map { select(moveInfos, temperature = 1f, random = random).move }
      .groupingBy { it }
      .eachCount()

    // Each move has an equal visit count, so each should be picked roughly a third of the time.
    counts.values.forEach { count ->
      assertTrue("Expected roughly even distribution, but counts were $counts", count in 800..1200)
    }
  }

  @Test
  fun `locality bias strongly favors a move near the last move over an equally-visited distant one`() {
    // D16 -> Cell(3, 3); D15 is one point away (distance 1); A1 is across the board (distance 15).
    val moveInfos = listOf(
      moveInfo("D15", visits = 50),
      moveInfo("A1", visits = 50),
    )
    val random = Random(3)

    val counts = (1..1000).map {
      select(
        moveInfos,
        temperature = 1.8f,
        random = random,
        lastMove = Cell(3, 3),
        localitySigma = 3f,
      ).move
    }.groupingBy { it }.eachCount()

    val nearCount = counts["D15"] ?: 0
    assertTrue(
      "Expected the nearby move to dominate despite equal visits, but counts were $counts",
      nearCount > 990
    )
  }

  @Test
  fun `null locality sigma leaves the visit-based distribution untouched`() {
    val moveInfos = listOf(
      moveInfo("D15", visits = 50),
      moveInfo("A1", visits = 50),
    )
    val random = Random(11)

    val counts = (1..2000).map {
      select(
        moveInfos,
        temperature = 1f,
        random = random,
        lastMove = Cell(3, 3),
        localitySigma = null,
      ).move
    }.groupingBy { it }.eachCount()

    // Equal visits and no locality bias should give a roughly even split.
    counts.values.forEach { count ->
      assertTrue("Expected roughly even distribution, but counts were $counts", count in 800..1200)
    }
  }

  @Test
  fun `pass is always played when it is the top move`() {
    // pass is listed first (the top move by KataGo's own ranking) despite fewer visits.
    val moveInfos = listOf(
      moveInfo("pass", visits = 10),
      moveInfo("D4", visits = 1000),
    )

    listOf(0f, 0.5f, 1.8f).forEach { temperature ->
      repeat(20) {
        val selected = select(moveInfos, temperature = temperature, random = Random(it))
        assertEquals("pass", selected.move)
      }
    }
  }

  @Test
  fun `pass is never played unless it is the top move`() {
    // D4 is the top move; pass is a lower-ranked alternative with a competitive visit count.
    val moveInfos = listOf(
      moveInfo("D4", visits = 100),
      moveInfo("pass", visits = 90),
      moveInfo("Q16", visits = 5),
    )
    val random = Random(5)

    val counts = (1..2000).map { select(moveInfos, temperature = 1.8f, random = random).move }
      .groupingBy { it }
      .eachCount()

    assertEquals("Expected pass to never be sampled, but counts were $counts", null, counts["pass"])
  }

  @Test
  fun `effectiveLocalitySigma is disabled during the opening and restored afterwards`() {
    assertEquals(null, effectiveLocalitySigma(localitySigma = 5f, plyNumber = 0, boardWidth = 19))
    assertEquals(null, effectiveLocalitySigma(localitySigma = 5f, plyNumber = 18, boardWidth = 19))
    assertEquals(5f, effectiveLocalitySigma(localitySigma = 5f, plyNumber = 19, boardWidth = 19))
    assertEquals(5f, effectiveLocalitySigma(localitySigma = 5f, plyNumber = 40, boardWidth = 19))
    assertEquals(
      null,
      effectiveLocalitySigma(localitySigma = null, plyNumber = 40, boardWidth = 19)
    )
  }
}
