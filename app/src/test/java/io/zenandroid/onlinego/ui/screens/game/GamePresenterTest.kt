package io.zenandroid.onlinego.ui.screens.game

import io.zenandroid.onlinego.di.allKoinModules
import io.zenandroid.onlinego.utils.formatMillis
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTestRule

/**
 * Created by alex on 24/11/2017.
 */
class GamePresenterTest {

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.DEBUG)
        modules(allKoinModules)
    }
    @Test
    fun whenFormatMillisIsCalled_thenCorrectValueIsReturned() {
        val MILLIS = 1L
        val SECONDS = 1000 * MILLIS
        val MINUTES = 60 * SECONDS
        val HOURS = 60 * MINUTES
        val DAYS = 24 * HOURS
        val WEEKS = 7 * DAYS

        assertEquals("0.0s", formatMillis(49 * MILLIS))
        assertEquals("0.1s", formatMillis(51 * MILLIS))
        assertEquals("0.1s", formatMillis(120 * MILLIS))
        assertEquals("0.9s", formatMillis(949 * MILLIS))
        assertEquals("1.0s", formatMillis(951 * MILLIS))
        assertEquals("1.0s", formatMillis(1 * SECONDS))
        assertEquals("1.5s", formatMillis(1 * SECONDS + 499 * MILLIS))
        assertEquals("1.5s", formatMillis(1 * SECONDS + 501 * MILLIS))
        assertEquals("9.9s", formatMillis(9 * SECONDS + 949 * MILLIS))
        assertEquals("10.0s", formatMillis(9 * SECONDS + 951 * MILLIS))
        assertEquals("10.0s", formatMillis(10 * SECONDS))
        assertEquals("11s", formatMillis(10 * SECONDS + 499 * MILLIS))
        assertEquals("11s", formatMillis(10 * SECONDS + 501 * MILLIS))
        assertEquals("1 : 00", formatMillis(59 * SECONDS + 499 * MILLIS))
        assertEquals("1 : 00", formatMillis(59 * SECONDS + 501 * MILLIS))
        assertEquals("1 : 00", formatMillis(1 * MINUTES + 0 * SECONDS))
        assertEquals("1 : 01", formatMillis(1 * MINUTES + 0 * SECONDS + 501 * MILLIS))
        assertEquals("2 : 00", formatMillis(1 * MINUTES + 59 * SECONDS + 499 * MILLIS))
        assertEquals("2 : 00", formatMillis(1 * MINUTES + 59 * SECONDS + 501 * MILLIS))
        assertEquals("2 : 00", formatMillis(2 * MINUTES + 0 * SECONDS + 0 * MILLIS))
        assertEquals("10 : 00", formatMillis(10 * MINUTES + 0 * SECONDS + 0 * MILLIS))
        assertEquals("59 : 00", formatMillis(59 * MINUTES + 0 * SECONDS + 0 * MILLIS))
        assertEquals("1h 00m", formatMillis(59 * MINUTES + 59 * SECONDS + 999 * MILLIS))
        assertEquals("1h 00m", formatMillis(1 * HOURS + 0 * MINUTES))
        assertEquals("1h 59m", formatMillis(1 * HOURS + 59 * MINUTES + 59 * SECONDS))
        assertEquals("2h 00m", formatMillis(2 * HOURS + 0 * MINUTES))
        assertEquals("23h 59m", formatMillis(23 * HOURS + 59 * MINUTES + 59 * SECONDS))
        assertEquals("1 day", formatMillis(1 * DAYS))
        assertEquals("1 day", formatMillis(1 * DAYS + 23 * HOURS + 59 * MINUTES))
        assertEquals("2 days", formatMillis(2 * DAYS))
        assertEquals("6 days", formatMillis(6 * DAYS + 23 * HOURS + 59 * MINUTES))
        assertEquals("1 week", formatMillis(1 * WEEKS))
        assertEquals("1 week", formatMillis(1 * WEEKS + 6 * DAYS + 23 * HOURS))
        assertEquals("2 weeks", formatMillis(2 * WEEKS))
    }
}