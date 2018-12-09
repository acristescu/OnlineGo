package io.zenandroid.onlinego.challenges

import io.zenandroid.onlinego.model.ogs.SeekGraphChallenge

/**
 * Created by alex on 05/11/2017.
 */
@Deprecated("Obsolete")
interface ChallengesContract {
    interface View {
        fun addChallenge(challenge: SeekGraphChallenge)
        fun removeChallenge(challenge: SeekGraphChallenge)
        fun removeAllChallenges()
        fun setOverlayVisibility(visibility: Boolean)
    }
    interface Presenter {
        fun subscribe()
        fun unsubscribe()
    }
}