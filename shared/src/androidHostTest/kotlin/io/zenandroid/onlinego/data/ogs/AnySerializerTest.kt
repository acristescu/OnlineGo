package io.zenandroid.onlinego.data.ogs

import io.zenandroid.onlinego.data.model.local.Time
import io.zenandroid.onlinego.utils.appJson
import kotlinx.serialization.builtins.nullable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The runtime representation matters: callers cast these values to Double and Map, so a JSON
 * number arriving as Int or Long would break `Time.fromMap` and the rating extraction in `Game`.
 */
class AnySerializerTest {

  private fun decode(json: String) = appJson.decodeFromString(AnySerializer.nullable, json)

  @Test
  fun `numbers decode to Double, as Moshi produced`() {
    assertEquals(1.0, decode("1"))
    assertTrue(decode("1") is Double)
    assertTrue(decode("2.5") is Double)
  }

  @Test
  fun `objects decode to a Map and arrays to a List`() {
    val map = decode("""{"a":1,"b":"x"}""") as Map<*, *>
    assertEquals(1.0, map["a"])
    assertEquals("x", map["b"])
    assertEquals(listOf(1.0, 2.0), decode("[1,2]"))
  }

  @Test
  fun `strings, booleans and null survive`() {
    assertEquals("x", decode("\"x\""))
    assertEquals(true, decode("true"))
    assertNull(decode("null"))
  }

  @Test
  fun `a decoded clock object still feeds Time fromMap`() {
    val map = decode("""{"thinking_time":258.44,"periods":5,"period_time":30}""") as Map<*, *>

    val time = Time.fromMap(map)

    assertEquals(258.44, time.thinking_time, 0.001)
    assertEquals(5L, time.periods)
  }
}
