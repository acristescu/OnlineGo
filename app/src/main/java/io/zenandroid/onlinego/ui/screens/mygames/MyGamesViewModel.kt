package io.zenandroid.onlinego.ui.screens.mygames

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.data.model.ogs.Phase
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import javax.annotation.concurrent.Immutable

class MyGamesViewModel(
    private val userSessionRepository: UserSessionRepository,
    ) : ViewModel() {
    private val _state = MutableLiveData(MyGamesState(userId = userSessionRepository.userId ?: 0))
    val state: LiveData<MyGamesState> = _state

    fun setGames(games: List<Game>) {
        val myTurnList = mutableListOf<Game>()
        val opponentTurnList = mutableListOf<Game>()
        for(game in games) {
            val myTurn = when (game.phase) {
                Phase.PLAY -> game.playerToMoveId == userSessionRepository.userId
                Phase.STONE_REMOVAL -> {
                    val myRemovedStones = if(userSessionRepository.userId == game.whitePlayer.id) game.whitePlayer.acceptedStones else game.blackPlayer.acceptedStones
                    game.removedStones != myRemovedStones
                }
                else -> false
            }

            if(myTurn) {
                myTurnList.add(game)
            } else {
                opponentTurnList.add(game)
            }
        }

        _state.value = _state.value?.copy(
            myTurnGames = myTurnList,
            opponentTurnGames = opponentTurnList
        )
    }

    fun setRecentGames(games: List<Game>) {
        _state.value = _state.value?.copy(
            recentGames = games
        )
    }
}

@Immutable
data class MyGamesState(
    val myTurnGames: List<Game> = emptyList(),
    val opponentTurnGames: List<Game> = emptyList(),
    val recentGames: List<Game> = emptyList(),
    val userId: Long
)