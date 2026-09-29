@file:UseSerializers(AnySerializer::class, LenientLongSerializer::class)

package io.zenandroid.onlinego.data.model.ogs

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import io.zenandroid.onlinego.data.ogs.AnySerializer
import kotlinx.serialization.UseSerializers
import io.zenandroid.onlinego.data.ogs.LenientLongSerializer


@Serializable
data class Chat (
    val channel: ChatChannel,
    val line: ChatLine,
    val game_id: Long?,
    val chat_id: String?
    )

@Serializable
enum class ChatChannel {
  @SerialName("main")
  MAIN,
  @SerialName("malkovich")
  MALKOVICH,
  @SerialName("spectator")
  SPECTATOR,
  @SerialName("personal")
  PERSONAL,
}

@Serializable
data class ChatLine (
        val username: String,
        val ratings: OGSPlayer.Ratings?,
        val player_id: Long,
        val move_number: Long?,
        val date: Long,
        val chat_id: String?,
        val body: Any
)