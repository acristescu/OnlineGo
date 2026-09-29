package io.zenandroid.onlinego.data.ogs

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
  .withLocale(Locale.US)
  .withZone(ZoneId.of("America/New_York"))

object OGSInstantSerializer : KSerializer<Instant> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("OGSInstant", PrimitiveKind.STRING)

  override fun deserialize(decoder: Decoder): Instant =
    Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(decoder.decodeString()))

  override fun serialize(encoder: Encoder, value: Instant) =
    encoder.encodeString(FORMATTER.format(value))
}
