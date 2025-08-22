package io.zenandroid.onlinego.data.repositories

import io.zenandroid.onlinego.data.db.GameDao
import io.zenandroid.onlinego.data.model.local.Player
import io.zenandroid.onlinego.data.ogs.OGSRestService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.rx2.asFlow
import kotlinx.coroutines.withContext

class PlayersRepository(
  private val restService: OGSRestService,
  private val userSessionRepository: UserSessionRepository,
  private val dao: GameDao
) {

  suspend fun getRecentOpponents() =
    withContext(Dispatchers.IO) {
      val userId = userSessionRepository.userIdObservable.asFlow().first()
      dao.getRecentOpponents(userId)
        .distinctBy { it.id }
    }

  suspend fun searchPlayers(query: String): List<Player> {
    return restService.searchPlayers(query).map(Player.Companion::fromOGSPlayer)
  }
}