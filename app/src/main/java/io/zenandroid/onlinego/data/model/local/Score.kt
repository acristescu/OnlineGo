package io.zenandroid.onlinego.data.model.local

data class Score (
        var handicap: Double? = null,
        var komi: Double? = null,
        var prisoners: Int? = null,
        var scoring_positions: String? = null,
        var stones: Int? = null,
        var territory: Int? = null,
        var total: Double? = null
)