package io.zenandroid.onlinego.ui.screens.localai.middlewares

import android.util.Log
import io.reactivex.Observable
import io.reactivex.rxkotlin.withLatestFrom
import io.zenandroid.onlinego.ai.KataGoAnalysisEngine
import io.zenandroid.onlinego.mvi.Middleware
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.AIHint
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.UserAskedForHint
import io.zenandroid.onlinego.ui.screens.localai.AiGameState
import io.zenandroid.onlinego.utils.recordException

class HintMiddleware : Middleware<AiGameState, AiGameAction> {
    override fun bind(actions: Observable<AiGameAction>, state: Observable<AiGameState>): Observable<AiGameAction> {
        return actions.ofType(UserAskedForHint::class.java)
                .withLatestFrom(state)
                .filter{ (_, state) -> state.engineStarted && state.position != null}
                .flatMap { (_, state) ->
                    KataGoAnalysisEngine.analyzeMoveSequence(
                            sequence = state.history,
                            maxVisits = 30,
                            komi = state.position?.komi ?: 0f,
                            includeOwnership = false
                    )
                            .map(::AIHint)
                            .toObservable()
                            .doOnError(this::onError)
                            .onErrorResumeNext(Observable.empty())
                }

    }

    private fun onError(throwable: Throwable) {
        Log.e("HintMiddleware", throwable.message, throwable)
        recordException(throwable)
    }
}