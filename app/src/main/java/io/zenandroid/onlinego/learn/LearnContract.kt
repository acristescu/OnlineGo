package io.zenandroid.onlinego.learn

/**
 * Created by alex on 05/11/2017.
 */
interface LearnContract {
    interface View {
    }
    interface Presenter {
        fun subscribe()
        fun unsubscribe()
    }
}