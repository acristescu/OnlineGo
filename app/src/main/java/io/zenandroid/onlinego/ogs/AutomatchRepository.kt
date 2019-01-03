package io.zenandroid.onlinego.ogs

import com.crashlytics.android.Crashlytics
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.zenandroid.onlinego.extensions.addToDisposable
import io.zenandroid.onlinego.model.ogs.OGSAutomatch

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
                .subscribe(this::addAutomatch) { Crashlytics.logException(it) }
                .addToDisposable(subscriptions)

        OGSServiceImpl.listenToCancelAutomatchNotifications()
                .subscribeOn(Schedulers.io())
                .subscribe(this::removeAutomatch) { Crashlytics.logException(it) }
                .addToDisposable(subscriptions)

        OGSServiceImpl.listenToStartAutomatchNotifications()
                .subscribeOn(Schedulers.io())
                .doOnNext(gameStartSubject::onNext)
                .subscribe(this::removeAutomatch) { Crashlytics.logException(it) }
                .addToDisposable(subscriptions)

        OGSServiceImpl.connectToAutomatch()
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