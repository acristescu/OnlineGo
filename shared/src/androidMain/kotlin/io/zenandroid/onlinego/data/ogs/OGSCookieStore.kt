package io.zenandroid.onlinego.data.ogs

import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.http.Cookie
import io.ktor.http.CookieEncoding
import io.ktor.http.Url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

internal const val CSRF_COOKIE = "csrftoken"
internal const val SESSION_COOKIE = "sessionid"

internal val KEPT_COOKIES = setOf(CSRF_COOKIE, SESSION_COOKIE)

@Serializable
data class StoredCookie(val value: String, val expiresAt: Long)

interface SessionCookiePersistence {
  fun load(): Map<String, StoredCookie>
  fun save(cookies: Map<String, StoredCookie>)
}

class OGSCookieStore(
  private val persistence: SessionCookiePersistence,
  private val host: String,
  private val now: () -> Long = System::currentTimeMillis,
) : CookiesStorage {

  private val mutex = Mutex()

  private val loaded = lazy { persistence.load() }

  @Volatile
  private var updated: Map<String, StoredCookie>? = null

  private val cookies: Map<String, StoredCookie>
    get() = updated ?: loaded.value

  val csrfToken: String?
    get() = unexpired()[CSRF_COOKIE]?.value

  val sessionId: String?
    get() = unexpired()[SESSION_COOKIE]?.value

  override suspend fun get(requestUrl: Url): List<Cookie> {
    if (requestUrl.host != host) return emptyList()
    ensureLoaded()
    return unexpired().map { (name, cookie) ->
      Cookie(name, cookie.value, encoding = CookieEncoding.RAW)
    }
  }

  override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
    if (requestUrl.host != host || cookie.name !in KEPT_COOKIES) return
    val expiresAt = cookie.expiresAt(now()) ?: return
    ensureLoaded()
    mutex.withLock {
      val next = unexpired() + (cookie.name to StoredCookie(cookie.value, expiresAt))
      updated = next
      withContext(Dispatchers.IO) { persistence.save(next) }
    }
  }

  override fun close() {}

  private suspend fun ensureLoaded() {
    if (!loaded.isInitialized()) withContext(Dispatchers.IO) { loaded.value }
  }

  private fun unexpired() = cookies.filterValues { it.expiresAt > now() }
}

private fun Cookie.expiresAt(now: Long): Long? =
  maxAge?.let { now + it * 1000L } ?: expires?.timestamp
