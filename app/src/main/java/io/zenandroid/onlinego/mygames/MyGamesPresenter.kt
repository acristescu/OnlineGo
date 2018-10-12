package io.zenandroid.onlinego.mygames

import android.util.Log
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.zenandroid.onlinego.extensions.addToDisposable
import io.zenandroid.onlinego.gamelogic.Util.isMyTurn
import io.zenandroid.onlinego.model.local.Game
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
        gameRepository.fetchActiveGames()
                .map(this::sortGames)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
                .subscribe(this::setGames, this::onError)
                .addToDisposable(subscriptions)
        gameRepository.fetchHistoricGames()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()) // TODO: remove me!!!
                .subscribe(this::setHistoricGames, this::onError)
                .addToDisposable(subscriptions)
        view.setLoading(true)
    }

    private fun sortGames(unsorted : List<Game>): List<Game> {
        return unsorted.sortedWith(Comparator { left, right ->
            when {
                isMyTurn(left) && !isMyTurn(right) -> -1
                !isMyTurn(left) && isMyTurn(right) -> 1
                else -> {
                    (timeLeftForCurrentPlayer(left) - timeLeftForCurrentPlayer(right)).toInt()
                }
            }
        })
    }

    private fun setGames(games : List<Game>) {
        view.setGames(games)
        view.setLoading(false)
    }

    private fun setHistoricGames(games: List<Game>) {
        view.setHistoricGames(games)
    }

    override fun unsubscribe() {
        subscriptions.clear()
    }

    override fun onGameSelected(game: Game) {
        view.navigateToGameScreen(game)
    }

    private fun onError(t: Throwable) {
        Log.e(TAG, t.message, t)
        view.setLoading(false)
    }
}