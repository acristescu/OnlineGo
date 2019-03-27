package io.zenandroid.onlinego.model.local

import android.arch.persistence.room.*
import android.arch.persistence.room.ForeignKey.NO_ACTION
import io.zenandroid.onlinego.model.ogs.Phase

@Entity
data class GameNotification (
        @PrimaryKey
        val gameId: Long,
        val moves: MutableList<MutableList<Int>>?,
        val phase: Phase?
)

class GameNotificationWithDetails {
        @Embedded
        lateinit var notification: GameNotification

        @Relation(parentColumn = "gameId", entityColumn = "id", entity = Game::class)
        var games: List<Game> = listOf()
}