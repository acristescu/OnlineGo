package io.zenandroid.onlinego.data.ogs

import io.zenandroid.onlinego.utils.appJson
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.nullable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * OGS has never been consistent here - an old game reports `1` where a newer one reports `true`
 * or `"true"`. These are the shapes the Moshi adapter accepted, so they must keep working.
 */
class OGSBooleanSerializerTest {

  private fun decode(json: String) =
    appJson.decodeFromString(OGSBooleanSerializer.nullable, json)

  @Test
  fun `accepts real booleans`() {
    assertEquals(true, decode("true"))
    assertEquals(false, decode("false"))
  }

  @Test
  fun `accepts numbers, zero being false`() {
    assertEquals(true, decode("1"))
    assertEquals(false, decode("0"))
  }

  @Test
  fun `accepts quoted strings, anything but true being false`() {
    assertEquals(true, decode("\"true\""))
    assertEquals(true, decode("\"TRUE\""))
    assertEquals(false, decode("\"false\""))
    assertEquals(false, decode("\"nonsense\""))
  }

  @Test
  fun `passes null through`() {
    assertNull(decode("null"))
  }

  @Test
  fun `rejects a structure`() {
    assertThrows(SerializationException::class.java) { decode("{}") }
  }

  @Test
  fun `encodes a real boolean, not a string`() {
    assertEquals("true", appJson.encodeToString(OGSBooleanSerializer, true))
  }
}
