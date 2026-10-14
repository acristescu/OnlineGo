package io.zenandroid.onlinego.data.model.ogs

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
enum class Phase {
    @SerialName("play")
    PLAY,
    @SerialName("stone removal")
    STONE_REMOVAL,
    @SerialName("finished")
    FINISHED;
}