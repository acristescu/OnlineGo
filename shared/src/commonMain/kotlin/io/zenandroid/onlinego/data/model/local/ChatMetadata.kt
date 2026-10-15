package io.zenandroid.onlinego.data.model.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ChatMetadata (
    @PrimaryKey
    val id: Long = 0L,
    val latestMessageId: String
)