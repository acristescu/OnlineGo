package io.zenandroid.onlinego.data.repositories

import android.util.Log
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.zenandroid.onlinego.data.model.ogs.OGSAutomatch
import io.zenandroid.onlinego.data.ogs.OGSWebSocketService
import io.zenandroid.onlinego.utils.recordException
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.plus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.launch

class AutomatchRepository(
        private val socketService: OGSWebSocketService
) : SocketConnectedRepository {
    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    var automatches = persistentListOf<OGSAutomatch>()
        private set

    private val automatchesSubject = BehaviorSubject.createDefault<ImmutableList<OGSAutomatch>>(automatches)
    private val gameStartSubject = PublishSubject.create<OGSAutomatch>()

    val automatchObservable = automatchesSubject.hide()
    val gameStartObservable = gameStartSubject.hide()

    override fun onSocketConnected() {
        automatches = automatches.clear()
        scope.launch {
            socketService.listenToNewAutomatchNotifications()
                .retry { onError(it); true }
                .collect { addAutomatch(it) }
        }

        scope.launch {
            socketService.listenToCancelAutomatchNotifications()
                .retry { onError(it); true }
                .collect { removeAutomatch(it) }
        }

        scope.launch {
            socketService.listenToStartAutomatchNotifications()
                .retry { onError(it); true }
                .collect {
                    gameStartSubject.onNext(it)
                    removeAutomatch(it)
                }
        }

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
            removeAll { it.liveOrBlitzOrRapid }
        }.build()
        automatchesSubject.onNext(automatches)
        scope.cancel()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

}