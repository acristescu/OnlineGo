@file:UseSerializers(LenientIntSerializer::class)

package io.zenandroid.onlinego.data.model.ogs

import io.zenandroid.onlinego.data.ogs.LenientIntSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
data class Warning(
    val id: Int? = null,
    val created: String? = null,
    val player_id: Int? = null,
    val moderator: Int? = null,
    val text: String? = null,
    val message_id: String? = null,
    val severity: String? = null,
    val interpolation_data: String? = null
)

