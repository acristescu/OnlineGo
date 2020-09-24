package io.zenandroid.onlinego.ui.screens.localai.middlewares

import io.reactivex.Observable
import io.zenandroid.onlinego.leela.LeelaZeroService
import io.zenandroid.onlinego.mvi.Middleware
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.*
import io.zenandroid.onlinego.ui.screens.localai.AiGameState

class EngineLogMiddleware : Middleware<AiGameState, AiGameAction> {
    override fun bind(actions: Observable<AiGameAction>, state: Observable<AiGameState>): Observable<AiGameAction> {
        return actions.ofType(ViewReady::class.java)
                .switchMap { LeelaZeroService.leelaLog }
                .map { EngineLogLine(it) }
    }
}