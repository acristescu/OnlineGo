package io.zenandroid.onlinego.ogs

import io.reactivex.Completable
import io.reactivex.Single
import io.zenandroid.onlinego.BuildConfig
import io.zenandroid.onlinego.model.ogs.*
import retrofit2.http.*

/**
 * Created by alex on 02/11/2017.
 */
interface OGSRestAPI {

    @POST("oauth2/token/")
    fun login(@Query("username") username: String,
              @Query("password") password: String,
              @Query("client_id") client_id: String = BuildConfig.CLIENT_ID,
              @Query("client_secret") client_secret: String = BuildConfig.CLIENT_SECRET,
              @Query("grant_type") grant_type: String = "password"): Single<LoginToken>

    @POST("oauth2/token/")
    fun refreshToken(@Query("refresh_token") refresh_token: String,
                     @Query("client_id") client_id: String = BuildConfig.CLIENT_ID,
                     @Query("client_secret") client_secret: String = BuildConfig.CLIENT_SECRET,
                     @Query("grant_type") grant_type: String = "refresh_token"): Single<LoginToken>

    @GET("api/v1/ui/config/")
    fun uiConfig(): Single<UIConfig>

    @GET("api/v1/games/{game_id}")
    fun fetchGame(@Path("game_id") game_id: Long): Single<Game>

    @GET("api/v1/ui/overview")
    fun fetchOverview(): Single<Overview>

    @POST("api/v0/register")
    fun createAccount(@Body request: CreateAccountRequest): Single<UIConfig>
}
