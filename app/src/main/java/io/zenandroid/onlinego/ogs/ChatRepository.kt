package io.zenandroid.onlinego.ogs

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.db.GameDao
import io.zenandroid.onlinego.model.local.Message

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