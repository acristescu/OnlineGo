package io.zenandroid.onlinego.ui.screens.game

import com.squareup.moshi.Moshi
import io.zenandroid.onlinego.data.model.ogs.User
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
        assertEquals("24h", formatMillis(1 * DAYS))
        assertEquals("47h", formatMillis(1 * DAYS + 23 * HOURS + 59 * MINUTES))
        assertEquals("48h", formatMillis(2 * DAYS))
        assertEquals("3 days", formatMillis(3 * DAYS))
        assertEquals("6d 23h", formatMillis(6 * DAYS + 23 * HOURS + 59 * MINUTES))
        assertEquals("7 days", formatMillis(1 * WEEKS + 10 * HOURS))
        assertEquals("7 days", formatMillis(1 * WEEKS))
        assertEquals("13 days", formatMillis(1 * WEEKS + 6 * DAYS + 23 * HOURS))
        assertEquals("14 days", formatMillis(2 * WEEKS))
    }

    @Test
    fun testBooleanAsIntWorks() {
        val moshi: Moshi = koinTestRule.koin.get()

        val user = moshi.adapter(User::class.java).fromJson("""
            {
              "anonymous":false,
              "id":1,
              "username":"aaa",
              "registration_date":"2014-08-02 18:13:19.269649+00:00",
              "ratings": {
                 "correspondence-9x9":{
                    "rating":1520.3359,
                    "deviation":70.7831,
                    "volatility":0.06
                 },
                 "correspondence-13x13":{
                    "rating":1594.28,
                    "deviation":175.7906,
                    "volatility":0.06
                 },
                 "correspondence-19x19":{
                    "rating":1779.1504,
                    "deviation":91.4745,
                    "volatility":0.06
                 }
              },
              "country":"gb",
              "professional":false,
              "ranking":23,
              "provisional":0,
              "pro": 1,
              "can_create_tournaments":true,
              "is_moderator":0,
              "is_superuser":false,
              "is_tournament_moderator":false,
              "supporter":true,
              "supporter_level":4,
              "tournament_admin":false,
              "ui_class":"supporter",
              "icon":"sss",
              "email":"aaa",
              "email_validated":true,
              "is_announcer":false
           }
        """.trimIndent()
        )

        assertEquals(false, user?.is_moderator )
        assertEquals(true, user?.supporter )
        assertEquals(true, user?.pro )
    }
}