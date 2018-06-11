package io.zenandroid.onlinego.model.local

import android.arch.persistence.room.PrimaryKey

/**
 * Created by 44108952 on 05/06/2018.
 */
data class Player(
    @PrimaryKey var id: Long,
    var username: String,
    var rating: Double?,
    var country: String?,
    var icon: String?
)