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
        val rootPolicyTemperature: Float? = null,
        val rootFpuReductionMax: Float? = null,
        val includeOwnership: Boolean? = null,
        val includeMovesOwnership: Boolean? = null,
        val includePolicy: Boolean? = null,
        val includePVVisits: Boolean? = null,
        val avoidMoves: List<List<String>>? = null,
        val allowMoves: List<List<String>>? = null,
        val overrideSettings: String? = null,
        val reportDuringSearchEvery: Float? = null,
        val priority: Int? = null,
)
