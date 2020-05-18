package io.zenandroid.onlinego.data.repositories

import android.util.Log
import com.crashlytics.android.Crashlytics
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.zenandroid.onlinego.utils.addToDisposable
import io.zenandroid.onlinego.data.model.ogs.OGSAutomatch
import io.zenandroid.onlinego.data.ogs.OGSServiceImpl

object AutomatchRepository {
    private val subscriptions = CompositeDisposable()

    var automatches = mutableListOf<OGSAutomatch>()
        private set

    private val automatchesSubject = BehaviorSubject.createDefault(automatches)
    private val gameStartSubject = PublishSubject.create<OGSAutomatch>()

    val automatchObservable = automatchesSubject.hide()
    val gameStartObservable = gameStartSubject.hide()

    internal fun subscribe() {
        automatches.clear()
        OGSServiceImpl.listenToNewAutomatchNotifications()
                .subscribeOn(Schedulers.io())
                .doOnError(this::onError)
                .retry()
                .subscribe(this::addAutomatch)
                .addToDisposable(subscriptions)

        OGSServiceImpl.listenToCancelAutomatchNotifications()
                .subscribeOn(Schedulers.io())
                .doOnError(this::onError)
                .retry()
                .subscribe(this::removeAutomatch)
                .addToDisposable(subscriptions)

        OGSServiceImpl.listenToStartAutomatchNotifications()
                .subscribeOn(Schedulers.io())
                .doOnNext(gameStartSubject::onNext)
                .doOnError(this::onError)
                .retry()
                .subscribe(this::removeAutomatch)
                .addToDisposable(subscriptions)

        OGSServiceImpl.connectToAutomatch()
    }

    private fun onError(t: Throwable) {
        Log.e("AutomatchRepository", t.message, t)
        Crashlytics.logException(t)
    }

    private fun removeAutomatch(automatch: OGSAutomatch) {
        automatches.find { it.uuid == automatch.uuid } ?.let {
            automatches.remove(it)
            automatchesSubject.onNext(automatches)
        }
    }

    private fun addAutomatch(automatch: OGSAutomatch) {
        if(automatches.find { it.uuid == automatch.uuid } == null) {
            automatches.add(automatch)
            automatchesSubject.onNext(automatches)
        }
    }

    internal fun unsubscribe() {
        subscriptions.clear()
    }

}