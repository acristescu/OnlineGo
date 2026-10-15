@file:UseSerializers(AnySerializer::class)

package io.zenandroid.onlinego.data.model.ogs

import io.zenandroid.onlinego.data.ogs.AnySerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
data class UIPush (
    val data: Any? = null,
    val event: String? = null
)