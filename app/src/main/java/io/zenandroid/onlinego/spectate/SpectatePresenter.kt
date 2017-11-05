package io.zenandroid.onlinego.spectate

import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.ogs.OGSService

/**
 * Created by alex on 05/11/2017.
 */
class SpectatePresenter(val view: SpectateContract.View, val service: OGSService) : SpectateContract.Presenter {

    val subscriptions = CompositeDisposable()

    override fun subscribe() {
        subscriptions.add(
                service.fetchGameList()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
                        .subscribe({ games -> view.games = games})
        )
    }

    override fun unsubscribe() {
        subscriptions.clear()
    }
}