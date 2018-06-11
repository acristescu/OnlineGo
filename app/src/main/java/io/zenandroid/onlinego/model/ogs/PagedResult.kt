package io.zenandroid.onlinego.model.ogs

/**
 * Created by alex on 31/05/2018.
 */
data class PagedResult(
        val count: Int,
        val next: String?,
        val previous: String?,
        val results: List<OGSGame>
        )