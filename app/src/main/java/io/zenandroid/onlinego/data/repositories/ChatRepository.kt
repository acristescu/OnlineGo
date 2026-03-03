package io.zenandroid.onlinego.data.repositories

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.zenandroid.onlinego.data.db.GameDao
import io.zenandroid.onlinego.data.model.local.ChatMetadata
import io.zenandroid.onlinego.data.model.local.Message
import io.zenandroid.onlinego.data.ogs.OGSRestAPI
import io.zenandroid.onlinego.utils.recordException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ChatRepository(
  private val gameDao: GameDao,
  private val restApi: OGSRestAPI,
  private val userSessionRepository: UserSessionRepository,
  private val applicationScope: CoroutineScope
) : SocketConnectedRepository {

  private val knownMessageIds = mutableSetOf<String>()
  private var lastRESTFetchedChatId: String = "00000000-0000-0000-0000-000000000000"
  private var socketScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  init {
    applicationScope.launch {
            val messageIDs =gameDao.getAllMessageIDs()
       knownMessageIds.addAll(messageIDs)
        }
  }

  override fun onSocketConnected() {
    socketScope.launch {
      try {
        gameDao.monitorChatMetadata()
          .filterNotNull()
          .distinctUntilChanged()
          .collect { onMetadata(it) }
      } catch (e: Exception) {
        onError(e, "monitorHistoricGameMetadata")
      }
    }
  }

  private fun onMetadata(metadata: ChatMetadata) {
    lastRESTFetchedChatId = metadata.latestMessageId
  }

  override fun onSocketDisconnected() {
    socketScope.cancel()
    socketScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  }

  suspend fun addMessage(message: Message) {
    val loggedInStatus = userSessionRepository.loginStatus.first()
    if (loggedInStatus is LoginStatus.LoggedIn) {
      if (!knownMessageIds.contains(message.chatId)) {
        gameDao.insertMessage(message)
        if (message.playerId == loggedInStatus.userId && message.gameId != null) {
          gameDao.markGameMessagesAsReadUpTo(message.gameId, message.date)
        }
        knownMessageIds.add(message.chatId)
      }
    }
  }

  fun fetchRecentChatMessages() {
    applicationScope.launch {
      try {
        val loggedInStatus = userSessionRepository.loginStatus
          .first { it is LoginStatus.LoggedIn }
        val messages = restApi.getMessages(lastRESTFetchedChatId)
        val localMessages = messages.map { Message.fromOGSMessage(it, it.game_id) }
        gameDao.insertMessagesFromRest(localMessages)
      } catch (e: Exception) {
        onError(e, "fetchRecentChatMessages")
      }
    }
  }

  fun monitorGameChat(gameId: Long): Flow<List<Message>> =
    gameDao.getMessagesForGame(gameId)
      .onEach { it.forEach { knownMessageIds.add(it.chatId) } }

  fun markMessagesAsRead(messages: List<Message>) {
    applicationScope.launch(Dispatchers.IO) {
      gameDao.markMessagesAsRead(messages.map { it.chatId })
    }
  }

  private fun onError(t: Throwable, request: String) {
    var message = request
    if (t is retrofit2.HttpException) {
      message = "$request: ${t.response()?.errorBody()?.string()}"
      if (t.code() == 429) {
        FirebaseCrashlytics.getInstance().setCustomKey("HIT_RATE_LIMITER", true)
      }
    }
    recordException(Exception(message, t))
    Log.e("ChatRepository", message, t)
  }

}