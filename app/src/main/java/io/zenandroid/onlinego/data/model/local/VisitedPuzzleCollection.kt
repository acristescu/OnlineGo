package io.zenandroid.onlinego.data.model.local

import android.os.Build
import android.util.Log
import androidx.compose.runtime.Immutable;
import androidx.annotation.RequiresApi
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.time.Instant

@RequiresApi(Build.VERSION_CODES.O)
@Entity
@Immutable
data class VisitedPuzzleCollection constructor(
    val collectionId: Long,
    val timestamp: Instant = Instant.now(),
    val count: Int = 1,
    @PrimaryKey(autoGenerate = true) val _id: Int? = null,
)
