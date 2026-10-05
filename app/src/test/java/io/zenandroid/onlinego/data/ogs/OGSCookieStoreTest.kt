package io.zenandroid.onlinego.data.ogs

import io.ktor.http.Cookie
import io.ktor.http.Url
import io.ktor.util.date.GMTDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val HOST = "online-go.com"
private val OGS_URL = Url("https://online-go.com/api/v1/ui/config/")
private val OTHER_URL = Url("https://cdn.example.com/avatar.png")
private const val NOW = 1_700_000_000_000L

private class InMemoryPersistence(initial: Map<String, StoredCookie> = emptyMap()) :
  SessionCookiePersistence {

  var saved: Map<String, StoredCookie> = initial
    private set

  var loads = 0
    private set

  override fun load() = saved.also { loads++ }

  override fun save(cookies: Map<String, StoredCookie>) {
    saved = cookies
  }
}

class OGSCookieStoreTest {

  private val persistence = InMemoryPersistence()
  private var clock = NOW
  private val store = OGSCookieStore(persistence, HOST) { clock }

  @Test
  fun `persisted cookies are not read until first use`() = runTest {
    val persistence =
      InMemoryPersistence(mapOf(SESSION_COOKIE to StoredCookie("abc", NOW + 60_000L)))
    val store = OGSCookieStore(persistence, HOST) { clock }

    assertEquals(0, persistence.loads)
    assertEquals(listOf("abc"), store.get(OGS_URL).map { it.value })
    assertEquals(1, persistence.loads)
  }

  @Test
  fun `max-age sets the expiry and the cookie is written through to persistence`() = runTest {
    store.addCookie(OGS_URL, Cookie(SESSION_COOKIE, "abc", maxAge = 60))

    assertEquals("abc", store.sessionId)
    assertEquals(
      mapOf(SESSION_COOKIE to StoredCookie("abc", NOW + 60_000L)),
      persistence.saved
    )
  }

  @Test
  fun `expires is used when there is no max-age`() = runTest {
    store.addCookie(OGS_URL, Cookie(SESSION_COOKIE, "abc", expires = GMTDate(NOW + 5_000L)))

    assertEquals(NOW + 5_000L, persistence.saved[SESSION_COOKIE]?.expiresAt)
  }

  @Test
  fun `a cookie with no expiry at all is dropped`() = runTest {
    store.addCookie(OGS_URL, Cookie(SESSION_COOKIE, "abc"))

    assertNull(store.sessionId)
    assertTrue(persistence.saved.isEmpty())
  }

  @Test
  fun `cookies outside the allowlist are dropped`() = runTest {
    store.addCookie(OGS_URL, Cookie("tracking", "abc", maxAge = 60))

    assertTrue(store.get(OGS_URL).isEmpty())
    assertTrue(persistence.saved.isEmpty())
  }

  @Test
  fun `cookies from another host are neither stored nor sent`() = runTest {
    store.addCookie(OTHER_URL, Cookie(SESSION_COOKIE, "abc", maxAge = 60))
    assertTrue(persistence.saved.isEmpty())

    store.addCookie(OGS_URL, Cookie(SESSION_COOKIE, "abc", maxAge = 60))
    assertTrue(store.get(OTHER_URL).isEmpty())
  }

  @Test
  fun `expired cookies are not returned`() = runTest {
    store.addCookie(OGS_URL, Cookie(SESSION_COOKIE, "abc", maxAge = 60))
    clock = NOW + 60_001L

    assertNull(store.sessionId)
    assertTrue(store.get(OGS_URL).isEmpty())
  }

  @Test
  fun `a re-issued token replaces the previous one`() = runTest {
    store.addCookie(OGS_URL, Cookie(CSRF_COOKIE, "old", maxAge = 60))
    store.addCookie(OGS_URL, Cookie(CSRF_COOKIE, "new", maxAge = 60))

    assertEquals("new", store.csrfToken)
    assertEquals(1, store.get(OGS_URL).size)
  }

  @Test
  fun `an existing session is available before any request is made`() = runTest {
    val restored = OGSCookieStore(
      InMemoryPersistence(
        mapOf(
          SESSION_COOKIE to StoredCookie("abc", NOW + 60_000L),
          CSRF_COOKIE to StoredCookie("def", NOW + 60_000L),
        )
      ),
      HOST,
    ) { clock }

    assertEquals("abc", restored.sessionId)
    assertEquals("def", restored.csrfToken)
    assertEquals(2, restored.get(OGS_URL).size)
  }
}
