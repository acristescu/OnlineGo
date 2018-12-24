package io.zenandroid.onlinego.model.ogs

/**
 * Created by alex on 03/11/2017.
 */
data class Bot (

    var username: String? = null,
    var rating: Double? = null,
    var ranking: Int? = null,
    var country: String? = null,
    var ui_class: String? = null,
    var id: Long,
    var icon: String? = null,

    var ratings: OGSPlayer.Ratings? = null

)