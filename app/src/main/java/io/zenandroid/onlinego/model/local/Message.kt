package io.zenandroid.onlinego.model.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.zenandroid.onlinego.model.ogs.Chat
import io.zenandroid.onlinego.model.ogs.ChatChannel

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
        DIRECT
    }

    companion object {
        fun fromOGSMessage(chat: Chat, gameId: Long?): Message {
            val type = when(chat.channel) {
                ChatChannel.MAIN -> Type.MAIN
                ChatChannel.SPECTATOR -> Type.SPECTATOR
                ChatChannel.MALKOVICH -> Type.MALKOVITCH
            }

            val text = chat.line.body as? String ?: "Variation (unsupported)"
            return Message(
                    type = type,
                    gameId = gameId,
                    username = chat.line.username,
                    playerId = chat.line.player_id,
                    moveNumber = chat.line.move_number,
                    date = chat.line.date,
                    chatId = chat.line.chat_id,
                    text = text
            )
        }
    }
}
