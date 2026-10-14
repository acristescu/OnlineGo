package io.zenandroid.onlinego.data.ogs

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.wss
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.zenandroid.onlinego.data.model.ogs.NetPong
import io.zenandroid.onlinego.data.model.ogs.OGSAutomatch
import io.zenandroid.onlinego.data.model.ogs.OGSGame
import io.zenandroid.onlinego.data.model.ogs.OGSPlayer
import io.zenandroid.onlinego.data.model.ogs.Phase
import io.zenandroid.onlinego.data.model.ogs.Size
import io.zenandroid.onlinego.data.model.ogs.Speed
import io.zenandroid.onlinego.data.model.ogs.UIPush
import io.zenandroid.onlinego.data.repositories.LoginStatus
import io.zenandroid.onlinego.data.repositories.SocketConnectedRepository
import io.zenandroid.onlinego.data.repositories.SocketDebugRepository
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import io.zenandroid.onlinego.utils.CrashReporter
import io.zenandroid.onlinego.utils.JsonObjectScope
import io.zenandroid.onlinego.utils.appJson
import io.zenandroid.onlinego.utils.createJsonArray
import io.zenandroid.onlinego.utils.json
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import org.koin.core.context.GlobalContext.get
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.update
import kotlin.uuid.Uuid

private const val TAG = "OGSWebSocketService"

private const val RECONNECT_DELAY_MIN_MS = 750L
private const val RECONNECT_DELAY_MAX_MS = 10000L

class OGSWebSocketService(
  private val restService: OGSRestService,
  private val userSessionRepository: UserSessionRepository,
  private val socketDebugRepository: SocketDebugRepository,
  private val client: HttpClient,
  private val applicationScope: CoroutineScope,
) {
  private val _connectionState = MutableStateFlow(false)
  val connectionState = _connectionState.asStateFlow()

  private val connected = AtomicBoolean(false)
  private val intentionalDisconnect = AtomicBoolean(false)
  private var reconnectDelay = RECONNECT_DELAY_MIN_MS
  private var connectedToChallenges = false

  // Note: Don't use constructor injection here as it creates a dependency loop
  private val socketConnectedRepositories: List<SocketConnectedRepository> by get().inject()

  private val eventListeners =
    AtomicReference(persistentMapOf<String, PersistentList<(JsonElement) -> Unit>>())

  private val wsHost = "wsp.online-go.com"

  private val outgoing = AtomicReference<Channel<String>?>(null)

  private fun connect() {
    val queue = Channel<String>(Channel.UNLIMITED)
    if (!outgoing.compareAndSet(expectedValue = null, newValue = queue)) {
      return
    }

    applicationScope.launch {
      try {
        client.wss(host = wsHost, path = "/") {
          Logger.i("WebSocket connected", tag = TAG)
          socketDebugRepository.logState("WS", "Connected")
          socketDebugRepository.updateConnectionState("Connected")
          connected.store(true)
          reconnectDelay = RECONNECT_DELAY_MIN_MS
          onSockedConnected()

          val sender = launch {
            for (message in queue) {
              send(Frame.Text(message))
            }
            close(CloseReason(CloseReason.Codes.NORMAL, "Client disconnect"))
          }
          try {
            for (frame in incoming) {
              if (frame is Frame.Text) {
                onTextMessage(frame.readText())
              }
            }
          } finally {
            sender.cancel()
          }
        }
        Logger.i("WebSocket closed", tag = TAG)
        socketDebugRepository.logState("WS", "Closed")
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Logger.w("WebSocket failure: ${e.message}", tag = TAG)
        socketDebugRepository.logError("WS", "Failure: ${e.message}")
      } finally {
        queue.close()
        outgoing.compareAndSet(expectedValue = queue, newValue = null)
        socketDebugRepository.updateConnectionState("Disconnected")
        handleDisconnect()
      }
    }
  }

  private fun onTextMessage(text: String) {
    Logger.v(tag = TAG) { "<== raw: $text" }
    try {
      socketDebugRepository.logReceived("WS", text.take(500))
      handleMessage(text)
    } catch (e: Exception) {
      socketDebugRepository.logError("WS", "Error handling message: ${e.message}")
      CrashReporter.recordException(Exception("Error handling WebSocket message: $text", e))
    }
  }

  private fun handleMessage(text: String) {
    val jsonArray = appJson.parseToJsonElement(text).jsonArray
    val first = jsonArray[0].jsonPrimitive

    if (first.isString) {
      // Server event: [event_name, data]
      val eventName = first.content
      val data = jsonArray.getOrNull(1) ?: JsonObject(emptyMap())
      Logger.d(tag = TAG) { "<== $eventName" }
      dispatchEvent(eventName, data)
    } else {
      // Response to a client request: [id, data?, error?]. We never send a request id, so
      // nothing should arrive here.
      Logger.w("Received unexpected response id=${first.longOrNull}", tag = TAG)
    }
  }

  private fun dispatchEvent(event: String, data: JsonElement) {
    eventListeners.load()[event]?.forEach { it(data) }
  }

  private fun handleDisconnect() {
    val wasConnected = connected.exchange(false)
    socketDebugRepository.logState(
      "WS",
      "handleDisconnect (wasConnected=$wasConnected, intentional=${intentionalDisconnect.load()})"
    )
    if (wasConnected) {
      onSocketDisconnected()
    }
    if (!intentionalDisconnect.load()) {
      scheduleReconnect()
    }
  }

  private fun scheduleReconnect() {
    socketDebugRepository.logState("WS", "Scheduling reconnect in ${reconnectDelay}ms")
    socketDebugRepository.updateConnectionState("Reconnecting (${reconnectDelay}ms)")
    applicationScope.launch {
      Logger.d(tag = TAG) { "Reconnecting in ${reconnectDelay}ms..." }
      delay(reconnectDelay)
      reconnectDelay = (reconnectDelay * 2).coerceAtMost(RECONNECT_DELAY_MAX_MS)
      if (!intentionalDisconnect.load()) {
        connect()
      }
    }
  }

  fun ensureSocketConnected() {
    if (userSessionRepository.requiresUIConfigRefresh()) {
      socketDebugRepository.logState("WS", "UIConfig refresh required")
      CoroutineScope(Dispatchers.IO).launch {
        try {
          restService.fetchUIConfig()
        } catch (e: Exception) {
          Logger.e("Failed to refresh UIConfig $e", tag = TAG)
          socketDebugRepository.logError("WS", "UIConfig refresh failed: ${e.message}")
        }
      }
    }
    if (outgoing.load() == null) {
      socketDebugRepository.logState(
        "WS",
        "ensureSocketConnected: not connected, connecting... (intentionalDisconnect was ${intentionalDisconnect.load()})"
      )
      socketDebugRepository.updateConnectionState("Connecting...")
      intentionalDisconnect.store(false)
      connect()
    }
  }

  private val gameConnections = mutableMapOf<Long, GameConnection>()
  private val connectionsMutex = Mutex()

  suspend fun connectToGame(id: Long, includeChat: Boolean): GameConnection {
    val userId = (userSessionRepository.loginStatus.first() as? LoginStatus.LoggedIn)?.userId
    return connectionsMutex.withLock {
      Logger.i("Acquired connection lock in connectToGame", tag = TAG)
      val connection = gameConnections[id] ?: GameConnection(
        userId = userId,
        gameId = id,
        includeChat = includeChat,
        gameDataFlow = observeEvent("game/$id/gamedata").parseJSON(),
        movesFlow = observeEvent("game/$id/move").parseJSON(),
        clockFlow = observeEvent("game/$id/clock").parseJSON(),
        phaseFlow = observeEvent("game/$id/phase").map { element ->
          Phase.valueOf(
            element.jsonPrimitive.content.uppercase().replace(' ', '_')
          )
        },
        removedStonesFlow = observeEvent("game/$id/removed_stones").parseJSON(),
        chatFlow = observeEvent("game/$id/chat").parseJSON(),
        undoRequestedFlow = observeEvent("game/$id/undo_requested").parseJSON(),
        removedStonesAcceptedFlow = observeEvent("game/$id/removed_stones_accepted").parseJSON(),
        undoAcceptedFlow = observeEvent("game/$id/undo_accepted").parseJSON()
      ).apply {
        emitGameConnection(id, includeChat)
        gameConnections[id] = this
      }
      if (includeChat && !connection.includeChat) {
        enableChatOnConnection(connection)
      }
      connection.refCount++
      Logger.i("Released connection lock in connectToGame", tag = TAG)
      connection
    }
  }

  fun release(connection: GameConnection) {
    applicationScope.launch {
      connectionsMutex.withLock {
        Logger.i("Acquired connection lock in release", tag = TAG)
        connection.refCount--
        if (connection.refCount == 0) {
          connection.dispose()
          if (gameConnections[connection.gameId] === connection) {
            gameConnections.remove(connection.gameId)
            if (connected.load()) {
              emitGameDisconnect(connection.gameId)
            }
          }
        }
        Logger.i("Released connection lock in release", tag = TAG)
      }
    }
  }

  suspend fun enableChatOnConnection(gameId: Long) {
    connectionsMutex.withLock {
      Logger.i("Acquired connection lock in enableChatOnConnection", tag = TAG)
      gameConnections[gameId]?.let {
        if (!it.includeChat) {
          enableChatOnConnection(it)
        }
      }
      Logger.i("Released connection lock in enableChatOnConnection", tag = TAG)
    }
  }

  private suspend fun enableChatOnConnection(connection: GameConnection) {
    emitGameDisconnect(connection.gameId)
    emitGameConnection(connection.gameId, true)
    connection.includeChat = true
  }

  private inline fun <reified T> decode(element: JsonElement): T {
    try {
      return appJson.decodeFromJsonElement<T>(element)
    } catch (e: SerializationException) {
      val up = Exception("Error parsing JSON: $element", e)
      CrashReporter.recordException(up)
      throw up
    }
  }

  private inline fun <reified T> Flow<JsonElement>.parseJSON() =
    map { decode<T>(it) }

  private suspend fun emitGameConnection(id: Long, includeChat: Boolean) {
    val loggedInStatus = userSessionRepository.loginStatus.first()
    if (loggedInStatus is LoginStatus.LoggedIn) {
      emit("game/connect") {
        "chat" - includeChat
        "game_id" - id
        "player_id" - loggedInStatus.userId
      }
      if (includeChat) {
        emit("chat/connect") {
          "player_id" - loggedInStatus.userId
          "username" - userSessionRepository.uiConfig?.user?.username
          "auth" - userSessionRepository.uiConfig?.chat_auth
        }
        emit("chat/join") {
          "channel" - "game-$id"
        }
      }
    }
  }

  fun connectToActiveGames(): Flow<OGSGame> {
    return observeEvent("active_game").parseJSON()
  }

  fun connectToUIPushes(): Flow<UIPush> {
    return observeEvent("ui-push").parseJSON<UIPush>()
      .onStart {
        this@OGSWebSocketService.emit("ui-pushes/subscribe") {
          "channel" - "undefined"
        }
      }
  }

  fun connectToBots(): Flow<List<OGSPlayer>> =
    observeEvent("active-bots")
      .map { element ->
        //
        // HACK alert!!! Oh creators of OGS why do you torment me so and have different names
        // for the same field in different places!?!?? :)
        //
        element.jsonObject.values.map { bot ->
          val renamed = bot.jsonObject.mapKeys { (key, _) ->
            if (key == "icon-url") "icon" else key
          }
          decode<OGSPlayer>(JsonObject(renamed))
        }
      }

  fun listenToNewAutomatchNotifications(): Flow<OGSAutomatch> =
    observeEvent("automatch/entry").parseJSON()

  fun listenToCancelAutomatchNotifications(): Flow<OGSAutomatch> =
    observeEvent("automatch/cancel").parseJSON()

  fun listenToStartAutomatchNotifications(): Flow<OGSAutomatch> =
    observeEvent("automatch/start").parseJSON()

  fun connectToAutomatch() {
    emit("automatch/list", null)
  }

  @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
  fun connectToServerNotifications(): Flow<JsonObject> =
    userSessionRepository.userId
      .flatMapLatest { userId ->
        observeEvent("notification")
          .map { it.jsonObject }
          .onStart {
            this@OGSWebSocketService.emit("notification/connect") {
              "player_id" - userId
              "auth" - userSessionRepository.uiConfig?.notification_auth
            }
          }.onCompletion {
            if (connected.load()) {
              this@OGSWebSocketService.emit("notification/disconnect", "")
            }
          }
      }

  fun listenToNetPongEvents(): Flow<NetPong> =
    observeEvent("net/pong").parseJSON()

  fun emit(event: String, params: Any?) {
    ensureSocketConnected()
    Logger.d(tag = TAG) { "==> $event with params $params" }
    val message = buildJsonArray {
      add(JsonPrimitive(event))
      add(jsonElementOf(params))
    }.toString()
    socketDebugRepository.logSent(event, message.take(500))
    outgoing.load()?.trySend(message)
  }

  fun emit(event: String, json: JsonObjectScope.() -> Unit) {
    emit(event, json { json() })
  }

  private fun observeEvent(event: String): Flow<JsonElement> {
    Logger.d(tag = TAG) { "Listening for event: $event" }
    return callbackFlow {
      val listener: (JsonElement) -> Unit = { data ->
        Logger.d(tag = TAG) { "<== $event, $data" }
        trySend(data)
      }

      eventListeners.update { it.put(event, (it[event] ?: persistentListOf()).add(listener)) }

      awaitClose {
        Logger.d(tag = TAG) { "Unregistering for event: $event" }
        eventListeners.update { listeners ->
          val remaining = listeners[event]?.remove(listener) ?: return@update listeners
          if (remaining.isEmpty()) listeners.remove(event) else listeners.put(event, remaining)
        }
      }
    }.buffer(Channel.UNLIMITED)
  }

  fun startAutomatch(sizes: List<Size>, speeds: List<Speed>): String {
    val uuid = Uuid.random().toString()

    emit("automatch/find_match") {
      "uuid" - uuid
      "size_speed_options" - createJsonArray {
        speeds.forEach { speed ->
          sizes.forEach { size ->
            put(json {
              "size" - size.getText()
              "speed" - speed.getText()
              "system" - "byoyomi"
            })
            put(json {
              "size" - size.getText()
              "speed" - speed.getText()
              "system" - "fischer"
            })
          }
        }
      }
      "lower_rank_diff" - 6
      "upper_rank_diff" - 6
      "rules" - json {
        "condition" - "required"
        "value" - "japanese"
      }
      "handicap" - json {
        "condition" - "preferred"
        "value" - "enabled"
      }
    }
    return uuid
  }

  fun cancelAutomatch(automatch: OGSAutomatch) {
    emit("automatch/cancel", automatch.uuid)
  }

  suspend fun disconnect() {
    //
    // Note: cleanup gets called twice, once before the disconnection and once after. If we only
    // call it after, then the messages to the server don't get sent (since the socket is already
    // closed). If we only call it before, then if the disconnection is caused by outside factors
    // then there is no cleanup and we end up subscribing twice...
    //
    socketDebugRepository.logState("WS", "disconnect() called (intentional)")
    cleanup()
    intentionalDisconnect.store(true)
    outgoing.load()?.close()
    socketDebugRepository.updateConnectionState("Disconnected (intentional)")
  }

  suspend fun deleteNotification(notificationId: String) {
    val loggedInStatus = userSessionRepository.loginStatus.first()
    if (loggedInStatus is LoginStatus.LoggedIn) {
      emit("notification/delete") {
        "player_id" - loggedInStatus.userId
        "auth" - userSessionRepository.uiConfig?.notification_auth
        "notification_id" - notificationId
      }
    }
  }

  private fun onSockedConnected() {
    applicationScope.launch {
      _connectionState.value = true
      resendAuth()
      socketConnectedRepositories.forEach { it.onSocketConnected() }
      connectionsMutex.withLock {
        Logger.i("Acquired connection lock in onSocketConnected", tag = TAG)
        gameConnections.values.forEach {
          emitGameConnection(it.gameId, it.includeChat)
        }
        Logger.i("Released connection lock in onSocketConnected", tag = TAG)
      }
      if (connectedToChallenges) {
        emit("seek_graph/connect") {
          "channel" - "global"
        }
      }
    }
  }

  private suspend fun cleanup() {
    Logger.i("Socket cleanup started", tag = TAG)
    socketConnectedRepositories.forEach {
      it.onSocketDisconnected()
      yield()
    }
    Logger.i("Socket clean up done", tag = TAG)
  }

  private fun onSocketDisconnected() {
    _connectionState.value = false
    applicationScope.launch { cleanup() }
  }

  private fun emitGameDisconnect(id: Long) {
    emit("game/disconnect") {
      "game_id" - id
    }
  }

  suspend fun resendAuth() {
    val loggedInStatus = userSessionRepository.loginStatus.first()
    if (loggedInStatus is LoginStatus.LoggedIn) {
      emit("authenticate", json {
        "player_id" - loggedInStatus.userId
        "username" - userSessionRepository.uiConfig?.user?.username
        "auth" - userSessionRepository.uiConfig?.chat_auth
      })
    }
  }
}