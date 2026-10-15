package io.zenandroid.onlinego.data.model.ogs

import kotlinx.serialization.Serializable

/**
 * Created by alex on 14/03/2018.
 */
@Serializable
data class Overview (
        val active_games : List<OGSGame>
)