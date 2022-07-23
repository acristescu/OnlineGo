package io.zenandroid.onlinego.data.model.local

import androidx.room.Embedded

data class Clock(
        val lastMove: Long,
        val expiration: Long? = null,
        val now: Long? = null,
        val receivedAt: Long,

        val whiteTimeSimple: Long?,

        @Embedded(prefix = "white_")
        val whiteTime: Time?,

        val blackTimeSimple: Long?,

        @Embedded(prefix = "black_")
        val blackTime: Time?,

        val newPausedSince: Long? = null,
        val newPausedState: Boolean? = null,

        val startMode: Boolean?
) {
    companion object {
        fun fromOGSClock(clock: io.zenandroid.onlinego.data.ogs.OGSClock?): Clock? =
                clock?.let {
                    Clock(
                            lastMove = it.last_move,
                            expiration = it.expiration?.toLong(),
                            now = it.now,
                            receivedAt = if (it.receivedAt != 0L) it.receivedAt else System.currentTimeMillis(),
                            whiteTimeSimple = it.whiteTimeSimple,
                            whiteTime = it.whiteTime,
                            blackTimeSimple = it.blackTimeSimple,
                            blackTime = it.blackTime,
                            startMode = it.start_mode,
                            newPausedSince = it.pause?.paused_since,
                            newPausedState = it.pause?.paused
                    )
                }
    }
}