package io.zenandroid.onlinego.data.model.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ChallengeNotification(
    @PrimaryKey val id: Long,
)