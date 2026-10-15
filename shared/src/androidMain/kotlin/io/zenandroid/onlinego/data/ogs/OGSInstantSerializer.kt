package io.zenandroid.onlinego.data.ogs

import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.offsetAt
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.Instant

private val OGS_SERVER_ZONE = TimeZone.of("America/New_York")

fun Instant.toOGSDateTime(): String =
  format(DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET, OGS_SERVER_ZONE.offsetAt(this))

object OGSInstantSerializer : KSerializer<Instant> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("OGSInstant", PrimitiveKind.STRING)

  override fun deserialize(decoder: Decoder): Instant = Instant.parse(decoder.decodeString())

  override fun serialize(encoder: Encoder, value: Instant) =
    encoder.encodeString(value.toOGSDateTime())
}
