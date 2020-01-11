package io.zenandroid.onlinego.newchallenge.selectopponent.searchplayer

import io.zenandroid.onlinego.model.local.Player

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