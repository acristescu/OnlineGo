package io.zenandroid.onlinego.ui.screens.stats

import com.github.mikephil.charting.data.Entry
import io.zenandroid.onlinego.data.model.ogs.Glicko2HistoryItem
import io.zenandroid.onlinego.data.model.ogs.OGSPlayer

/**
 * Created by alex on 05/11/2017.
 */
interface StatsContract {
    interface View {
        var title: String?

        fun fillPlayerDetails(playerDetails: OGSPlayer)
        fun mostFacedOpponent(playerDetails: OGSPlayer, total: Int, won: Int)
        fun fillHighestWin(playerDetails: OGSPlayer, winningGame: Glicko2HistoryItem)
        fun fillHighestRank(highestRank: Float, highestRankTimestamp: Long)
        fun fillRankGraph(entries: List<Entry>)
        fun fillOutcomePieChart(lostCount: Int, wonCount: Int)
        fun fillCurrentForm(lastGames: List<Glicko2HistoryItem>)
        fun fillLongestStreak(length: Int, start: Long = 0L, end: Long = 0L)
    }
    interface Presenter {
        fun subscribe()
        fun unsubscribe()
    }
}