package io.zenandroid.onlinego.data.model.ogs

/**
 * Created by alex on 04/11/2017.
 */
data class GameList(
        var size: Int? = null,
        var from: String? = null,
        var limit: String? = null,
        var results: List<OGSGame>
)