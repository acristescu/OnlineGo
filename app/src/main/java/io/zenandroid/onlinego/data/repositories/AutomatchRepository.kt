package io.zenandroid.onlinego.data.repositories

import android.util.Log
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.launch

class AutomatchRepository(
        private val socketService: OGSWebSocketService
) : SocketConnectedRepository {
    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    var automatches = persistentListOf<OGSAutomatch>()
        private set

    private val _automatches = MutableStateFlow<ImmutableList<OGSAutomatch>>(automatches)
    val automatchFlow: StateFlow<ImmutableList<OGSAutomatch>> = _automatches.asStateFlow()

    private val _gameStart = MutableSharedFlow<OGSAutomatch>()
    val gameStartFlow: SharedFlow<OGSAutomatch> = _gameStart.asSharedFlow()

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
                    _gameStart.emit(it)
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
        _automatches.value = automatches
    }

    private fun addAutomatch(automatch: OGSAutomatch) {
        if(automatches.find { it.uuid == automatch.uuid } == null) {
            automatches += automatch
            _automatches.value = automatches
        }
    }

    override fun onSocketDisconnected() {
        automatches = automatches.builder().apply {
            removeAll { it.liveOrBlitzOrRapid }
        }.build()
        _automatches.value = automatches
        scope.cancel()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

}