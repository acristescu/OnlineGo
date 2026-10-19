package io.zenandroid.onlinego.utils

import platform.Foundation.NSNumber
import platform.Foundation.NSUserDefaults

class UserDefaultsKeyValueStore(suiteName: String) : KeyValueStore {
  private val defaults = NSUserDefaults(suiteName = suiteName)

  override fun getString(key: String): String? = defaults.stringForKey(key)

  override fun getLong(key: String, default: Long): Long =
    (defaults.objectForKey(key) as? NSNumber)?.longLongValue ?: default

  override fun putString(key: String, value: String) = defaults.setObject(value, key)

  override fun putLong(key: String, value: Long) =
    defaults.setObject(NSNumber(longLong = value), key)
}
