package io.zenandroid.onlinego.data.model.local

import androidx.room.Embedded

data class Clock(
        var lastMove: Long,
        var expiration: Long? = null,
        var now: Long? = null,
        var receivedAt: Long,

        var whiteTimeSimple: Long?,

        @Embedded(prefix = "white_")
        var whiteTime: Time?,

        var blackTimeSimple: Long?,

        @Embedded(prefix = "black_")
        var blackTime: Time?,

        var newPausedSince: Long? = null,
        var newPausedState: Boolean? = null,

        var startMode: Boolean?
) {
    companion object {
        fun fromOGSClock(clock: io.zenandroid.onlinego.data.ogs.OGSClock?): Clock? =
                clock?.let {
                    Clock(
                            lastMove = it.last_move,
                            expiration = it.expiration,
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