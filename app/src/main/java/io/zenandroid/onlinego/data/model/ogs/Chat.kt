package io.zenandroid.onlinego.data.model.ogs

import com.squareup.moshi.Json

data class Chat (
    val channel: ChatChannel,
    val line: ChatLine
)

enum class ChatChannel {
    @Json(name = "main") MAIN,
    @Json(name = "malkovich") MALKOVICH,
    @Json(name = "spectator") SPECTATOR
}

data class ChatLine (
        val username: String,
        val ratings: OGSPlayer.Ratings,
        val player_id: Long,
        val move_number: Long?,
        val date: Long,
        val chat_id: String,
        val body: Any
)