package io.zenandroid.onlinego.data.model.ogs

import kotlinx.serialization.Serializable

@Serializable
data class CreateAccountRequest (val username: String, val password: String, val email: String, val ebi: String)

@Serializable
data class PasswordBody (val password: String)

@Serializable
data class AcknowledgeWarningRequest(val accept: Boolean = true)
