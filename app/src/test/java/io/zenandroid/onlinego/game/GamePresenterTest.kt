package io.zenandroid.onlinego.game

import com.nhaarman.mockito_kotlin.mock
import io.zenandroid.onlinego.model.ogs.Game
import io.zenandroid.onlinego.ogs.Clock
import io.zenandroid.onlinego.ogs.GameData
import io.zenandroid.onlinego.ogs.OGSService
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Created by alex on 24/11/2017.
 */
class GamePresenterTest {

    val view = mock<GameContract.View>()
    val service = mock<OGSService>()

    @Test
    fun whenFormatMillisIsCalled_thenCorrectValueIsReturned() {
        val game = Game(null, null, 0L, Game.Phase.PLAY, null, 19, 19, null, null, null, null,0L,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null, null,null,null,null,null,null,null,
                GameData(null,null,null,19,null,null,null,null,null,null,null,null,null,null,null,Game.Phase.PLAY,null, listOf(),null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null, Clock(0L,0L,0L,0L,null,0L,null,0L,null,false,null,0,0),null,null,null,null,null,null,null)
        )
        val presenter = GamePresenter(view, service, game)

        val MILLIS = 1L
        val SECONDS = 1000 * MILLIS
        val MINUTES = 60 * SECONDS
        val HOURS = 60 * MINUTES
        val DAYS = 24 * HOURS
        val WEEKS = 7 * DAYS

        assertEquals("0.0s", presenter.formatMillis(49 * MILLIS))
        assertEquals("0.1s", presenter.formatMillis(51 * MILLIS))
        assertEquals("0.1s", presenter.formatMillis(120 * MILLIS))
        assertEquals("0.9s", presenter.formatMillis(949 * MILLIS))
        assertEquals("1.0s", presenter.formatMillis(951 * MILLIS))
        assertEquals("1.0s", presenter.formatMillis(1 * SECONDS))
        assertEquals("1.5s", presenter.formatMillis(1 * SECONDS + 499 * MILLIS))
        assertEquals("1.5s", presenter.formatMillis(1 * SECONDS + 501 * MILLIS))
        assertEquals("9.9s", presenter.formatMillis(9 * SECONDS + 949 * MILLIS))
        assertEquals("10.0s", presenter.formatMillis(9 * SECONDS + 951 * MILLIS))
        assertEquals("10s", presenter.formatMillis(10 * SECONDS))
        assertEquals("10s", presenter.formatMillis(10 * SECONDS + 499 * MILLIS))
        assertEquals("11s", presenter.formatMillis(10 * SECONDS + 501 * MILLIS))
        assertEquals("59s", presenter.formatMillis(59 * SECONDS + 499 * MILLIS))
        assertEquals("60s", presenter.formatMillis(59 * SECONDS + 501 * MILLIS))
        assertEquals("1 : 00", presenter.formatMillis(1 * MINUTES + 0 * SECONDS))
        assertEquals("1 : 00", presenter.formatMillis(1 * MINUTES + 0 * SECONDS + 501 * MILLIS))
        assertEquals("1 : 59", presenter.formatMillis(1 * MINUTES + 59 * SECONDS + 499 * MILLIS))
        assertEquals("1 : 59", presenter.formatMillis(1 * MINUTES + 59 * SECONDS + 501 * MILLIS))
        assertEquals("2 : 00", presenter.formatMillis(2 * MINUTES + 0 * SECONDS + 0 * MILLIS))
        assertEquals("10 : 00", presenter.formatMillis(10 * MINUTES + 0 * SECONDS + 0 * MILLIS))
        assertEquals("59 : 00", presenter.formatMillis(59 * MINUTES + 0 * SECONDS + 0 * MILLIS))
        assertEquals("59 : 59", presenter.formatMillis(59 * MINUTES + 59 * SECONDS + 999 * MILLIS))
        assertEquals("1h 00m", presenter.formatMillis(1 * HOURS + 0 * MINUTES))
        assertEquals("1h 59m", presenter.formatMillis(1 * HOURS + 59 * MINUTES + 59 * SECONDS))
        assertEquals("2h 00m", presenter.formatMillis(2 * HOURS + 0 * MINUTES))
        assertEquals("23h 59m", presenter.formatMillis(23 * HOURS + 59 * MINUTES + 59 * SECONDS))
        assertEquals("1 day(s)", presenter.formatMillis(1 * DAYS))
        assertEquals("1 day(s)", presenter.formatMillis(1 * DAYS + 23 * HOURS + 59 * MINUTES))
        assertEquals("2 day(s)", presenter.formatMillis(2 * DAYS))
        assertEquals("6 day(s)", presenter.formatMillis(6 * DAYS + 23 * HOURS + 59 * MINUTES))
        assertEquals("1 week(s)", presenter.formatMillis(1 * WEEKS))
        assertEquals("1 week(s)", presenter.formatMillis(1 * WEEKS + 6 * DAYS + 23 * HOURS))
        assertEquals("2 week(s)", presenter.formatMillis(2 * WEEKS))



    }
}