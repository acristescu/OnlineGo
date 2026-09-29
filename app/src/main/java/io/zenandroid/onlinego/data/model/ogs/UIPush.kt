@file:UseSerializers(AnySerializer::class)

package io.zenandroid.onlinego.data.model.ogs

import kotlinx.serialization.Serializable
import io.zenandroid.onlinego.data.ogs.AnySerializer
import kotlinx.serialization.UseSerializers

@Serializable
data class UIPush (
    val data: Any?,
    val event: String?
)