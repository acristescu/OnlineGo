package io.zenandroid.onlinego.data.ogs

import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.DELETE
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.HTTP
import de.jensklingenberg.ktorfit.http.Headers
import de.jensklingenberg.ktorfit.http.PATCH
import de.jensklingenberg.ktorfit.http.POST
import de.jensklingenberg.ktorfit.http.PUT
import de.jensklingenberg.ktorfit.http.Path
import de.jensklingenberg.ktorfit.http.Query
import de.jensklingenberg.ktorfit.http.ReqBuilder
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.HttpResponse
import io.zenandroid.onlinego.data.model.ogs.AcknowledgeWarningRequest
import io.zenandroid.onlinego.data.model.ogs.Chat
import io.zenandroid.onlinego.data.model.ogs.CreateAccountRequest
import io.zenandroid.onlinego.data.model.ogs.GameData
import io.zenandroid.onlinego.data.model.ogs.Glicko2History
import io.zenandroid.onlinego.data.model.ogs.JosekiPosition
import io.zenandroid.onlinego.data.model.ogs.OGSChallenge
import io.zenandroid.onlinego.data.model.ogs.OGSChallengeRequest
import io.zenandroid.onlinego.data.model.ogs.OGSGame
import io.zenandroid.onlinego.data.model.ogs.OGSPlayer
import io.zenandroid.onlinego.data.model.ogs.OGSPlayerProfile
import io.zenandroid.onlinego.data.model.ogs.OGSPuzzle
import io.zenandroid.onlinego.data.model.ogs.OGSPuzzleCollection
import io.zenandroid.onlinego.data.model.ogs.OmniSearchResponse
import io.zenandroid.onlinego.data.model.ogs.Overview
import io.zenandroid.onlinego.data.model.ogs.PagedResult
import io.zenandroid.onlinego.data.model.ogs.PasswordBody
import io.zenandroid.onlinego.data.model.ogs.PuzzleRating
import io.zenandroid.onlinego.data.model.ogs.PuzzleSolution
import io.zenandroid.onlinego.data.model.ogs.UIConfig
import io.zenandroid.onlinego.data.model.ogs.Warning

private const val GOOGLE_OAUTH_SCOPE =
  "email profile https://www.googleapis.com/auth/userinfo.email openid https://www.googleapis.com/auth/userinfo.profile"

interface OGSRestAPI {

  @GET("login/google-oauth2/")
  suspend fun initiateGoogleAuthFlow(
    @ReqBuilder builder: HttpRequestBuilder.() -> Unit = { expectSuccess = false }
  ): HttpResponse

  @GET("complete/google-oauth2/")
  suspend fun loginWithGoogleAuth(
    @Query("code") code: String,
    @Query("state") state: String,
    @Query("scope") scope: String = GOOGLE_OAUTH_SCOPE,
    @Query("authuser") authuser: Int = 0,
    @Query("prompt") prompt: String = "none",
    @ReqBuilder builder: HttpRequestBuilder.() -> Unit = { expectSuccess = false }
  ): HttpResponse

  @POST("api/v0/login")
  suspend fun login(@Body request: CreateAccountRequest): UIConfig

  @GET("api/v1/ui/config/")
  suspend fun uiConfig(): UIConfig

  @GET("api/v1/games/{game_id}")
  suspend fun fetchGame(@Path("game_id") game_id: Long): OGSGame

  @GET("termination-api/game/{game_id}")
  suspend fun fetchTerminationGameData(@Path("game_id") game_id: Long): GameData

  @GET("api/v1/ui/overview")
  suspend fun fetchOverview(): Overview

  @GET("api/v1/players/{player_id}/full")
  suspend fun getPlayerFullProfileAsync(@Path("player_id") playerId: Long): OGSPlayerProfile

  @POST("api/v0/register")
  suspend fun createAccount(@Body request: CreateAccountRequest): UIConfig

  @GET("api/v1/players/{player_id}/games/?source=play&ended__isnull=false&annulled=false&ordering=-ended")
  suspend fun fetchPlayerFinishedGames(
    @Path("player_id") playerId: Long,
    @Query("page_size") pageSize: Int = 10,
    @Query("page") page: Int = 1
  ): PagedResult<OGSGame>

  @GET("api/v1/players/{player_id}/games/?source=play&ended__isnull=false&annulled=false&ordering=-ended")
  suspend fun fetchPlayerFinishedBeforeGames(
    @Path("player_id") playerId: Long,
    @Query("page_size") pageSize: Int = 10,
    @Query("ended__lt") ended: String,
    @Query("page") page: Int = 1
  ): PagedResult<OGSGame>

  // NOTE: This is ordered the other way as all the others!!!
  @GET("api/v1/players/{player_id}/games/?source=play&ended__isnull=false&annulled=false&ordering=ended")
  suspend fun fetchPlayerFinishedAfterGames(
    @Path("player_id") playerId: Long,
    @Query("page_size") pageSize: Int = 100,
    @Query("ended__gt") ended: String,
    @Query("page") page: Int = 1
  ): PagedResult<OGSGame>

  @GET("api/v1/me/challenges?page_size=100")
  suspend fun fetchChallenges(): PagedResult<OGSChallenge>

  @POST("api/v1/me/challenges/{challenge_id}/accept")
  suspend fun acceptChallenge(@Path("challenge_id") id: Long)

  @DELETE("api/v1/me/challenges/{challenge_id}")
  suspend fun declineChallenge(@Path("challenge_id") id: Long)

  @POST("api/v1/challenges")
  suspend fun openChallenge(@Body request: OGSChallengeRequest)

  @POST("api/v1/players/{id}/challenge")
  suspend fun challengePlayer(@Path("id") id: Long, @Body request: OGSChallengeRequest)

  @GET("api/v1/ui/omniSearch")
  suspend fun omniSearch(@Query("q") q: String): OmniSearchResponse

  @Headers("x-godojo-auth-token: foofer")
  @GET("oje/positions?mode=0")
  suspend fun getJosekiPositions(@Query("id") id: String): List<JosekiPosition>

  @GET("api/v1/players/{player_id}/")
  suspend fun getPlayerProfile(@Path("player_id") playerId: Long): OGSPlayer

  @GET("api/v1/players/{player_id}/")
  suspend fun getPlayerProfileAsync(@Path("player_id") playerId: Long): OGSPlayer

  @GET("termination-api/player/{player_id}/v5-rating-history")
  suspend fun getPlayerStatsAsync(
    @Path("player_id") playerId: Long,
    @Query("speed") speed: String,
    @Query("size") size: Int,
  ): Glicko2History

  @GET("termination-api/my/game-chat-history-since/{last_message_id}")
  suspend fun getMessages(@Path("last_message_id") lastMessageId: String): List<Chat>

  @GET("api/v1/puzzles/collections?ordering=-rating,-rating_count")
  suspend fun getPuzzleCollections(
    @Query("page_size") pageSize: Int = 1000,
    @Query("puzzle_count__gt") minimumCount: Int,
    @Query("name__istartswith") namePrefix: String,
    @Query("page") page: Int = 1
  ): PagedResult<OGSPuzzleCollection>

  @GET("api/v1/puzzles/collections/{collection_id}")
  suspend fun getPuzzleCollection(@Path("collection_id") collectionId: Long): OGSPuzzleCollection

  @GET("api/v1/puzzles/collections/{collection_id}/puzzles")
  suspend fun getPuzzleCollectionContents(@Path("collection_id") collectionId: Long): List<OGSPuzzle>

  @GET("api/v1/puzzles/{puzzle_id}")
  suspend fun getPuzzle(@Path("puzzle_id") puzzleId: Long): OGSPuzzle

  @GET("api/v1/puzzles/{puzzle_id}/solutions")
  suspend fun getPuzzleSolutions(
    @Path("puzzle_id") puzzleId: Long,
    @Query("player_id") playerId: Long,
    @Query("page_size") pageSize: Int = 1000,
    @Query("page") page: Int = 1
  ): PagedResult<PuzzleSolution>

  @GET("api/v1/puzzles/{puzzle_id}/rate")
  suspend fun getPuzzleRating(@Path("puzzle_id") puzzleId: Long): PuzzleRating

  @POST("api/v1/puzzles/{puzzle_id}/solutions")
  suspend fun markPuzzleSolved(
    @Path("puzzle_id") puzzleId: Long,
    @Body request: PuzzleSolution
  )

  @PUT("api/v1/puzzles/{puzzle_id}/rate")
  suspend fun ratePuzzle(
    @Path("puzzle_id") puzzleId: Long,
    @Body request: PuzzleRating
  )

  @HTTP(method = "DELETE", path = "api/v1/players/{player_id}", hasBody = true)
  suspend fun deleteAccount(@Path("player_id") playerId: Long, @Body body: PasswordBody)

  @GET("api/v1/me/warning")
  suspend fun getWarning(): Warning

  @PATCH("api/v1/me/warning/{id}")
  suspend fun acknowledgeWarning(
    @Path("id") id: Int,
    @Body body: AcknowledgeWarningRequest
  ): Warning
}

/*
Other interesting APIs:

https://online-go.com/api/v1/players/89194/full -> gives full list of moves!!!

https://forums.online-go.com/t/ogs-api-notes/17136
https://ogs.readme.io/docs/real-time-api
https://ogs.docs.apiary.io/#reference/games

https://github.com/flovo/ogs_api
https://forums.online-go.com/t/live-games-via-api/1867/2

power user - 126739
 */
