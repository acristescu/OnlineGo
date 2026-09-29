@file:UseSerializers(LenientIntSerializer::class)

package io.zenandroid.onlinego.data.model.ogs

import kotlinx.serialization.Serializable
import io.zenandroid.onlinego.data.ogs.LenientIntSerializer
import kotlinx.serialization.UseSerializers

/**
 * Created by alex on 04/11/2017.
 */
@Serializable
data class GameList(
        var size: Int? = null,
        var from: String? = null,
        var limit: String? = null,
        var results: List<OGSGame>
)