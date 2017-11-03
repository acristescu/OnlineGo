package io.zenandroid.onlinego.ogs

import com.squareup.moshi.Moshi
import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.parser.IOParser
import io.zenandroid.onlinego.AndroidLoggingHandler
import io.zenandroid.onlinego.login.createJsonArray
import io.zenandroid.onlinego.login.createJsonObject
import io.zenandroid.onlinego.model.ogs.LoginToken
import io.zenandroid.onlinego.model.ogs.UIConfig
import okhttp3.OkHttpClient
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
    private var uiConfig: UIConfig? = null
    private var socket: Socket? = null

    private lateinit var api: OGSRestAPI

    fun isLoggedIn(): Boolean {
        return token != null
    }

    fun login(username: String, password: String): Completable {
        return api.login(username, password)
                .flatMap ({ token ->
                    println(token)
                    this.token = token
                    api.uiConfig()
                }).flatMapCompletable ({ uiConfig ->
                    this.uiConfig = uiConfig
                    initSocket()
                    Completable.complete()
                })
    }

    private fun initSocket() {
        val options = IO.Options()
        options.transports = arrayOf("websocket")
        socket = IO.socket("https://online-go.com", options)


        AndroidLoggingHandler.reset(AndroidLoggingHandler())
//                    Logger.getLogger(Socket::class.java.name).level = Level.FINEST
//                    Logger.getLogger(Manager::class.java.name).level = Level.FINEST
        Logger.getLogger(IOParser::class.java.name).level = Level.FINEST

        socket!!.on(Socket.EVENT_CONNECT) {
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

        socket!!.connect()

        var obj = JSONObject()
        obj.put("player_id", uiConfig!!.user.id)
        obj.put("username", uiConfig!!.user.username)
        obj.put("auth", uiConfig!!.chat_auth)
        socket!!.emit("authenticate", obj)


        //                    socket.emit("gamelist/count/subscribe")

        socket!!.emit("gamelist/query", createJsonObject {
            put("list", "live")
            put("sort_by", "rank")
            put("from", 0)
            put("limit", 9)
        })

//                    obj = JSONObject()
//                    obj.put("chat", 0)
//                    obj.put("game_id", 10493024)
//                    obj.put("player_id", 89194)
//                    socket.emit("game/connect", obj)



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
//                    socket!!.emit("automatch/find_match", json)

    }

    private fun observeEvent(event: String): Flowable<Any> {
        return Flowable.create({ emitter ->
            socket!!.on(event, {
                params -> emitter.onNext(params[0])
            })
        }
        , BackpressureStrategy.BUFFER)
    }

    fun registerSeekgraph(): Flowable<Any> {
        return observeEvent("seekgraph/global").doOnSubscribe({
            socket!!.emit("seek_graph/connect", createJsonObject {
                put("channel", "global")
            })
        })

    }

    fun disconnect() {
        socket?.disconnect()
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
        val m = Moshi.Builder()
                .build()
        api = Retrofit.Builder()
                .baseUrl("https://online-go.com/")
                .client(httpClient)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(OGSRestAPI::class.java)
    }


}