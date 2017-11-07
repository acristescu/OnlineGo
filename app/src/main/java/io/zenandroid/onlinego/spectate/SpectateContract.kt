package io.zenandroid.onlinego.spectate

import io.zenandroid.onlinego.model.ogs.GameList
import io.zenandroid.onlinego.ogs.GameData
import io.zenandroid.onlinego.ogs.Move

/**
 * Created by alex on 05/11/2017.
 */
interface SpectateContract {
    interface View {
        var games: GameList?
        fun setGameData(index: Int, gameData: GameData)
        fun doMove(index: Int, move: Move)
    }
    interface Presenter {
        fun subscribe()
        fun unsubscribe()
    }
}