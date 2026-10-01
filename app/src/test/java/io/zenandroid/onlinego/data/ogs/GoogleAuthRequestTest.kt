package io.zenandroid.onlinego.data.ogs

import de.jensklingenberg.ktorfit.Ktorfit
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.zenandroid.onlinego.utils.appJson
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

private const val BASE_URL = "https://online-go.com/"

private const val EXPECTED_SCOPE =
  "email profile https://www.googleapis.com/auth/userinfo.email openid https://www.googleapis.com/auth/userinfo.profile"

class GoogleAuthRequestTest {

  private val requests = mutableListOf<HttpRequestData>()

  private fun api(location: String = "/"): OGSRestAPI {
    val engine = MockEngine { request ->
      requests += request
      respond(
        content = "",
        status = HttpStatusCode.Found,
        headers = headersOf(HttpHeaders.Location, location),
      )
    }
    return Ktorfit.Builder()
      .baseUrl(BASE_URL)
      .httpClient(HttpClient(engine) { configureOGSClient(appJson) })
      .build()
      .createOGSRestAPI()
  }

  @Test
  fun `initiateGoogleAuthFlow hits the login endpoint and does not follow the redirect`() =
    runTest {
      val response = api(location = "https://accounts.google.com/o/oauth2/auth?x=1&state=abc&y=2")
        .initiateGoogleAuthFlow()

      assertEquals(1, requests.size)
      assertEquals(
        "https://online-go.com/login/google-oauth2/",
        requests.single().url.toString()
      )
      assertEquals(302, response.status.value)
      assertEquals(
        "https://accounts.google.com/o/oauth2/auth?x=1&state=abc&y=2",
        response.headers["location"]
      )
    }

  @Test
  fun `loginWithGoogleAuth preserves the google scope and carries code and state`() = runTest {
    val response = api().loginWithGoogleAuth(code = "the-code", state = "the-state")

    assertEquals(1, requests.size)
    val url = requests.single().url
    assertEquals("/complete/google-oauth2/", url.encodedPath)
    assertEquals(EXPECTED_SCOPE, url.parameters["scope"])
    assertEquals("0", url.parameters["authuser"])
    assertEquals("none", url.parameters["prompt"])
    assertEquals("the-code", url.parameters["code"])
    assertEquals("the-state", url.parameters["state"])
    assertEquals(
      "code=the-code&state=the-state" +
          "&scope=email+profile" +
          "+https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fuserinfo.email+openid" +
          "+https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fuserinfo.profile" +
          "&authuser=0&prompt=none",
      url.encodedQuery
    )
    assertEquals(302, response.status.value)
    assertEquals("/", response.headers["location"])
  }

  @Test
  fun `the location header is found regardless of its casing`() = runTest {
    val engine = MockEngine {
      respond(
        content = "",
        status = HttpStatusCode.Found,
        headers = headersOf("Location", "/"),
      )
    }
    val response = Ktorfit.Builder()
      .baseUrl(BASE_URL)
      .httpClient(HttpClient(engine) { configureOGSClient(appJson) })
      .build()
      .createOGSRestAPI()
      .initiateGoogleAuthFlow()

    assertEquals("/", response.headers["location"])
  }
}
