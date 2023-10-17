package io.zenandroid.onlinego.data.ogs

import android.preference.PreferenceManager
import com.squareup.moshi.Moshi
import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.data.model.local.Puzzle
import io.zenandroid.onlinego.data.model.local.PuzzleCollection
import io.zenandroid.onlinego.data.model.ogs.CreateAccountRequest
import io.zenandroid.onlinego.data.model.ogs.Glicko2History
import io.zenandroid.onlinego.data.model.ogs.JosekiPosition
import io.zenandroid.onlinego.data.model.ogs.OGSChallenge
import io.zenandroid.onlinego.data.model.ogs.OGSChallengeRequest
import io.zenandroid.onlinego.data.model.ogs.OGSGame
import io.zenandroid.onlinego.data.model.ogs.OGSPlayer
import io.zenandroid.onlinego.data.model.ogs.PagedResult
import io.zenandroid.onlinego.data.model.ogs.PasswordBody
import io.zenandroid.onlinego.data.model.ogs.OGSPuzzle
import io.zenandroid.onlinego.data.model.ogs.OGSPuzzleCollection
import io.zenandroid.onlinego.data.model.ogs.PuzzleRating
import io.zenandroid.onlinego.data.model.ogs.PuzzleSolution
import io.zenandroid.onlinego.data.model.ogs.VersusStats
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import io.zenandroid.onlinego.data.model.ogs.ChallengeParams
import io.zenandroid.onlinego.utils.CountingIdlingResource
import io.zenandroid.onlinego.utils.microsToISODateTime
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.util.Date
import java.util.concurrent.TimeUnit

private const val TAG = "OGSRestService"
private const val OGS_EBI = "OGS_EBI"

class OGSRestService(
        val moshi: Moshi,
        val restApi: OGSRestAPI,
        val idlingResource: CountingIdlingResource,
        val userSessionRepository: UserSessionRepository,
) {
    private val ebi by lazy {
        val prefs = PreferenceManager.getDefaultSharedPreferences(OnlineGoApplication.instance)!!
        if (prefs.contains(OGS_EBI)) {
            prefs.getString(OGS_EBI, "")!!
        } else {
            val newEbi = "${Math.random().toString().split(".")[1]}.0.0.0.0.xxx.xxx.${Date().timezoneOffset + 13}"
            prefs.edit().putString(OGS_EBI, newEbi).apply()
            newEbi
        }
    }

    fun fetchUIConfig(): Completable {
        return restApi.uiConfig().doOnSuccess(userSessionRepository::storeUIConfig).ignoreElement()
    }

    fun login(username: String, password: String): Completable {
        idlingResource.increment()
        return restApi.login(CreateAccountRequest(username, password, "", ebi))
                .doOnSuccess {
                    //
                    // Hack alert!!! The server sometimes returns 200 even on wrong password :facepalm:
                    //
                    if (it.csrf_token.isNullOrBlank() || it.redirect != null) {
                        throw HttpException(Response.error<Any>(403, "login failed".toResponseBody()))
                    }
                }
                .doOnSuccess (userSessionRepository::storeUIConfig)
                .ignoreElement()
                .doAfterTerminate { idlingResource.decrement() }
    }

    fun loginWithGoogle(code: String): Completable {
        return restApi.initiateGoogleAuthFlow()
                .map {
                    if(it.code() != 302) {
                        throw Exception("got code ${it.code()} instead of 302")
                    }
                    it.headers().forEach {
                        if(it.first == "location") {
                            return@map "&state=([^&]*)&".toRegex().find(it.second)!!.groupValues[1]
                        }
                    }
                    throw Exception("Cannot log in (can't follow redirect)")
                }
                .flatMap { state -> restApi.loginWithGoogleAuth(code, state) }
                .flatMap {
                    if(it.code() != 302) {
                        throw Exception("got code ${it.code()} instead of 302")
                    }
                    it.headers().forEach {
                        if(it.first == "location" && it.second == "/") {
                            return@flatMap restApi.uiConfig()
                        }
                    }
                    throw Exception ("Login failed")
                }
                .doOnSuccess(userSessionRepository::storeUIConfig)
                .ignoreElement()
    }

    fun createAccount(username: String, password: String, email: String): Completable {
        return restApi.createAccount(CreateAccountRequest(username, password, email, ebi))
                .ignoreElement()
    }

    fun challengeBot(challengeParams: ChallengeParams): Completable {
        val size = when(challengeParams.size) {
            "9x9", "9×9" -> 9
            "13x13", "13×13" -> 13
            "19x19", "19×19" -> 19
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
                    pause_on_weekends = false
            )
            "blitz" -> TimeControl(
                    system = "byoyomi",
                    time_control = "byoyomi",
                    speed = "blitz",
                    main_time = 30,
                    period_time = 5,
                    periods = 5,
                    pause_on_weekends = false
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
                        pause_on_weekends = timeControl.pause_on_weekends == true,
                        private = challengeParams.private,
                        rules = "japanese",
                        time_control = "byoyomi",
                        time_control_parameters = timeControl
                )
        )
        return when {
            challengeParams.opponent != null -> {
                restApi.challengePlayer(challengeParams.opponent?.id!!, request)
            }
            else -> {
                restApi.openChallenge(request)
            }
        }
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

    fun fetchHistoricGamesBefore(beforeDate: Long?): Single<List<OGSGame>> =
            if(beforeDate == null) {
                restApi.fetchPlayerFinishedGames(userSessionRepository.userId!!)
            } else {
                restApi.fetchPlayerFinishedBeforeGames(userSessionRepository.userId!!, 10, beforeDate.microsToISODateTime(), 1)
            }.map { it.results }

    fun fetchHistoricGamesAfter(afterDate: Long?): Single<List<OGSGame>> =
            if(afterDate == null) {
                restApi.fetchPlayerFinishedGames(userSessionRepository.userId!!)
            } else {
                restApi.fetchPlayerFinishedAfterGames(userSessionRepository.userId!!, 10, afterDate.microsToISODateTime(), 1)
            }.map { it.results }

    fun searchPlayers(query: String): Single<List<OGSPlayer>> =
            restApi.omniSearch(query).map { it.players }

    fun getJosekiPositions(id: Long?): Single<List<JosekiPosition>> =
            restApi.getJosekiPositions(id?.toString() ?: "root")

    fun getPlayerProfile(id: Long): Single<OGSPlayer> =
            restApi.getPlayerProfile(id)

    suspend fun getPlayerProfileAsync(id: Long): OGSPlayer =
            restApi.getPlayerProfileAsync(id)

    suspend fun getPlayerStatsAsync(id: Long): Glicko2History {
        return getPlayerStatsAsync(id, "overall", 0)
    }

    suspend fun getPlayerStatsAsync(id: Long, speed: String, size: Int): Glicko2History {
        return restApi.getPlayerStatsAsync(id, speed, size)
    }

    suspend fun getPlayerVersusStats(id: Long): VersusStats {
        return restApi.getPlayerFullProfileAsync(id).vs
    }

    fun getPuzzleCollections(minCount: Int? = null, namePrefix: String? = null): Flowable<List<PuzzleCollection>> {
        var page = 0

        fun fetchPage(): Single<PagedResult<OGSPuzzleCollection>> = restApi.getPuzzleCollections(
            minimumCount = minCount ?: 0,
            namePrefix = namePrefix ?: "",
            page = ++page
        )

        fun unfold(result: Single<PagedResult<OGSPuzzleCollection>>): Observable<List<OGSPuzzleCollection>> {
            return result.toObservable().flatMap { pre ->
                Observable.just(pre.results).let {
                    if (pre.next == null) {
                        it
                    } else {
                        val wait = Observable.timer(5, TimeUnit.SECONDS).take(1)
                        it.concatWith(wait.map { emptyList<OGSPuzzleCollection>() }.ignoreElements())
                            .concatWith(unfold(fetchPage()))
                    }
                }
            }
        }

        return unfold(fetchPage()).toFlowable(BackpressureStrategy.BUFFER)
            .map { it.map(PuzzleCollection::fromOGSPuzzleCollection) }
    }

    fun getPuzzleCollection(id: Long): Single<PuzzleCollection> =
        restApi.getPuzzleCollection(collectionId = id)
            .map { PuzzleCollection.fromOGSPuzzleCollection(it) }

    fun getPuzzleCollectionContents(id: Long): Single<List<Puzzle>> =
        restApi.getPuzzleCollectionContents(collectionId = id)
            .map { it.map(Puzzle::fromOGSPuzzle) }

    fun getPuzzle(id: Long): Single<Puzzle> =
        restApi.getPuzzle(puzzleId = id)
            .map { Puzzle.fromOGSPuzzle(it) }

    fun getPuzzleSolutions(id: Long): Single<List<PuzzleSolution>> {
        var page = 0

        fun fetchPage(): Single<PagedResult<PuzzleSolution>> = restApi.getPuzzleSolutions(
            puzzleId = id,
            playerId = userSessionRepository.userId!!,
            page = ++page
        )

        fun unfold(result: Single<PagedResult<PuzzleSolution>>): Single<List<PuzzleSolution>> {
            return result.flatMap { pre ->
                if (pre.next == null) {
                    Single.just(pre.results)
                } else {
                    unfold(fetchPage()).map { pre.results.plus(it) }
                }
            }
        }

        return unfold(fetchPage())
    }

    fun getPuzzleRating(id: Long): Single<PuzzleRating> =
        restApi.getPuzzleRating(puzzleId = id)

    fun markPuzzleSolved(id: Long, solution: PuzzleSolution): Completable =
        restApi.markPuzzleSolved(puzzleId = id, request = solution)

    fun ratePuzzle(id: Long, rating: Int): Completable =
        restApi.ratePuzzle(puzzleId = id, request = PuzzleRating(rating = rating))

  suspend fun deleteMyAccount(password: String) {
    return restApi.deleteAccount(
      userSessionRepository.userId!!,
      PasswordBody(password)
    )
  }
}
