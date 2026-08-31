package io.zenandroid.onlinego.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.zenandroid.onlinego.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.security.MessageDigest

private const val WHATS_NEW = "WHATS_NEW"
private val WHATS_NEW_KEY = stringPreferencesKey(WHATS_NEW)
val Context.whatsNewDataStore by preferencesDataStore(name = "whats_new")

object WhatsNewUtils {
  suspend fun shouldDisplayDialog(context: Context): Boolean {
    return withContext(Dispatchers.IO) {
      val hash = currentHash(context)
      val stored = context.whatsNewDataStore.data.map { it[WHATS_NEW_KEY] }.first()
      stored != null && stored != hash
    }
  }

  suspend fun textShown(context: Context) {
    withContext(Dispatchers.IO) {
      val hash = currentHash(context)
      context.whatsNewDataStore.edit { prefs ->
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
  private fun currentHash(context: Context) =
    hashString(whatsNewItems(context).joinToString("\n"))

  private fun hashString(text: String): String {
    return MessageDigest.getInstance("MD5").digest(text.toByteArray(Charsets.UTF_8))
      .fold("", { str, it -> str + "%02x".format(it) })
  }
}

/**
 * The release notes, one entry per bullet, from the `whats_new_changelog` string-array.
 *
 * Editing that array is what makes the sheet appear again after an update, so it is the one thing
 * to remember to update per release. It is `translatable="false"` and stays English on purpose:
 * release notes churn every version and translating them is not sustainable. Write one item per
 * bullet without a bullet glyph — the UI draws those.
 */
fun whatsNewItems(context: Context): List<String> =
  context.resources.getStringArray(R.array.whats_new_changelog).toList()
