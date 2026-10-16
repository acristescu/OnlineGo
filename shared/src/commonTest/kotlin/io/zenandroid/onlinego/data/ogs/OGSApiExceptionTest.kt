package io.zenandroid.onlinego.data.ogs

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.zenandroid.onlinego.utils.appJson
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OGSApiExceptionTest {

  @Test
  fun `reads code and body from an OGS exception`() {
    val t: Throwable = OGSApiException(403, "login failed")

    assertEquals(403, t.httpStatusCode)
    assertEquals("login failed", t.httpErrorBody)
  }

  @Test
  fun `reports nothing for a non-http failure`() {
    val t: Throwable = IllegalStateException("boom")

    assertNull(t.httpStatusCode)
    assertNull(t.httpErrorBody)
    assertNull(null.httpStatusCode)
  }

  @Test
  fun `ktor failures surface as OGSApiException carrying the body`() = runTest {
    val client = HttpClient(
      MockEngine {
        respond(
          content = """{"error":"nope"}""",
          status = HttpStatusCode.Unauthorized,
          headers = headersOf(),
        )
      }
    ) { configureOGSClient(appJson) }

    val thrown = runCatching {
      client.get("https://online-go.com/api/v1/me/warning")
    }.exceptionOrNull()

    assertTrue(thrown is OGSApiException, "expected OGSApiException but got $thrown")
    assertEquals(401, thrown.httpStatusCode)
    assertEquals("""{"error":"nope"}""", thrown.httpErrorBody)
  }
}
