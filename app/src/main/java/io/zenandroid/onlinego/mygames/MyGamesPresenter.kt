package io.zenandroid.onlinego.mygames

import android.util.Log
import com.crashlytics.android.Crashlytics
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.extensions.addToDisposable
import io.zenandroid.onlinego.gamelogic.Util.isMyTurn
import io.zenandroid.onlinego.model.local.Challenge
import io.zenandroid.onlinego.model.local.Game
import io.zenandroid.onlinego.model.ogs.OGSAutomatch
import io.zenandroid.onlinego.ogs.ActiveGameRepository
import io.zenandroid.onlinego.ogs.AutomatchRepository
import io.zenandroid.onlinego.ogs.ChallengesRepository
import io.zenandroid.onlinego.ogs.OGSServiceImpl
import io.zenandroid.onlinego.utils.timeLeftForCurrentPlayer

/**
 * Created by alex on 05/11/2017.
 */
class MyGamesPresenter(
        private val view: MyGamesContract.View,
        private val gameRepository: ActiveGameRepository,
        private val challengesRepository: ChallengesRepository,
        private val automatchRepository: AutomatchRepository
) : MyGamesContract.Presenter {
    companion object {
        val TAG = MyGamesPresenter::class.java.simpleName
    }

    private val subscriptions = CompositeDisposable()

    override fun subscribe() {
        gameRepository.fetchActiveGames()
                .map(this::sortGames)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
                .subscribe(this::setGames, this::onError)
                .addToDisposable(subscriptions)
        gameRepository.fetchHistoricGames()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
                .subscribe(this::setHistoricGames, this::onError)
                .addToDisposable(subscriptions)
        challengesRepository.monitorChallenges()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
                .subscribe(this::setChallenges, this::onError)
                .addToDisposable(subscriptions)
        automatchRepository.automatchObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
                .subscribe(this::setAutomatches, this::onError)
                .addToDisposable(subscriptions)
        view.setLoading(true)
    }

    private fun sortGames(unsorted : List<Game>): List<Game> {
        return unsorted.sortedWith(Comparator { left, right ->
            when {
                isMyTurn(left) && !isMyTurn(right) -> -1
                !isMyTurn(left) && isMyTurn(right) -> 1
                else -> {
                    (timeLeftForCurrentPlayer(left) - timeLeftForCurrentPlayer(right)).toInt()
                }
            }
        })
    }

    private fun setGames(games : List<Game>) {
        view.setGames(games)
        view.setLoading(false)
    }

    private fun setHistoricGames(games: List<Game>) {
        view.setHistoricGames(games)
    }

    private fun setChallenges(challenges: List<Challenge>) {
        view.setChallenges(challenges)
    }

    private fun setAutomatches(automatches: List<OGSAutomatch>) {
        view.setAutomatches(automatches)
    }

    override fun onAutomatchCancelled(automatch: OGSAutomatch) {
        OGSServiceImpl.instance.cancelAutomatch(automatch)
    }

    override fun unsubscribe() {
        subscriptions.clear()
    }

    override fun onGameSelected(game: Game) {
        view.navigateToGameScreen(game)
    }

    private fun onError(t: Throwable) {
        if(t is retrofit2.HttpException) {
            Crashlytics.logException(Exception(t.response().errorBody()?.string(), t))
        } else {
            Crashlytics.logException(t)
        }

        Log.e(TAG, t.message, t)
        view.setLoading(false)
    }

    override fun onChallengeCancelled(challenge: Challenge) {
        OGSServiceImpl.instance.declineChallenge(challenge.id)
                .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
                .subscribe({}, this::onError)
                .addToDisposable(subscriptions)
    }

    override fun onChallengeAccepted(challenge: Challenge) {
        OGSServiceImpl.instance.acceptChallenge(challenge.id)
                .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
                .subscribe({}, this::onError)
                .addToDisposable(subscriptions)
    }

    override fun onChallengeDeclined(challenge: Challenge) {
        OGSServiceImpl.instance.declineChallenge(challenge.id)
                .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
                .subscribe({}, this::onError)
                .addToDisposable(subscriptions)
    }

}