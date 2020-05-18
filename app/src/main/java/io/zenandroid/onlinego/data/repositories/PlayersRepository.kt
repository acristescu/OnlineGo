package io.zenandroid.onlinego.data.repositories

import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.data.model.local.Player
import io.zenandroid.onlinego.data.ogs.OGSServiceImpl

object PlayersRepository {

    private val subscriptions = CompositeDisposable()
    private val ogs = OGSServiceImpl
    private val dao = OnlineGoApplication.instance.db.gameDao()

    internal fun subscribe() {
    }

    internal fun unsubscribe() {
        subscriptions.clear()
    }

    fun getRecentOpponents() =
        dao
                .getRecentOpponents(OGSServiceImpl.uiConfig?.user?.id)
                .map { it.distinctBy { it.id } }

    fun searchPlayers(query: String): Single<List<Player>> {
        return OGSServiceImpl.searchPlayers(query).map { it.map(Player.Companion::fromOGSPlayer) }
    }
}