package io.zenandroid.onlinego.ui.screens.mygames

import android.os.Bundle
import android.util.Log
import com.crashlytics.android.Crashlytics
import com.google.firebase.analytics.FirebaseAnalytics
import io.reactivex.Maybe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.utils.addToDisposable
import io.zenandroid.onlinego.gamelogic.RulesManager
import io.zenandroid.onlinego.gamelogic.Util.isMyTurn
import io.zenandroid.onlinego.data.model.local.Challenge
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.data.model.ogs.OGSAutomatch
import io.zenandroid.onlinego.data.ogs.*
import io.zenandroid.onlinego.data.repositories.*
import io.zenandroid.onlinego.utils.timeLeftForCurrentPlayer
import org.json.JSONObject

/**
 * Created by alex on 05/11/2017.
 */
class MyGamesPresenter(
        private val view: MyGamesContract.View,
        private val analytics: FirebaseAnalytics,
        private val activeGamesRepository: ActiveGamesRepository,
        private val finishedGamesRepository: FinishedGamesRepository,
        private val challengesRepository: ChallengesRepository,
        private val automatchRepository: AutomatchRepository,
        private val notificationsRepository: ServerNotificationsRepository
) : MyGamesContract.Presenter {
    companion object {
        val TAG = MyGamesPresenter::class.java.simpleName
    }

    private val subscriptions = CompositeDisposable()
    private var loadOlderGamesSubscription: Disposable? = null

    override fun subscribe() {
        activeGamesRepository.monitorActiveGames()
                .subscribeOn(Schedulers.io())
                .map(this::sortGames)
                .map(this::computePositions)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
                .subscribe(this::setGames, this::onError)
                .addToDisposable(subscriptions)
        activeGamesRepository.refreshActiveGames()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
                .subscribe({}, this::onError)
                .addToDisposable(subscriptions)
        finishedGamesRepository.getRecentlyFinishedGames()
                .subscribeOn(Schedulers.io())
                .map(this::computePositions)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
                .subscribe(this::setRecentGames, this::onError)
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
        automatchRepository.gameStartObservable
                .flatMapMaybe { it.game_id?.let { activeGamesRepository.getGameSingle(it).toMaybe() } ?: Maybe.empty() }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
                .subscribe(view::navigateToGameScreen, this::onError)
                .addToDisposable(subscriptions)
        notificationsRepository.notificationsObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
                .subscribe(this::onNotification, this::onError)
                .addToDisposable(subscriptions)

        view.needsMoreOlderGames
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
                .subscribe(this::onNeedMoreOlderGames, this::onError)
                .addToDisposable(subscriptions)

        if(view.isHistoricGamesSectionEmpty()) {
            onNeedMoreOlderGames(MoreDataRequest())
        }

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

    private fun computePositions(games: List<Game>): List<Game> =
        games.apply {
            forEach { it.position = RulesManager.replay(it, computeTerritory = false) }
        }

    private fun setGames(games : List<Game>) {
        view.setGames(games)
        view.setLoading(false)
    }

    private fun setRecentGames(games: List<Game>) {
        view.setRecentGames(games)
    }

    private fun setChallenges(challenges: List<Challenge>) {
        view.setChallenges(challenges)
    }

    private fun setAutomatches(automatches: List<OGSAutomatch>) {
        view.setAutomatches(automatches)
    }

    override fun onAutomatchCancelled(automatch: OGSAutomatch) {
        analytics.logEvent("new_game_cancelled", null)
        OGSServiceImpl.cancelAutomatch(automatch)
    }

    override fun unsubscribe() {
        subscriptions.clear()
    }

    override fun onGameSelected(game: Game) {
        analytics.logEvent("clicked_game", Bundle().apply {
            putLong("GAME_ID", game.id)
            putBoolean("ACTIVE_GAME", game.ended == null)
        })
        view.navigateToGameScreen(game)
    }

    private fun onError(t: Throwable) {
        if(t is retrofit2.HttpException) {
            if(t.code() in arrayOf(401, 403)) {
                Crashlytics.setLong("AUTO_LOGOUT", System.currentTimeMillis())
                OGSServiceImpl.logOut()
                view.showLoginScreen()
            } else {
                Crashlytics.logException(Exception(t.response()?.errorBody()?.string(), t))
            }
        } else {
            if(t is com.squareup.moshi.JsonDataException) {
                view.showMessage(
                        "OGS API error",
                        "An error occurred white talking to the OGS Server. This usually means the website devs have changed something in the API. Please report this error as the app will probably not work until we adapt to this change."
                )
            }
            Crashlytics.logException(t)
        }

        Log.e(TAG, t.message, t)
        view.setLoading(false)
    }

    override fun onChallengeCancelled(challenge: Challenge) {
        analytics.logEvent("challenge_cancelled", null)
        OGSServiceImpl.declineChallenge(challenge.id)
                .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
                .subscribe({}, this::onError)
                .addToDisposable(subscriptions)
    }

    override fun onChallengeAccepted(challenge: Challenge) {
        analytics.logEvent("challenge_accepted", null)
        OGSServiceImpl.acceptChallenge(challenge.id)
                .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
                .subscribe({}, this::onError)
                .addToDisposable(subscriptions)
    }

    override fun onChallengeDeclined(challenge: Challenge) {
        analytics.logEvent("challenge_declined", null)
        OGSServiceImpl.declineChallenge(challenge.id)
                .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
                .subscribe({}, this::onError)
                .addToDisposable(subscriptions)
    }

    private fun onNeedMoreOlderGames(request: MoreDataRequest) {
        loadOlderGamesSubscription?.dispose()
        loadOlderGamesSubscription =
                finishedGamesRepository.getHistoricGames(request.game?.ended)
                        .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
                        .distinctUntilChanged()
                        .doOnNext { view.setLoadingMoreHistoricGames(it.loading) }
                        .map { it.games }
                        .map(this::computePositions)
                        .subscribe(view::appendHistoricGames, this::onError)
        loadOlderGamesSubscription?.addToDisposable(subscriptions)
    }

    private fun onNotification(notification: JSONObject) {
        if(notification["type"] == "gameOfferRejected") {
            notificationsRepository.acknowledgeNotification(notification)
            val message = if(notification.has("message") && notification["message"].toString() != "null") "Message is:\n\n${notification["message"]}" else ""
            if (notification["name"].toString() == "Bot Match")
                view.showMessage("Bot rejected challenge", "This might happen because the opponent's maintainer has set some conditions on the challenge parameters. $message")
            else
                view.showMessage("Opponent rejected challenge", "You may try again or otherwise contact the opponent to clarify his/her reasons for the rejection. $message")
            analytics.logEvent("bot_refused_challenge", null)
            Crashlytics.log("Bot refused challenge. $message")
        }
    }

}