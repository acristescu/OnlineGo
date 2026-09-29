@file:UseSerializers(LenientLongSerializer::class)

package io.zenandroid.onlinego.data.model.ogs

import kotlinx.serialization.Serializable
import io.zenandroid.onlinego.data.ogs.LenientLongSerializer
import kotlinx.serialization.UseSerializers

@Serializable
data class OGSChallenge(
        val id: Long,
        val challenger: OGSPlayer?,
        val challenged: OGSPlayer?,
        val game: OGSGame?
)