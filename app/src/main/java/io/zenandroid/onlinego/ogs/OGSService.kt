package io.zenandroid.onlinego.ogs

import io.reactivex.Single
import io.zenandroid.onlinego.model.ogs.Game
import io.zenandroid.onlinego.model.ogs.UIConfig

/**
 * Created by alex on 24/11/2017.
 */
interface OGSService {
    fun connectToGame(id: Long): GameConnection
    val restApi: OGSRestAPI
    var uiConfig: UIConfig?

    fun startGameSearch(size: Size, speed: Speed): AutomatchChallenge
    fun cancelAutomatchChallenge(challenge: AutomatchChallenge)
    fun fetchGame(gameId: Long): Single<Game>
}