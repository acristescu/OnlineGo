package io.zenandroid.onlinego.data.repositories

import android.util.Log
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.zenandroid.onlinego.data.model.ogs.OGSAutomatch
import io.zenandroid.onlinego.data.ogs.OGSWebSocketService
import io.zenandroid.onlinego.utils.addToDisposable
import io.zenandroid.onlinego.utils.recordException
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.plus

class AutomatchRepository(
        private val socketService: OGSWebSocketService
) : SocketConnectedRepository {
    private val subscriptions = CompositeDisposable()

    var automatches = persistentListOf<OGSAutomatch>()
        private set

    private val automatchesSubject = BehaviorSubject.createDefault<ImmutableList<OGSAutomatch>>(automatches)
    private val gameStartSubject = PublishSubject.create<OGSAutomatch>()

    val automatchObservable = automatchesSubject.hide()
    val gameStartObservable = gameStartSubject.hide()

    override fun onSocketConnected() {
        automatches = automatches.clear()
        socketService.listenToNewAutomatchNotifications()
                .subscribeOn(Schedulers.io())
                .doOnError(this::onError)
                .retry()
                .subscribe(this::addAutomatch)
                .addToDisposable(subscriptions)

        socketService.listenToCancelAutomatchNotifications()
                .subscribeOn(Schedulers.io())
                .doOnError(this::onError)
                .retry()
                .subscribe(this::removeAutomatch)
                .addToDisposable(subscriptions)

        socketService.listenToStartAutomatchNotifications()
                .subscribeOn(Schedulers.io())
                .doOnNext(gameStartSubject::onNext)
                .doOnError(this::onError)
                .retry()
                .subscribe(this::removeAutomatch)
                .addToDisposable(subscriptions)

        socketService.connectToAutomatch()
    }

    private fun onError(t: Throwable) {
        Log.e("AutomatchRepository", t.message, t)
        recordException(t)
    }

    private fun removeAutomatch(automatch: OGSAutomatch) {
        automatches = automatches.removeAll { it.uuid == automatch.uuid }
        automatchesSubject.onNext(automatches)
    }

    private fun addAutomatch(automatch: OGSAutomatch) {
        if(automatches.find { it.uuid == automatch.uuid } == null) {
            automatches += automatch
            automatchesSubject.onNext(automatches)
        }
    }

    override fun onSocketDisconnected() {
        automatches = automatches.builder().apply {
            removeAll { it.liveOrBlitz }
        }.build()
        automatchesSubject.onNext(automatches)
        subscriptions.clear()
    }

}