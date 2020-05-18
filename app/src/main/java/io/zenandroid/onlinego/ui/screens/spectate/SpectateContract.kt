package io.zenandroid.onlinego.ui.screens.spectate

import io.zenandroid.onlinego.data.model.ogs.OGSGame
import io.zenandroid.onlinego.data.model.ogs.GameList
import io.zenandroid.onlinego.data.model.ogs.GameData
import io.zenandroid.onlinego.data.ogs.Move

/**
 * Created by alex on 05/11/2017.
 */
@Deprecated("")
interface SpectateContract {
    interface View {
        var games: GameList?
        fun setGameData(id: Long, gameData: GameData)
        fun doMove(id: Long, move: Move)
        fun navigateToGameScreen(game: OGSGame)
    }
    interface Presenter {
        fun subscribe()
        fun unsubscribe()
        fun onGameSelected(game: OGSGame)
    }
}