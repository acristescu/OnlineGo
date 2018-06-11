package io.zenandroid.onlinego.spectate

import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.model.ogs.OGSGame
import io.zenandroid.onlinego.model.ogs.GameList
import io.zenandroid.onlinego.ogs.OGSServiceImpl

/**
 * Created by alex on 05/11/2017.
 */
class SpectatePresenter(val view: SpectateContract.View, private val service: OGSServiceImpl) : SpectateContract.Presenter {
    override fun onGameSelected(game: OGSGame) {
        view.navigateToGameScreen(game)
    }

    private val subscriptions = CompositeDisposable()

    override fun subscribe() {
        subscriptions.add(
                service.fetchGameList()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
                        .subscribe(this::setGames)
        )
    }

    private fun setGames(games: GameList) {
        games.results.forEach { game ->
            val gameConnection = service.connectToGame(game.id)
            subscriptions.add(gameConnection)
            subscriptions.add(gameConnection.gameData
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
                    .subscribe({ gameData -> view.setGameData(game.id, gameData) }))
            subscriptions.add(gameConnection.moves
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
                    .subscribe({ move -> view.doMove(game.id, move) }))
        }

        view.games = games
    }

    override fun unsubscribe() {
        subscriptions.clear()
    }
}