package io.zenandroid.onlinego.data.ogs

import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun HttpClientConfig<*>.configureOGSClient(jsonFormat: Json) {
  expectSuccess = true
  followRedirects = false

  defaultRequest {
    contentType(ContentType.Application.Json)
  }

  install(ContentNegotiation) {
    json(jsonFormat)
  }

  HttpResponseValidator {
    handleResponseExceptionWithRequest { cause, _ ->
      val response =
        (cause as? ResponseException)?.response ?: return@handleResponseExceptionWithRequest
      throw OGSApiException(
        code = response.status.value,
        errorBody = runCatching { response.bodyAsText() }.getOrNull(),
        cause = cause,
      )
    }
  }
}
