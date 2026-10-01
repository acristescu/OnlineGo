package io.zenandroid.onlinego.data.ogs

class OGSApiException(
  val code: Int,
  val errorBody: String?,
  cause: Throwable? = null,
) : Exception("HTTP $code", cause)

val Throwable?.httpStatusCode: Int?
  get() = when (this) {
    is OGSApiException -> code
    else -> null
  }

val Throwable?.httpErrorBody: String?
  get() = when (this) {
    is OGSApiException -> errorBody
    else -> null
  }
