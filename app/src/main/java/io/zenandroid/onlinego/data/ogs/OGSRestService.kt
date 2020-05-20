package io.zenandroid.onlinego.data.ogs

import android.util.Log
import com.crashlytics.android.Crashlytics
import com.facebook.stetho.okhttp3.StethoInterceptor
import com.google.android.gms.common.util.IOUtils
import com.squareup.moshi.Moshi
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.BuildConfig
import io.zenandroid.onlinego.data.model.ogs.*
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import io.zenandroid.onlinego.ui.screens.newchallenge.ChallengeParams
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.ByteArrayInputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.GZIPInputStream

private const val TAG = "OGSRestService"

class OGSRestService(
        val moshi: Moshi,
        val restApi: OGSRestAPI,
        val userSessionRepository: UserSessionRepository
) {

    fun fetchUIConfig(): Completable {
        return restApi.uiConfig().doOnSuccess(userSessionRepository::storeUIConfig).ignoreElement()
    }

    fun login(username: String, password: String): Completable {
        val ebi = "${Math.random().toString().split(".")[1]}.0.0.0.0.xxx.xxx.${Date().timezoneOffset + 13}"
        return restApi.login(CreateAccountRequest(username, password, "", ebi))
                .doOnSuccess {
                    //
                    // Hack alert!!! The server sometimes returns 200 even on wrong password :facepalm:
                    //
                    if (it.csrf_token.isNullOrBlank()) {
                        throw HttpException(Response.error<Any>(403, "login failed".toResponseBody()))
                    }
                }
                .doOnSuccess (userSessionRepository::storeUIConfig)
                .ignoreElement()
    }

    fun createAccount(username: String, password: String, email: String): Completable {
        val ebi = "${Math.random().toString().split(".")[1]}.0.0.0.0.xxx.xxx.${Date().timezoneOffset + 13}"
        return restApi.createAccount(CreateAccountRequest(username, password, email, ebi))
                .ignoreElement()
    }

    fun challengeBot(challengeParams: ChallengeParams): Completable {
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
                        ranked = challengeParams.ranked,
                        name = if (challengeParams.opponent?.ui_class != null &&
                                challengeParams.opponent?.ui_class!!.startsWith("bot")) "Bot Match"
                        else "Friendly Match",
                        disable_analysis = challengeParams.disable_analysis,
                        height = size,
                        width = size,
                        initial_state = null,
                        komi = null,
                        komi_auto = "automatic",
                        pause_on_weekends = true,
                        private = challengeParams.private,
                        rules = "japanese",
                        time_control = "byoyomi",
                        time_control_parameters = timeControl
                )
        )
        return restApi.challengePlayer(challengeParams.opponent?.id!!, request)
    }

    fun acceptChallenge(id: Long): Completable =
            restApi.acceptChallenge(id)

    fun declineChallenge(id: Long): Completable =
            restApi.declineChallenge(id)

    fun fetchGame(gameId: Long): Single<OGSGame> =
            restApi.fetchGame(gameId)
                    //
                    // Hack alert! just to keep us on our toes, the same thing is called
                    // different things when coming through the REST API and the Socket.IO one...
                    //
                    .doOnSuccess { it.json = it.gamedata }

    fun fetchActiveGames(): Single<List<OGSGame>> =
            restApi.fetchOverview()
                    .map { it.active_games }
                    .map {
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

    fun fetchChallenges(): Single<List<OGSChallenge>> =
            restApi.fetchChallenges().map { it.results }

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.UK);

    fun fetchHistoricGamesBefore(beforeDate: Long?): Single<List<OGSGame>> =
            if(beforeDate == null) {
                restApi.fetchPlayerFinishedGames(userSessionRepository.userId!!)
            } else {
                restApi.fetchPlayerFinishedBeforeGames(userSessionRepository.userId!!, 10, dateFormatter.format(Date(beforeDate)), 1)
            }.map { it.results }

    fun fetchHistoricGamesAfter(afterDate: Long?): Single<List<OGSGame>> =
            if(afterDate == null) {
                restApi.fetchPlayerFinishedGames(userSessionRepository.userId!!)
            } else {
                restApi.fetchPlayerFinishedAfterGames(userSessionRepository.userId!!, 10, dateFormatter.format(Date(afterDate)), 1)
            }.map { it.results }

    fun searchPlayers(query: String): Single<List<OGSPlayer>> =
            restApi.omniSearch(query).map { it.players }

    fun getJosekiPositions(id: Long?): Single<List<JosekiPosition>> =
            restApi.getJosekiPositions(id?.toString() ?: "root")

}