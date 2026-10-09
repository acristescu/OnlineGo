package io.zenandroid.onlinego.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class ClockFaceTest {

    @Test
    fun whenClockFaceIsCalled_thenCorrectValueIsReturned() {
        val MILLIS = 1L
        val SECONDS = 1000 * MILLIS
        val MINUTES = 60 * SECONDS
        val HOURS = 60 * MINUTES
        val DAYS = 24 * HOURS
        val WEEKS = 7 * DAYS

        assertEquals(ClockFace.Tenths(0), clockFace(-5 * SECONDS))
        assertEquals(ClockFace.Tenths(0), clockFace(0))
        assertEquals(ClockFace.Tenths(0), clockFace(49 * MILLIS))
        assertEquals(ClockFace.Tenths(1), clockFace(51 * MILLIS))
        assertEquals(ClockFace.Tenths(1), clockFace(120 * MILLIS))
        assertEquals(ClockFace.Tenths(9), clockFace(949 * MILLIS))
        assertEquals(ClockFace.Tenths(10), clockFace(951 * MILLIS))
        assertEquals(ClockFace.Tenths(10), clockFace(1 * SECONDS))
        assertEquals(ClockFace.Tenths(15), clockFace(1 * SECONDS + 499 * MILLIS))
        assertEquals(ClockFace.Tenths(15), clockFace(1 * SECONDS + 501 * MILLIS))
        assertEquals(ClockFace.Tenths(99), clockFace(9 * SECONDS + 949 * MILLIS))
        assertEquals(ClockFace.Tenths(100), clockFace(9 * SECONDS + 951 * MILLIS))
        assertEquals(ClockFace.Tenths(100), clockFace(10 * SECONDS))
        assertEquals(ClockFace.Seconds(11), clockFace(10 * SECONDS + 499 * MILLIS))
        assertEquals(ClockFace.Seconds(11), clockFace(10 * SECONDS + 501 * MILLIS))
        assertEquals(ClockFace.MinutesSeconds(1, 0), clockFace(59 * SECONDS + 499 * MILLIS))
        assertEquals(ClockFace.MinutesSeconds(1, 0), clockFace(59 * SECONDS + 501 * MILLIS))
        assertEquals(ClockFace.MinutesSeconds(1, 0), clockFace(1 * MINUTES + 0 * SECONDS))
        assertEquals(
            ClockFace.MinutesSeconds(1, 1),
            clockFace(1 * MINUTES + 0 * SECONDS + 501 * MILLIS)
        )
        assertEquals(
            ClockFace.MinutesSeconds(2, 0),
            clockFace(1 * MINUTES + 59 * SECONDS + 499 * MILLIS)
        )
        assertEquals(
            ClockFace.MinutesSeconds(2, 0),
            clockFace(1 * MINUTES + 59 * SECONDS + 501 * MILLIS)
        )
        assertEquals(
            ClockFace.MinutesSeconds(2, 0),
            clockFace(2 * MINUTES + 0 * SECONDS + 0 * MILLIS)
        )
        assertEquals(
            ClockFace.MinutesSeconds(10, 0),
            clockFace(10 * MINUTES + 0 * SECONDS + 0 * MILLIS)
        )
        assertEquals(
            ClockFace.MinutesSeconds(59, 0),
            clockFace(59 * MINUTES + 0 * SECONDS + 0 * MILLIS)
        )
        assertEquals(
            ClockFace.HoursMinutes(1, 0),
            clockFace(59 * MINUTES + 59 * SECONDS + 999 * MILLIS)
        )
        assertEquals(ClockFace.HoursMinutes(1, 0), clockFace(1 * HOURS + 0 * MINUTES))
        assertEquals(
            ClockFace.HoursMinutes(1, 59),
            clockFace(1 * HOURS + 59 * MINUTES + 59 * SECONDS)
        )
        assertEquals(ClockFace.HoursMinutes(2, 0), clockFace(2 * HOURS + 0 * MINUTES))
        assertEquals(
            ClockFace.HoursMinutes(23, 59),
            clockFace(23 * HOURS + 59 * MINUTES + 59 * SECONDS)
        )
        assertEquals(ClockFace.Hours(24), clockFace(1 * DAYS))
        assertEquals(ClockFace.Hours(47), clockFace(1 * DAYS + 23 * HOURS + 59 * MINUTES))
        assertEquals(ClockFace.Hours(48), clockFace(2 * DAYS))
        assertEquals(ClockFace.Days(3), clockFace(3 * DAYS))
        assertEquals(ClockFace.DaysHours(6, 23), clockFace(6 * DAYS + 23 * HOURS + 59 * MINUTES))
        assertEquals(ClockFace.Days(7), clockFace(1 * WEEKS + 10 * HOURS))
        assertEquals(ClockFace.Days(7), clockFace(1 * WEEKS))
        assertEquals(ClockFace.Days(13), clockFace(1 * WEEKS + 6 * DAYS + 23 * HOURS))
        assertEquals(ClockFace.Days(14), clockFace(2 * WEEKS))
        assertEquals(ClockFace.Unlimited, clockFace(Long.MAX_VALUE))
    }
}
