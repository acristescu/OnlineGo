package io.zenandroid.onlinego.ui.screens.newchallenge.selectopponent.searchplayer

import io.zenandroid.onlinego.data.model.local.Player

data class SearchPlayerState(
        var searchText: String = "",
        var loading: Boolean = false,
        val players: List<Player> = listOf()
)