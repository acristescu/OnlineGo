package io.zenandroid.onlinego.mvi

interface Reducer<S, A> {
    fun reduce(state: S, action: A): S
}