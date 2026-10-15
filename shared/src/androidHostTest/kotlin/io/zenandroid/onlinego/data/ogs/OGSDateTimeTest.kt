package io.zenandroid.onlinego.data.ogs

import io.zenandroid.onlinego.utils.microsToISODateTime
import io.zenandroid.onlinego.utils.toEpochMicros
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.time.Instant

/**
 * These used to be `java.time`. It is still on the JVM test classpath, so it serves as the oracle:
 * the strings sent to OGS and the instants decoded from it must not change.
 */
class OGSDateTimeTest {

  private val javaFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
    .withLocale(Locale.US)
    .withZone(ZoneId.of("America/New_York"))

  private val micros = listOf(
    0L,
    1_648_170_011_253_336L,
    1_648_170_011_000_000L,
    1_648_170_011_250_000L,
    1_648_170_011_000_001L,
    1_720_000_000_123_000L,
    1_700_000_000_000_000L,
    1_741_503_600_000_000L,
  )

  @Test
  fun `query timestamps format exactly as java time did`() {
    micros.forEach {
      val expected = javaFormatter.format(java.time.Instant.EPOCH.plus(it, ChronoUnit.MICROS))
      assertEquals("micros=$it", expected, it.microsToISODateTime())
    }
  }

  @Test
  fun `epoch micros round-trip`() {
    micros.forEach {
      val instant = Instant.parse(it.microsToISODateTime())
      assertEquals(it, instant.toEpochMicros())
    }
  }

  @Test
  fun `decodes the shapes OGS sends`() {
    listOf(
      "2022-03-24T21:00:11.253336-04:00",
      "2015-06-18T03:51:23.090817Z",
      "2026-09-29T12:55:57Z",
      "2024-01-05T10:00:00+05:30",
    ).forEach {
      val decoded =
        Json.decodeFromString(OGSInstantSerializer, Json.encodeToString(String.serializer(), it))
      val expected = java.time.Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(it))
      assertEquals(
        it,
        ChronoUnit.MICROS.between(java.time.Instant.EPOCH, expected),
        decoded.toEpochMicros()
      )
    }
  }

  @Test
  fun `encodes exactly as java time did`() {
    micros.forEach {
      val instant = Instant.parse(it.microsToISODateTime())
      val javaInstant = java.time.Instant.EPOCH.plus(it, ChronoUnit.MICROS)
      assertEquals(
        Json.encodeToString(String.serializer(), javaFormatter.format(javaInstant)),
        Json.encodeToString(OGSInstantSerializer, instant)
      )
    }
  }
}
