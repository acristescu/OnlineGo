package io.zenandroid.onlinego.mygames

import io.zenandroid.onlinego.model.ogs.Game
import io.zenandroid.onlinego.ogs.Clock
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
        fun navigateToGameScreen(game: Game)
        fun setClock(id: Long, clock: Clock)
        fun removeGame(game: Game)
        fun setGames(games: List<Game>)
    }
    interface Presenter {
        fun subscribe()
        fun unsubscribe()
        fun onGameSelected(game: Game)
    }
}