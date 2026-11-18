package io.zenandroid.onlinego.data.ogs

enum class OgsEnvironment(
  val baseUrl: String,
  val websocketHost: String,
  val pushClientId: String,
) {
  PRODUCTION(
    baseUrl = "https://online-go.com/",
    websocketHost = "wsp.online-go.com",
    pushClientId = "kmHPHGhVS1wYCIVhFQtcuzmiDbegEhq3AT3W1xCs",
  ),
  BETA(
    baseUrl = "https://beta.online-go.com/",
    websocketHost = "beta.online-go.com",
    pushClientId = "13PJMNu1QjWIHpwDt0lDlSFRajwycrj0iFYaHfmk",
  ),
}

val OGS_ENVIRONMENT = OgsEnvironment.PRODUCTION
