package io.zenandroid.onlinego.spectate

import io.zenandroid.onlinego.model.ogs.GameList
import io.zenandroid.onlinego.ogs.GameData

/**
 * Created by alex on 05/11/2017.
 */
interface SpectateContract {
    interface View {
        var games: GameList?
        fun setGameData(index: Int, gameData: GameData?)
    }
    interface Presenter {
        fun subscribe()
        fun unsubscribe()
    }
}