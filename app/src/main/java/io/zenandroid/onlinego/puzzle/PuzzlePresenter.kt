package io.zenandroid.onlinego.puzzle

import io.reactivex.disposables.CompositeDisposable
import io.zenandroid.onlinego.model.ogs.OGSGame
import io.zenandroid.onlinego.ogs.OGSService

class PuzzlePresenter(
        private val view: PuzzleContract.View,
        private val service: OGSService,
        private val puzzleId: Long
) : PuzzleContract.Presenter {

    private val subscriptions = CompositeDisposable()

    override fun subscribe() {
        subscriptions.add(
                service.fetchGame(puzzleId)
                        .subscribe(this::onPuzzleFetched, this::onPuzzleFetchError)
        )
    }

    override fun unsubscribe() {
        subscriptions.clear()
    }

    private fun onPuzzleFetched(game: OGSGame) {

    }

    private fun onPuzzleFetchError(t: Throwable) {

    }
}