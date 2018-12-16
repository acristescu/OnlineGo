package io.zenandroid.onlinego.ogs

import io.reactivex.Completable
import io.reactivex.Single
import io.zenandroid.onlinego.model.ogs.OGSAutomatch
import io.zenandroid.onlinego.model.ogs.OGSChallenge
import io.zenandroid.onlinego.model.ogs.OGSGame
import io.zenandroid.onlinego.model.ogs.UIConfig

/**
 * Created by alex on 24/11/2017.
 */
interface OGSService {
    fun connectToGame(id: Long): GameConnection
    var uiConfig: UIConfig?

    fun startGameSearch(sizes: List<Size>, speed: Speed): AutomatchChallenge
    fun cancelAutomatchChallenge(challenge: AutomatchChallenge)
    fun fetchGame(gameId: Long): Single<OGSGame>
    fun fetchActiveGames(): Single<List<OGSGame>>
    fun resendAuth()
    fun fetchHistoricGames(): Single<List<OGSGame>>
    fun fetchChallenges(): Single<List<OGSChallenge>>
    fun acceptChallenge(id: Long): Completable
    fun declineChallenge(id: Long): Completable
    fun cancelAutomatch(automatch: OGSAutomatch)
    fun startAutomatch(sizes: List<Size>, speed: Speed): String
}