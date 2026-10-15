package io.zenandroid.onlinego.data.ogs

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.zenandroid.onlinego.data.model.ogs.CreateAccountRequest
import io.zenandroid.onlinego.utils.appJson
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test

private const val BASE_URL = "https://online-go.com/"

private class RecordingPersistence : SessionCookiePersistence {
  private var cookies: Map<String, StoredCookie> = emptyMap()
  override fun load() = cookies
  override fun save(cookies: Map<String, StoredCookie>) {
    this.cookies = cookies
  }
}

class OGSEngineBridgeTest {

  private val sent = mutableListOf<Request>()

  private val okHttpClient = OkHttpClient.Builder()
    .addInterceptor { chain ->
      sent += chain.request()
      Response.Builder()
        .request(chain.request())
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .addHeader("Set-Cookie", "sessionid=the-session; Max-Age=157800000; Path=/; HttpOnly")
        .addHeader("Set-Cookie", "csrftoken=the-token; Max-Age=31449600; Path=/")
        .body("{}".toResponseBody("application/json".toMediaType()))
        .build()
    }
    .build()

  private val store = OGSCookieStore(RecordingPersistence(), "online-go.com")

  private val client = HttpClient(OkHttp) {
    configureOGSClient(appJson, store, BASE_URL)
    engine { preconfigured = okHttpClient }
  }

  @Test
  fun `a login hands the session to every request that follows it`() = runBlocking {
    client.post("${BASE_URL}api/v0/login") {
      setBody(CreateAccountRequest("user", "password", "", "ebi"))
    }

    coroutineScope {
      launch { client.get("${BASE_URL}api/v1/ui/overview") }
      launch { client.get("${BASE_URL}api/v1/me/challenges?page_size=100") }
    }

    assertEquals(3, sent.size)
    sent.drop(1).forEach { request ->
      assertEquals(
        setOf("sessionid=the-session", "csrftoken=the-token"),
        request.header("Cookie")?.split("; ")?.toSet()
      )
      assertEquals("the-token", request.header("x-csrftoken"))
    }
  }

  @Test
  fun `the referer survives the bridge down to the engine`() = runBlocking {
    client.get("${BASE_URL}api/v1/ui/overview")

    assertEquals("${BASE_URL}overview", sent.single().header("Referer"))
  }
}
