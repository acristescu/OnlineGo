package io.zenandroid.onlinego.ui.screens.main

import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.data.model.ogs.OGSGame
import io.zenandroid.onlinego.data.model.ogs.Size
import io.zenandroid.onlinego.data.model.ogs.Speed
import io.zenandroid.onlinego.ui.screens.newchallenge.ChallengeParams

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
        fun onNewFriendChallenge(challengeParams: ChallengeParams)
    }
}