package io.zenandroid.onlinego.ui.screens.newchallenge.selectopponent.searchplayer

import io.zenandroid.onlinego.data.model.local.Player

sealed class SearchPlayerAction {
    object SearchStarted: SearchPlayerAction()

    class Search(
            val query: String
    ): SearchPlayerAction()

    class Results(
            val query: String,
            val results: List<Player>
    ): SearchPlayerAction()
}