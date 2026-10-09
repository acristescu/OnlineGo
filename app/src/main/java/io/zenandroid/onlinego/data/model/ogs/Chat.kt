@file:UseSerializers(AnySerializer::class, LenientLongSerializer::class)

package io.zenandroid.onlinego.data.model.ogs

import io.zenandroid.onlinego.data.ogs.AnySerializer
import io.zenandroid.onlinego.data.ogs.LenientLongSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers


@Serializable
data class Chat (
    val channel: ChatChannel,
    val line: ChatLine,
    val game_id: Long? = null,
    val chat_id: String? = null
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
        val ratings: OGSPlayer.Ratings? = null,
        val player_id: Long,
        val move_number: Long? = null,
        val date: Long,
        val chat_id: String? = null,
        val body: Any
)