package io.zenandroid.onlinego.data.ogs

import de.jensklingenberg.ktorfit.Ktorfit
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.content.TextContent
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.zenandroid.onlinego.data.model.ogs.PushDeviceRequest
import io.zenandroid.onlinego.utils.appJson
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PushDevicesApiTest {

  private lateinit var sent: HttpRequestData

  private val api = Ktorfit.Builder()
    .baseUrl("https://beta.online-go.com/")
    .httpClient(HttpClient(MockEngine { request ->
      sent = request
      respond(content = "", status = HttpStatusCode.OK)
    }) { configureOGSClient(appJson) })
    .build()
    .createOGSRestAPI()

  @Test
  fun `registers a device with the fields OGS documents`() = runTest {
    api.registerPushDevice(
      PushDeviceRequest(
        clientId = "client",
        provider = "fcm",
        token = "token",
        fcmMessageType = "data",
      )
    )

    assertEquals(HttpMethod.Post, sent.method)
    assertEquals("/api/v1/me/push_devices", sent.url.encodedPath)
    assertEquals(
      """{"client_id":"client","provider":"fcm","token":"token","fcm_message_type":"data"}""",
      (sent.body as TextContent).text
    )
  }

  @Test
  fun `unregisters a device by its token`() = runTest {
    api.unregisterPushDevice("token")

    assertEquals(HttpMethod.Delete, sent.method)
    assertEquals("/api/v1/me/push_devices", sent.url.encodedPath)
    assertEquals("token", sent.url.parameters["token"])
  }
}
