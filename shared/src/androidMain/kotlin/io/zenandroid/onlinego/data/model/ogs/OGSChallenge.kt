@file:UseSerializers(LenientLongSerializer::class)

package io.zenandroid.onlinego.data.model.ogs

import io.zenandroid.onlinego.data.ogs.LenientLongSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
data class OGSChallenge(
        val id: Long,
        val challenger: OGSPlayer? = null,
        val challenged: OGSPlayer? = null,
        val game: OGSGame? = null
)