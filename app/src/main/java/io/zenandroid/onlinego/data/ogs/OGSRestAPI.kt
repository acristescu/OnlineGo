package io.zenandroid.onlinego.data.ogs

import io.reactivex.Completable
import io.reactivex.Single
import io.zenandroid.onlinego.data.model.ogs.Chat
import io.zenandroid.onlinego.data.model.ogs.CreateAccountRequest
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
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Created by alex on 02/11/2017.
 */
interface OGSRestAPI {

    @GET("login/google-oauth2/")
    fun initiateGoogleAuthFlow(): Single<Response<ResponseBody>>

    @GET("/complete/google-oauth2/?scope=email+profile+https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fuserinfo.email+openid+https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fuserinfo.profile&authuser=0&prompt=none")
    fun loginWithGoogleAuth(
            @Query("code") code: String,
            @Query("state") state: String
    ): Single<Response<ResponseBody>>

    @POST("api/v0/login")
    fun login(@Body request: CreateAccountRequest): Single<UIConfig>

    @GET("api/v1/ui/config/")
    fun uiConfig(): Single<UIConfig>

    @GET("api/v1/games/{game_id}")
    fun fetchGame(@Path("game_id") game_id: Long): Single<OGSGame>

    @GET("api/v1/ui/overview")
//    @GET("api/v1/players/126739/full")
    fun fetchOverview(): Single<Overview>

    @GET("api/v1/players/{player_id}/full")
    suspend fun getPlayerFullProfileAsync(@Path("player_id") playerId: Long): OGSPlayerProfile

    @POST("api/v0/register")
    fun createAccount(@Body request: CreateAccountRequest): Single<UIConfig>

    @GET("api/v1/players/{player_id}/games/?source=play&ended__isnull=false&annulled=false&ordering=-ended")
    fun fetchPlayerFinishedGames(
            @Path("player_id") playerId: Long,
            @Query("page_size") pageSize: Int = 10,
            @Query("page") page: Int = 1): Single<PagedResult<OGSGame>>

    @GET("api/v1/players/{player_id}/games/?source=play&ended__isnull=false&annulled=false&ordering=-ended")
    fun fetchPlayerFinishedBeforeGames(
            @Path("player_id") playerId: Long,
            @Query("page_size") pageSize: Int = 10,
            @Query("ended__lt") ended: String,
            @Query("page") page: Int = 1): Single<PagedResult<OGSGame>>

    // NOTE: This is ordered the other way as all the others!!!
    @GET("api/v1/players/{player_id}/games/?source=play&ended__isnull=false&annulled=false&ordering=ended")
    fun fetchPlayerFinishedAfterGames(
            @Path("player_id") playerId: Long,
            @Query("page_size") pageSize: Int = 100,
            @Query("ended__gt") ended: String,
            @Query("page") page: Int = 1): Single<PagedResult<OGSGame>>

    @POST("/api/v1/challenges/{challenge_id}/accept")
    fun acceptOpenChallenge(@Path("challenge_id") id: Long): Completable

    @GET("/api/v1/me/challenges?page_size=100")
    fun fetchChallenges(): Single<PagedResult<OGSChallenge>>

    @POST("/api/v1/me/challenges/{challenge_id}/accept")
    fun acceptChallenge(@Path("challenge_id") id: Long): Completable

    @DELETE("/api/v1/me/challenges/{challenge_id}")
    fun declineChallenge(@Path("challenge_id") id: Long): Completable

    @POST("/api/v1/challenges")
    fun openChallenge(@Body request: OGSChallengeRequest): Completable

    @POST("/api/v1/players/{id}/challenge")
    fun challengePlayer(@Path("id") id: Long, @Body request: OGSChallengeRequest): Completable

    @GET("/api/v1/ui/omniSearch")
    fun omniSearch(@Query("q") q: String): Single<OmniSearchResponse>

    @Headers("x-godojo-auth-token: foofer")
    @GET("/oje/positions?mode=0")
    fun getJosekiPositions(@Query("id") id: String): Single<List<JosekiPosition>>

    @GET("api/v1/players/{player_id}/")
    fun getPlayerProfile(@Path("player_id") playerId: Long): Single<OGSPlayer>

    @GET("api/v1/players/{player_id}/")
    suspend fun getPlayerProfileAsync(@Path("player_id") playerId: Long): OGSPlayer

    @GET("termination-api/player/{player_id}/v5-rating-history")
    suspend fun getPlayerStatsAsync(
      @Path("player_id") playerId: Long,
      @Query("speed") speed: String,
      @Query("size") size: Int,
    ): Glicko2History

    @GET("termination-api/my/game-chat-history-since/{last_message_id}")
    fun getMessages(@Path("last_message_id") lastMessageId: String): Single<List<Chat>>

    @GET("api/v1/puzzles/collections?ordering=-rating,-rating_count")
    suspend fun getPuzzleCollections(
        @Query("page_size") pageSize: Int = 1000,
        @Query("puzzle_count__gt") minimumCount: Int,
        @Query("name__istartswith") namePrefix: String,
        @Query("page") page: Int = 1): PagedResult<OGSPuzzleCollection>

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
        @Query("page") page: Int = 1): PagedResult<PuzzleSolution>

    @GET("api/v1/puzzles/{puzzle_id}/rate")
    suspend fun getPuzzleRating(@Path("puzzle_id") puzzleId: Long): PuzzleRating

    @POST("api/v1/puzzles/{puzzle_id}/solutions")
    suspend fun markPuzzleSolved(
        @Path("puzzle_id") puzzleId: Long,
        @Body request: PuzzleSolution)

    @PUT("api/v1/puzzles/{puzzle_id}/rate")
    suspend fun ratePuzzle(
        @Path("puzzle_id") puzzleId: Long,
        @Body request: PuzzleRating)

    @HTTP(method = "DELETE", path="api/v1/players/{player_id}", hasBody = true)
    suspend fun deleteAccount(@Path("player_id") playerId: Long, @Body body: PasswordBody)
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
