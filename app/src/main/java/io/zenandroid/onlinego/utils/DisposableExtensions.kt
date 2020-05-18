package io.zenandroid.onlinego.utils

import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

fun Disposable.addToDisposable(disposable: CompositeDisposable) {
    disposable.add(this)
}