package io.zenandroid.onlinego.stats

/**
 * Created by alex on 05/11/2017.
 */
interface StatsContract {
    interface View {
    }
    interface Presenter {
        fun subscribe()
        fun unsubscribe()
    }
}