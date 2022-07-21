package io.zenandroid.onlinego.data.model.local

data class Score (
        val handicap: Int? = null,
        val komi: Float? = null,
        val prisoners: Int? = null,
        val scoring_positions: String? = null,
        val stones: Int? = null,
        val territory: Int? = null,
        val total: Float? = null
)