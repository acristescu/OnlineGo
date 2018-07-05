package io.zenandroid.onlinego.learn

import com.google.firebase.analytics.FirebaseAnalytics
import io.reactivex.disposables.CompositeDisposable

/**
 * Created by alex on 05/11/2017.
 */
class LearnPresenter(val view: LearnContract.View, val analytics: FirebaseAnalytics) : LearnContract.Presenter {

    private val subscriptions = CompositeDisposable()

    override fun subscribe() {
    }

    override fun unsubscribe() {
        subscriptions.clear()
    }
}