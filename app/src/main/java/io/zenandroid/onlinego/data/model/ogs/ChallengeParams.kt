@file:UseSerializers(OGSBooleanSerializer::class)

package io.zenandroid.onlinego.data.model.ogs

import androidx.annotation.Keep
import kotlinx.serialization.Serializable
import io.zenandroid.onlinego.data.ogs.OGSBooleanSerializer
import kotlinx.serialization.UseSerializers

@Keep
@Serializable
data class ChallengeParams(
        var opponent: OGSPlayer? = null,
        var color: String,
        var size: String,
        var handicap: String,
        var speed: String,
        var ranked: Boolean,
        var disable_analysis: Boolean = false,
        var private: Boolean = false
)