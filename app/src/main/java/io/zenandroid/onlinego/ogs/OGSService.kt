package io.zenandroid.onlinego.ogs

import io.zenandroid.onlinego.model.ogs.UIConfig

/**
 * Created by alex on 24/11/2017.
 */
interface OGSService {
    fun connectToGame(id: Long): GameConnection
    val restApi: OGSRestAPI
    var uiConfig: UIConfig?

}