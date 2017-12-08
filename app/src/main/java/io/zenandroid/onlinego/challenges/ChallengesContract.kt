package io.zenandroid.onlinego.spectate

import io.zenandroid.onlinego.model.ogs.Challenge

/**
 * Created by alex on 05/11/2017.
 */
interface ChallengesContract {
    interface View {
        fun addChallenge(challenge: Challenge)
        fun removeChallenge(challenge: Challenge)
        fun removeAllChallenges()
        fun setOverlayVisibility(visibility: Boolean)
    }
    interface Presenter {
        fun subscribe()
        fun unsubscribe()
    }
}