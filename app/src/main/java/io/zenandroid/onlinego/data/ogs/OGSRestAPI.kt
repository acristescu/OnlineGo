package io.zenandroid.onlinego.data.ogs

import io.reactivex.Completable
import io.reactivex.Single
import io.zenandroid.onlinego.BuildConfig
import io.zenandroid.onlinego.data.model.ogs.JosekiPosition
import io.zenandroid.onlinego.data.model.ogs.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

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
    fun fetchOverview(): Single<Overview>

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

    @GET("/api/v1/me/challenges?page_size=100")
    fun fetchChallenges(): Single<PagedResult<OGSChallenge>>

    @POST("/api/v1/me/challenges/{challenge_id}/accept")
    fun acceptChallenge(@Path("challenge_id") id: Long): Completable

    @DELETE("/api/v1/me/challenges/{challenge_id}")
    fun declineChallenge(@Path("challenge_id") id: Long): Completable

    @POST("/api/v1/players/{id}/challenge")
    fun challengePlayer(@Path("id") id: Long, @Body request: OGSChallengeRequest): Completable

    @GET("/api/v1/ui/omniSearch")
    fun omniSearch(@Query("q") q: String): Single<OmniSearchResponse>

    @Headers("x-godojo-auth-token: foofer")
    @GET("/godojo/positions?mode=0")
    fun getJosekiPositions(@Query("id") id: String): Single<List<JosekiPosition>>

    @GET("api/v1/players/{player_id}/")
    fun getPlayerProfile(@Path("player_id") playerId: Long): Single<OGSPlayer>

    @GET("termination-api/player/{player_id}/glicko2-history?speed=overall&size=0")
    fun getPlayerStats(@Path("player_id") playerId: Long): Single<Glicko2History>
}
