package io.zenandroid.onlinego.mvi

import io.reactivex.Observable

interface Middleware<S, A> {
    fun bind(actions: Observable<A>, state: Observable<S>): Observable<A>
}