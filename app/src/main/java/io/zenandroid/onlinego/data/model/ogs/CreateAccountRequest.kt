package io.zenandroid.onlinego.data.model.ogs

data class CreateAccountRequest (val username: String, val password: String, val email: String, val ebi: String)

data class PasswordBody (val password: String)