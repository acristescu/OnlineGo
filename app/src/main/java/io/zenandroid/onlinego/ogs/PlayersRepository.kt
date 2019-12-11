package io.zenandroid.onlinego.ogs

import io.reactivex.disposables.CompositeDisposable
import io.zenandroid.onlinego.OnlineGoApplication

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
}