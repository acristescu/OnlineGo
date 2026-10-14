package io.zenandroid.onlinego.data.model.ogs

import kotlinx.serialization.Serializable

/**
 * Created by alex on 03/11/2017.
 */
@Serializable
data class Ogs (
    var channels: List<Channel>? = null,
    var preferences: Preferences? = null
)