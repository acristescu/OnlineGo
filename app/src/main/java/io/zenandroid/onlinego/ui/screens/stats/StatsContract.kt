package io.zenandroid.onlinego.ui.screens.stats

import io.zenandroid.onlinego.data.model.ogs.OGSPlayer

/**
 * Created by alex on 05/11/2017.
 */
interface StatsContract {
    interface View {
        fun fillPlayerDetails(playerDetails: OGSPlayer)
        fun fillPlayerStats(playerStats: String): Pair<Long, Long>
        fun mostFacedOpponent(playerDetails: OGSPlayer)
        fun highestWin(playerDetails: OGSPlayer)
    }
    interface Presenter {
        fun subscribe()
        fun unsubscribe()
    }
}