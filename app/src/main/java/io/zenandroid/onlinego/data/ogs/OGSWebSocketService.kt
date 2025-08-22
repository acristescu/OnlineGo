package io.zenandroid.onlinego.data.ogs

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.squareup.moshi.JsonEncodingException
import com.squareup.moshi.Moshi
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.socket.client.Ack
import io.socket.client.IO
import io.socket.client.Socket
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
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import io.zenandroid.onlinego.utils.AndroidLoggingHandler
import io.zenandroid.onlinego.utils.JsonObjectScope
import io.zenandroid.onlinego.utils.createJsonArray
import io.zenandroid.onlinego.utils.json
import io.zenandroid.onlinego.utils.recordException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.json.JSONObject
import org.koin.core.context.GlobalContext.get
import java.util.Locale
import java.util.UUID
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.concurrent.thread

private const val TAG = "OGSWebSocketService"

class OGSWebSocketService(
  private val moshi: Moshi,
  private val restService: OGSRestService,
  private val userSessionRepository: UserSessionRepository,
) {
  private val _connectionState = MutableStateFlow(false)
  val connectionState = _connectionState.asStateFlow()

  private val socket: Socket
  private var connectedToChallenges = false

  // Note: Don't use constructor injection here as it creates a dependency loop
  private val socketConnectedRepositories: List<SocketConnectedRepository> by get().inject()

  private val loggingAck = Ack {
    if (BuildConfig.DEBUG) {
      val debugItem = if (it is Array<*>) it[0] else it
      Log.i(TAG, "ack: $debugItem")
    }
  }

  init {
    socket = IO.socket(BuildConfig.BASE_URL, IO.Options().apply {
      transports = arrayOf("websocket")
      reconnection = true
      reconnectionDelay = 750
      reconnectionDelayMax = 10000
    })

    socket.on(Socket.EVENT_CONNECT) {
      if (BuildConfig.DEBUG) Logger.getLogger(TAG).warning("socket connect id=${socket.id()}")
      FirebaseCrashlytics.getInstance().log("Websocket connected")
      onSockedConnected()
      FirebaseCrashlytics.getInstance()
        .log("Websocket connected - called all onSocketConnected() methods")
    }.on(Socket.EVENT_DISCONNECT) {
      if (BuildConfig.DEBUG) Logger.getLogger(TAG)
        .warning("socket disconnect id=${socket.id()}")
      FirebaseCrashlytics.getInstance().log("Websocket disconnected")
      onSocketDisconnected()
    }.on(Socket.EVENT_CONNECT_ERROR) {
      if (BuildConfig.DEBUG) Logger.getLogger(TAG)
        .warning("socket connect error id=${socket.id()}")
      FirebaseCrashlytics.getInstance().log("Websocket connect error")
    }

    if (BuildConfig.DEBUG) {
      AndroidLoggingHandler.reset(AndroidLoggingHandler())
      Logger.getLogger(Socket::class.java.name).level = Level.FINEST
//            Logger.getLogger(Manager::class.java.name).level = Level.FINEST
//            Logger.getLogger(io.socket.engineio.client.Socket::class.java.name).level = Level.FINEST
//            Logger.getLogger(IOParser::class.java.name).level = Level.FINEST
    }

  }

  fun ensureSocketConnected() {
    if (userSessionRepository.requiresUIConfigRefresh()) {
      restService.fetchUIConfig()
        .subscribeOn(Schedulers.io())
        .subscribe(
          {},
          {
            FirebaseCrashlytics.getInstance()
              .log("E/$TAG: Failed to refresh UIConfig $it")
          })
    }
    socket.connect()
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
        undoRequestedObservable = observeEvent("game/$id/undo_requested").map { string ->
          string.toString().toInt()
        },
        removedStonesAcceptedObservable = observeEvent("game/$id/removed_stones_accepted").parseJSON(),
        undoAcceptedObservable = observeEvent("game/$id/undo_accepted").map { string ->
          string.toString().toInt()
        }
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
            if (socket.connected()) {
              emit("notification/disconnect", "")
            }
          }
      }

  fun listenToNetPongEvents(): Flowable<NetPong> =
    observeEvent("net/pong").parseJSON()

  fun emit(event: String, params: Any?) {
    ensureSocketConnected()
    if (BuildConfig.DEBUG) Log.i(TAG, "==> $event with params $params")
    socket.emit(event, params, loggingAck)
  }

  fun emit(event: String, json: JsonObjectScope.() -> Unit) {
    emit(event, json { json() })
  }

  private fun observeEvent(event: String): Flowable<Any> {
    if (BuildConfig.DEBUG) Log.i(TAG, "Listening for event: $event")
    return Flowable.create({ emitter ->
      socket.on(event) { params ->
        if (BuildConfig.DEBUG) Log.i(TAG, "<== $event, ${params[0]}")

        if (params.size != 1) {
          recordException(
            Exception(
              "Unexpected response (${params.size} params) while listening for event $event: parameter list is ${
                params.joinToString(
                  "|||"
                )
              }"
            )
          )
        } else if (params[0] == event) {
          recordException(
            Exception(
              "Unexpected response (params[0] == event) while listening for event $event: parameter list is ${
                params.joinToString(
                  "|||"
                )
              }"
            )
          )
        }

        if (params[0] != null) {
          // Sometimes, rarely, the first parameter is the name of the channel repeated (!?!?)
          val response =
            if (params[0] == event && params.size > 1) params[1] else params[0]
          emitter.onNext(response)
        } else {
          recordException(Exception("Unexpected null parameter for event $event"))
          emitter.onNext("")
        }
      }

      emitter.setCancellable {
        if (BuildConfig.DEBUG) Log.i(TAG, "Unregistering for event: $event")
        if (socket.connected()) {
          socket.off(event)
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
      socket.emit("gamelist/query", json {
        "list" - "live"
        "sort_by" - "rank"
        "from" - 0
        "limit" - 9
      }, Ack { args ->
        emitter.onSuccess(adapter(args[0].toString())!!)
      })
    }
  }

  suspend fun disconnect() {
    //
    // Note: cleanup gets called twice, once before the disconnection and once after. If we only
    // call it after, then the messages to the server don't get sent (since the socket is already
    // closed). If we only call it before, then if the disconnection is caused by outside factors
    // then there is no cleanup and we end up subscribing twice...
    //
    cleanup()
    socket.disconnect()
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
      if (socket.connected()) {
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
      val obj = JSONObject().apply {
        put("player_id", loggedInStatus.userId)
        put("username", userSessionRepository.uiConfig?.user?.username)
        put("auth", userSessionRepository.uiConfig?.chat_auth)
      }
      if (BuildConfig.DEBUG) Log.i(TAG, "==> authenticate with params $obj")
      socket.emit("authenticate", obj, loggingAck)
    }
  }
}