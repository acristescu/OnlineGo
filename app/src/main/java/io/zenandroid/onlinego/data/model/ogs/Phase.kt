package io.zenandroid.onlinego.data.model.ogs

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName


@Serializable
enum class Phase {
    @SerialName("play")
    PLAY,
    @SerialName("stone removal")
    STONE_REMOVAL,
    @SerialName("finished")
    FINISHED;
}