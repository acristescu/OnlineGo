package io.zenandroid.onlinego.data.model.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.zenandroid.onlinego.data.model.ogs.Chat
import io.zenandroid.onlinego.data.model.ogs.ChatChannel
import io.zenandroid.onlinego.gamelogic.Util

@Entity
data class Message (
        val type: Type,
        val gameId: Long?,
        val username: String,
        val playerId: Long,
        val moveNumber: Long?,
        val date: Long,
        @PrimaryKey val chatId: String,
        val text: String,
        val seen: Boolean = false

) {
    enum class Type {
        MAIN,
        MALKOVITCH,
        SPECTATOR,
        PERSONAL,
        DIRECT
    }

    companion object {
        fun fromOGSMessage(chat: Chat, gameId: Long?, gameSize: Int?): Message {
            val type = when(chat.channel) {
                ChatChannel.MAIN -> Type.MAIN
                ChatChannel.SPECTATOR -> Type.SPECTATOR
                ChatChannel.MALKOVICH -> Type.MALKOVITCH
                ChatChannel.PERSONAL -> Type.PERSONAL
            }

            val text = chat.line.body as? String
                ?: (chat.line.body as? Map<String, Any>)?.let {
                    val type = it["type"] as? String
                    if(type == "analysis") {
                        val name = it["name"] as? String
                        val from = (it["from"] as? Double)?.toLong()
                        val moves = (it["moves"] as? String)?.let {
                            if (it.matches("\\w+".toRegex())) {
                                it.lowercase().chunked(2).map {
                                    val point = Util.getCoordinatesFromSGF(it)
                                    Util.getGTPCoordinates(point, gameSize!!)
                                }.joinToString(" ")
                            } else it
                        }
                        "Variation \"${name ?: ""}\" from move ${from ?: "?"}: ${moves}"
                    } else type?.capitalize()?.let { "${it} (unsupported)" }
                } ?: "(Unsupported message)"
            return Message(
                    type = type,
                    gameId = gameId,
                    username = chat.line.username,
                    playerId = chat.line.player_id,
                    moveNumber = chat.line.move_number,
                    date = chat.line.date,
                    chatId = chat.line.chat_id ?: chat.chat_id!!,
                    text = text
            )
        }
    }
}
