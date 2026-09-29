package io.zenandroid.onlinego.data.model.ogs

import kotlinx.serialization.Serializable

@Serializable
data class OmniSearchResponse(
        val q: String,
        val players: List<OGSPlayer>
// groups
// tournaments
)