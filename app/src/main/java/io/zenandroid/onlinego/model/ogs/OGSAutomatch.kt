package io.zenandroid.onlinego.model.ogs

data class OGSAutomatch(
        val uuid: String,
        val game_id: Long?,
        val size_speed_options: List<SizeSpeedOption>?
) {
    val liveOrBlitz: Boolean
        get() = size_speed_options?.find { it.speed == "blitz" || it.speed == "live" } != null
}

data class SizeSpeedOption(
        val size: String,
        val speed: String
)

enum class Size {
    SMALL, MEDIUM, LARGE;

    fun getText() = when(this) {
        SMALL -> "9x9"
        MEDIUM -> "13x13"
        LARGE -> "19x19"
    }
}
enum class Speed {
    BLITZ, NORMAL, LONG;
    fun getText() = when(this) {
        BLITZ -> "blitz"
        NORMAL -> "live"
        LONG -> "correspondence"
    }
}