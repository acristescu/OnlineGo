@file:UseSerializers(LenientLongSerializer::class)

package io.zenandroid.onlinego.data.model.ogs

import io.zenandroid.onlinego.data.ogs.LenientLongSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
data class NetPing (
        val client: Long,
        val drift: Long,
        val latency: Long
)