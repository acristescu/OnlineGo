package io.zenandroid.onlinego.data.model.local

import androidx.compose.runtime.Immutable;
import androidx.room.Entity

@Entity
@Immutable
data class PuzzleCollectionSolutionMetadata constructor(
    val collectionId: Long,
    val count: Int = 0,
)
