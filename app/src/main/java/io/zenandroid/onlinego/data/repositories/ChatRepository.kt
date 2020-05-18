package io.zenandroid.onlinego.data.repositories

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.data.db.GameDao
import io.zenandroid.onlinego.data.model.local.Message

class ChatRepository(private val gameDao: GameDao) {
    fun addMessage(message: Message) {
        gameDao.insertMessage(message)
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