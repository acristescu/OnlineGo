package io.zenandroid.onlinego.data.model.ogs

/**
 * Created by alex on 02/11/2017.
 */
data class LoginToken(
        val access_token: String,
        val refresh_token: String,
        val expires_in: Long
)