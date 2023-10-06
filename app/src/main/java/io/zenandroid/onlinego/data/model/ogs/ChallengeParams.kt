package io.zenandroid.onlinego.data.model.ogs

import androidx.annotation.Keep

@Keep
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