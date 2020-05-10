package io.zenandroid.onlinego.newchallenge

import io.zenandroid.onlinego.model.ogs.OGSPlayer

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