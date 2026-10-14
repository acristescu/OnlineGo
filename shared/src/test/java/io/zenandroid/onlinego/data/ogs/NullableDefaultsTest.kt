package io.zenandroid.onlinego.data.ogs

import io.zenandroid.onlinego.data.model.ogs.PuzzleSolution
import io.zenandroid.onlinego.utils.appJson
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * `explicitNulls = false` lets a nullable property without a default decode from a payload that
 * omits it, the way Moshi did. Its one subtlety is a nullable property that
 * carries a *non-null* default - these three are the only ones in the codebase, so pin their
 * behaviour rather than reasoning about it.
 */
class NullableDefaultsTest {

  @Test
  fun `an absent key falls back to the declared default`() {
    val solution = appJson.decodeFromString<PuzzleSolution>("""{"puzzle":1}""")

    assertEquals(0L, solution.time_elapsed)
    assertEquals(0, solution.attempts)
  }

  @Test
  fun `an explicit null stays null rather than falling back to the default`() {
    val solution =
      appJson.decodeFromString<PuzzleSolution>("""{"puzzle":1,"time_elapsed":null,"attempts":null}""")

    // Same as Moshi: a default only fills an *absent* key, never an explicit null.
    assertNull(solution.time_elapsed)
    assertNull(solution.attempts)
  }

  @Test
  fun `a supplied value still wins`() {
    val solution =
      appJson.decodeFromString<PuzzleSolution>("""{"puzzle":1,"time_elapsed":42,"attempts":3}""")

    assertEquals(42L, solution.time_elapsed)
    assertEquals(3, solution.attempts)
  }

  @Test
  fun `a nullable property without a default still decodes to null when absent`() {
    val decoded = appJson.decodeFromString<NoDefault>("""{"id":1}""")

    assertNull(decoded.note)
  }

  @Serializable
  private data class NoDefault(val id: Int, val note: String?)
}
