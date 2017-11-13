package io.zenandroid.onlinego.ogs

import android.graphics.Point
import com.squareup.moshi.Moshi
import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.socket.client.Ack
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.parser.IOParser
import io.zenandroid.onlinego.AndroidLoggingHandler
import io.zenandroid.onlinego.model.ogs.Game
import io.zenandroid.onlinego.model.ogs.GameList
import io.zenandroid.onlinego.model.ogs.LoginToken
import io.zenandroid.onlinego.model.ogs.UIConfig
import io.zenandroid.onlinego.utils.PersistenceManager
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Created by alex on 03/11/2017.
 */
class OGSService {

    companion object {
        @JvmField
        val instance = OGSService()
        val TAG = OGSService::class.java.name!!
    }

    private var token: LoginToken? = null
    var uiConfig: UIConfig? = null
    private val socket: Socket
    private var tokenExpiry: Date
    private val api: OGSRestAPI
    private val moshi = Moshi.Builder().build()

    private val loggingAck = Ack { println("ack: $it") }

    fun login(username: String, password: String): Completable {
        return api.login(username, password)
                .doOnSuccess(this::storeToken)
                .flatMap { api.uiConfig() }
                .doOnSuccess(this::storeUIConfig)
                .doOnSuccess({ ensureSocketConnected() })
                .toCompletable()
    }

    fun loginWithToken(): Completable {
        if(token == null || token?.access_token == null || token?.refresh_token == null) {
            //
            // No token, we need to log in with password
            //
            return Completable.error(Throwable())
        }

        val tokenSource: Single<LoginToken>

        if(tokenExpiry.before(Date())) {
            //
            // We do have a token but it's expired, we need to refresh everything
            //
            tokenSource = api.refreshToken(token!!.refresh_token).doOnSuccess(this::storeToken)
            uiConfig = null
        } else {
            //
            // Just use the token we have
            //
            tokenSource = Single.just(token)
        }

        val uiConfigSource: Single<UIConfig>
        if(uiConfig == null) {
            uiConfigSource = tokenSource.flatMap { api.uiConfig() }.doOnSuccess(this::storeUIConfig)
        } else {
            uiConfigSource = tokenSource.flatMap { Single.just(uiConfig!!)}
        }

       return uiConfigSource
               .doOnSuccess({ ensureSocketConnected() })
               .toCompletable()
    }

    private fun storeToken(token: LoginToken) {
        this.token = token
        Date(Date().time + token.expires_in * 1000).let {
            this.tokenExpiry = it
            PersistenceManager.instance.storeToken(token, it)
        }
    }

    private fun storeUIConfig(uiConfig: UIConfig) {
        this.uiConfig = uiConfig
        PersistenceManager.instance.storeUIConfig(uiConfig)
    }

    private fun ensureSocketConnected() {
        if(socket.connected()) {
            return
        }
        AndroidLoggingHandler.reset(AndroidLoggingHandler())
//                    Logger.getLogger(Socket::class.java.name).level = Level.FINEST
//                    Logger.getLogger(Manager::class.java.name).level = Level.FINEST
        Logger.getLogger(IOParser::class.java.name).level = Level.FINEST

        socket.on(Socket.EVENT_CONNECT) {
            Logger.getLogger(TAG).warning("socket connect")
        }.on(Socket.EVENT_DISCONNECT) {
            Logger.getLogger(TAG).warning("socket disconnect")
        }.on(Socket.EVENT_CONNECT_ERROR) {
            Logger.getLogger(TAG).warning("socket connect error")
        }.on(Socket.EVENT_ERROR) {
            Logger.getLogger(TAG).warning("socket error")
        }.on(Socket.EVENT_CONNECT_TIMEOUT) {
            Logger.getLogger(TAG).warning("socket connect timeout")
        }.on(Socket.EVENT_RECONNECT) {
            Logger.getLogger(TAG).warning("socket reconnect")
        }.on(Socket.EVENT_MESSAGE) {
            Logger.getLogger(TAG).warning(it.toString())
        }.on(Socket.EVENT_CONNECTING) {
            Logger.getLogger(TAG).warning("socket connecting")
        }.on(Socket.EVENT_CONNECT_ERROR) {
            Logger.getLogger(TAG).severe("socket connect error")
        }

        socket.connect()

        val obj = JSONObject()
        obj.put("player_id", uiConfig?.user?.id)
        obj.put("username", uiConfig?.user?.username)
        obj.put("auth", uiConfig?.chat_auth)
        emit("authenticate", obj)


        //                    socket.emit("gamelist/count/subscribe")

//                    obj = JSONObject()
//                    obj.put("chat", 0)
//                    obj.put("game_id", 10493024)
//                    obj.put("player_id", 89194)
//                    socket.emit("game/connect", obj)
    }


    fun connectToGame(id: Long): GameConnection {
        val connection = GameConnection(id)

        connection.gameData = observeEvent("game/$id/gamedata")
                    .map { string -> moshi.adapter(GameData::class.java).fromJson(string.toString()) }
                    .map { gameData ->
                        connection.gameAuth = gameData.auth
                        gameData
                    }
        connection.moves = observeEvent("game/$id/move")
                    .map { string -> moshi.adapter(Move::class.java).fromJson(string.toString()) }

        emit("game/connect", createJsonObject {
            put("chat", false)
            put("game_id", id)
            put("player_id", uiConfig!!.user.id)
        })
        return connection
    }

    fun connectToNotifications(): Flowable<Game> {
        val returnVal = observeEvent("active_game")
                .map { string -> moshi.adapter(Game::class.java).fromJson(string.toString()) }

        emit("notification/connect", createJsonObject {
            put("player_id", uiConfig!!.user.id)
            put("auth", uiConfig!!.notification_auth)
        })

        return returnVal
    }

    private fun emit(event: String, params:Any?) {
        println("Emit: $event with params $params")
        socket.emit(event, params, loggingAck)
    }

    private fun observeEvent(event: String): Flowable<Any> {
        println("Listening for event: $event")
        return Flowable.create({ emitter ->
            socket.on(event, {
                params ->
                    println("Received event: $event, ${params[0]}")
                    emitter.onNext(params[0])
            })
        }
        , BackpressureStrategy.BUFFER)
    }

    fun registerSeekgraph(): Flowable<Any> {
        return observeEvent("seekgraph/global").doOnSubscribe({
            emit("seek_graph/connect", createJsonObject {
                put("channel", "global")
            })
        })
    }

    fun startGameSearch() {
        val uuid = UUID.randomUUID().toString()
        val json = createJsonObject {
            put("uuid", uuid)
            put("size_speed_options", createJsonArray {
                put(createJsonObject {
                    put("size", "13x13")
                    put("speed", "live")
                })
                put(createJsonObject {
                    put("size", "9x9")
                    put("speed", "live")
                })
                put(createJsonObject {
                    put("size", "19x19")
                    put("speed", "live")
                })
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
    }

    fun fetchGameList(): Single<GameList> {
        return Single.create({emitter ->
            socket.emit("gamelist/query", createJsonObject {
                put("list", "live")
                put("sort_by", "rank")
                put("from", 0)
                put("limit", 9)
            }, Ack {
                args -> emitter.onSuccess(moshi.adapter(GameList::class.java).fromJson(args[0].toString()))
            })
        })

    }

    fun disconnect() {
        socket.disconnect()
    }

    init {
        val httpClient = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    var request = chain.request()
                    if (token != null) {
                        request = request.newBuilder()
                                .addHeader("Authorization", "Bearer ${token?.access_token}")
                                .build()
                    }
                    val response = chain.proceed(request)
                    println("${request.method()} ${request.url()} -> ${response.code()} ${response.message()}")
                    response
                }
                .build()
        api = Retrofit.Builder()
                .baseUrl("https://online-go.com/")
                .client(httpClient)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(OGSRestAPI::class.java)

        uiConfig = PersistenceManager.instance.getUIConfig()
        token = PersistenceManager.instance.getToken()
        tokenExpiry = PersistenceManager.instance.getTokenExpiry()

        val options = IO.Options()
        options.transports = arrayOf("websocket")
        socket = IO.socket("https://online-go.com", options)
    }

    private fun createJsonObject(func: JSONObject.() -> Unit): JSONObject {
        val obj = JSONObject()
        func(obj)
        return obj
    }

    private fun createJsonArray(func: JSONArray.() -> Unit): JSONArray {
        val obj = JSONArray()
        func(obj)
        return obj
    }

    fun disconnectFromGame(id: Long) {
        emit("game/disconnect", createJsonObject {
            put("game_id", id)
        })
    }

    fun submitMove(move: Point, gameId: Long, gameAuth: String?) {
        val encodedMove = ('a' +  move.x) + "" + ('a' + move.y)
        emit("game/move", createJsonObject {
            put("auth", gameAuth)
            put("game_id", gameId)
            put("player_id", uiConfig?.user?.id)
            put("move", encodedMove)
        })
    }
}