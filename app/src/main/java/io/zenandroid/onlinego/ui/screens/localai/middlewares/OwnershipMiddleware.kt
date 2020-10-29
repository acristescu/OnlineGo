package io.zenandroid.onlinego.ui.screens.localai.middlewares

import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.withLatestFrom
import io.zenandroid.onlinego.ai.KataGoAnalysisEngine
import io.zenandroid.onlinego.mvi.Middleware
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.*
import io.zenandroid.onlinego.ui.screens.localai.AiGameState

class OwnershipMiddleware : Middleware<AiGameState, AiGameAction> {
    override fun bind(actions: Observable<AiGameAction>, state: Observable<AiGameState>): Observable<AiGameAction> {
        return actions.ofType(UserAskedForOwnership::class.java)
                .withLatestFrom(state)
                .filter{ (_, state) -> state.engineStarted && state.position != null }
                .flatMapSingle { (_, state) ->
                    if(state.showAiEstimatedTerritory) {
                        Single.just(HideOwnership)
                    } else {
                        KataGoAnalysisEngine.analyzePosition(
                                pos = state.position!!,
                                maxVisits = 30,
                                komi = state.position.komi,
                                includeOwnership = true
                        ).map {
                            state.position.aiAnalysisResult = it
                            AIOwnershipResponse
                        }
                    }
                }

    }
}