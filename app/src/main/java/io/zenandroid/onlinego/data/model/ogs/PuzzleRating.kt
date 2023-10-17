package io.zenandroid.onlinego.data.model.ogs

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity
data class PuzzleRating (
    @PrimaryKey var puzzleId: Long = -1,

    @Ignore var error: String? = null,
    var rating: Int = 0
) {
    constructor(puzzleId: Long, rating: Int) : this() {
        this.puzzleId = puzzleId
        this.rating = rating
    }
}
