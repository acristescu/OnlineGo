package io.zenandroid.onlinego.main

import io.zenandroid.onlinego.model.ogs.Game

/**
 * Created by alex on 14/03/2018.
 */
interface MainContract {
    interface View {
        fun showError(msg: String?)
        var mainTitle: CharSequence?
        var subtitle: CharSequence?
        var notificationsButtonEnabled: Boolean
        var notificationsBadgeVisible: Boolean
        var notificationsBadgeCount: String?
        fun cancelNotification()
        fun updateNotification(sortedMyTurnGames: List<Game>)
        fun navigateToGameScreen(game: Game)
    }
    interface Presenter {
        fun subscribe()
        fun unsubscribe()
        fun onNotificationClicked()
    }
}