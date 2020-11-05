package io.zenandroid.onlinego.mvi

import io.reactivex.Observable

interface MviView<S, A> {
    val actions: Observable<A>
    fun render(state: S)
}