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

  fun searchPlayers(query: String): Single<List<Player>> {
    return restService.searchPlayers(query).map { it.map(Player.Companion::fromOGSPlayer) }
  }
}