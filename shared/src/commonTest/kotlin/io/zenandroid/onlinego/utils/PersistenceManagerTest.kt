package io.zenandroid.onlinego.utils

import io.zenandroid.onlinego.data.model.ogs.UIConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PersistenceManagerTest {

  private val store = InMemoryKeyValueStore()
  private val persistenceManager = PersistenceManager(store)

  @Test
  fun aStoredUIConfigReadsBackWithItsTimestamp() {
    val config = UIConfig(csrf_token = "the-token", chat_auth = "the-chat-auth")

    persistenceManager.storeUIConfig(config)

    assertEquals(config, persistenceManager.getUIConfig())
    assertTrue(persistenceManager.getUIConfigTimestamp() > 0)
  }

  @Test
  fun nothingStoredMeansNoUIConfigAndAZeroTimestamp() {
    assertNull(persistenceManager.getUIConfig())
    assertEquals(0, persistenceManager.getUIConfigTimestamp())
  }

  @Test
  fun thePuzzleRefreshTimeDefaultsToZeroAndPersists() {
    assertEquals(0, persistenceManager.puzzleCollectionLastRefresh)

    persistenceManager.puzzleCollectionLastRefresh = 1234

    assertEquals(1234, persistenceManager.puzzleCollectionLastRefresh)
  }

  @Test
  fun keysMatchWhatInstalledBuildsHaveStored() {
    persistenceManager.storeUIConfig(UIConfig())
    persistenceManager.puzzleCollectionLastRefresh = 1

    assertEquals(
      setOf("UICONFIG_KEY", "UICONFIG_TIMESTAMP_KEY", "PUZZLE_DIRECTORY_REFRESH"),
      store.values.keys,
    )
  }
}

private class InMemoryKeyValueStore : KeyValueStore {
  val values = mutableMapOf<String, Any>()

  override fun getString(key: String) = values[key] as String?

  override fun getLong(key: String, default: Long) = values[key] as Long? ?: default

  override fun putString(key: String, value: String) {
    values[key] = value
  }

  override fun putLong(key: String, value: Long) {
    values[key] = value
  }
}
