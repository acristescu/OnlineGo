package io.zenandroid.onlinego.ui.screens.spectate

import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.utils.addToDisposable
import io.zenandroid.onlinego.data.model.ogs.GameList
import io.zenandroid.onlinego.data.model.ogs.OGSGame
import io.zenandroid.onlinego.data.ogs.OGSServiceImpl

/**
 * Created by alex on 05/11/2017.
 */
@Deprecated("")
class SpectatePresenter(val view: SpectateContract.View, private val service: OGSServiceImpl) : SpectateContract.Presenter {
    override fun onGameSelected(game: OGSGame) {
        view.navigateToGameScreen(game)
    }

    private val subscriptions = CompositeDisposable()

    override fun subscribe() {
        service.fetchGameList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
                .subscribe(this::setGames)
                .addToDisposable(subscriptions)
    }

    private fun setGames(games: GameList) {
        games.results.forEach { game ->
            val gameConnection = service.connectToGame(game.id)
            gameConnection.addToDisposable(subscriptions)
            gameConnection.gameData
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
                    .subscribe { gameData -> view.setGameData(game.id, gameData) }
                    .addToDisposable(subscriptions)

            gameConnection.moves
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
                    .subscribe { move -> view.doMove(game.id, move) }
                    .addToDisposable(subscriptions)
        }

        view.games = games
    }

    override fun unsubscribe() {
        subscriptions.clear()
    }
}