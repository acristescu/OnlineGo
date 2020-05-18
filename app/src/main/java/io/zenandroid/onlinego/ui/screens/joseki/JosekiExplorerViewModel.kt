package io.zenandroid.onlinego.ui.screens.joseki

import androidx.lifecycle.ViewModel
import io.reactivex.disposables.Disposable
import io.zenandroid.onlinego.mvi.MviView
import io.zenandroid.onlinego.mvi.Store

class JosekiExplorerViewModel (private val store: Store<JosekiExplorerState, JosekiExplorerAction>): ViewModel()
{
    private val wiring = store.wire()
    private var viewBinding: Disposable? = null

    override fun onCleared() {
        wiring.dispose()
    }

    fun bind(view: MviView<JosekiExplorerState, JosekiExplorerAction>) {
        viewBinding = store.bind(view)
    }

    fun unbind() {
        viewBinding?.dispose()
    }
}