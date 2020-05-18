package io.zenandroid.onlinego.ui.screens.challenges

import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.utils.addToDisposable
import io.zenandroid.onlinego.data.model.ogs.SeekGraphChallenge
import io.zenandroid.onlinego.data.ogs.OGSServiceImpl
import kotlin.math.abs

/**
 * Created by alex on 05/11/2017.
 */
@Deprecated("Obsolete")
class ChallengesPresenter(val view: ChallengesContract.View, private val service: OGSServiceImpl) : ChallengesContract.Presenter {

    private val challenges = mutableListOf<SeekGraphChallenge>()

    private val subscriptions = CompositeDisposable()

    override fun subscribe() {
        challenges.clear()
        view.removeAllChallenges()
        service.connectToChallenges()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
                .filter { it.challenge_id != null }
                .filter { canAccept(it) }
                .subscribe(this::setChallenge)
                .addToDisposable(subscriptions)
    }

    private fun setChallenge(challenge: SeekGraphChallenge) {
        if(challenge.delete != null) {
            challenges.filter { it.challenge_id == challenge.challenge_id }.forEach {
                view.removeChallenge(it)
                challenges.remove(it)
            }
        } else {
            challenges.add(challenge)
            view.addChallenge(challenge)
        }
        view.setOverlayVisibility(challenges.isEmpty())
    }

    override fun unsubscribe() {
        subscriptions.clear()
    }

    private fun canAccept(challenge: SeekGraphChallenge): Boolean {
        val myRank = service.uiConfig?.user?.ranking?.toDouble() ?: 0.0
        return myRank in challenge.min_rank .. challenge.max_rank &&
                (!challenge.ranked || abs(myRank - (challenge.rank ?: 0.0)) <= 9)
    }
}