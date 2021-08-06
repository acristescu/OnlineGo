package io.zenandroid.onlinego.data.repositories

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.data.db.GameDao
import io.zenandroid.onlinego.data.model.local.Message
import io.zenandroid.onlinego.gamelogic.Util.getCurrentUserId

class ChatRepository(private val gameDao: GameDao) {
    fun addMessage(message: Message) {
        gameDao.insertMessage(message)
        if(message.playerId == getCurrentUserId() && message.gameId != null) {
            gameDao.markGameMessagesAsReadUpTo(message.gameId, message.date)
        }
    }

    fun monitorGameChat(gameId: Long): Flowable<List<Message>> =
        gameDao.getMessagesForGame(gameId)

    fun markMessagesAsRead(messages: List<Message>) {
        Completable
                .create { gameDao.markMessagesAsRead(messages.map { it.chatId }) }
                .subscribeOn(Schedulers.io())
                .subscribe()
    }
}