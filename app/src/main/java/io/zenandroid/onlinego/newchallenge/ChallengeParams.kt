package io.zenandroid.onlinego.newchallenge

import io.zenandroid.onlinego.model.ogs.Bot

data class ChallengeParams(
        var bot: Bot? = null,
        var color: String,
        var size: String,
        var handicap: String,
        var speed: String,
        var ranked: Boolean
)