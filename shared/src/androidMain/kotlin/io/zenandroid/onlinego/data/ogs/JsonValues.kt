package io.zenandroid.onlinego.data.ogs

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Conversions between [JsonElement] and the plain Kotlin values Moshi used to produce, kept in one
 * place because both [AnySerializer] and the websocket emit DSL depend on the exact mapping.
 *
 * Numbers decode to [Double], objects to [LinkedHashMap] and arrays to [ArrayList], because
 * callers cast to those types - see `Time.fromMap`, `Game`'s rating extraction and
 * `Message.fromChat`.
 */
internal fun JsonElement.toPlainValue(): Any? = when (this) {
  is JsonNull -> null
  is JsonPrimitive -> toPlainValue()
  is JsonObject -> entries.associateTo(LinkedHashMap()) { (k, v) -> k to v.toPlainValue() }
  is JsonArray -> mapTo(ArrayList()) { it.toPlainValue() }
}

private fun JsonPrimitive.toPlainValue(): Any {
  if (isString) return content
  content.toBooleanStrictOrNull()?.let { return it }
  content.toDoubleOrNull()?.let { return it }
  return content
}

internal fun jsonElementOf(value: Any?): JsonElement = when (value) {
  null -> JsonNull
  is JsonElement -> value
  is String -> JsonPrimitive(value)
  is Boolean -> JsonPrimitive(value)
  is Number -> JsonPrimitive(value)
  is Enum<*> -> JsonPrimitive(value.name)
  is Map<*, *> -> JsonObject(value.entries.associate { (k, v) -> k.toString() to jsonElementOf(v) })
  is Iterable<*> -> JsonArray(value.map(::jsonElementOf))
  is Array<*> -> JsonArray(value.map(::jsonElementOf))
  else -> JsonPrimitive(value.toString())
}
