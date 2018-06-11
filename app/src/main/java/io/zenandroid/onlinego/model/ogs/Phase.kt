package io.zenandroid.onlinego.model.ogs

import com.squareup.moshi.Json

enum class Phase {
    @Json(name = "play") PLAY,
    @Json(name = "stone removal") STONE_REMOVAL,
    @Json(name = "finished") FINISHED;
}