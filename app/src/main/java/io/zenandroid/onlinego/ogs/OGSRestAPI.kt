package io.zenandroid.onlinego.ogs

import io.reactivex.Completable
import io.reactivex.Single
import io.zenandroid.onlinego.model.ogs.JosekiPosition
import io.zenandroid.onlinego.model.ogs.*
import retrofit2.http.*

/**
 * Created by alex on 02/11/2017.
 */
interface OGSRestAPI {

    /*
    @FormUrlEncoded
    @POST("oauth2/token/")
    fun login(@Field("username") username: String,
              @Field("password") password: String,
              @Field("client_id") client_id: String = BuildConfig.CLIENT_ID,
              @Field("client_secret") client_secret: String = BuildConfig.CLIENT_SECRET,
              @Field("grant_type") grant_type: String = "password"): Single<LoginToken>

    @FormUrlEncoded
    @POST("oauth2/token/")
    fun refreshToken(@Field("refresh_token") refresh_token: String,
                     @Field("client_id") client_id: String = BuildConfig.CLIENT_ID,
                     @Field("client_secret") client_secret: String = BuildConfig.CLIENT_SECRET,
                     @Field("grant_type") grant_type: String = "refresh_token"): Single<LoginToken>
    */

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
}
