package io.zenandroid.onlinego.data.model.local

import androidx.room.*
import io.zenandroid.onlinego.data.model.ogs.Phase

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