package io.zenandroid.onlinego.game

import android.graphics.Point
import io.reactivex.Observable
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
        val cellSelection: Observable<Point>
        fun unselectMove()
        var interactive: Boolean
    }

    interface Presenter {
        fun subscribe()
        fun unsubscribe()

    }
}