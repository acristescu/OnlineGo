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
 * OGS has never been consistent about booleans: an old game may report `1` where a newer one
 * reports `true` or `"true"`. This accepts all three, matching the Moshi adapter it replaces.
 *
 * Unlike that adapter, which wrote the *string* `"true"`, this encodes a real JSON boolean. The
 * old write path was unreachable - every `Boolean` in a request body is non-null, and the Moshi
 * adapter was bound to the boxed type only.
 */
object OGSBooleanSerializer : KSerializer<Boolean> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("OGSBoolean", PrimitiveKind.BOOLEAN)

  override fun deserialize(decoder: Decoder): Boolean {
    val input = decoder as? JsonDecoder
      ?: throw SerializationException("OGSBooleanSerializer only supports JSON")
    val primitive = input.decodeJsonElement() as? JsonPrimitive
      ?: throw SerializationException("Error trying to parse a non-primitive as boolean")
    if (primitive.isString) return primitive.content.equals("true", ignoreCase = true)
    primitive.content.toBooleanStrictOrNull()?.let { return it }
    primitive.content.toDoubleOrNull()?.let { return it != 0.0 }
    throw SerializationException("Error trying to parse `${primitive.content}` as boolean")
  }

  override fun serialize(encoder: Encoder, value: Boolean) = encoder.encodeBoolean(value)
}
