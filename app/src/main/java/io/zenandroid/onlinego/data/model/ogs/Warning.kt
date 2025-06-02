package io.zenandroid.onlinego.data.model.ogs

data class Warning(
    val id: Int?,
    val created: String?,
    val player_id: Int?,
    val moderator: Int?,
    val text: String?,
    val message_id: String?,
    val severity: String?,
    val interpolation_data: String?
)

