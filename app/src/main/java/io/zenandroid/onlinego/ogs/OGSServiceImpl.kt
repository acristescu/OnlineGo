package io.zenandroid.onlinego.ogs

import android.util.Log
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.socket.client.Ack
import io.socket.client.IO
import io.socket.client.Socket
import io.zenandroid.onlinego.AndroidLoggingHandler
import io.zenandroid.onlinego.main.MainActivity
import io.zenandroid.onlinego.model.ogs.*
import io.zenandroid.onlinego.utils.PersistenceManager
import io.zenandroid.onlinego.utils.createJsonArray
import io.zenandroid.onlinego.utils.createJsonObject
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
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
class OGSServiceImpl private constructor(): OGSService {

    companion object {
        @JvmField
        val instance = OGSServiceImpl()
        val TAG: String = OGSServiceImpl::class.java.simpleName
    }

    private var token: LoginToken? = null
    override var uiConfig: UIConfig? = null
    private val socket: Socket
    private var tokenExpiry: Date
    override val restApi: OGSRestAPI
    private val moshi = Moshi.Builder().build()
    private var authSent = false

    private val loggingAck = Ack {
        Log.i(TAG, "ack: $it")
    }

    fun login(username: String, password: String): Completable {
        return restApi.login(username, password)
                .doOnSuccess(this::storeToken)
                .flatMap { restApi.uiConfig() }
                .doOnSuccess(this::storeUIConfig)
                .toCompletable()
    }

    fun createAccount(username: String, password: String, email: String): Completable {
        val ebi = "${Math.random().toString().split(".")[1]}.0.0.0.0.xxx.xxx.${Date().timezoneOffset + 13}"
        return restApi.createAccount(CreateAccountRequest(username, password, email, ebi))
                .toCompletable()
    }

    fun loginWithToken(): Completable {
        if(token == null || token?.access_token == null || token?.refresh_token == null) {
            //
            // No token, we need to log in with password
            //
            return Completable.error(Throwable())
        }

        Log.d(TAG, "Token expiry $tokenExpiry")
        val tokenSource: Single<LoginToken>

        if(tokenExpiry.before(Date())) {
            //
            // We do have a token but it's expired, we need to refresh everything
            //
            tokenSource = restApi.refreshToken(token!!.refresh_token).doOnSuccess(this::storeToken)
            uiConfig = null
        } else {
            //
            // Just use the token we have
            //
            tokenSource = Single.just(token)
        }

        val uiConfigSource = uiConfig?.let { uiConfig ->
            tokenSource.flatMap { Single.just(uiConfig)}
        } ?: tokenSource.flatMap { restApi.uiConfig() }.doOnSuccess(this::storeUIConfig)

       return uiConfigSource
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
        authSent = false
        MainActivity.userId = uiConfig.user.id
        PersistenceManager.instance.storeUIConfig(uiConfig)
    }

    fun ensureSocketConnected() {
        if(!socket.connected()) {
            socket.connect()
        }

        if(!authSent) {
            resendAuth()
        }
    }

    override fun fetchGame(gameId: Long): Single<Game> = loginWithToken().andThen(restApi.fetchGame(gameId))

    override fun connectToGame(id: Long): GameConnection {
        val connection = GameConnection(id)

        connection.gameData = observeEvent("game/$id/gamedata")
                    .map { string -> moshi.adapter(GameData::class.java).fromJson(string.toString()) }
                    .doOnNext {
                        connection.gameAuth = it.auth
                    }

        connection.moves = observeEvent("game/$id/move")
                    .map { string -> moshi.adapter(Move::class.java).fromJson(string.toString()) }
        connection.clock = observeEvent("game/$id/clock")
                    .map { string -> moshi.adapter(Clock::class.java).fromJson(string.toString()) }
        connection.phase = observeEvent("game/$id/phase")
                    .map { string -> Game.Phase.valueOf(string.toString().toUpperCase().replace(' ', '_')) }
        connection.removedStones = observeEvent("game/$id/removed_stones")
                    .map { string -> moshi.adapter(RemovedStones::class.java).fromJson(string.toString()) }

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

        emit("notification/disconnect", "")
        emit("notification/connect", createJsonObject {
            put("player_id", uiConfig?.user?.id)
            put("auth", uiConfig?.notification_auth)
        })

        return returnVal
    }

    fun connectToChallenges(): Flowable<Challenge> {
        val listMyData = Types.newParameterizedType(List::class.java, Challenge::class.java)
        val adapter:JsonAdapter<List<Challenge>> = moshi.adapter(listMyData)

        val returnVal = observeEvent("seekgraph/global")
                .map { string -> adapter.fromJson(string.toString()) }
                .flatMapIterable { it -> it }
                .doOnCancel {
                    emit("seek_graph/disconnect", createJsonObject {
                        put("channel", "global")
                    })
                }

        emit("seek_graph/connect", createJsonObject {
            put("channel", "global")
        })

        return returnVal
    }

    internal fun emit(event: String, params:Any?) {
        ensureSocketConnected()
        Log.i(TAG, "Emit: $event with params $params")
        socket.emit(event, params, loggingAck)
    }

    override fun resendAuth() {
        val obj = JSONObject()
        obj.put("player_id", uiConfig?.user?.id)
        obj.put("username", uiConfig?.user?.username)
        obj.put("auth", uiConfig?.chat_auth)
        socket.emit("authenticate", obj, loggingAck)
        authSent = true
    }

    private fun observeEvent(event: String): Flowable<Any> {
        Log.i(TAG, "Listening for event: $event")
        return Flowable.create({ emitter ->
            socket.on(event, {
                params ->
                Log.i(TAG, "Received event: $event, ${params[0]}")
                    emitter.onNext(params[0])
            })

            emitter.setCancellable({
                Log.i(TAG, "Unregistering for event: $event")
                socket.off(event)
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

    override fun startGameSearch(size: Size, speed: Speed) : AutomatchChallenge {
        val uuid = UUID.randomUUID().toString()
        val startFlowable = observeEvent("automatch/start")
                .map { string -> moshi.adapter(AutomatchChallengeSuccess::class.java).fromJson(string.toString()) }
        val challenge = AutomatchChallenge(uuid, startFlowable)

        val json = createJsonObject {
            put("uuid", uuid)
            put("size_speed_options", createJsonArray {
                put(createJsonObject {
                    put("size", size.getText())
                    put("speed", speed.getText())
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
        return challenge
    }

    override fun cancelAutomatchChallenge(challenge: AutomatchChallenge) {
        emit("automatch/cancel", challenge.uuid)
        socket.off("automatch/start")
    }

    fun fetchGameList(): Single<GameList> {
        ensureSocketConnected()
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
                    Log.i(TAG, "${request.method()} ${request.url()} -> ${response.code()} ${response.message()}")
                    response
                }
                .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                .build()
        restApi = Retrofit.Builder()
                .baseUrl("https://online-go.com/")
                .client(httpClient)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(OGSRestAPI::class.java)

        uiConfig = PersistenceManager.instance.getUIConfig()
        MainActivity.userId = uiConfig?.user?.id
        token = PersistenceManager.instance.getToken()
        tokenExpiry = PersistenceManager.instance.getTokenExpiry()

        val options = IO.Options()
        options.transports = arrayOf("websocket")
        socket = IO.socket("https://online-go.com", options)

        socket.on(Socket.EVENT_CONNECT) {
            Logger.getLogger(TAG).warning("socket connect id=${socket.id()}")
        }.on(Socket.EVENT_DISCONNECT) {
            Logger.getLogger(TAG).warning("socket disconnect id=${socket.id()}")
        }.on(Socket.EVENT_CONNECT_ERROR) {
            Logger.getLogger(TAG).warning("socket connect error id=${socket.id()}")
        }.on(Socket.EVENT_ERROR) {
            Logger.getLogger(TAG).warning("socket error id=${socket.id()}")
        }.on(Socket.EVENT_CONNECT_TIMEOUT) {
            Logger.getLogger(TAG).warning("socket connect timeout id=${socket.id()}")
        }.on(Socket.EVENT_RECONNECT) {
            Logger.getLogger(TAG).warning("socket reconnect id=${socket.id()}")
        }.on(Socket.EVENT_MESSAGE) {
            Logger.getLogger(TAG).warning(it.toString())
        }.on(Socket.EVENT_CONNECTING) {
            Logger.getLogger(TAG).warning("socket connecting id=${socket.id()}")
        }.on(Socket.EVENT_CONNECT_ERROR) {
            Logger.getLogger(TAG).severe("socket connect error id=${socket.id()}")
        }.on(Socket.EVENT_PING) {
            Logger.getLogger(TAG).warning("ping id=${socket.id()}")
        }.on(Socket.EVENT_PONG) {
            Logger.getLogger(TAG).warning("pong id=${socket.id()}")
        }

        AndroidLoggingHandler.reset(AndroidLoggingHandler())
//                    Logger.getLogger(Socket::class.java.name).level = Level.FINEST
//                    Logger.getLogger(Manager::class.java.name).level = Level.FINEST
        Logger.getLogger(io.socket.engineio.client.Socket::class.java.name).level = Level.FINEST
//        Logger.getLogger(IOParser::class.java.name).level = Level.FINEST
    }

    fun disconnectFromGame(id: Long) {
        emit("game/disconnect", createJsonObject {
            put("game_id", id)
        })
    }

    override fun fetchActiveGames(): Single<List<Game>> =
            loginWithToken().andThen(
                    restApi.fetchOverview()
                            .map { it -> it.active_games }
                            .map { it ->
                                for (game in it) {
                                    game.json?.clock?.current_player?.let {
                                        game.player_to_move = it
                                    }
                                }
                                it
                            }
            )

    override fun fetchHistoricGames(): Single<List<Game>> =
        loginWithToken().andThen(
                restApi.fetchPlayerFinishedGames(uiConfig?.user?.id!!)
                        .map { it -> it.results }
        )

}