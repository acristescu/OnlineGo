package io.zenandroid.onlinego.mygames

import android.util.Log
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.gamelogic.Util.isMyTurn
import io.zenandroid.onlinego.model.local.DbGame
import io.zenandroid.onlinego.model.ogs.Game
import io.zenandroid.onlinego.ogs.ActiveGameRepository
import io.zenandroid.onlinego.ogs.OGSServiceImpl
import io.zenandroid.onlinego.utils.timeLeftForCurrentPlayer

/**
 * Created by alex on 05/11/2017.
 */
class MyGamesPresenter(val view: MyGamesContract.View, private val activeGameService: ActiveGameRepository) : MyGamesContract.Presenter {

    companion object {
        val TAG = MyGamesPresenter::class.java.simpleName
    }

    private val subscriptions = CompositeDisposable()

    override fun subscribe() {
        subscriptions.add(
                activeGameService.fetchActiveGames()
//                        .map(this::sortGames)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
                        .subscribe(this::setGames, this::onError)
        )
//        subscriptions.add(
//                OGSServiceImpl.instance.fetchHistoricGames()
//                        .subscribeOn(Schedulers.io())
//                        .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
//                        .subscribe(this::setHistoricGames, this::onError)
//        )
        view.setLoading(true)
    }

    private fun sortGames(unsorted : List<Game>): List<Game> {
        return unsorted.sortedWith(Comparator { left, right ->
            when {
                isMyTurn(left) && !isMyTurn(right) -> -1
                !isMyTurn(left) && isMyTurn(right) -> 1
                else -> {
                    (timeLeftForCurrentPlayer(left, left.json!!) - timeLeftForCurrentPlayer(right, right.json!!)).toInt()
                }
            }
        })
    }

    private fun addGame(game : DbGame) {
        connectToGame(game)
        view.addOrUpdateGame(game)
    }

    private fun setGames(games : List<DbGame>) {
        for(game in games) {
            connectToGame(game)
        }

        view.setGames(games)
//        subscriptions.add(
//                activeGameService.activeGamesObservable
//                        .subscribeOn(Schedulers.io())
//                        .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
//                        .subscribe(this::addGame)
//        )
        view.setLoading(false)
    }

    private fun setHistoricGames(games: List<DbGame>) {
        for(game in games) {
            connectToGame(game, true)
        }
        view.setHistoricGames(games)
    }

    private fun connectToGame(game: DbGame, historic: Boolean = false) {
//        val gameConnection = OGSServiceImpl.instance.connectToGame(game.id)
//        subscriptions.add(gameConnection)
//        subscriptions.add(gameConnection.gameData
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
//                .subscribe { gameData ->
//                    view.setGameData(game.id, gameData)
//                    if (gameData.phase == Game.Phase.FINISHED && !historic) {
//                        removeGame(game)
//                    }
//                })
//        subscriptions.add(gameConnection.moves
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
//                .subscribe { move ->
//                    view.doMove(game.id, move)
//                })
//        subscriptions.add(gameConnection.clock
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
//                .subscribe { clock ->
//                    view.setClock(game.id, clock)
//                })
    }

    private fun removeGame(game: DbGame) {
        view.removeGame(game)
    }

    override fun unsubscribe() {
        subscriptions.clear()
    }

    override fun onGameSelected(game: DbGame) {
        view.navigateToGameScreen(game)
    }

    private fun onError(t: Throwable) {
        Log.e(TAG, t.message, t)
        view.setLoading(false)
    }
}