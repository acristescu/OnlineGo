package io.zenandroid.onlinego.model.local

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

/**
 * Created by 44108952 on 05/06/2018.
 */
@Entity
data class DbGame(
        @PrimaryKey var id: Long,
        var width: Int,
        var height: Int
)