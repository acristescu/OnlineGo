package io.zenandroid.onlinego.data.model.local

import kotlinx.serialization.Serializable

@Serializable
data class InitialState (
        val black: String? = null,
        val white: String? = null,
)