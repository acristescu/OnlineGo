package io.zenandroid.onlinego.data.ogs

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.squareup.moshi.JsonEncodingException
import com.squareup.moshi.Moshi
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.BuildConfig
import io.zenandroid.onlinego.data.model.ogs.GameList
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
import io.zenandroid.onlinego.utils.JsonObjectScope
import io.zenandroid.onlinego.utils.createJsonArray
import io.zenandroid.onlinego.utils.json
import io.zenandroid.onlinego.utils.recordException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import org.koin.core.context.GlobalContext.get
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

private const val TAG = "OGSWebSocketService"

private const val RECONNECT_DELAY_MIN_MS = 750L
private const val RECONNECT_DELAY_MAX_MS = 10000L
private const val NORMAL_CLOSURE_STATUS = 1000

class OGSWebSocketService(
  private val moshi: Moshi,
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
  private val eventListeners = ConcurrentHashMap<String, MutableList<(Any) -> Unit>>()

  // Pending request/response callbacks: id -> callback
  private val nextRequestId = AtomicInteger(1)
  private val pendingRequests = ConcurrentHashMap<Int, (data: Any?, error: JSONObject?) -> Unit>()

  private val wsUrl = "wss://wsp.online-go.com/"

  private val wsClient: OkHttpClient by lazy {
    httpClient.newBuilder()
      .pingInterval(15, TimeUnit.SECONDS)
      .build()
  }

  private val webSocketListener = object : WebSocketListener() {
    override fun onOpen(webSocket: WebSocket, response: Response) {
      if (BuildConfig.DEBUG) Log.i(TAG, "WebSocket connected")
      FirebaseCrashlytics.getInstance().log("Websocket connected")
      socketDebugRepository.logState("WS", "Connected (code=${response.code})")
      socketDebugRepository.updateConnectionState("Connected")
      connected.set(true)
      reconnectDelay = RECONNECT_DELAY_MIN_MS
      onSockedConnected()
      FirebaseCrashlytics.getInstance()
        .log("Websocket connected - called all onSocketConnected() methods")
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
      if (BuildConfig.DEBUG) Log.v(TAG, "<== raw: $text")
      try {
        socketDebugRepository.logReceived("WS", text.take(500))
        handleMessage(text)
      } catch (e: Exception) {
        socketDebugRepository.logError("WS", "Error handling message: ${e.message}")
        recordException(Exception("Error handling WebSocket message: $text", e))
      }
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
      if (BuildConfig.DEBUG) Log.i(TAG, "WebSocket closing: $code $reason")
      socketDebugRepository.logState("WS", "Closing (code=$code, reason=$reason)")
      webSocket.close(NORMAL_CLOSURE_STATUS, null)
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
      if (BuildConfig.DEBUG) Log.i(TAG, "WebSocket closed: $code $reason")
      FirebaseCrashlytics.getInstance().log("Websocket disconnected")
      socketDebugRepository.logState("WS", "Closed (code=$code, reason=$reason)")
      socketDebugRepository.updateConnectionState("Disconnected")
      handleDisconnect()
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
      if (BuildConfig.DEBUG) Log.w(TAG, "WebSocket failure: ${t.message}")
      FirebaseCrashlytics.getInstance().log("Websocket connect error: ${t.message}")
      socketDebugRepository.logError("WS", "Failure: ${t.message} (response=${response?.code})")
      socketDebugRepository.updateConnectionState("Disconnected")
      handleDisconnect()
    }
  }

  private fun handleMessage(text: String) {
    val jsonArray = JSONArray(text)
    val first = jsonArray.get(0)

    if (first is String) {
      // Server event: [event_name, data]
      val eventName = first
      val data = if (jsonArray.length() > 1) jsonArray.get(1) else JSONObject()
      if (BuildConfig.DEBUG) Log.i(TAG, "<== $eventName")
      dispatchEvent(eventName, data)
    } else if (first is Number) {
      // Response to a client request: [id, data?, error?]
      val id = first.toInt()
      val data = if (jsonArray.length() > 1 && !jsonArray.isNull(1)) jsonArray.get(1) else null
      val error =
        if (jsonArray.length() > 2 && !jsonArray.isNull(2)) jsonArray.optJSONObject(2) else null
      if (BuildConfig.DEBUG) Log.i(TAG, "<== response id=$id")

      val callback = pendingRequests.remove(id)
      if (callback != null) {
        callback(data, error)
      } else {
        if (BuildConfig.DEBUG) Log.w(TAG, "Received response for unknown request id=$id")
      }
    }
  }

  private fun dispatchEvent(event: String, data: Any) {
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
        if (BuildConfig.DEBUG) Log.i(TAG, "Reconnecting in ${reconnectDelay}ms...")
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
      restService.fetchUIConfig()
        .subscribeOn(Schedulers.io())
        .subscribe(
          {},
          {
            FirebaseCrashlytics.getInstance()
              .log("E/$TAG: Failed to refresh UIConfig $it")
            socketDebugRepository.logError("WS", "UIConfig refresh failed: ${it.message}")
          })
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
    val userId =
      (userSessionRepository.loggedInObservable.blockingFirst() as? LoginStatus.LoggedIn)?.userId
    synchronized(connectionsLock) {
      FirebaseCrashlytics.getInstance().log("Acquired connection lock in connectToGame")
      val connection = gameConnections[id] ?: GameConnection(
        userId = userId,
        gameId = id,
        connectionLock = connectionsLock,
        includeChat = includeChat,
        gameDataObservable = observeEvent("game/$id/gamedata").parseJSON(),
        movesObservable = observeEvent("game/$id/move").parseJSON(),
        clockObservable = observeEvent("game/$id/clock").parseJSON(),
        phaseObservable = observeEvent("game/$id/phase").map { string ->
          Phase.valueOf(
            string.toString().uppercase(Locale.ENGLISH).replace(' ', '_')
          )
        },
        removedStonesObservable = observeEvent("game/$id/removed_stones").parseJSON(),
        chatObservable = observeEvent("game/$id/chat").parseJSON(),
        undoRequestedObservable = observeEvent("game/$id/undo_requested").parseJSON(),
        removedStonesAcceptedObservable = observeEvent("game/$id/removed_stones_accepted").parseJSON(),
        undoAcceptedObservable = observeEvent("game/$id/undo_accepted").parseJSON()
      ).apply {
        emitGameConnection(id, includeChat)
        gameConnections[id] = this
      }
      if (includeChat && !connection.includeChat) {
        enableChatOnConnection(connection)
      }
      connection.incrementCounter()
      FirebaseCrashlytics.getInstance().log("Released connection lock in connectToGame")
      return connection
    }
  }

  fun enableChatOnConnection(gameId: Long) {
    synchronized(connectionsLock) {
      FirebaseCrashlytics.getInstance().log("Acquired connection lock in enableChatOnConnection")
      gameConnections[gameId]?.let {
        if (!it.includeChat) {
          enableChatOnConnection(it)
        }
      }
      FirebaseCrashlytics.getInstance().log("Released connection lock in enableChatOnConnection")
    }
  }

  private fun enableChatOnConnection(connection: GameConnection) {
    emitGameDisconnect(connection.gameId)
    emitGameConnection(connection.gameId, true)
    connection.includeChat = true
  }

  private inline fun <reified T> adapter(string: Any): T? {
    try {
      return moshi.adapter(T::class.java).fromJson(string.toString())
    } catch (e: JsonEncodingException) {
      val up = Exception("Error parsing JSON: $string", e)
      recordException(up)
      throw up
    }
  }

  private inline fun <reified T> Flowable<Any>.parseJSON() =
    map { adapter<T>(it)!! }

  private fun emitGameConnection(id: Long, includeChat: Boolean) {
    val loggedInStatus = userSessionRepository.loggedInObservable.blockingFirst()
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

  fun connectToActiveGames(): Flowable<OGSGame> {
    return observeEvent("active_game").parseJSON()
  }

  fun connectToUIPushes(): Flowable<UIPush> {
    val returnVal: Flowable<UIPush> = observeEvent("ui-push").parseJSON()

    emit("ui-pushes/subscribe") {
      "channel" - "undefined"
    }

    return returnVal
  }

  fun connectToBots(): Flowable<List<OGSPlayer>> =
    observeEvent("active-bots")
      .map { string ->
        //
        // HACK alert!!! Oh creators of OGS why do you torment me so and have different names
        // for the same field in different places!?!?? :)
        //
        val fixedString = string.toString().replace("\"icon-url\":", "\"icon\":")
        val json = JSONObject(fixedString)
        val retval = mutableListOf<OGSPlayer>()
        for (key in json.keys()) {
          adapter<OGSPlayer>(json[key])?.let {
            retval.add(it)
          }
        }
        return@map retval as List<OGSPlayer>
      }

  fun listenToNewAutomatchNotifications(): Flowable<OGSAutomatch> =
    observeEvent("automatch/entry").parseJSON()

  fun listenToCancelAutomatchNotifications(): Flowable<OGSAutomatch> =
    observeEvent("automatch/cancel").parseJSON()

  fun listenToStartAutomatchNotifications(): Flowable<OGSAutomatch> =
    observeEvent("automatch/start").parseJSON()

  fun connectToAutomatch() {
    emit("automatch/list", null)
  }

  fun connectToServerNotifications(): Flowable<JSONObject> =
    userSessionRepository.userIdObservable.toFlowable(BackpressureStrategy.BUFFER)
      .switchMap { userId ->
        observeEvent("notification")
          .map { JSONObject(it.toString()) }
          .doOnSubscribe {
            emit("notification/connect") {
              "player_id" - userId
              "auth" - userSessionRepository.uiConfig?.notification_auth
            }
          }.doOnCancel {
            if (connected.get()) {
              emit("notification/disconnect", "")
            }
          }
      }

  fun listenToNetPongEvents(): Flowable<NetPong> =
    observeEvent("net/pong").parseJSON()

  fun emit(event: String, params: Any?) {
    ensureSocketConnected()
    if (BuildConfig.DEBUG) Log.i(TAG, "==> $event with params $params")
    val message = JSONArray().apply {
      put(event)
      put(params ?: JSONObject.NULL)
    }
    socketDebugRepository.logSent(event, message.toString().take(500))
    webSocket?.send(message.toString())
  }

  fun emit(event: String, json: JsonObjectScope.() -> Unit) {
    emit(event, json { json() })
  }

  /**
   * Sends a command and expects a single response identified by a request id.
   * Returns the response data via the callback.
   */
  private fun emitWithResponse(
    event: String,
    params: Any?,
    callback: (data: Any?, error: JSONObject?) -> Unit
  ) {
    ensureSocketConnected()
    val id = nextRequestId.getAndIncrement()
    pendingRequests[id] = callback
    if (BuildConfig.DEBUG) Log.i(TAG, "==> $event with params $params (id=$id)")
    val message = JSONArray().apply {
      put(event)
      put(params ?: JSONObject.NULL)
      put(id)
    }
    socketDebugRepository.logSent("$event (id=$id)", message.toString().take(500))
    webSocket?.send(message.toString())
  }

  private fun observeEvent(event: String): Flowable<Any> {
    if (BuildConfig.DEBUG) Log.i(TAG, "Listening for event: $event")
    return Flowable.create({ emitter ->
      val listener: (Any) -> Unit = { data ->
        if (BuildConfig.DEBUG) Log.i(TAG, "<== $event, $data")
        emitter.onNext(data)
      }

      val listeners = eventListeners.getOrPut(event) { mutableListOf() }
      synchronized(listeners) {
        listeners.add(listener)
      }

      emitter.setCancellable {
        if (BuildConfig.DEBUG) Log.i(TAG, "Unregistering for event: $event")
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
    }, BackpressureStrategy.BUFFER)
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

  fun fetchGameList(): Single<GameList> {
    ensureSocketConnected()
    return Single.create { emitter ->
      val params = json {
        "list" - "live"
        "sort_by" - "rank"
        "from" - 0
        "limit" - 9
      }
      emitWithResponse("gamelist/query", params) { data, error ->
        if (error != null) {
          emitter.onError(Exception("Server error: ${error.optString("message", "unknown")}"))
        } else if (data != null) {
          try {
            emitter.onSuccess(adapter(data.toString())!!)
          } catch (e: Exception) {
            emitter.onError(e)
          }
        } else {
          emitter.onError(Exception("No data in gamelist/query response"))
        }
      }
    }
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

  fun deleteNotification(notificationId: String) {
    val loggedInStatus = userSessionRepository.loggedInObservable.blockingFirst()
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
        FirebaseCrashlytics.getInstance().log("Acquired connection lock in onSocketConnected")
        gameConnections.values.forEach {
          emitGameConnection(it.gameId, it.includeChat)
        }
        FirebaseCrashlytics.getInstance().log("Released connection lock in onSocketConnected")
      }
      if (connectedToChallenges) {
        emit("seek_graph/connect") {
          "channel" - "global"
        }
      }
    }
  }

  private suspend fun cleanup() {
    FirebaseCrashlytics.getInstance().log("Socket cleanup started")
    socketConnectedRepositories.forEach {
      it.onSocketDisconnected()
      yield()
    }
    FirebaseCrashlytics.getInstance().log("Socket clean up done")
  }

  private fun onSocketDisconnected() {
    _connectionState.value = false
    thread(start = true, name = "socket-disconnect-thread") {
      runBlocking { cleanup() }
    }
  }

  fun disconnectFromGame(id: Long) {
    synchronized(connectionsLock) {
      FirebaseCrashlytics.getInstance().log("Acquired connection lock in disconnectFromGame")
      gameConnections.remove(id)
      if (connected.get()) {
        emitGameDisconnect(id)
      }
      FirebaseCrashlytics.getInstance().log("Released connection lock in disconnectFromGame")
    }
  }

  private fun emitGameDisconnect(id: Long) {
    emit("game/disconnect") {
      "game_id" - id
    }
  }

  fun resendAuth() {
    val loggedInStatus = userSessionRepository.loggedInObservable.blockingFirst()
    if (loggedInStatus is LoginStatus.LoggedIn) {
      emit("authenticate", json {
        "player_id" - loggedInStatus.userId
        "username" - userSessionRepository.uiConfig?.user?.username
        "auth" - userSessionRepository.uiConfig?.chat_auth
      })
    }
  }
}