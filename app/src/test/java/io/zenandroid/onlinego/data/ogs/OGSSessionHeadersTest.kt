package io.zenandroid.onlinego.data.ogs

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.zenandroid.onlinego.utils.appJson
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private const val BASE_URL = "https://online-go.com/"
private const val HOST = "online-go.com"
private const val NOW = 1_700_000_000_000L

private class FixedPersistence(private val cookies: Map<String, StoredCookie>) :
  SessionCookiePersistence {
  override fun load() = cookies
  override fun save(cookies: Map<String, StoredCookie>) {}
}

class OGSSessionHeadersTest {

  private val requests = mutableListOf<HttpRequestData>()

  private fun store(vararg cookies: Pair<String, String>) = OGSCookieStore(
    FixedPersistence(cookies.associate { (name, value) ->
      name to StoredCookie(value, NOW + 60_000L)
    }),
    HOST,
  ) { NOW }

  private fun client(store: OGSCookieStore, setCookies: List<String> = emptyList()) =
    HttpClient(
      MockEngine { request ->
        requests += request
        respond(
          content = "{}",
          status = HttpStatusCode.OK,
          headers = headersOf(
            HttpHeaders.ContentType to listOf("application/json"),
            HttpHeaders.SetCookie to setCookies,
          ),
        )
      }
    ) { configureOGSClient(appJson, store, BASE_URL) }

  @Test
  fun `the session is sent as a cookie and the csrf token also as a header`() = runTest {
    client(store(SESSION_COOKIE to "the-session", CSRF_COOKIE to "the-token"))
      .get("${BASE_URL}api/v1/ui/overview")

    val headers = requests.single().headers
    val cookie = headers[HttpHeaders.Cookie]!!
    assertEquals(
      setOf("sessionid=the-session", "csrftoken=the-token"),
      cookie.split("; ").toSet()
    )
    assertEquals("the-token", headers["x-csrftoken"])
    assertEquals("${BASE_URL}overview", headers[HttpHeaders.Referrer])
  }

  @Test
  fun `no session is leaked to another host`() = runTest {
    client(store(SESSION_COOKIE to "the-session", CSRF_COOKIE to "the-token"))
      .get("https://cdn.example.com/avatar.png")

    val headers = requests.single().headers
    assertNull(headers[HttpHeaders.Cookie])
    assertNull(headers["x-csrftoken"])
  }

  @Test
  fun `cookies set by the server are captured by the store`() = runTest {
    val store = store()
    client(
      store,
      setCookies = listOf(
        "sessionid=fresh-session; expires=Wed, 01 Oct 2031 22:44:17 GMT; HttpOnly; Max-Age=157800000; Path=/; SameSite=Lax",
        "csrftoken=fresh-token; expires=Thu, 30 Sep 2027 13:24:17 GMT; Max-Age=31449600; Path=/; SameSite=Lax",
      ),
    ).get("${BASE_URL}api/v1/ui/config/")

    assertEquals("fresh-session", store.sessionId)
    assertEquals("fresh-token", store.csrfToken)
  }
}
