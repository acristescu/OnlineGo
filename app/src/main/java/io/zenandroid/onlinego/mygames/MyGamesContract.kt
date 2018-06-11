package io.zenandroid.onlinego.mygames

import io.zenandroid.onlinego.model.local.Game

/**
 * Created by alex on 05/11/2017.
 */
interface MyGamesContract {
    interface View {
        fun navigateToGameScreen(game: Game)
        fun setGames(games: List<Game>)
        fun setLoading(loading: Boolean)
        fun setHistoricGames(games: List<Game>)
    }
    interface Presenter {
        fun subscribe()
        fun unsubscribe()
        fun onGameSelected(game: Game)
    }
}