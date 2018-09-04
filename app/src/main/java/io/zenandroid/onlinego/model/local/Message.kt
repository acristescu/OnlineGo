package io.zenandroid.onlinego.model.local

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
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
        val text: String

) {
    enum class Type {
        MAIN,
        MALKOVITCH,
        DIRECT
    }

    companion object {
        fun fromOGSMessage(chat: Chat, gameId: Long?): Message {
            val type = if(chat.channel == ChatChannel.MAIN) Type.MAIN else Type.MALKOVITCH
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
