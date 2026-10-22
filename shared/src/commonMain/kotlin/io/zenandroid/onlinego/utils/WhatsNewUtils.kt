package io.zenandroid.onlinego.utils

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.zenandroid.onlinego.resources.Res
import io.zenandroid.onlinego.resources.whats_new_changelog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okio.ByteString.Companion.encodeUtf8
import org.jetbrains.compose.resources.getStringArray

private const val WHATS_NEW = "WHATS_NEW"
private val WHATS_NEW_KEY = stringPreferencesKey(WHATS_NEW)

class WhatsNewUtils(private val dataStore: DataStore<Preferences>) {
  suspend fun shouldDisplayDialog(): Boolean {
    return withContext(Dispatchers.IO) {
      val hash = currentHash()
      val stored = dataStore.data.map { it[WHATS_NEW_KEY] }.first()
      stored != null && stored != hash
    }
  }

  suspend fun textShown() {
    withContext(Dispatchers.IO) {
      val hash = currentHash()
      dataStore.edit { prefs ->
        prefs[WHATS_NEW_KEY] = hash
      }
    }
  }

  /**
   * Hashes the changelog alone, deliberately not the whole sheet text: the surrounding labels are
   * translated, so hashing them would make the hash change with the app language and the sheet
   * would pop up again after every language switch. The changelog is translatable="false", so this
   * hash is the same in every locale and only changes when the release notes actually change.
   */
  private suspend fun currentHash() = changelogHash(whatsNewItems())
}

internal fun changelogHash(items: List<String>): String =
  items.joinToString("\n").encodeUtf8().md5().hex()

/**
 * The release notes, one entry per bullet, from the `whats_new_changelog` string-array.
 *
 * Editing that array is what makes the sheet appear again after an update, so it is the one thing
 * to remember to update per release. It is `translatable="false"` and stays English on purpose:
 * release notes churn every version and translating them is not sustainable. Write one item per
 * bullet without a bullet glyph — the UI draws those.
 */
suspend fun whatsNewItems(): List<String> = getStringArray(Res.array.whats_new_changelog)
