package io.zenandroid.onlinego.newchallenge.selectopponent.searchplayer

import androidx.lifecycle.ViewModel
import io.reactivex.disposables.Disposable
import io.zenandroid.onlinego.mvi.MviView
import io.zenandroid.onlinego.mvi.Store

class SearchPlayerViewModel (private val store: Store<SearchPlayerAction, SearchPlayerState>): ViewModel() {

    private val wiring = store.wire()
    private var viewBinding: Disposable? = null

    override fun onCleared() {
        wiring.dispose()
    }

    fun bind(view: MviView<SearchPlayerAction, SearchPlayerState>) {
        viewBinding = store.bind(view)
    }

    fun unbind() {
        viewBinding?.dispose()
    }
}