package io.zenandroid.onlinego.spectate

import io.zenandroid.onlinego.model.ogs.GameList

/**
 * Created by alex on 05/11/2017.
 */
interface SpectateContract {
    interface View {
        var games: GameList?

    }
    interface Presenter {
        fun subscribe()
        fun unsubscribe()
    }
}