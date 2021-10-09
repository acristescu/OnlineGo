package io.zenandroid.onlinego.data.ogs

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonEncodingException
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.socket.client.Ack
import io.socket.client.IO
import io.socket.client.Manager
import io.socket.client.Socket
import io.socket.parser.IOParser
import io.zenandroid.onlinego.BuildConfig
import io.zenandroid.onlinego.data.model.ogs.*
import io.zenandroid.onlinego.data.repositories.*
import io.zenandroid.onlinego.utils.AndroidLoggingHandler
import io.zenandroid.onlinego.utils.StethoWebSocketsFactory
import io.zenandroid.onlinego.utils.createJsonArray
import io.zenandroid.onlinego.utils.createJsonObject
import okhttp3.OkHttpClient
import org.json.JSONObject
import org.koin.core.context.GlobalContext.get
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger

private const val TAG = "OGSWebSocketService"

class OGSWebSocketService(
        private val httpClient: OkHttpClient,
        private val moshi: Moshi,
        private val restService: OGSRestService,
        private val userSessionRepository: UserSessionRepository
) {
    private val socket: Socket
    private var connectedToChallenges = false

    // Note: Don't use constructor injection here as it creates a dependency loop
    private val socketConnectedRepositories: List<SocketConnectedRepository> by get().inject()

    var authSent = false

    private val loggingAck = Ack {
        Log.i(TAG, "ack: $it")
    }

    init {
        socket = IO.socket("https://online-go.com", IO.Options().apply {
            transports = arrayOf("websocket")
            reconnection = true
            reconnectionDelay = 750
            reconnectionDelayMax = 10000
            if(BuildConfig.DEBUG) {
                webSocketFactory = StethoWebSocketsFactory(httpClient)
            }
        })

        socket.on(Socket.EVENT_CONNECT) {
            Logger.getLogger(TAG).warning("socket connect id=${socket.id()}")
            onSockedConnected()
        }.on(Socket.EVENT_DISCONNECT) {
            Logger.getLogger(TAG).warning("socket disconnect id=${socket.id()}")
            onSocketDisconnected()
        }.on(Socket.EVENT_CONNECT_ERROR) {
            Logger.getLogger(TAG).warning("socket connect error id=${socket.id()}")
        }.on(Socket.EVENT_CONNECT_ERROR) {
            Logger.getLogger(TAG).severe("socket connect error id=${socket.id()}")
        }

        AndroidLoggingHandler.reset(AndroidLoggingHandler())
        Logger.getLogger(Socket::class.java.name).level = Level.FINEST
        Logger.getLogger(Manager::class.java.name).level = Level.FINEST
        Logger.getLogger(io.socket.engineio.client.Socket::class.java.name).level = Level.FINEST
        Logger.getLogger(IOParser::class.java.name).level = Level.FINEST

    }
    
    fun ensureSocketConnected() {
        if(userSessionRepository.requiresUIConfigRefresh()) {
            restService.fetchUIConfig()
                    .subscribeOn(Schedulers.io())
                    .subscribe({}, { FirebaseCrashlytics.getInstance().log("E/$TAG: Failed to refresh UIConfig $it") })
        }
        socket.connect()
    }

    private val gameConnections = mutableMapOf<Long, GameConnection>()
    private val connectionsLock = Any()

    fun connectToGame(id: Long): GameConnection {
        synchronized(connectionsLock) {
            val connection = gameConnections[id] ?:
            GameConnection(id, connectionsLock,
                    observeEvent("game/$id/gamedata").parseJSON(),
                    observeEvent("game/$id/move").parseJSON(),
                    observeEvent("game/$id/clock").parseJSON(),
                    observeEvent("game/$id/phase").map { string -> Phase.valueOf(string.toString().uppercase(Locale.ENGLISH).replace(' ', '_')) },
                    observeEvent("game/$id/removed_stones").parseJSON(),
                    observeEvent("game/$id/chat").parseJSON(),
                    observeEvent("game/$id/undo_requested").map { string -> string.toString().toInt() },
                    observeEvent("game/$id/removed_stones_accepted").parseJSON(),
                    observeEvent("game/$id/undo_accepted").map { string -> string.toString().toInt() }
            ).apply {
                emitGameConnection(id)
                gameConnections[id] = this
            }
            connection.incrementCounter()
            return connection
        }
    }

    private inline fun <reified T> adapter(string: Any): T? {
        try {
            return moshi.adapter(T::class.java).fromJson(string.toString())
        } catch (e: JsonEncodingException) {
            val up = Exception("Error parsing JSON: $string", e)
            FirebaseCrashlytics.getInstance().recordException(up)
            throw up
        }
    }

    private inline fun <reified T> Flowable<Any>.parseJSON() =
        map { adapter<T>(it)!! }

    private fun emitGameConnection(id: Long) {
        emit("game/connect", createJsonObject {
            put("chat", true)
            put("game_id", id)
            put("player_id", userSessionRepository.userId)
        })
    }

    fun connectToActiveGames(): Flowable<OGSGame> {
        return observeEvent("active_game").parseJSON()
    }

    fun connectToUIPushes(): Flowable<UIPush> {
        val returnVal: Flowable<UIPush> = observeEvent("ui-push").parseJSON()

        emit("ui-pushes/subscribe", createJsonObject {
            put("channel", "undefined")
        })

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
            observeEvent("notification")
                    .map { JSONObject(it.toString()) }
                    .doOnSubscribe {
                        emit("notification/connect", createJsonObject {
                            put("player_id", userSessionRepository.userId)
                            put("auth", userSessionRepository.uiConfig?.notification_auth)
                        })
                    }.doOnCancel {
                        if(socket.connected()) {
                            emit("notification/disconnect", "")
                        }
                    }

    fun connectToChallenges(): Flowable<SeekGraphChallenge> {
        val listMyData = Types.newParameterizedType(List::class.java, SeekGraphChallenge::class.java)
        val adapter: JsonAdapter<List<SeekGraphChallenge>> = moshi.adapter(listMyData)

        val returnVal = observeEvent("seekgraph/global")
                .map { string -> adapter.fromJson(string.toString()) }
                .flatMapIterable { it -> it }
                .doOnCancel {
                    connectedToChallenges = false
                    emit("seek_graph/disconnect", createJsonObject {
                        put("channel", "global")
                    })
                }

        connectedToChallenges = true
        emit("seek_graph/connect", createJsonObject {
            put("channel", "global")
        })

        return returnVal
    }

    fun listenToNetPongEvents(): Flowable<NetPong> =
        observeEvent("net/pong").parseJSON()

    fun emit(event: String, params:Any?) {
        ensureSocketConnected()
        Log.i(TAG, "Emit: $event with params $params thread = ${Thread.currentThread().id}")
        socket.emit(event, params, loggingAck)
    }

    private fun observeEvent(event: String): Flowable<Any> {
        Log.i(TAG, "Listening for event: $event")
        return Flowable.create({ emitter ->
            socket.on(event) { params ->
                Log.i(TAG, "Received event: $event, ${params[0]}")

                if(params.size != 1) {
                    FirebaseCrashlytics.getInstance().recordException(Exception("Unexpected response (${params.size} params) while listening for event $event: parameter list is ${params.joinToString("|||")}"))
                } else if(params[0] == event) {
                    FirebaseCrashlytics.getInstance().recordException(Exception("Unexpected response (params[0] == event) while listening for event $event: parameter list is ${params.joinToString("|||")}"))
                }

                if(params[0] != null) {
                    emitter.onNext(params[0])
                } else {
                    FirebaseCrashlytics.getInstance().recordException(Exception("Unexpected null parameter for event $event"))
                    emitter.onNext("")
                }
            }

            emitter.setCancellable {
                Log.i(TAG, "Unregistering for event: $event")
                if(socket.connected()) {
                    socket.off(event)
                }
            }
        }
                , BackpressureStrategy.BUFFER)
    }

    fun startAutomatch(sizes: List<Size>, speed: Speed) : String {
        val uuid = UUID.randomUUID().toString()
        val json = createJsonObject {
            put("uuid", uuid)
            put("size_speed_options", createJsonArray {
                sizes.forEach { size ->
                    put(createJsonObject {
                        put("size", size.getText())
                        put("speed", speed.getText())
                    })
                }
            })
            put("lower_rank_diff", 6)
            put("upper_rank_diff", 6)
            put("rules", createJsonObject {
                put("condition", "no-preference")
                put("value", "japanese")
            })
            put("time_control", createJsonObject {
                put("condition", "no-preference")
                put("value", createJsonObject {
                    put("system", "byoyomi")
                })
            })
            put("handicap", createJsonObject {
                put("condition", "no-preference")
                put("value", "enabled")
            })
        }

        emit("automatch/find_match", json)
        return uuid
    }

    fun cancelAutomatch(automatch: OGSAutomatch) {
        emit("automatch/cancel", automatch.uuid)
    }

    fun fetchGameList(): Single<GameList> {
        ensureSocketConnected()
        return Single.create { emitter ->
            socket.emit("gamelist/query", createJsonObject {
                put("list", "live")
                put("sort_by", "rank")
                put("from", 0)
                put("limit", 9)
            }, Ack {
                args -> emitter.onSuccess(adapter(args[0].toString())!!)
            })
        }
    }

    fun disconnect() {
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
        emit("notification/delete", createJsonObject {
            put("player_id", userSessionRepository.userId)
            put("auth", userSessionRepository.uiConfig?.notification_auth)
            put("notification_id", notificationId)
        })
    }

    private fun onSockedConnected() {
        authSent = false
        resendAuth()
        socketConnectedRepositories.forEach { it.onSocketConnected()}
        synchronized(connectionsLock) {
            gameConnections.keys.forEach {
                emitGameConnection(it)
            }
        }
        if(connectedToChallenges) {
            emit("seek_graph/connect", createJsonObject {
                put("channel", "global")
            })
        }
    }

    private fun cleanup() {
        socketConnectedRepositories.forEach { it.onSocketDisconnected()}
    }

    private fun onSocketDisconnected() {
        cleanup()
    }

    fun disconnectFromGame(id: Long) {
        synchronized(connectionsLock) {
            gameConnections.remove(id)
            if(socket.connected()) {
                emit("game/disconnect", createJsonObject {
                    put("game_id", id)
                })
            }
        }
    }

    fun resendAuth() {
        val obj = JSONObject()
        obj.put("player_id", userSessionRepository.userId)
        obj.put("username", userSessionRepository.uiConfig?.user?.username)
        obj.put("auth", userSessionRepository.uiConfig?.chat_auth)
        Log.i(TAG, "Emit: authenticate with params obj")
        socket.emit("authenticate", obj, loggingAck)
        authSent = true
    }

}