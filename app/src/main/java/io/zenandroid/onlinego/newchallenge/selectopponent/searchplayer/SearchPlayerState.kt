package io.zenandroid.onlinego.newchallenge.selectopponent.searchplayer

import io.zenandroid.onlinego.model.local.Player

data class SearchPlayerState(
        var searchText: String = "",
        var loading: Boolean = false,
        val players: List<Player> = listOf()
)