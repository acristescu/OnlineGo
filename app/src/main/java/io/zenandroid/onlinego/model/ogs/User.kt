package io.zenandroid.onlinego.model.ogs

/**
 * Created by alex on 03/11/2017.
 */
data class User (

    var username: String,
    var ranking: Int = 0,
    var ui_class: String? = null,
    var is_tournament_moderator: Boolean? = null,
    var can_create_tournaments: Boolean? = null,
    var setup_rank_set: Boolean? = null,
    var country: String? = null,
    var pro: Boolean? = null,
    var aga_valid: Any? = null,
    var supporter: Boolean? = null,
    var provisional: Int? = null,
    var is_moderator: Boolean? = null,
    var is_superuser: Boolean? = null,
    var supporter_last_nagged: String? = null,
    var anonymous: Boolean? = null,
    var tournament_admin: Boolean? = null,
    var auto_advance_after_submit: Boolean? = null,
    var hide_recently_finished_games: Boolean? = null,
    var id: Long = 0,
    var icon: String? = null

)