package io.zenandroid.onlinego.ui.screens.localai.middlewares

import android.util.Log
import androidx.preference.PreferenceManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.reactivex.Observable
import io.reactivex.rxkotlin.withLatestFrom
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.gamelogic.RulesManager
import io.zenandroid.onlinego.gamelogic.Util.toGTP
import io.zenandroid.onlinego.mvi.Middleware
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.CantRestoreState
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.RestoredState
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.ViewPaused
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.ViewReady
import io.zenandroid.onlinego.ui.screens.localai.AiGameState
import io.zenandroid.onlinego.utils.moshiadapters.HashMapOfCellToStoneTypeMoshiAdapter
import io.zenandroid.onlinego.utils.moshiadapters.ResponseBriefMoshiAdapter
import io.zenandroid.onlinego.utils.recordException

private const val STATE_KEY = "AIGAME_STATE_KEY"

class StatePersistenceMiddleware : Middleware<AiGameState, AiGameAction> {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(OnlineGoApplication.instance.baseContext)

    private val stateAdapter = Moshi.Builder()
            .add(ResponseBriefMoshiAdapter())
            .add(HashMapOfCellToStoneTypeMoshiAdapter())
            .add(KotlinJsonAdapterFactory())
            .build()
            .adapter(AiGameState::class.java)


    override fun bind(actions: Observable<AiGameAction>, state: Observable<AiGameState>): Observable<AiGameAction> =
            Observable.merge(
                    deserializeObservable(actions, state),
                    serializeObservable(actions, state)
            )

    private fun deserializeObservable(actions: Observable<AiGameAction>, state: Observable<AiGameState>) =
        actions.ofType(ViewReady::class.java)
                .withLatestFrom(state)
                .map { (_, state) ->
                    if(state.position != null) {
                        RestoredState(state)
                    } else if(prefs.contains(STATE_KEY)) {
                        val json = prefs.getString(STATE_KEY, "")!!
                        val newState = try {
                            stateAdapter.fromJson(json)
                        } catch (e: java.lang.Exception) {
                            Log.e("StatePersistenceMiddlew", "Cannot deserialize state", e)
                            null
                        }
                        newState?.let {
                            if(validState(it)) {
                                RestoredState(it)
                            } else {
                                null
                            }
                        } ?: run {
                            recordException(Exception("Cannot deserialize state $json"))
                            Log.e("StatePersistenceMiddlew", "Cannot deserialize state")
                            CantRestoreState
                        }
                    } else {
                        CantRestoreState
                    }
                }

    private fun validState(state: AiGameState): Boolean {
        if(state.history.isNotEmpty()) {
            val whiteInitial = state.history[0].whiteStones
            val blackInitial = state.history[0].blackStones
            val moves = mutableListOf<Cell>()
            state.history.drop(1).forEach {
                if(it.lastMove == null || it.boardHeight != state.boardSize) {
                    FirebaseCrashlytics.getInstance().log("Invalid position in history: lastMove=${it.lastMove} boardHeight=${it.boardHeight} boardSize=${state.boardSize}")
                    return false
                }
                moves.add(it.lastMove)
                val pos = RulesManager.buildPos(moves, state.boardSize, state.boardSize, state.handicap, whiteInitialState = whiteInitial, blackInitialState = blackInitial)
                if(pos == null) {
                    FirebaseCrashlytics.getInstance().log("Invalid history: ${moves.toGTP(it.boardHeight)} whiteInitial=$whiteInitial blackInitial=$blackInitial")
                    return false
                }
            }
        }
        return true
    }

    private fun serializeObservable(actions: Observable<AiGameAction>, state: Observable<AiGameState>) =
            actions.ofType(ViewPaused::class.java)
                    .withLatestFrom(state)
                    .doOnNext { (_, state) ->
                        val json = stateAdapter.toJson(state)
                        prefs.edit().putString(STATE_KEY, json).apply()
                    }
                    .switchMap { Observable.empty<AiGameAction>() }
}