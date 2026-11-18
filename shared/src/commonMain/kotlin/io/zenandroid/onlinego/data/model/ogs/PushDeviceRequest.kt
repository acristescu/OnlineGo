package io.zenandroid.onlinego.data.model.ogs

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PushDeviceRequest(
  @SerialName("client_id") val clientId: String,
  val provider: String,
  val token: String,
  @SerialName("fcm_message_type") val fcmMessageType: String,
)
