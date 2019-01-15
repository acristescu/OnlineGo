package io.zenandroid.onlinego.model.local

import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.ForeignKey.NO_ACTION
import android.arch.persistence.room.PrimaryKey
import io.zenandroid.onlinego.model.ogs.Phase

@Entity(foreignKeys = [
    ForeignKey(
            entity = Game::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = NO_ACTION
    )]
)
data class GameNotification (
        @PrimaryKey
        val gameId: Long,
        val moves: MutableList<MutableList<Int>>?,
        val phase: Phase?
)