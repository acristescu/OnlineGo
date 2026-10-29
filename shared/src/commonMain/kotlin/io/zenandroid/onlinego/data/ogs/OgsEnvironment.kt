package io.zenandroid.onlinego.data.ogs

enum class OgsEnvironment(val baseUrl: String, val websocketHost: String) {
  PRODUCTION(baseUrl = "https://online-go.com/", websocketHost = "wsp.online-go.com"),
  BETA(baseUrl = "https://beta.online-go.com/", websocketHost = "beta.online-go.com"),
}

val OGS_ENVIRONMENT = OgsEnvironment.PRODUCTION
