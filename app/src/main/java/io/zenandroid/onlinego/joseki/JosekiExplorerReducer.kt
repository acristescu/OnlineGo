package io.zenandroid.onlinego.joseki

import io.zenandroid.onlinego.joseki.JosekiExplorerAction.*
import io.zenandroid.onlinego.model.Position
import io.zenandroid.onlinego.mvi.Reducer

class JosekiExplorerReducer : Reducer<JosekiExplorerState, JosekiExplorerAction> {
    override fun reduce(state: JosekiExplorerState, action: JosekiExplorerAction): JosekiExplorerState {
        return when (action) {
            is PositionLoaded ->
                if(state.lastRequestedNodeId == null || state.lastRequestedNodeId == action.position.node_id) {
                    state.copy(
                            position = action.position,
                            boardPosition = Position.fromJosekiPosition(action.position),
                            loading = false,
                            candidateMove = null,
                            error = null
                    )
                } else {
                    state
                }
            is UserTappedCoordinate, is LoadPosition, is UserHotTrackedCoordinate, ViewReady -> state
            is StartDataLoading -> state.copy(
                    loading = true,
                    lastRequestedNodeId = action.id
            )
            is ShowCandidateMove -> state.copy(
                    candidateMove = action.placement
            )
            is DataLoadingError -> state.copy(
                    loading = false,
                    error = action.e
            )
        }
    }
}