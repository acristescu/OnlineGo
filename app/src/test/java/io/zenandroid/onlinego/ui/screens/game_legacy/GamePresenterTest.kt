package io.zenandroid.onlinego.ui.screens.game_legacy

import io.zenandroid.onlinego.data.model.ogs.User
import io.zenandroid.onlinego.di.allKoinModules
import io.zenandroid.onlinego.utils.ClockFace
import io.zenandroid.onlinego.utils.appJson
import io.zenandroid.onlinego.utils.clockFace
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

    @Test
    fun testBooleanAsIntWorks() {
      val user = appJson.decodeFromString<User>(
        """
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