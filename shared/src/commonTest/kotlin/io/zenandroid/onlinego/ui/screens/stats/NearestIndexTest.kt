package io.zenandroid.onlinego.ui.screens.stats

import kotlin.test.Test
import kotlin.test.assertEquals

class NearestIndexTest {

  private val xs = listOf(0f, 10f, 20f, 40f)

  @Test
  fun findsAnExactMatch() {
    assertEquals(2, xs.nearestIndexOf(20f))
  }

  @Test
  fun picksTheCloserNeighbour() {
    assertEquals(1, xs.nearestIndexOf(13f))
    assertEquals(3, xs.nearestIndexOf(31f))
  }

  @Test
  fun breaksATieTowardsTheEarlierPoint() {
    assertEquals(0, xs.nearestIndexOf(5f))
  }

  @Test
  fun clampsOutsideTheRange() {
    assertEquals(0, xs.nearestIndexOf(-5f))
    assertEquals(3, xs.nearestIndexOf(100f))
  }

  @Test
  fun handlesASinglePoint() {
    assertEquals(0, listOf(7f).nearestIndexOf(0f))
  }
}
