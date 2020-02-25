package io.zenandroid.onlinego.ogs

import io.reactivex.Completable
import io.reactivex.Single
import io.zenandroid.onlinego.model.ogs.JosekiPosition
import io.zenandroid.onlinego.model.ogs.*
import io.zenandroid.onlinego.newchallenge.ChallengeParams

/**
 * Created by alex on 24/11/2017.
 */
interface OGSService {
    fun connectToGame(id: Long): GameConnection
    var uiConfig: UIConfig?

    fun fetchGame(gameId: Long): Single<OGSGame>
    fun fetchActiveGames(): Single<List<OGSGame>>
    fun resendAuth()
    fun fetchHistoricGames(): Single<List<OGSGame>>
    fun fetchChallenges(): Single<List<OGSChallenge>>
    fun acceptChallenge(id: Long): Completable
    fun declineChallenge(id: Long): Completable
    fun cancelAutomatch(automatch: OGSAutomatch)
    fun startAutomatch(sizes: List<Size>, speed: Speed): String
    fun challengeBot(challengeParams: ChallengeParams): Completable
    fun deleteNotification(notificationId: String)
    fun searchPlayers(query: String): Single<List<OGSPlayer>>
    fun getJosekiPosition(id: Long?): Single<JosekiPosition>
}