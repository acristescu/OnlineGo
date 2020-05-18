package io.zenandroid.onlinego.ui.screens.mygames

import io.reactivex.Observable
import io.zenandroid.onlinego.data.model.local.Challenge
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.data.model.ogs.OGSAutomatch

/**
 * Created by alex on 05/11/2017.
 */
interface MyGamesContract {
    interface View {
        fun navigateToGameScreen(game: Game)
        fun setGames(games: List<Game>)
        fun setLoading(loading: Boolean)
        fun setRecentGames(games: List<Game>)
        fun setChallenges(challenges: List<Challenge>)
        fun setAutomatches(automatches: List<OGSAutomatch>)
        fun showMessage(title: String, message: String)
        fun showLoginScreen()
        val needsMoreOlderGames: Observable<MoreDataRequest>
        fun appendHistoricGames(games: List<Game>)
        fun isHistoricGamesSectionEmpty(): Boolean
        fun setLoadingMoreHistoricGames(loading: Boolean)
    }
    interface Presenter {
        fun subscribe()
        fun unsubscribe()
        fun onGameSelected(game: Game)
        fun onChallengeCancelled(challenge: Challenge)
        fun onChallengeAccepted(challenge: Challenge)
        fun onChallengeDeclined(challenge: Challenge)
        fun onAutomatchCancelled(automatch: OGSAutomatch)
    }
}