package io.zenandroid.onlinego.puzzle

/**
 * Created by alex on 05/11/2017.
 */
interface PuzzleContract {
    interface View {
    }
    interface Presenter {
        fun subscribe()
        fun unsubscribe()
    }
}