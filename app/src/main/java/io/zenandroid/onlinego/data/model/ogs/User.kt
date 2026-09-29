@file:UseSerializers(
  OGSBooleanSerializer::class,
  AnySerializer::class,
  LenientIntSerializer::class,
  LenientLongSerializer::class
)

package io.zenandroid.onlinego.data.model.ogs

import kotlinx.serialization.Serializable
import io.zenandroid.onlinego.data.ogs.AnySerializer
import io.zenandroid.onlinego.data.ogs.OGSBooleanSerializer
import kotlinx.serialization.UseSerializers
import io.zenandroid.onlinego.data.ogs.LenientIntSerializer
import io.zenandroid.onlinego.data.ogs.LenientLongSerializer

/**
 * Created by alex on 03/11/2017.
 */
@Serializable
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