package io.zenandroid.onlinego.data.repositories
import android.util.Log
import io.zenandroid.onlinego.data.db.GameDao
import io.zenandroid.onlinego.data.model.local.Challenge
import io.zenandroid.onlinego.data.model.ogs.OGSChallenge
import io.zenandroid.onlinego.data.ogs.OGSRestService
import io.zenandroid.onlinego.data.ogs.OGSWebSocketService
import io.zenandroid.onlinego.utils.recordException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
class ChallengesRepository(
        private val restService: OGSRestService,
        private val socketService: OGSWebSocketService,
        private val dao: GameDao
): SocketConnectedRepository {
    private var flowScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val TAG = ChallengesRepository::class.java.simpleName
    override fun onSocketConnected() {
        flowScope.launch {
            try {
                refreshChallenges()
            } catch (e: Exception) {
                onError(e)
            }
        }
        flowScope.launch {
            try {
                socketService.connectToUIPushes()
                    .filter { it.event == "challenge-list-updated" }
                    .collect {
                        try {
                            refreshChallenges()
                        } catch (e: Exception) {
                            onError(e)
                        }
                    }
            } catch (e: Exception) {
                onError(e)
            }
        }
    }
    private fun storeChallenges(challenges: List<OGSChallenge>) {
        dao.replaceAllChallenges(challenges.map { Challenge.fromOGSChallenge(it) })
    }
    suspend fun refreshChallenges() {
        Log.i(TAG, "Fetching challenges")
        val challenges = restService.fetchChallenges()
        storeChallenges(challenges)
    }
    fun monitorChallenges(): Flow<List<Challenge>> =
        dao.getChallenges().distinctUntilChanged()
    fun onError(throwable: Throwable) {
        if (throwable is CancellationException) {
            throw throwable
        }
        Log.e(TAG, throwable.message, throwable)
        recordException(throwable)
    }
    override fun onSocketDisconnected() {
        flowScope.cancel()
        flowScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
