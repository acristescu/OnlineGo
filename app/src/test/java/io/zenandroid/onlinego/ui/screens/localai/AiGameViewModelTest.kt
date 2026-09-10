package io.zenandroid.onlinego.ui.screens.localai

import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.katago.MoveInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class AiGameViewModelTest {

  private val boardHeight = 19

  private fun moveInfo(move: String, visits: Int, winrate: Float = 0.5f) = MoveInfo(
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
    moveInfos: List<MoveInfo>,
    temperature: Float,
    random: Random,
    lastMove: Cell? = null,
    localitySigma: Float? = null,
    aiWinrate: Float = 0f,
    comebackProbability: Float = 0f,
  ) = selectAiMove(
    moveInfos = moveInfos,
    temperature = temperature,
    boardHeight = boardHeight,
    lastMove = lastMove,
    localitySigma = localitySigma,
    aiWinrate = aiWinrate,
    comebackProbability = comebackProbability,
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
  fun `a move with a dominant winrate is still picked almost always at low temperature`() {
    val moveInfos = listOf(
      moveInfo("saveGroup", visits = 1, winrate = 0.95f),
      moveInfo("blunder1", visits = 1, winrate = 0.05f),
      moveInfo("blunder2", visits = 1, winrate = 0.05f),
    )
    val random = Random(42)

    val counts = (1..1000).map { select(moveInfos, temperature = 0.05f, random = random).move }
      .groupingBy { it }
      .eachCount()

    val dominantCount = counts["saveGroup"] ?: 0
    assertTrue(
      "Expected the dominant move to be picked almost always, but counts were $counts",
      dominantCount > 990
    )
  }

  @Test
  fun `wildly different visit counts with equal winrate still get roughly even sampling`() {
    val moveInfos = listOf(
      moveInfo("moveA", visits = 100_000, winrate = 0.5f),
      moveInfo("moveB", visits = 1, winrate = 0.5f),
    )
    val random = Random(19)

    val counts = (1..2000).map { select(moveInfos, temperature = 1f, random = random).move }
      .groupingBy { it }
      .eachCount()

    counts.values.forEach { count ->
      assertTrue("Expected roughly even distribution, but counts were $counts", count in 900..1100)
    }
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
  fun `comeback override never fires at or below 70 percent aiWinrate`() {
    // Low temperature makes normal sampling deterministically favor "best", isolating
    // whether the override incorrectly kicks in.
    val moveInfos = listOf(
      moveInfo("best", visits = 100, winrate = 0.68f),
      moveInfo("worst", visits = 1, winrate = 0.10f),
    )
    val random = Random(9)

    repeat(200) {
      val selected = select(
        moveInfos,
        temperature = 0.05f,
        random = random,
        aiWinrate = 0.70f,
        comebackProbability = 1f
      )
      assertEquals("best", selected.move)
    }
  }

  @Test
  fun `comeback override never fires when comebackProbability is zero`() {
    // Low temperature makes normal sampling deterministically favor "best", isolating
    // whether the override incorrectly kicks in.
    val moveInfos = listOf(
      moveInfo("best", visits = 100, winrate = 0.95f),
      moveInfo("closeToEven", visits = 1, winrate = 0.51f),
    )
    val random = Random(11)

    repeat(200) {
      val selected = select(
        moveInfos,
        temperature = 0.05f,
        random = random,
        aiWinrate = 0.95f,
        comebackProbability = 0f
      )
      assertEquals("best", selected.move)
    }
  }

  @Test
  fun `comeback override always plays the candidate closest to 50 percent winrate once triggered`() {
    val moveInfos = listOf(
      moveInfo("best", visits = 100, winrate = 0.95f),
      moveInfo("closeToEven", visits = 1, winrate = 0.51f),
      moveInfo("tooFar", visits = 1, winrate = 0.20f),
    )
    val random = Random(13)

    repeat(200) {
      val selected = select(
        moveInfos,
        temperature = 1f,
        random = random,
        aiWinrate = 0.95f,
        comebackProbability = 1f
      )
      assertEquals("closeToEven", selected.move)
    }
  }

  @Test
  fun `comeback override fires roughly comebackProbability of the time above the threshold`() {
    // A very low temperature makes normal sampling pick "best" almost every time it isn't
    // overridden, so the observed rate of "closeToEven" isolates how often the override fires.
    val moveInfos = listOf(
      moveInfo("best", visits = 100, winrate = 0.95f),
      moveInfo("closeToEven", visits = 1, winrate = 0.51f),
    )
    val random = Random(15)

    val counts = (1..3000).map {
      select(
        moveInfos,
        temperature = 0.05f,
        random = random,
        aiWinrate = 0.95f,
        comebackProbability = 0.5f
      ).move
    }.groupingBy { it }.eachCount()

    val overrideCount = counts["closeToEven"] ?: 0
    assertTrue(
      "Expected the override to fire roughly half the time, but counts were $counts",
      overrideCount in 1350..1650
    )
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

  @Test
  fun `orientedWinrate is unchanged when the engine plays white and flipped when it plays black`() {
    assertEquals(0.73f, orientedWinrate(rawWinrate = 0.73f, engineIsWhite = true), 0.0001f)
    assertEquals(0.27f, orientedWinrate(rawWinrate = 0.73f, engineIsWhite = false), 0.0001f)
  }

  @Test
  fun `logit is zero at 50 percent, symmetric, and unbounded away from the edges`() {
    assertEquals(0.0, logit(0.5f), 0.0001)
    assertEquals(-logit(0.9f), logit(0.1f), 0.0001)
    assertTrue("Expected logit to grow well past 1 near the edges", logit(0.98f) > 3.5)
    assertTrue("Expected logit to shrink well past -1 near the edges", logit(0.02f) < -3.5)
  }

  @Test
  fun `a large winrate gap dominates sampling even at a temperature that used to be too flat`() {
    // Regression check: under the old raw-winrate exp(winrate/temperature) formula, this
    // 34-point gap at temperature 1 only produced a ~1.4x weight difference, making the AI
    // sample close to randomly regardless of how much better one move actually was.
    val moveInfos = listOf(
      moveInfo("clearlyBest", visits = 3, winrate = 0.41f),
      moveInfo("clearlyWorse", visits = 9, winrate = 0.08f),
    )
    val random = Random(21)

    val counts = (1..2000).map { select(moveInfos, temperature = 1f, random = random).move }
      .groupingBy { it }
      .eachCount()

    val bestCount = counts["clearlyBest"] ?: 0
    assertTrue(
      "Expected the clearly-better move to dominate, but counts were $counts",
      bestCount > 1600
    )
  }
}
