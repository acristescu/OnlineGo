package io.zenandroid.onlinego.ui.screens.main

import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.data.model.ogs.Size
import io.zenandroid.onlinego.data.model.ogs.Speed
import io.zenandroid.onlinego.data.model.ogs.ChallengeParams

/**
 * Created by alex on 14/03/2018.
 */
interface MainContract {
    interface View {
        fun showLogin()
        fun askForNotificationsPermission(delayed: Boolean)
    }
    interface Presenter {
        fun subscribe()
        fun unsubscribe()
    }
}