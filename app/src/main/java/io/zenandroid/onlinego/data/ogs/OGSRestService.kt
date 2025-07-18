package io.zenandroid.onlinego.data.ogs

import android.preference.PreferenceManager
import com.squareup.moshi.Moshi
import io.reactivex.Completable
import io.reactivex.Single
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.data.model.local.Puzzle
import io.zenandroid.onlinego.data.model.local.PuzzleCollection
import io.zenandroid.onlinego.data.model.ogs.CannedMessages
import io.zenandroid.onlinego.data.model.ogs.ChallengeParams
import io.zenandroid.onlinego.data.model.ogs.CreateAccountRequest
import io.zenandroid.onlinego.data.model.ogs.Glicko2History
import io.zenandroid.onlinego.data.model.ogs.JosekiPosition
import io.zenandroid.onlinego.data.model.ogs.OGSChallenge
import io.zenandroid.onlinego.data.model.ogs.OGSChallengeRequest
import io.zenandroid.onlinego.data.model.ogs.OGSGame
import io.zenandroid.onlinego.data.model.ogs.OGSPlayer
import io.zenandroid.onlinego.data.model.ogs.PasswordBody
import io.zenandroid.onlinego.data.model.ogs.PuzzleRating
import io.zenandroid.onlinego.data.model.ogs.PuzzleSolution
import io.zenandroid.onlinego.data.model.ogs.VersusStats
import io.zenandroid.onlinego.data.model.ogs.Warning
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import io.zenandroid.onlinego.utils.CountingIdlingResource
import io.zenandroid.onlinego.utils.microsToISODateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.util.Date
import androidx.core.content.edit

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
      val newEbi =
        "${Math.random().toString().split(".")[1]}.0.0.0.0.xxx.xxx.${Date().timezoneOffset + 13}"
      prefs.edit { putString(OGS_EBI, newEbi) }
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
      .doOnSuccess(userSessionRepository::storeUIConfig)
      .ignoreElement()
      .doAfterTerminate { idlingResource.decrement() }
  }

  fun loginWithGoogle(code: String): Completable {
    return restApi.initiateGoogleAuthFlow()
      .map {
        if (it.code() != 302) {
          throw Exception("got code ${it.code()} instead of 302")
        }
        it.headers().forEach {
          if (it.first == "location") {
            return@map "&state=([^&]*)&".toRegex().find(it.second)!!.groupValues[1]
          }
        }
        throw Exception("Cannot log in (can't follow redirect)")
      }
      .flatMap { state -> restApi.loginWithGoogleAuth(code, state) }
      .flatMap {
        if (it.code() != 302) {
          throw Exception("got code ${it.code()} instead of 302")
        }
        it.headers().forEach {
          if (it.first == "location" && it.second == "/") {
            return@flatMap restApi.uiConfig()
          }
        }
        throw Exception("Login failed")
      }
      .doOnSuccess(userSessionRepository::storeUIConfig)
      .ignoreElement()
  }

  fun createAccount(username: String, password: String, email: String): Completable {
    return restApi.createAccount(CreateAccountRequest(username, password, email, ebi))
      .ignoreElement()
  }

  fun challengeBot(challengeParams: ChallengeParams): Completable {
    val size = when (challengeParams.size) {
      "9x9", "9×9" -> 9
      "13x13", "13×13" -> 13
      "19x19", "19×19" -> 19
      else -> 19
    }

    val color = when (challengeParams.color) {
      "Auto" -> "automatic"
      "Black" -> "black"
      "White" -> "white"
      else -> "automatic"
    }

    val timeControl = when (challengeParams.speed.lowercase()) {
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
        handicap = if (challengeParams.handicap == "Auto") "-1" else challengeParams.handicap,
        ranked = challengeParams.ranked,
        name = if (challengeParams.opponent?.ui_class != null &&
          challengeParams.opponent?.ui_class!!.startsWith("bot")
        ) "Bot Match"
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
    userSessionRepository.loggedInObservable.singleOrError().flatMap {
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
    }

  fun fetchChallenges(): Single<List<OGSChallenge>> =
    restApi.fetchChallenges().map { it.results }

  fun fetchHistoricGamesBefore(beforeDate: Long?): Single<List<OGSGame>> =
    if (beforeDate == null) {
      userSessionRepository.userIdObservable.firstOrError().flatMap {
        restApi.fetchPlayerFinishedGames(it)
      }
    } else {
      userSessionRepository.userIdObservable.firstOrError().flatMap {
        restApi.fetchPlayerFinishedBeforeGames(
          it,
          10,
          beforeDate.microsToISODateTime(),
          1
        )
      }
    }.map { it.results }

  fun fetchHistoricGamesAfter(afterDate: Long?): Single<List<OGSGame>> =
    if (afterDate == null) {
      userSessionRepository.userIdObservable.firstOrError().flatMap {
        restApi.fetchPlayerFinishedGames(it)
      }
    } else {
      userSessionRepository.userIdObservable.firstOrError().flatMap {
        restApi.fetchPlayerFinishedAfterGames(
          it,
          10,
          afterDate.microsToISODateTime(),
          1
        )
      }
    }.map { it.results }

  suspend fun searchPlayers(query: String): List<OGSPlayer> =
    restApi.omniSearch(query).players


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

  fun getPuzzleCollections(
    minCount: Int? = null,
    namePrefix: String? = null
  ): Flow<List<PuzzleCollection>> {
    var page = 0

    return flow {
      do {
        val result = restApi.getPuzzleCollections(
          minimumCount = minCount ?: 0,
          namePrefix = namePrefix ?: "",
          page = ++page
        )
        emit(result.results)
        delay(5000)
      } while (result.next != null)
    }.map { it.map(PuzzleCollection::fromOGSPuzzleCollection) }
  }

  suspend fun getPuzzleCollection(id: Long): PuzzleCollection =
    restApi.getPuzzleCollection(collectionId = id)
      .let { PuzzleCollection.fromOGSPuzzleCollection(it) }

  suspend fun getPuzzleCollectionContents(id: Long): List<Puzzle> =
    restApi.getPuzzleCollectionContents(collectionId = id)
      .map(Puzzle::fromOGSPuzzle)

  suspend fun getPuzzle(id: Long): Puzzle =
    restApi.getPuzzle(puzzleId = id)
      .let { Puzzle.fromOGSPuzzle(it) }

  // TODO: This causes HTTP 429s, so we need to throttle it somehow
  suspend fun getPuzzleSolutions(id: Long): List<PuzzleSolution> {
    var page = 0

    val list = mutableListOf<PuzzleSolution>()
    do {
      if(list.isNotEmpty()) {
        delay(1000)
      }
      val result = restApi.getPuzzleSolutions(
        puzzleId = id,
        playerId = userSessionRepository.userIdObservable.blockingFirst(), //TODO: fixme
        page = ++page
      )
      list.addAll(result.results)
    } while (result.next != null)
    return list
  }

  suspend fun getPuzzleRating(id: Long): PuzzleRating =
    restApi.getPuzzleRating(puzzleId = id)

  suspend fun markPuzzleSolved(id: Long, solution: PuzzleSolution) =
    restApi.markPuzzleSolved(puzzleId = id, request = solution)

  suspend fun ratePuzzle(id: Long, rating: PuzzleRating) =
    restApi.ratePuzzle(puzzleId = id, request = rating)

  suspend fun deleteMyAccount(password: String) {
    return restApi.deleteAccount(
      userSessionRepository.userIdObservable.blockingFirst(), //TODO: fixme
      PasswordBody(password)
    )
  }

  suspend fun checkForWarnings(): Warning {
    var warning = restApi.getWarning()
    if(warning.text.isNullOrBlank() && warning.message_id != null) {
      warning = warning.copy(
        text = CannedMessages.convertCannedMessage(warning.message_id!!, warning.interpolation_data)
      )
    }
    return warning
  }

  suspend fun acknowledgeWarning(warning: Warning) {
    if (warning.id != null) {
      restApi.acknowledgeWarning(
        warning.id,
        "{accept: true}"
      )
    }
  }
}
