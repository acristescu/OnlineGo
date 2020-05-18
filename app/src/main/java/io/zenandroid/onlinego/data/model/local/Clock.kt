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

        var startMode: Boolean?
) {
    companion object {
        fun fromOGSClock(clock: io.zenandroid.onlinego.data.ogs.OGSClock?): Clock? =
                clock?.let {
                    Clock(
                            lastMove = clock.last_move,
                            expiration = clock.expiration,
                            now = clock.now,
                            receivedAt = if (clock.receivedAt != 0L) clock.receivedAt else System.currentTimeMillis(),
                            whiteTimeSimple = clock.whiteTimeSimple,
                            whiteTime = clock.whiteTime,
                            blackTimeSimple = clock.blackTimeSimple,
                            blackTime = clock.blackTime,
                            startMode = clock.start_mode
                    )
                }
    }
}