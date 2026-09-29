package io.zenandroid.onlinego.data.ogs

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive

/**
 * OGS returns whole numbers as floats in places - `"order": 99999999.0` on a puzzle, for one.
 * Moshi's `nextInt`/`nextLong` accepted that whenever the value was exactly integral and threw
 * otherwise, so these do the same. `Json.isLenient` does not cover this; it only relaxes quoting.
 */
object LenientIntSerializer : KSerializer<Int> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("LenientInt", PrimitiveKind.INT)

  override fun deserialize(decoder: Decoder): Int {
    val content = decoder.numericContent()
    content.toIntOrNull()?.let { return it }
    val asDouble = content.toDoubleOrNull()
      ?: throw SerializationException("Expected an int but was `$content`")
    val asInt = asDouble.toInt()
    if (asInt.toDouble() != asDouble) {
      throw SerializationException("Expected an int but was `$content`")
    }
    return asInt
  }

  override fun serialize(encoder: Encoder, value: Int) = encoder.encodeInt(value)
}

object LenientLongSerializer : KSerializer<Long> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("LenientLong", PrimitiveKind.LONG)

  override fun deserialize(decoder: Decoder): Long {
    val content = decoder.numericContent()
    content.toLongOrNull()?.let { return it }
    val asDouble = content.toDoubleOrNull()
      ?: throw SerializationException("Expected a long but was `$content`")
    val asLong = asDouble.toLong()
    if (asLong.toDouble() != asDouble) {
      throw SerializationException("Expected a long but was `$content`")
    }
    return asLong
  }

  override fun serialize(encoder: Encoder, value: Long) = encoder.encodeLong(value)
}

private fun Decoder.numericContent(): String {
  val input = this as? JsonDecoder
    ?: throw SerializationException("Lenient number serializers only support JSON")
  val primitive = input.decodeJsonElement() as? JsonPrimitive
    ?: throw SerializationException("Expected a number but was a structure")
  return primitive.content
}
