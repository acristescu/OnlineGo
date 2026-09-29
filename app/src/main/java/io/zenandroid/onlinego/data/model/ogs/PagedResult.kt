@file:UseSerializers(LenientIntSerializer::class)

package io.zenandroid.onlinego.data.model.ogs

import kotlinx.serialization.Serializable
import io.zenandroid.onlinego.data.ogs.LenientIntSerializer
import kotlinx.serialization.UseSerializers

/**
 * Created by alex on 31/05/2018.
 */
@Serializable
data class PagedResult<T>(
        val count: Int,
        val next: String?,
        val previous: String?,
        val results: List<T>
        )