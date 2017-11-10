package io.zenandroid.onlinego.game

import io.zenandroid.onlinego.model.Position

/**
 * Created by alex on 10/11/2017.
 */
interface GameContract {

    interface View {
        var boardSize: Int
        var blackName: String?
        var blackRank: String?
        var whiteName: String?
        var whiteRank: String?
        var position: Position?
        var highlightBlackName: Boolean
        var highlightWhiteName: Boolean
    }

    interface Presenter {
        fun subscribe()
        fun unsubscribe()

    }
}