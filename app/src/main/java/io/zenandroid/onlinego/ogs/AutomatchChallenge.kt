package io.zenandroid.onlinego.ogs

import io.reactivex.Flowable

/**
 * Created by alex on 22/02/2018.
 */
class AutomatchChallenge(val uuid: String, val start: Flowable<AutomatchChallengeSuccess>)
enum class Size {
    SMALL, MEDIUM, LARGE;

    fun getText() = when(this) {
        SMALL -> "9x9"
        MEDIUM -> "13x13"
        LARGE -> "19x19"
    }
}
enum class Speed {
    BLITZ, NORMAL, LONG;
    fun getText() = when(this) {
        BLITZ -> "blitz"
        NORMAL -> "live"
        LONG -> "correspondence"
    }
}

class AutomatchChallengeSuccess(val game_id: Long, val uuid: String)