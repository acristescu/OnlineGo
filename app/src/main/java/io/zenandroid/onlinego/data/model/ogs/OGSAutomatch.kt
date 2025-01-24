package io.zenandroid.onlinego.data.model.ogs

data class OGSAutomatch(
        val uuid: String,
        val game_id: Long?,
        val size_speed_options: List<SizeSpeedOption>?
) {
    val liveOrBlitzOrRapid: Boolean
        get() = size_speed_options?.find { it.speed == "blitz" || it.speed == "live" || it.speed == "rapid" } != null
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
    BLITZ, RAPID, LIVE, LONG;
    fun getText() = when(this) {
        BLITZ -> "blitz"
        RAPID -> "rapid"
        LIVE -> "live"
        LONG -> "correspondence"
    }
}