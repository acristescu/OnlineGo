@file:UseSerializers(OGSBooleanSerializer::class)

package io.zenandroid.onlinego.data.model.ogs

import kotlinx.serialization.Serializable
import io.zenandroid.onlinego.data.ogs.OGSBooleanSerializer
import kotlinx.serialization.UseSerializers

/**
 * Created by alex on 03/11/2017.
 */
@Serializable
data class Channel (

    var country: String? = null,
    var id: String? = null,
    var name: String? = null,
    var rtl: Boolean? = null

)