package io.zenandroid.onlinego.data.ogs

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.cookies.cookies
import io.ktor.client.plugins.cookies.get
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.zenandroid.onlinego.BuildConfig
import kotlinx.serialization.json.Json

private const val CSRF_HEADER = "x-csrftoken"

private val OGSCsrfHeader = createClientPlugin("OGSCsrfHeader") {
  val httpClient = client
  onRequest { request, _ ->
    httpClient.cookies(request.url.build())[CSRF_COOKIE]?.let {
      request.header(CSRF_HEADER, it.value)
    }
  }
}

fun HttpClientConfig<*>.configureOGSClient(
  jsonFormat: Json,
  cookiesStorage: CookiesStorage = AcceptAllCookiesStorage(),
  baseUrl: String = BuildConfig.BASE_URL,
) {
  expectSuccess = true
  followRedirects = false

  install(HttpCookies) {
    storage = cookiesStorage
  }

  install(OGSCsrfHeader)

  defaultRequest {
    contentType(ContentType.Application.Json)
    header(HttpHeaders.Referrer, "${baseUrl}overview")
  }

  install(ContentNegotiation) {
    json(jsonFormat)
  }

  HttpResponseValidator {
    validateResponse { response ->
      Logger.i(
        "${response.request.method.value} ${response.request.url} -> ${response.status.value}",
        tag = "HTTP_REQUEST"
      )
    }

    handleResponseExceptionWithRequest { cause, request ->
      val response =
        (cause as? ResponseException)?.response ?: return@handleResponseExceptionWithRequest
      val errorBody = runCatching { response.bodyAsText() }.getOrNull()
      Logger.e(
        "${request.method.value} ${request.url} -> ${response.status} $errorBody",
        tag = "HTTP_REQUEST"
      )
      throw OGSApiException(
        code = response.status.value,
        errorBody = errorBody,
        cause = cause,
      )
    }
  }
}
