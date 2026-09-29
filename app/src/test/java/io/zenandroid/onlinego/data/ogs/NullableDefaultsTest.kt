package io.zenandroid.onlinego.data.ogs

import io.zenandroid.onlinego.data.model.ogs.Chat
import io.zenandroid.onlinego.data.model.ogs.PuzzleSolution
import io.zenandroid.onlinego.utils.appJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * `explicitNulls = false` is what lets the ~130 nullable-without-default properties keep decoding
 * from payloads that omit them, the way Moshi did. Its one subtlety is a nullable property that
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
    val chat = appJson.decodeFromString<Chat>(
      """{"channel":"main","line":{"username":"a","player_id":1,"date":0,"body":"hi"}}"""
    )

    assertNull(chat.game_id)
    assertNull(chat.chat_id)
    assertNull(chat.line.ratings)
    assertNull(chat.line.move_number)
  }
}
