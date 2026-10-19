package io.zenandroid.onlinego.utils

import io.zenandroid.onlinego.data.ogs.KeyValueCookiePersistence
import io.zenandroid.onlinego.data.ogs.StoredCookie
import platform.Foundation.NSUserDefaults
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UserDefaultsKeyValueStoreTest {

  private val suiteName = "test-${Random.nextLong().toULong()}"
  private val store = UserDefaultsKeyValueStore(suiteName)

  @AfterTest
  fun tearDown() {
    NSUserDefaults.standardUserDefaults.removePersistentDomainForName(suiteName)
  }

  @Test
  fun stringsAndLongsRoundTrip() {
    store.putString("name", "value")
    store.putLong("number", 1_700_000_000_123)

    assertEquals("value", UserDefaultsKeyValueStore(suiteName).getString("name"))
    assertEquals(1_700_000_000_123, UserDefaultsKeyValueStore(suiteName).getLong("number", 0))
  }

  @Test
  fun missingKeysReturnTheDefault() {
    assertNull(store.getString("missing"))
    assertEquals(-1, store.getLong("missing", -1))
  }

  @Test
  fun cookiesRoundTripAndASaveReplacesTheSet() {
    val persistence = KeyValueCookiePersistence(store)
    persistence.save(
      mapOf(
        "csrftoken" to StoredCookie("csrf", 1_000),
        "sessionid" to StoredCookie("session", 2_000),
      )
    )
    persistence.save(mapOf("sessionid" to StoredCookie("new-session", 3_000)))

    assertEquals(
      mapOf("sessionid" to StoredCookie("new-session", 3_000)),
      KeyValueCookiePersistence(UserDefaultsKeyValueStore(suiteName)).load(),
    )
  }

  @Test
  fun noStoredCookiesLoadsEmpty() {
    assertEquals(emptyMap(), KeyValueCookiePersistence(store).load())
  }
}
