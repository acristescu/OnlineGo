package io.zenandroid.onlinego.mygames

import io.zenandroid.onlinego.model.local.DbGame

/**
 * Created by alex on 05/11/2017.
 */
interface MyGamesContract {
    interface View {
        fun navigateToGameScreen(game: DbGame)
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