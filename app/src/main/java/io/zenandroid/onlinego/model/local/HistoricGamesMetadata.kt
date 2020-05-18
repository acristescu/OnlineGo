package io.zenandroid.onlinego.model.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class HistoricGamesMetadata(
        @PrimaryKey
        val id: Long = 0L,
        val oldestGameEnded: Long? = null,
        val newestGameEnded: Long? = null,
        val loadedOldestGame: Boolean? = null
)
