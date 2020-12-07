package io.zenandroid.onlinego.ui.screens.supporter

/**
 * Created by alex on 05/11/2017.
 */
interface SupporterContract {
    interface View {
        fun showError(t: Throwable)
        fun renderState(state: State)
    }
    interface Presenter {
        fun subscribe()
        fun unsubscribe()
        fun onSubscribeClick()
        fun onUserDragSlider(value: Float)
    }
}