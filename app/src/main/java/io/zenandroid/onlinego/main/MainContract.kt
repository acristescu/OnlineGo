package io.zenandroid.onlinego.main

import io.zenandroid.onlinego.model.local.Game
import io.zenandroid.onlinego.model.ogs.OGSGame
import io.zenandroid.onlinego.model.ogs.Size
import io.zenandroid.onlinego.model.ogs.Speed
import io.zenandroid.onlinego.newchallenge.ChallengeParams

/**
 * Created by alex on 14/03/2018.
 */
interface MainContract {
    interface View {
        fun showError(msg: String?)
        var notificationsButtonEnabled: Boolean
        var notificationsBadgeVisible: Boolean
        var notificationsBadgeCount: String?
        fun cancelNotification()
        fun updateNotification(sortedMyTurnGames: List<OGSGame>)
        fun navigateToGameScreen(game: Game)
        fun showLogin()
        fun vibrate()
    }
    interface Presenter {
        fun subscribe()
        fun unsubscribe()
        fun onNotificationClicked()
        fun onStartSearch(sizes: List<Size>, speed: Speed)
        fun onNewBotChallenge(challengeParams: ChallengeParams)
    }
}