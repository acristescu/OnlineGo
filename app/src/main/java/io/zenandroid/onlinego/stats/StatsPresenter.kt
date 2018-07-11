package io.zenandroid.onlinego.stats

import com.google.firebase.analytics.FirebaseAnalytics
import io.reactivex.disposables.CompositeDisposable

/**
 * Created by alex on 05/11/2017.
 */
class StatsPresenter(val view: StatsContract.View, val analytics: FirebaseAnalytics) : StatsContract.Presenter {

    private val subscriptions = CompositeDisposable()

    override fun subscribe() {
    }

    override fun unsubscribe() {
        subscriptions.clear()
    }
}