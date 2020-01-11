package io.zenandroid.onlinego.newchallenge.selectopponent.searchplayer

import io.zenandroid.onlinego.mvi.Reducer

class SearchPlayerReducer : Reducer<SearchPlayerState, SearchPlayerAction> {
    override fun reduce(state: SearchPlayerState, action: SearchPlayerAction): SearchPlayerState {
        return when(action) {
            is SearchPlayerAction.Search -> state.copy(
                    searchText = action.query
            )
            is SearchPlayerAction.Results -> if(state.searchText == action.query) state.copy(
                    players = action.results,
                    loading = false
            ) else state
            is SearchPlayerAction.SearchStarted -> state.copy(loading = true)
        }
    }

}