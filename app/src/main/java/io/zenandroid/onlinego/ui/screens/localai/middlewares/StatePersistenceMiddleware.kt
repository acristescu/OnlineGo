package io.zenandroid.onlinego.ui.screens.localai.middlewares

import android.util.Log
import androidx.preference.PreferenceManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.reactivex.Observable
import io.zenandroid.onlinego.mvi.Middleware
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction
import io.zenandroid.onlinego.ui.screens.localai.AiGameState
import io.reactivex.rxkotlin.withLatestFrom
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.*
import io.zenandroid.onlinego.utils.moshiadapters.HashMapOfPointToStoneTypeMoshiAdapter
import io.zenandroid.onlinego.utils.moshiadapters.PointMoshiAdapter
import io.zenandroid.onlinego.utils.moshiadapters.ResponseBriefMoshiAdapter

private const val STATE_KEY = "AIGAME_STATE_KEY"

class StatePersistenceMiddleware : Middleware<AiGameState, AiGameAction> {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(OnlineGoApplication.instance.baseContext)

    private val stateAdapter = Moshi.Builder()
            .add(PointMoshiAdapter())
            .add(ResponseBriefMoshiAdapter())
            .add(HashMapOfPointToStoneTypeMoshiAdapter())
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
                .map {
                    if(prefs.contains(STATE_KEY)) {
                        val json = prefs.getString(STATE_KEY, "")!!
                        val newState = try {
                            stateAdapter.fromJson(json)
                        } catch (e: java.lang.Exception) {
                            Log.e("StatePersistenceMiddlew", "Cannot deserialize state", e)
                            null
                        }
                        newState?.let { RestoredState(it) }
                                ?: run {
                                    FirebaseCrashlytics.getInstance().recordException(Exception("Cannot deserialize state"))
                                    Log.e("StatePersistenceMiddlew", "Cannot deserialize state")
                                    ShowNewGameDialog
                                }
                    } else {
                        ShowNewGameDialog
                    }
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