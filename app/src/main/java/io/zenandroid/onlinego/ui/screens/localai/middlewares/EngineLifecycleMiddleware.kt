package io.zenandroid.onlinego.ui.screens.localai.middlewares

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.leela.LeelaZeroService
import io.zenandroid.onlinego.mvi.Middleware
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.*
import io.zenandroid.onlinego.ui.screens.localai.AiGameState

class EngineLifecycleMiddleware : Middleware<AiGameState, AiGameAction> {
    override fun bind(actions: Observable<AiGameAction>, state: Observable<AiGameState>): Observable<AiGameAction> {
        return Observable.merge(
                startEngineObservable(actions, state),
                stopEngineObservable(actions, state)
        )
    }

    private fun startEngineObservable(actions: Observable<AiGameAction>, state: Observable<AiGameState>): Observable<AiGameAction> =
            actions.ofType(ViewReady.javaClass)
                    .withLatestFrom(state)
                    .filter { (_, state) -> !state.leelaStarted }
                    .flatMapSingle { (_, _) ->
                        Completable.fromAction {
                            LeelaZeroService.startEngine()
                        }
                                .subscribeOn(Schedulers.io())
                                .toSingle { EngineStarted }
                    }

    private fun stopEngineObservable(actions: Observable<AiGameAction>, state: Observable<AiGameState>): Observable<AiGameAction> =
            actions.ofType(ViewPaused.javaClass)
                    .withLatestFrom(state)
                    .filter { (_, state) -> state.leelaStarted }
                    .flatMapSingle {
                        Completable.fromAction { LeelaZeroService.stopEngine() }
                                .subscribeOn(Schedulers.io())
                                .toSingle { EngineStopped }
                    }
}