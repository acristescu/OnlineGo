package io.zenandroid.onlinego.ogs

import io.reactivex.Flowable
import io.zenandroid.onlinego.db.GameDao
import io.zenandroid.onlinego.model.local.Message

class ChatRepository(private val gameDao: GameDao) {
    fun addMessage(message: Message) {
        gameDao.insertMessage(message)
    }

    fun monitorGameChat(gameId: Long): Flowable<List<Message>> =
        gameDao.getMessagesForGame(gameId)
}