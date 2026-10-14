package io.zenandroid.onlinego.data.ogs

import de.jensklingenberg.ktorfit.Ktorfit
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.content.TextContent
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.zenandroid.onlinego.data.model.ogs.AcknowledgeWarningRequest
import io.zenandroid.onlinego.utils.appJson
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AcknowledgeWarningBodyTest {

  @Test
  fun `serializes to the object the OGS web client sends`() {
    assertEquals(
      """{"accept":true}""",
      appJson.encodeToString(AcknowledgeWarningRequest.serializer(), AcknowledgeWarningRequest())
    )
  }

  @Test
  fun `goes on the wire as a json object, not a json string`() = runTest {
    lateinit var sent: HttpRequestData
    val engine = MockEngine { request ->
      sent = request
      respond(
        content = """{"id":23,"created":null,"player_id":null,"moderator":null,"text":null,""" +
            """"message_id":null,"severity":null,"interpolation_data":null}""",
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
      )
    }

    Ktorfit.Builder()
      .baseUrl("https://online-go.com/")
      .httpClient(HttpClient(engine) { configureOGSClient(appJson) })
      .build()
      .createOGSRestAPI()
      .acknowledgeWarning(23, AcknowledgeWarningRequest())

    assertEquals(HttpMethod.Patch, sent.method)
    assertEquals("/api/v1/me/warning/23", sent.url.encodedPath)
    assertEquals(
      ContentType.Application.Json,
      (sent.body as TextContent).contentType.withoutParameters()
    )
    assertEquals("""{"accept":true}""", (sent.body as TextContent).text)
  }
}
