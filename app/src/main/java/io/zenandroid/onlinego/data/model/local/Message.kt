package io.zenandroid.onlinego.data.model.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.util.Locale
import io.zenandroid.onlinego.data.model.ogs.AnalysisMessage
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
        val reviewUrl: String? = null,
        @Embedded(prefix = "variation_") val variation: AnalysisMessage? = null,
        val seen: Boolean = false
// TODO: PolymorphicJsonAdapterFactory
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

            var message = Message(
                    type = type,
                    gameId = gameId,
                    username = chat.line.username,
                    playerId = chat.line.player_id,
                    moveNumber = chat.line.move_number,
                    date = chat.line.date,
                    chatId = chat.line.chat_id ?: chat.chat_id!!,
                    text = ""
            ).run {
                when {
                    chat.line.body is String? -> {
                        copy(text = chat.line.body)
                    }
                    (chat.line.body as? Map<String, Any>) != null -> {
                        val content = chat.line.body as Map<String, Any>
                        when (content["type"] as? String) {
                            "analysis" -> content.let {
                                val name = it["name"] as? String
                                val from = (it["from"] as? Double)?.toInt()
                                val movestring = (it["moves"] as? String) ?: ""
                                val moves = movestring.lowercase().run {
                                    if (matches("\\w+".toRegex())) {
                                        chunked(2).map {
                                            Util.getCoordinatesFromSGF(it)
                                        }
                                    }
                                    else null
                                }
                                if (moves != null) {
                                    copy(
                                        text = "Variation \"${name ?: ""}\": ",
                                        variation = AnalysisMessage(
                                            name = name,
                                            from = from,
                                            moves = moves,
                                        ),
                                    )
                                }
                                else {
                                    copy(
                                        text = "Variation \"${name ?: ""}\" from move ${from ?: "?"}: ${movestring}",
                                    )
                                }
                            }
                            "review" -> content.let {
                                val reviewURL = "https://online-go.com/review/${it["review_id"] as? String}"
                                copy(
                                    text = "Review: ",
                                    reviewUrl = reviewURL,
                                )
                            }
                            "translated" -> content.let {
                                val bcp46 = Locale.getDefault().getLanguage()
                                val translated = it[bcp46.lowercase()] as? String
                                val original = (it["en"] as? String) ?: ""
                                copy(text = translated ?: original)
                            }
                            else -> (content["type"] as? String)?.capitalize()?.let {
                                copy(text = "${it} (unsupported)")
                            } ?: copy(text = "(Unsupported message)")
                        }
                    }
                    else -> copy(text = "(Unsupported message)")
                }
            }

            return message
        }
    }
}
