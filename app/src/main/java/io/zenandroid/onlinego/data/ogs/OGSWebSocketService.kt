package io.zenandroid.onlinego.data.ogs

import co.touchlab.kermit.Logger
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
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
import kotlinx.coroutines.runBlocking
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
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.koin.core.context.GlobalContext.get
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

private const val TAG = "OGSWebSocketService"

private const val RECONNECT_DELAY_MIN_MS = 750L
private const val RECONNECT_DELAY_MAX_MS = 10000L
private const val NORMAL_CLOSURE_STATUS = 1000

class OGSWebSocketService(
  private val restService: OGSRestService,
  private val userSessionRepository: UserSessionRepository,
  private val httpClient: OkHttpClient,
  private val socketDebugRepository: SocketDebugRepository,
) {
  private val _connectionState = MutableStateFlow(false)
  val connectionState = _connectionState.asStateFlow()

  private var webSocket: WebSocket? = null
  private val connected = AtomicBoolean(false)
  private val intentionalDisconnect = AtomicBoolean(false)
  private var reconnectDelay = RECONNECT_DELAY_MIN_MS
  private var connectedToChallenges = false

  // Note: Don't use constructor injection here as it creates a dependency loop
  private val socketConnectedRepositories: List<SocketConnectedRepository> by get().inject()

  // Event listeners: event_name -> list of callbacks
  private val eventListeners = ConcurrentHashMap<String, MutableList<(JsonElement) -> Unit>>()

  private val wsUrl = "wss://wsp.online-go.com/"

  private val wsClient: OkHttpClient by lazy {
    httpClient.newBuilder()
      .pingInterval(15, TimeUnit.SECONDS)
      .build()
  }

  private val webSocketListener = object : WebSocketListener() {
    override fun onOpen(webSocket: WebSocket, response: Response) {
      Logger.i("WebSocket connected", tag = TAG)
      socketDebugRepository.logState("WS", "Connected (code=${response.code})")
      socketDebugRepository.updateConnectionState("Connected")
      connected.set(true)
      reconnectDelay = RECONNECT_DELAY_MIN_MS
      onSockedConnected()
      Logger.i("WebSocket connected - called all onSocketConnected() methods", tag = TAG)
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
      Logger.v(tag = TAG) { "<== raw: $text" }
      try {
        socketDebugRepository.logReceived("WS", text.take(500))
        handleMessage(text)
      } catch (e: Exception) {
        socketDebugRepository.logError("WS", "Error handling message: ${e.message}")
        CrashReporter.recordException(Exception("Error handling WebSocket message: $text", e))
      }
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
      Logger.d(tag = TAG) { "WebSocket closing: $code $reason" }
      socketDebugRepository.logState("WS", "Closing (code=$code, reason=$reason)")
      webSocket.close(NORMAL_CLOSURE_STATUS, null)
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
      Logger.i("WebSocket closed: $code $reason", tag = TAG)
      socketDebugRepository.logState("WS", "Closed (code=$code, reason=$reason)")
      socketDebugRepository.updateConnectionState("Disconnected")
      handleDisconnect()
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
      Logger.w("WebSocket failure: ${t.message}", tag = TAG)
      socketDebugRepository.logError("WS", "Failure: ${t.message} (response=${response?.code})")
      socketDebugRepository.updateConnectionState("Disconnected")
      handleDisconnect()
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
    val listeners = eventListeners[event]
    if (listeners != null) {
      synchronized(listeners) {
        listeners.forEach { it(data) }
      }
    }
  }

  private fun handleDisconnect() {
    webSocket = null
    val wasConnected = connected.getAndSet(false)
    socketDebugRepository.logState(
      "WS",
      "handleDisconnect (wasConnected=$wasConnected, intentional=${intentionalDisconnect.get()})"
    )
    if (wasConnected) {
      onSocketDisconnected()
    }
    if (!intentionalDisconnect.get()) {
      scheduleReconnect()
    }
  }

  private fun scheduleReconnect() {
    socketDebugRepository.logState("WS", "Scheduling reconnect in ${reconnectDelay}ms")
    socketDebugRepository.updateConnectionState("Reconnecting (${reconnectDelay}ms)")
    thread(start = true, name = "ws-reconnect-thread") {
      try {
        Logger.d(tag = TAG) { "Reconnecting in ${reconnectDelay}ms..." }
        Thread.sleep(reconnectDelay)
        reconnectDelay = (reconnectDelay * 2).coerceAtMost(RECONNECT_DELAY_MAX_MS)
        if (!intentionalDisconnect.get()) {
          doConnect()
        }
      } catch (_: InterruptedException) {
        // ignore
      }
    }
  }

  @Synchronized
  private fun doConnect() {
    if (webSocket != null) {
      return
    }
    val request = Request.Builder()
      .url(wsUrl)
      .build()
    webSocket = wsClient.newWebSocket(request, webSocketListener)
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
    if (webSocket == null) {
      socketDebugRepository.logState(
        "WS",
        "ensureSocketConnected: not connected, connecting... (intentionalDisconnect was ${intentionalDisconnect.get()})"
      )
      socketDebugRepository.updateConnectionState("Connecting...")
      intentionalDisconnect.set(false)
      doConnect()
    }
  }

  private val gameConnections = mutableMapOf<Long, GameConnection>()
  private val connectionsLock = Any()

  fun connectToGame(id: Long, includeChat: Boolean): GameConnection {
    var userId: Long? = null
    runBlocking {
      userId = (userSessionRepository.loginStatus.first() as? LoginStatus.LoggedIn)?.userId
    }
    synchronized(connectionsLock) {
      Logger.i("Acquired connection lock in connectToGame", tag = TAG)
      val connection = gameConnections[id] ?: GameConnection(
        userId = userId,
        gameId = id,
        connectionLock = connectionsLock,
        includeChat = includeChat,
        gameDataFlow = observeEvent("game/$id/gamedata").parseJSON(),
        movesFlow = observeEvent("game/$id/move").parseJSON(),
        clockFlow = observeEvent("game/$id/clock").parseJSON(),
        phaseFlow = observeEvent("game/$id/phase").map { element ->
          Phase.valueOf(
            element.jsonPrimitive.content.uppercase(Locale.ENGLISH).replace(' ', '_')
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
      connection.incrementCounter()
      Logger.i("Released connection lock in connectToGame", tag = TAG)
      return connection
    }
  }

  fun enableChatOnConnection(gameId: Long) {
    synchronized(connectionsLock) {
      Logger.i("Acquired connection lock in enableChatOnConnection", tag = TAG)
      gameConnections[gameId]?.let {
        if (!it.includeChat) {
          enableChatOnConnection(it)
        }
      }
      Logger.i("Released connection lock in enableChatOnConnection", tag = TAG)
    }
  }

  private fun enableChatOnConnection(connection: GameConnection) {
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

  private fun emitGameConnection(id: Long, includeChat: Boolean) {
    runBlocking {
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
            if (connected.get()) {
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
    webSocket?.send(message)
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

      val listeners = eventListeners.getOrPut(event) { mutableListOf() }
      synchronized(listeners) {
        listeners.add(listener)
      }

      awaitClose {
        Logger.d(tag = TAG) { "Unregistering for event: $event" }
        val list = eventListeners[event]
        if (list != null) {
          synchronized(list) {
            list.remove(listener)
            if (list.isEmpty()) {
              eventListeners.remove(event)
            }
          }
        }
      }
    }.buffer(Channel.UNLIMITED)
  }

  fun startAutomatch(sizes: List<Size>, speeds: List<Speed>): String {
    val uuid = UUID.randomUUID().toString()

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
    intentionalDisconnect.set(true)
    webSocket?.close(NORMAL_CLOSURE_STATUS, "Client disconnect")
    webSocket = null
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
    thread(start = true, name = "socket-connect-thread") {
      _connectionState.value = true
      resendAuth()
      socketConnectedRepositories.forEach { it.onSocketConnected() }
      synchronized(connectionsLock) {
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
    thread(start = true, name = "socket-disconnect-thread") {
      runBlocking { cleanup() }
    }
  }

  fun disconnectFromGame(id: Long) {
    synchronized(connectionsLock) {
      Logger.i("Acquired connection lock in disconnectFromGame", tag = TAG)
      gameConnections.remove(id)
      if (connected.get()) {
        emitGameDisconnect(id)
      }
      Logger.i("Released connection lock in disconnectFromGame", tag = TAG)
    }
  }

  private fun emitGameDisconnect(id: Long) {
    emit("game/disconnect") {
      "game_id" - id
    }
  }

  fun resendAuth() {
    runBlocking {
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
}