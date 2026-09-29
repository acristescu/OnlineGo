package io.zenandroid.onlinego.utils

import kotlinx.serialization.json.Json

/**
 * Every flag here reproduces a behaviour of the reflective Moshi setup this replaced, because OGS
 * payloads have drifted over the years and old games still return shapes newer ones do not.
 *
 * [Json.ignoreUnknownKeys] - OGS sends fields we do not model.
 * [Json.explicitNulls] - an absent key decodes to null even where the property has no default, and
 * nulls are omitted when encoding. Both match Moshi, and the first is what keeps the ~130
 * nullable-without-default properties working without touching them.
 * [Json.encodeDefaults] - Moshi wrote every non-null field regardless of its default.
 * [Json.isLenient] - Moshi's reader accepted quoted numbers; `tutorials.json` relies on it, and so
 * do older OGS responses.
 */
val appJson: Json = Json {
  ignoreUnknownKeys = true
  explicitNulls = false
  encodeDefaults = true
  isLenient = true
}
