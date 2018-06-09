package io.zenandroid.onlinego.mygames

import android.util.Log
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.gamelogic.Util.isMyTurn
import io.zenandroid.onlinego.model.local.DbGame
import io.zenandroid.onlinego.model.ogs.Game
import io.zenandroid.onlinego.ogs.ActiveGameRepository
import io.zenandroid.onlinego.utils.timeLeftForCurrentPlayer

/**
 * Created by alex on 05/11/2017.
 */
class MyGamesPresenter(val view: MyGamesContract.View, private val gameRepository: ActiveGameRepository) : MyGamesContract.Presenter {

    companion object {
        val TAG = MyGamesPresenter::class.java.simpleName
    }

    private val subscriptions = CompositeDisposable()

    override fun subscribe() {
        subscriptions.add(
                gameRepository.fetchActiveGames()
//                        .map(this::sortGames)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
                        .subscribe(this::setGames, this::onError)
        )
        subscriptions.add(
                gameRepository.fetchHistoricGames()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
                        .subscribe(this::setHistoricGames, this::onError)
        )
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

    private fun setGames(games : List<DbGame>) {
        view.setGames(games)
        view.setLoading(false)
    }

    private fun setHistoricGames(games: List<DbGame>) {
        view.setHistoricGames(games)
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