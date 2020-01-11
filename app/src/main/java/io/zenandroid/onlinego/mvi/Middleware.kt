package io.zenandroid.onlinego.mvi

import io.reactivex.Observable

interface Middleware<A, S> {
    fun bind(actions: Observable<A>, state: Observable<S>): Observable<A>
}