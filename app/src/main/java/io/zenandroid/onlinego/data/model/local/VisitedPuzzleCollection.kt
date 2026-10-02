package io.zenandroid.onlinego.data.model.local

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.time.Clock
import kotlin.time.Instant

@Entity
@Immutable
data class VisitedPuzzleCollection constructor(
    val collectionId: Long,
    val timestamp: Instant = Clock.System.now(),
    val count: Int = 1,
    @PrimaryKey(autoGenerate = true) val _id: Int? = null,
)
