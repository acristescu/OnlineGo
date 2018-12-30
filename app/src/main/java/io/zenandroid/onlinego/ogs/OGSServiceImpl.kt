package io.zenandroid.onlinego.ogs

import android.util.Log
import com.crashlytics.android.Crashlytics
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import com.google.android.gms.common.util.IOUtils
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.socket.client.Ack
import io.socket.client.IO
import io.socket.client.Manager
import io.socket.client.Socket
import io.socket.parser.IOParser
import io.zenandroid.onlinego.AndroidLoggingHandler
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.main.MainActivity
import io.zenandroid.onlinego.model.ogs.*
import io.zenandroid.onlinego.newchallenge.ChallengeParams
import io.zenandroid.onlinego.utils.PersistenceManager
import io.zenandroid.onlinego.utils.createJsonArray
import io.zenandroid.onlinego.utils.createJsonObject
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.ByteArrayInputStream
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import java.util.zip.GZIPInputStream


/**
 * Created by alex on 03/11/2017.
 */
object OGSServiceImpl : OGSService {

    val TAG: String = OGSServiceImpl::class.java.simpleName
    override var uiConfig: UIConfig? = null
    private val socket: Socket
    private val restApi: OGSRestAPI
    private val moshi = Moshi.Builder().add(Date::class.java, Rfc3339DateJsonAdapter().nullSafe()).build()
    private var authSent = false

    val cookieJar = PersistentCookieJar(SetCookieCache(), SharedPrefsCookiePersistor(OnlineGoApplication.instance))

    private var connectedToChallenges = false

    private val loggingAck = Ack {
        Log.i(TAG, "ack: $it")
    }

    fun fetchUIConfig(): Completable {
        return restApi.uiConfig().doOnSuccess(this::storeUIConfig).ignoreElement()
    }

    fun login(username: String, password: String): Completable {
        val ebi = "${Math.random().toString().split(".")[1]}.0.0.0.0.xxx.xxx.${Date().timezoneOffset + 13}"
        return restApi.login(CreateAccountRequest(username, password, "", ebi))
                .doOnSuccess {
                    //
                    // Hack alert!!! The server sometimes returns 200 even on wrong password :facepalm:
                    //
                    if (it.csrf_token.isNullOrBlank()) {
                        throw HttpException(Response.error<Any>(403, ResponseBody.create(null, "login failed")))
                    }
                }
                .doOnSuccess (this::storeUIConfig)
                .ignoreElement()
    }

    fun createAccount(username: String, password: String, email: String): Completable {
        val ebi = "${Math.random().toString().split(".")[1]}.0.0.0.0.xxx.xxx.${Date().timezoneOffset + 13}"
        return restApi.createAccount(CreateAccountRequest(username, password, email, ebi))
                .ignoreElement()
    }

    fun isLoggedIn() =
            (uiConfig != null) &&
                    cookieJar.loadForRequest(HttpUrl.parse("https://online-go.com/")!!)
                            .any { it.name() == "sessionid" }

    private fun storeUIConfig(uiConfig: UIConfig) {
        this.uiConfig = uiConfig
        authSent = false
        MainActivity.userId = uiConfig.user.id
        PersistenceManager.instance.storeUIConfig(uiConfig)
    }

    fun ensureSocketConnected() {
        socket.connect()
    }

    override fun challengeBot(challengeParams: ChallengeParams): Completable {
        val size = when(challengeParams.size) {
            "9x9" -> 9
            "13x13" -> 13
            "19x19" -> 19
            else -> 19
        }

        val color = when(challengeParams.color) {
            "Auto" -> "automatic"
            "Black" -> "black"
            "White" -> "white"
            else -> "automatic"
        }

        val timeControl = when(challengeParams.speed.toLowerCase()) {
            "correspondence" -> TimeControl(
                    system = "byoyomi",
                    time_control = "byoyomi",
                    speed = "correspondence",
                    main_time = 604800,
                    period_time = 86400,
                    periods = 5,
                    pause_on_weekends = true
            )
            "live" -> TimeControl(
                    system = "byoyomi",
                    time_control = "byoyomi",
                    speed = "live",
                    main_time = 600,
                    period_time = 30,
                    periods = 5,
                    pause_on_weekends = true
            )
            "blitz" -> TimeControl(
                    system = "byoyomi",
                    time_control = "byoyomi",
                    speed = "blitz",
                    main_time = 30,
                    period_time = 5,
                    periods = 5,
                    pause_on_weekends = true
            )
            else -> TimeControl()
        }
        val request = OGSChallengeRequest(
                initialized = false,
                aga_ranked = false,
                challenger_color = color,
                game = OGSChallengeRequest.Game(
                        handicap = if(challengeParams.handicap == "Auto") "-1" else challengeParams.handicap,
                        challenger_color = "black",
                        ranked = challengeParams.ranked,
                        name = "Bot game",
                        disable_analysis = false,
                        height = size,
                        width = size,
                        initial_state = null,
                        komi = null,
                        komi_auto = "automatic",
                        pause_on_weekends = true,
                        private = true,
                        rules = "japanese",
                        time_control = "byoyomi",
                        time_control_parameters = timeControl
                )
        )
        return restApi.challengePlayer(challengeParams.bot?.id!!, request)
    }

    override fun acceptChallenge(id: Long): Completable =
            restApi.acceptChallenge(id)

    override fun declineChallenge(id: Long): Completable =
            restApi.declineChallenge(id)

    override fun fetchGame(gameId: Long): Single<OGSGame> =
            restApi.fetchGame(gameId)
                    //
                    // Hack alert! just to keep us on our toes, the same thing is called
                    // different things when coming through the REST API and the Socket.IO one...
                    //
                    .doOnSuccess { it.json = it.gamedata }


    private val gameConnections = mutableMapOf<Long, GameConnection>()

    override fun connectToGame(id: Long): GameConnection {
        synchronized(gameConnections) {
            val connection = gameConnections[id] ?:
            GameConnection(id,
                    observeEvent("game/$id/gamedata").map { string -> moshi.adapter(GameData::class.java).fromJson(string.toString())!! },
                    observeEvent("game/$id/move").map { string -> moshi.adapter(Move::class.java).fromJson(string.toString())!! },
                    observeEvent("game/$id/clock").map { string -> moshi.adapter(OGSClock::class.java).fromJson(string.toString()) },
                    observeEvent("game/$id/phase").map { string -> Phase.valueOf(string.toString().toUpperCase(Locale.ENGLISH).replace(' ', '_')) },
                    observeEvent("game/$id/removed_stones").map { string -> moshi.adapter(RemovedStones::class.java).fromJson(string.toString()) },
                    observeEvent("game/$id/chat").map { string -> moshi.adapter(Chat::class.java).fromJson(string.toString()) },
                    observeEvent("game/$id/undo_requested").map { string -> string.toString().toInt() }
            ).apply {
                emitGameConnection(id)
                gameConnections[id] = this
            }
            connection.incrementCounter()
            return connection
        }
    }

    private fun emitGameConnection(id: Long) {
        emit("game/connect", createJsonObject {
            put("chat", true)
            put("game_id", id)
            put("player_id", uiConfig?.user?.id)
        })
    }

    fun connectToActiveGames(): Flowable<OGSGame> {
        return observeEvent("active_game")
                .map { string -> moshi.adapter(OGSGame::class.java).fromJson(string.toString()) as OGSGame }
    }

    fun connectToUIPushes(): Flowable<UIPush> {
        val returnVal = observeEvent("ui-push")
                .map { string -> moshi.adapter(UIPush::class.java).fromJson(string.toString()) as UIPush }

        emit("ui-pushes/subscribe", createJsonObject {
            put("channel", "undefined")
        })

        return returnVal
    }

    fun connectToBots(): Flowable<List<Bot>> =
            observeEvent("active-bots")
                    .map { string ->
                        val json = JSONObject(string.toString())
                        val retval = mutableListOf<Bot>()
                        for (key in json.keys()) {
                            moshi.adapter(Bot::class.java).fromJson(json[key].toString())?.let {
                                retval.add(it)
                            }
                        }
                        return@map retval as List<Bot>
                    }

    fun listenToNewAutomatchNotifications(): Flowable<OGSAutomatch> =
            observeEvent("automatch/entry")
                    .map { string -> moshi.adapter(OGSAutomatch::class.java).fromJson(string.toString()) as OGSAutomatch }

    fun listenToCancelAutomatchNotifications(): Flowable<OGSAutomatch> =
            observeEvent("automatch/cancel")
                    .map { string -> moshi.adapter(OGSAutomatch::class.java).fromJson(string.toString()) as OGSAutomatch }

    fun listenToStartAutomatchNotifications(): Flowable<OGSAutomatch> =
            observeEvent("automatch/start")
                    .map { string -> moshi.adapter(OGSAutomatch::class.java).fromJson(string.toString()) as OGSAutomatch }


    fun connectToAutomatch() {
        emit("automatch/list", null)
    }

    fun connectToServerNotifications(): Flowable<JSONObject> =
            observeEvent("notification")
                    .map { JSONObject(it.toString()) }
                    .doOnSubscribe {
                        emit("notification/connect", createJsonObject {
                            put("player_id", uiConfig?.user?.id)
                            put("auth", uiConfig?.notification_auth)
                        })
                    }.doOnCancel {
                        emit("notification/disconnect", "")
                    }

    fun connectToChallenges(): Flowable<SeekGraphChallenge> {
        val listMyData = Types.newParameterizedType(List::class.java, SeekGraphChallenge::class.java)
        val adapter:JsonAdapter<List<SeekGraphChallenge>> = moshi.adapter(listMyData)

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

    internal fun emit(event: String, params:Any?) {
        ensureSocketConnected()
        Log.i(TAG, "Emit: $event with params $params thread = ${Thread.currentThread().id}")
        socket.emit(event, params, loggingAck)
    }

    override fun resendAuth() {
        val obj = JSONObject()
        obj.put("player_id", uiConfig?.user?.id)
        obj.put("username", uiConfig?.user?.username)
        obj.put("auth", uiConfig?.chat_auth)
        Log.i(TAG, "Emit: authenticate with params obj")
        socket.emit("authenticate", obj, loggingAck)
        authSent = true
    }

    private fun observeEvent(event: String): Flowable<Any> {
        Log.i(TAG, "Listening for event: $event")
        return Flowable.create({ emitter ->
            socket.on(event) { params ->
                Log.i(TAG, "Received event: $event, ${params[0]}")
                emitter.onNext(params[0])
            }

            emitter.setCancellable {
                Log.i(TAG, "Unregistering for event: $event")
                socket.off(event)
            }
        }
        , BackpressureStrategy.BUFFER)
    }

    override fun startAutomatch(sizes: List<Size>, speed: Speed) : String {
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

    override fun cancelAutomatch(automatch: OGSAutomatch) {
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
                args -> emitter.onSuccess(moshi.adapter(GameList::class.java).fromJson(args[0].toString()) as GameList )
            })
        }
    }

    fun disconnect() {
        ActiveGameRepository.unsubscribe()
        BotsRepository.unsubscribe()
        AutomatchRepository.unsubscribe()
        ChallengesRepository.unsubscribe()
        NotificationsRepository.unsubscribe()
//        socket.off()
        socket.disconnect()
    }

    fun logOut() {
        uiConfig = null
        PersistenceManager.instance.deleteUIConfig()
        cookieJar.clear()
        disconnect()
    }

    init {
        uiConfig = PersistenceManager.instance.getUIConfig()
        MainActivity.userId = uiConfig?.user?.id
        Crashlytics.log("Startup")

        val httpClient = OkHttpClient.Builder()
                .cookieJar(cookieJar)
                .addNetworkInterceptor { chain ->
                    var request = chain.request()
                    val csrftoken = cookieJar.loadForRequest(request.url()).firstOrNull { it.name() == "csrftoken" }?.value()
                    request = request.newBuilder()
                            .addHeader("referer", "https://online-go.com/overview")
                            .apply { csrftoken?.let { addHeader("x-csrftoken",  it) } }
                            .build()

                    val hasSessionCookieInJar = cookieJar.loadForRequest(request.url()).any { it.name() == "sessionid" }
                    val isSessionCookieExpired = cookieJar.loadForRequest(request.url()).any { it.name() == "sessionid" && it.expiresAt() < System.currentTimeMillis() }

                    val response = chain.proceed(request)

                    if(response.isSuccessful) {
                        Crashlytics.log(Log.INFO, TAG, "${request.method()} ${request.url()} -> ${response.code()}")
                    } else {
                        val sessionCookieSent = request.header("Cookie")?.contains("sessionid=") == true
                        val bodyBytes = response.peekBody(1024 * 1024).bytes()
                        val body =
                                if(IOUtils.isGzipByteBuffer(bodyBytes))
                                    String(IOUtils.toByteArray(GZIPInputStream(ByteArrayInputStream(bodyBytes))))
                                else
                                    String(bodyBytes)

                        val csrftokenInfo = if(csrftoken == null) "no csrf" else "csrf present"
                        val cookieJarInfo = when {
                            isSessionCookieExpired -> "expired session cookie"
                            hasSessionCookieInJar -> "session cookie in jar"
                            else -> "no session cookie"
                        }
                        val sessionCookieInfo = if(sessionCookieSent) "session cookie sent" else "session cookie not sent"
                        Crashlytics.log(Log.ERROR, TAG, "${request.method()} ${request.url()} -> ${response.code()} ${response.message()} [$cookieJarInfo] [$csrftokenInfo] [$sessionCookieInfo] $body")

                        if(!sessionCookieSent && hasSessionCookieInJar && !isSessionCookieExpired) {
                            Crashlytics.logException(Exception("Possible cookie jar problem"))
                        }
                    }
                    response
                }
                .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                .build()
        restApi = Retrofit.Builder()
                .baseUrl("https://online-go.com/")
                .client(httpClient)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(OGSRestAPI::class.java)

        socket = IO.socket("https://online-go.com", IO.Options().apply {
            transports = arrayOf("websocket")
            reconnection = true
            reconnectionDelay = 750
            reconnectionDelayMax = 10000
        })

        socket.on(Socket.EVENT_CONNECT) {
            Logger.getLogger(TAG).warning("socket connect id=${socket.id()}")
            onSockedConnected()
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
        Logger.getLogger(Socket::class.java.name).level = Level.FINEST
        Logger.getLogger(Manager::class.java.name).level = Level.FINEST
        Logger.getLogger(io.socket.engineio.client.Socket::class.java.name).level = Level.FINEST
        Logger.getLogger(IOParser::class.java.name).level = Level.FINEST
    }

    override fun deleteNotification(notificationId: String) {
        emit("notification/delete", createJsonObject {
            put("player_id", uiConfig?.user?.id)
            put("auth", uiConfig?.notification_auth)
            put("notification_id", notificationId)
        })
    }

    private fun onSockedConnected() {
        authSent = false
        resendAuth()
        ActiveGameRepository.subscribe()
        BotsRepository.subscribe()
        AutomatchRepository.subscribe()
        ChallengesRepository.subscribe()
        NotificationsRepository.subscribe()
        synchronized(gameConnections) {
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

    fun disconnectFromGame(id: Long) {
        synchronized(gameConnections) {
            gameConnections.remove(id)
            emit("game/disconnect", createJsonObject {
                put("game_id", id)
            })
        }
    }

    override fun fetchActiveGames(): Single<List<OGSGame>> =
            restApi.fetchOverview()
                .map { it -> it.active_games }
                .map { it ->
                    for (game in it) {
                        game.json?.clock?.current_player?.let {
                            game.player_to_move = it
                        }
                        game.json?.handicap?.let {
                            game.handicap = it
                        }
                    }
                    it
                }

    override fun fetchChallenges(): Single<List<OGSChallenge>> =
            restApi.fetchChallenges().map { it.results }

    override fun fetchHistoricGames(): Single<List<OGSGame>> =
            Single.defer {
                uiConfig?.user?.id?.let { restApi.fetchPlayerFinishedGames(it) }
                        ?: Single.error(RuntimeException("Null UI Config"))
            }.map { it.results }
}