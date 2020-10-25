package io.zenandroid.onlinego.ui.screens.localai.middlewares

import io.reactivex.Observable
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.ai.KataGoAnalysisEngine
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.data.model.katago.MoveInfo
import io.zenandroid.onlinego.data.model.katago.Response
import io.zenandroid.onlinego.gamelogic.RulesManager
import io.zenandroid.onlinego.gamelogic.Util
import io.zenandroid.onlinego.mvi.Middleware
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction
import io.zenandroid.onlinego.ui.screens.localai.AiGameAction.*
import io.zenandroid.onlinego.ui.screens.localai.AiGameState

class AIMoveMiddleware : Middleware<AiGameState, AiGameAction> {
    override fun bind(actions: Observable<AiGameAction>, state: Observable<AiGameState>): Observable<AiGameAction> =
        actions.ofType(GenerateAiMove::class.java)
                .withLatestFrom(state)
                .filter { (_, state) -> state.engineStarted && state.position != null }
                .flatMapSingle { (_, state) ->
                    KataGoAnalysisEngine.analyzePosition(
                            pos = state.position!!,
                            maxVisits = 20,
                            komi = state.position.komi,
                            includeOwnership = true,
                            includeMovesOwnership = true
                    )
                            .map {
                                val selectedMove = selectMove(it)
                                val move = Util.getCoordinatesFromGTP(selectedMove.move, state.boardSize)
                                val side = if(state.enginePlaysBlack) StoneType.BLACK else StoneType.WHITE
                                state.position.aiAnalysisResult = it
                                val newPos = RulesManager.makeMove(state.position, side, move)!!.apply {
                                    nextToMove = nextToMove.opponent
                                    aiQuickEstimation = selectedMove
                                }
                                AIMove(newPos)
                            }
                            .subscribeOn(Schedulers.io())
                }

    private fun selectMove(analysis: Response): MoveInfo {
        return analysis.moveInfos[0]
    }
}