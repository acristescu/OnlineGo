package io.zenandroid.onlinego.utils

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WhatsNewUtilsTest {

  @Test
  fun showsOnlyWhenAStoredChangelogDiffersFromTheCurrentOne() = runTest {
    val dataStore = PreferenceDataStoreFactory.createWithPath(scope = backgroundScope) {
      FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "whats-new-${
        Random.nextLong().toULong()
      }.preferences_pb"
    }
    val whatsNew = WhatsNewUtils(dataStore)

    assertFalse(whatsNew.shouldDisplayDialog(), "fresh install")
    dataStore.edit {
      it[stringPreferencesKey("WHATS_NEW")] = changelogHash(listOf("an older release"))
    }
    assertTrue(whatsNew.shouldDisplayDialog(), "after an update")
    whatsNew.textShown()
    assertFalse(whatsNew.shouldDisplayDialog(), "once shown")
  }
}
