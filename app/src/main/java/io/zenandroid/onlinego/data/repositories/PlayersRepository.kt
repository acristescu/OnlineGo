package io.zenandroid.onlinego.data.repositories

import io.reactivex.Single
import io.zenandroid.onlinego.data.db.GameDao
import io.zenandroid.onlinego.data.model.local.Player
import io.zenandroid.onlinego.data.ogs.OGSRestService

class PlayersRepository(
  private val restService: OGSRestService,
  private val userSessionRepository: UserSessionRepository,
  private val dao: GameDao
) {

  suspend fun getRecentOpponents() =
    dao
      .getRecentOpponents(userSessionRepository.userId)
      .distinctBy { it.id }

  suspend fun searchPlayers(query: String): List<Player> {
    return restService.searchPlayers(query).map (Player.Companion::fromOGSPlayer)
  }
}