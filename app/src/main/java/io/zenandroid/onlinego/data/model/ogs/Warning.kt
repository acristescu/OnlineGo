@file:UseSerializers(LenientIntSerializer::class)

package io.zenandroid.onlinego.data.model.ogs

import kotlinx.serialization.Serializable
import io.zenandroid.onlinego.data.ogs.LenientIntSerializer
import kotlinx.serialization.UseSerializers

@Serializable
data class Warning(
    val id: Int?,
    val created: String?,
    val player_id: Int?,
    val moderator: Int?,
    val text: String?,
    val message_id: String?,
    val severity: String?,
    val interpolation_data: String?
)

