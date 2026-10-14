package io.zenandroid.onlinego.data.ogs

import io.zenandroid.onlinego.data.model.ogs.OGSPuzzle
import io.zenandroid.onlinego.utils.appJson
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * OGS returns whole numbers as floats in places. Moshi accepted those whenever the value was
 * exactly integral; `Json.isLenient` does not, since it only relaxes quoting.
 */
class LenientNumberSerializersTest {

  private fun int(json: String) = appJson.decodeFromString(LenientIntSerializer, json)
  private fun long(json: String) = appJson.decodeFromString(LenientLongSerializer, json)

  @Test
  fun `plain integers decode`() {
    assertEquals(42, int("42"))
    assertEquals(-1, int("-1"))
    assertEquals(42L, long("42"))
  }

  @Test
  fun `a float with an exactly integral value decodes`() {
    assertEquals(99999999, int("99999999.0"))
    assertEquals(3L, long("3.0"))
  }

  @Test
  fun `a quoted number still decodes`() {
    assertEquals(9, int("\"9\""))
    assertEquals(9L, long("\"9\""))
  }

  @Test
  fun `a genuinely fractional value is rejected, as Moshi rejected it`() {
    assertThrows(SerializationException::class.java) { int("1.5") }
    assertThrows(SerializationException::class.java) { long("1.5") }
  }

  /** The payload that actually broke the puzzle directory. */
  @Test
  fun `a puzzle with a float order decodes`() {
    val puzzle = appJson.decodeFromString<OGSPuzzle>(
      """{"id":2625,"order":99999999.0,"name":"Exercise 001","width":19,"height":19}"""
    )

    assertEquals(2625L, puzzle.id)
    assertEquals(99999999, puzzle.order)
  }
}
