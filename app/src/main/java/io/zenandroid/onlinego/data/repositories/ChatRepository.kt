package io.zenandroid.onlinego.data.repositories

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.data.db.GameDao
import io.zenandroid.onlinego.data.model.local.Message
import io.zenandroid.onlinego.gamelogic.Util.getCurrentUserId

class ChatRepository(private val gameDao: GameDao) {

    private val knownMessageIds = mutableSetOf<String>()

    init {
        gameDao.getAllMessageIDs()
            .subscribeOn(Schedulers.io())
            .subscribe ( {knownMessageIds.addAll(it) }, {} )
    }

    fun addMessage(message: Message) {
        if(!knownMessageIds.contains(message.chatId)) {
            gameDao.insertMessage(message)
            if (message.playerId == getCurrentUserId() && message.gameId != null) {
                gameDao.markGameMessagesAsReadUpTo(message.gameId, message.date)
            }
            knownMessageIds.add(message.chatId)
        }
    }

    fun monitorGameChat(gameId: Long): Flowable<List<Message>> =
        gameDao.getMessagesForGame(gameId)
            .doOnNext { it.forEach { knownMessageIds.add(it.chatId) } }

    fun markMessagesAsRead(messages: List<Message>) {
        Completable
                .create { gameDao.markMessagesAsRead(messages.map { it.chatId }) }
                .subscribeOn(Schedulers.io())
                .subscribe()
    }
}