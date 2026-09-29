@file:UseSerializers(LenientLongSerializer::class)

package io.zenandroid.onlinego.data.model.ogs

import kotlinx.serialization.Serializable
import io.zenandroid.onlinego.data.ogs.LenientLongSerializer
import kotlinx.serialization.UseSerializers

/**
 * Created by alex on 02/11/2017.
 */
@Serializable
data class LoginToken(
        val access_token: String,
        val refresh_token: String,
        val expires_in: Long
)