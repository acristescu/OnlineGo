package io.zenandroid.onlinego.data.ogs

import com.squareup.moshi.Moshi
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
import io.zenandroid.onlinego.data.repositories.LoginStatus
import io.zenandroid.onlinego.data.repositories.SettingsRepository
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import io.zenandroid.onlinego.utils.microsToISODateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.util.Date

private const val TAG = "OGSRestService"

class OGSRestService(
  val moshi: Moshi,
  val restApi: OGSRestAPI,
  val userSessionRepository: UserSessionRepository,
  val settingsRepository: SettingsRepository,
  private val applicationScope: CoroutineScope,
) {
  private var ebi: String? = null

  init {
    applicationScope.launch {
      settingsRepository.ogsEbiFlow.collect {
        ebi = it
      }
    }
  }

  private suspend fun getOrCreateEbi(): String {
    if (!ebi.isNullOrBlank()) return ebi!!
    val newEbi =
      "${Math.random().toString().split(".")[1]}.0.0.0.0.xxx.xxx.${Date().timezoneOffset + 13}"
    settingsRepository.setOgsEbi(newEbi)
    ebi = newEbi
    return newEbi
  }

  suspend fun fetchUIConfig() {
    val config = restApi.uiConfig()
    userSessionRepository.storeUIConfig(config)
  }

  suspend fun login(username: String, password: String) {
    val ebiValue = getOrCreateEbi()
    val uiConfig = restApi.login(CreateAccountRequest(username, password, "", ebiValue))
    //
    // Hack alert!!! The server sometimes returns 200 even on wrong password :facepalm:
    //
    if (uiConfig.csrf_token.isNullOrBlank() || uiConfig.redirect != null) {
      throw HttpException(Response.error<Any>(403, "login failed".toResponseBody()))
    }
    userSessionRepository.storeUIConfig(uiConfig)
  }

  suspend fun loginWithGoogle(code: String) {
    val authResponse = restApi.initiateGoogleAuthFlow()
    if (authResponse.code() != 302) {
      throw Exception("got code ${authResponse.code()} instead of 302")
    }
    val state = authResponse.headers().firstOrNull { it.first == "location" }
      ?.let { "&state=([^&]*)&".toRegex().find(it.second)?.groupValues?.get(1) }
      ?: throw Exception("Cannot log in (can't follow redirect)")

    val loginResponse = restApi.loginWithGoogleAuth(code, state)
    if (loginResponse.code() != 302) {
      throw Exception("got code ${loginResponse.code()} instead of 302")
    }
    val isRedirectToHome =
      loginResponse.headers().any { it.first == "location" && it.second == "/" }
    if (!isRedirectToHome) {
      throw Exception("Login failed")
    }
    val uiConfig = restApi.uiConfig()
    userSessionRepository.storeUIConfig(uiConfig)
  }

  suspend fun createAccount(username: String, password: String, email: String) {
    val ebiValue = getOrCreateEbi()
    restApi.createAccount(CreateAccountRequest(username, password, email, ebiValue))
  }

  suspend fun challengeBot(challengeParams: ChallengeParams) {
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
    when {
      challengeParams.opponent != null -> {
        restApi.challengePlayer(challengeParams.opponent?.id!!, request)
      }

      else -> {
        restApi.openChallenge(request)
      }
    }
  }

  suspend fun acceptChallenge(id: Long) =
    restApi.acceptChallenge(id)

  suspend fun declineChallenge(id: Long) =
    restApi.declineChallenge(id)

  suspend fun fetchGame(gameId: Long): OGSGame =
    restApi.fetchGame(gameId)
      //
      // Hack alert! just to keep us on our toes, the same thing is called
      // different things when coming through the REST API and the Socket.IO one...
      //
      .also { it.json = it.gamedata }

  suspend fun fetchActiveGames(): List<OGSGame> {
    userSessionRepository.loggedInObservable.asFlow().first { it is LoginStatus.LoggedIn }
    val overview = restApi.fetchOverview()
    val games = overview.active_games
    for (game in games) {
      game.json?.clock?.current_player?.let {
        game.player_to_move = it
      }
      game.json?.handicap?.let {
        game.handicap = it
      }
    }
    return games
  }

  suspend fun fetchChallenges(): List<OGSChallenge> =
    restApi.fetchChallenges().results

  suspend fun fetchHistoricGamesBefore(beforeDate: Long?): List<OGSGame> {
    val userId = userSessionRepository.userIdObservable.asFlow().first()
    val result = if (beforeDate == null) {
      restApi.fetchPlayerFinishedGames(userId)
    } else {
      restApi.fetchPlayerFinishedBeforeGames(userId, 10, beforeDate.microsToISODateTime(), 1)
    }
    return result.results
  }

  suspend fun fetchHistoricGamesAfter(afterDate: Long?): List<OGSGame> {
    val userId = userSessionRepository.userIdObservable.asFlow().first()
    val result = if (afterDate == null) {
      restApi.fetchPlayerFinishedGames(userId)
    } else {
      restApi.fetchPlayerFinishedAfterGames(userId, 10, afterDate.microsToISODateTime(), 1)
    }
    return result.results
  }

  suspend fun searchPlayers(query: String): List<OGSPlayer> =
    restApi.omniSearch(query).players


  suspend fun getJosekiPositions(id: Long?): List<JosekiPosition> =
    restApi.getJosekiPositions(id?.toString() ?: "root")

  suspend fun getPlayerProfile(id: Long): OGSPlayer =
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
      if (list.isNotEmpty()) {
        delay(1000)
      }
      val loggedInStatus = userSessionRepository.loggedInObservable.asFlow().first()
      val userId = (loggedInStatus as? LoginStatus.LoggedIn)?.userId
      val result = restApi.getPuzzleSolutions(
        puzzleId = id,
        playerId = userId ?: 0,
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
    val loggedInStatus = userSessionRepository.loggedInObservable.asFlow().first()
    if (loggedInStatus is LoginStatus.LoggedIn) {
      restApi.deleteAccount(
        loggedInStatus.userId,
        PasswordBody(password)
      )
    }
  }

  suspend fun checkForWarnings(): Warning {
    var warning = restApi.getWarning()
    if (warning.text.isNullOrBlank() && warning.message_id != null) {
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
