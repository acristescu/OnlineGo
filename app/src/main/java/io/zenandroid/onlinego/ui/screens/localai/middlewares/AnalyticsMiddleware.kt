package io.zenandroid.onlinego.ui.screens.localai.middlewares

import android.os.Bundle
import android.util.Log
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
                    Log.d("ai.state", "${action::class.simpleName}: ${action.toString()}")
                    when(action) {
                        CantRestoreState, is ViewReady, is RestoredState, ViewPaused, ShowNewGameDialog, DismissNewGameDialog, PromptUserForMove, is NewPosition, is UserHotTrackedCoordinate, is AIOwnershipResponse,
                        HideOwnership -> Unit
                        is NewGame -> {
                            analytics.logEvent("ai_game_new_game", null)
                            if(state.history.size > state.boardSize) {
                                analytics.logEvent("ai_game_abandoned_late", bundleOf("MOVES" to state.history.size))
                            } else {
                                analytics.logEvent("ai_game_abandoned_early", bundleOf("MOVES" to state.history.size))
                            }
                        }
                        EngineStarted -> analytics.logEvent("katago_started", null)
                        EngineStopped -> analytics.logEvent("katago_stopped", null)
                        GenerateAiMove -> analytics.logEvent("katago_generate_move", null)
                        is AIMove -> analytics.logEvent("katago_move", null)
                        is AIHint -> analytics.logEvent("katago_hint", null)

                        is ScoreComputed -> if(action.whiteWon) {
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
                        is EngineWouldNotStart -> analytics.logEvent("katago_would_not_start", null)
                        AIError -> analytics.logEvent("katago_error", null)
                        UserAskedForOwnership -> analytics.logEvent("ai_game_user_asked_territory", null)
                        is UserTriedSuicidalMove -> analytics.logEvent("ai_game_user_tried_suicide", null)
                        is UserTriedKoMove -> analytics.logEvent("ai_game_user_tried_ko", null)
                        NextPlayerChanged -> analytics.logEvent("ai_game_next_player_changed", null)
                        ToggleAIBlack -> analytics.logEvent("ai_game_toggled_ai_black", null)
                        ToggleAIWhite -> analytics.logEvent("ai_game_toggled_ai_white", null)
                    }
                }
                .switchMap { Observable.empty<AiGameAction>() }
    }

}
