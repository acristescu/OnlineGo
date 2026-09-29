@file:UseSerializers(OGSBooleanSerializer::class, LenientLongSerializer::class)

package io.zenandroid.onlinego.data.model.ogs

import kotlinx.serialization.Serializable
import io.zenandroid.onlinego.data.ogs.OGSBooleanSerializer
import kotlinx.serialization.UseSerializers
import io.zenandroid.onlinego.data.ogs.LenientLongSerializer

@Serializable
data class OGSAutomatch(
  val uuid: String?,
        val game_id: Long?,
        val size_speed_options: List<SizeSpeedOption>?
) {
    val liveOrBlitzOrRapid: Boolean
        get() = size_speed_options?.find { it.speed == "blitz" || it.speed == "live" || it.speed == "rapid" } != null
}

@Serializable
data class SizeSpeedOption(
        val size: String,
        val speed: String
)

@Serializable
enum class Size {
    SMALL, MEDIUM, LARGE;

    fun getText() = when(this) {
        SMALL -> "9x9"
        MEDIUM -> "13x13"
        LARGE -> "19x19"
    }
}

@Serializable
enum class Speed {
    BLITZ, RAPID, LIVE, LONG;
    fun getText() = when(this) {
        BLITZ -> "blitz"
        RAPID -> "rapid"
        LIVE -> "live"
        LONG -> "correspondence"
    }
}