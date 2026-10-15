package io.zenandroid.onlinego.data.ogs

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder

/**
 * Stands in for Moshi's built-in `Object` adapter on the `Any`-typed OGS fields, whose runtime
 * representation is load-bearing - see [toPlainValue].
 */
object AnySerializer : KSerializer<Any> {
  override val descriptor: SerialDescriptor = JsonElement.serializer().descriptor

  override fun deserialize(decoder: Decoder): Any {
    val input = decoder as? JsonDecoder
      ?: throw SerializationException("AnySerializer only supports JSON")
    return input.decodeJsonElement().toPlainValue()
      ?: throw SerializationException("Unexpected null for a non-null Any")
  }

  override fun serialize(encoder: Encoder, value: Any) {
    val output = encoder as? JsonEncoder
      ?: throw SerializationException("AnySerializer only supports JSON")
    output.encodeJsonElement(jsonElementOf(value))
  }
}
