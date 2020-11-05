package io.zenandroid.onlinego.ui.screens.localai.middlewares

import android.os.Bundle
import androidx.core.os.bundleOf
import io.reactivex.Observable
import io.reactivex.rxkotlin.withLatestFrom
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import io.zenandroid.onlinego.mvi.Middleware
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.*
import io.zenandroid.onlinego.ui.screens.localai.AiGameState
import org.koin.android.ext.android.get

class AnalyticsMiddleware: Middleware<AiGameState, AiGameAction> {
    private var analytics = OnlineGoApplication.instance.analytics

    override fun bind(actions: Observable<AiGameAction>, state: Observable<AiGameState>): Observable<AiGameAction> {
        return actions.withLatestFrom(state)
                .doOnNext { (action, state) ->
                    when(action) {
                        ViewReady, is RestoredState, ViewPaused, ShowNewGameDialog, DismissNewGameDialog, PromptUserForMove, is NewPosition, is UserHotTrackedCoordinate -> Unit
                        is NewGame -> {
                            analytics.logEvent("ai_game_new_game", null)
                            state.position?.let {
                                var moves = 0
                                var cursor = it
                                while(cursor.parentPosition != null) {
                                    moves++
                                    cursor = cursor.parentPosition!!
                                }
                                if(moves > it.boardSize) {
                                    analytics.logEvent("ai_game_abandoned_late", bundleOf("MOVES" to moves))
                                } else {
                                    analytics.logEvent("ai_game_abandoned_early", bundleOf("MOVES" to moves))
                                }
                            }
                        }
                        EngineStarted -> analytics.logEvent("katago_started", null)
                        EngineStopped -> analytics.logEvent("katago_stopped", null)
                        GenerateAiMove -> analytics.logEvent("katago_generate_move", null)
                        is AIMove -> analytics.logEvent("katago_move", null)
                        AIHint -> analytics.logEvent("katago_hint", null)

                        is ScoreComputed -> if(action.aiWon) {
                            analytics.logEvent("katago_won", Bundle().apply {
                                OnlineGoApplication.instance.get< UserSessionRepository>().uiConfig?.user?.ranking?.let {
                                    putInt("RANKING", it)
                                }
                            })
                        } else {
                            analytics.logEvent("katago_lost", Bundle().apply {
                                OnlineGoApplication.instance.get< UserSessionRepository>().uiConfig?.user?.ranking?.let {
                                    putInt("RANKING", it)
                                }
                            })
                        }

                        is UserTappedCoordinate -> analytics.logEvent("ai_game_user_move", null)
                        UserPressedPrevious -> analytics.logEvent("ai_game_user_undo", null)
                        UserPressedBack -> analytics.logEvent("ai_game_user_back", null)
                        UserPressedNext -> analytics.logEvent("ai_game_user_redo", null)
                        UserPressedPass -> analytics.logEvent("ai_game_user_pass", null)
                        UserAskedForHint -> analytics.logEvent("ai_game_user_asked_hint", null)
                    }
                }
                .switchMap { Observable.empty<AiGameAction>() }
    }

}