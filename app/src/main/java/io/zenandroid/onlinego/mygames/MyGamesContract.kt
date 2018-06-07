package io.zenandroid.onlinego.mygames

import io.zenandroid.onlinego.model.local.DbGame
import io.zenandroid.onlinego.model.ogs.Game
import io.zenandroid.onlinego.ogs.Clock
import io.zenandroid.onlinego.model.ogs.GameData
import io.zenandroid.onlinego.ogs.Move

/**
 * Created by alex on 05/11/2017.
 */
interface MyGamesContract {
    interface View {
        fun setGameData(id: Long, gameData: GameData)
        fun doMove(id: Long, move: Move)
        fun addOrUpdateGame(game: DbGame)
        fun clearGames()
        fun navigateToGameScreen(game: DbGame)
        fun setClock(id: Long, clock: Clock)
        fun removeGame(game: DbGame)
        fun setGames(games: List<DbGame>)
        fun setLoading(loading: Boolean)
        fun setHistoricGames(games: List<DbGame>)
    }
    interface Presenter {
        fun subscribe()
        fun unsubscribe()
        fun onGameSelected(game: DbGame)
    }
}