package io.zenandroid.onlinego.data.repositories

import android.util.Log
import io.zenandroid.onlinego.data.model.ogs.OGSAutomatch
import io.zenandroid.onlinego.data.ogs.OGSWebSocketService
import io.zenandroid.onlinego.utils.recordException
import kotlinx.collections.immutable.PersistentList
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AutomatchRepository(
        private val socketService: OGSWebSocketService
) : SocketConnectedRepository {
    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _automatches = MutableStateFlow<PersistentList<OGSAutomatch>>(persistentListOf())
    val automatchFlow: StateFlow<PersistentList<OGSAutomatch>> = _automatches.asStateFlow()

    private val _gameStart = MutableSharedFlow<OGSAutomatch>()
    val gameStartFlow: SharedFlow<OGSAutomatch> = _gameStart.asSharedFlow()

    override fun onSocketConnected() {
        _automatches.value = persistentListOf()
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
        _automatches.update { it.removeAll { it.uuid == automatch.uuid } }
    }

    private fun addAutomatch(automatch: OGSAutomatch) {
        _automatches.update { current ->
            if (current.find { it.uuid == automatch.uuid } == null) {
                current + automatch
            } else {
                current
            }
        }
    }

    override fun onSocketDisconnected() {
        _automatches.update { current ->
            current.builder().apply {
                removeAll { it.liveOrBlitzOrRapid }
            }.build()
        }
        scope.cancel()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

}