package io.zenandroid.onlinego.mvi

import io.reactivex.Observable

interface MviView<A, S> {
    val actions: Observable<A>
    fun render(state: S)
}