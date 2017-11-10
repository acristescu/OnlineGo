package io.zenandroid.onlinego.mygames

import io.zenandroid.onlinego.model.ogs.Game
import io.zenandroid.onlinego.ogs.GameData
import io.zenandroid.onlinego.ogs.Move

/**
 * Created by alex on 05/11/2017.
 */
interface MyGamesContract {
    interface View {
        fun setGameData(id: Long, gameData: GameData)
        fun doMove(id: Long, move: Move)
        fun addGame(game: Game)
        fun clearGames()
    }
    interface Presenter {
        fun subscribe()
        fun unsubscribe()
    }
}