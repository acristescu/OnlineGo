package io.zenandroid.onlinego.ogs

import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.model.local.Player

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

    fun seachPlayers(query: String): Single<List<Player>> {
        return ogs.searchPlayers(query).map { it.map(Player.Companion::fromOGSPlayer) }
    }
}