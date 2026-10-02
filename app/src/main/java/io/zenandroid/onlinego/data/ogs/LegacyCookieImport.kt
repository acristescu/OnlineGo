package io.zenandroid.onlinego.data.ogs

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.ObjectInputStream
import java.io.ObjectStreamClass
import java.io.Serializable

private const val LEGACY_COOKIE_CLASS =
  "com.franmontiel.persistentcookiejar.persistence.SerializableCookie"

private const val NON_PERSISTENT = -1L

internal fun importLegacyCookies(serializedCookies: Collection<String>): Map<String, StoredCookie> =
  serializedCookies
    .mapNotNull(::decodeLegacyCookie)
    .filter { it.name in KEPT_COOKIES && it.expiresAt != NON_PERSISTENT }
    .associate { it.name to StoredCookie(it.value, it.expiresAt) }

private fun decodeLegacyCookie(serialized: String): SerializableCookie? = runCatching {
  LegacyCookieInputStream(ByteArrayInputStream(serialized.decodeHex())).use {
    it.readObject() as SerializableCookie
  }
}.getOrNull()

private fun String.decodeHex(): ByteArray =
  ByteArray(length / 2) { i ->
    ((this[i * 2].digitToInt(16) shl 4) or this[i * 2 + 1].digitToInt(16)).toByte()
  }

private class LegacyCookieInputStream(input: InputStream) : ObjectInputStream(input) {
  override fun resolveClass(desc: ObjectStreamClass): Class<*> =
    if (desc.name == LEGACY_COOKIE_CLASS) SerializableCookie::class.java
    else super.resolveClass(desc)
}

private class SerializableCookie : Serializable {

  @Transient
  var name: String = ""

  @Transient
  var value: String = ""

  @Transient
  var expiresAt: Long = NON_PERSISTENT

  private fun readObject(input: ObjectInputStream) {
    name = input.readObject() as String
    value = input.readObject() as String
    expiresAt = input.readLong()
    input.readObject()
    input.readObject()
    input.readBoolean()
    input.readBoolean()
    input.readBoolean()
  }

  companion object {
    private const val serialVersionUID: Long = -8594045714036645534L
  }
}
