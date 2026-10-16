package io.zenandroid.onlinego.utils

import android.content.Context
import androidx.core.content.edit

class SharedPreferencesKeyValueStore(context: Context, name: String) : KeyValueStore {
  private val prefs by lazy { context.getSharedPreferences(name, Context.MODE_PRIVATE) }

  override fun getString(key: String): String? = prefs.getString(key, null)

  override fun getLong(key: String, default: Long): Long = prefs.getLong(key, default)

  override fun putString(key: String, value: String) = prefs.edit { putString(key, value) }

  override fun putLong(key: String, value: Long) = prefs.edit { putLong(key, value) }
}
