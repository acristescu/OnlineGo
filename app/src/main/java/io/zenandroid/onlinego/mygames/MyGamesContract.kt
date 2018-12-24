package io.zenandroid.onlinego.mygames

import io.zenandroid.onlinego.model.local.Challenge
import io.zenandroid.onlinego.model.local.Game
import io.zenandroid.onlinego.model.ogs.OGSAutomatch

/**
 * Created by alex on 05/11/2017.
 */
interface MyGamesContract {
    interface View {
        fun navigateToGameScreen(game: Game)
        fun setGames(games: List<Game>)
        fun setLoading(loading: Boolean)
        fun setHistoricGames(games: List<Game>)
        fun setChallenges(challenges: List<Challenge>)
        fun setAutomatches(automatches: List<OGSAutomatch>)
        fun showMessage(title: String, message: String)
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