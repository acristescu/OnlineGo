package io.zenandroid.onlinego.reusable

import com.xwray.groupie.kotlinandroidextensions.Item
import io.zenandroid.onlinego.model.local.DbGame
import io.zenandroid.onlinego.model.ogs.Game
import io.zenandroid.onlinego.model.ogs.GameData

abstract class GameItem(var game: DbGame) : Item(game.id) {
    var gameData : GameData? = null
}

