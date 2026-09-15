package io.zenandroid.onlinego.data.model.katago

// Note: see https://github.com/lightvector/KataGo/blob/master/docs/Analysis_Engine.md for meanings
data class Query (
        val id: String,
        val moves: List<List<String>>,
        val initialStones: List<List<String>>? = null,
        val initialPlayer: String? = null,
        val rules: String,
        val komi: Float? = null,
        val boardXSize: Int,
        val boardYSize: Int,
        val analyzeTurns: List<Int>? = null,
        val maxVisits: Int? = null,
        val rootFpuReductionMax: Float? = null,
        val includeOwnership: Boolean? = null,
        val includeMovesOwnership: Boolean? = null,
        val includePolicy: Boolean? = null,
        val includePVVisits: Boolean? = null,
        val avoidMoves: List<List<String>>? = null,
        val allowMoves: List<List<String>>? = null,
        val overrideSettings: OverrideSettings? = null,
        val priority: Int? = null
)

// humanSLProfile format: "rank_{RANK}" (or "preaz_{RANK}"), RANK spanning 20k-9d - see
// AiDifficulty.humanSLProfile for how tiers map to it.
data class OverrideSettings(
        val humanSLProfile: String? = null,
)