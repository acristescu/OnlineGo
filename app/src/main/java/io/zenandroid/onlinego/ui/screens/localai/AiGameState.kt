package io.zenandroid.onlinego.ui.screens.localai

import android.graphics.Point
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.repositories.ActiveGamesRepository
import io.zenandroid.onlinego.gamelogic.RulesManager
import io.zenandroid.onlinego.ui.screens.joseki.JosekiExplorerAction
import org.koin.core.context.KoinContextHandler.get

data class AiGameState(
    val leelaStarted: Boolean = false,
//    val position: Position = RulesManager.replay(get().get<ActiveGamesRepository>().monitorGame(23575151).blockingFirst(), computeTerritory = false),
    val position: Position? = null,
    val boardSize: Int = 19,
    val secondsPerMove: Int = 10,
    val leelaPlaysBlack: Boolean = false,
    val handicap: Int = 3,
    val boardIsInteractive: Boolean = false,
    val candidateMove: Point? = null,
    val passButtonEnabled: Boolean = false,
    val nextButtonEnabled: Boolean = false,
    val previousButtonEnabled: Boolean = false,
    val engineLog: String = "",
    val redoPosStack: List<Position> = emptyList()
)