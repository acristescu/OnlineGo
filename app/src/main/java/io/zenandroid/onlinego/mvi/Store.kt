package io.zenandroid.onlinego.mvi

import android.util.Log
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.withLatestFrom
import io.zenandroid.onlinego.utils.recordException

class Store<S: Any, A: Any> (
        private val reducer: Reducer<S, A>,
        private val middlewares: List<Middleware<S, A>>,
        private val initialState: S
){
    private val state = BehaviorRelay.createDefault(initialState)
    private val actions = PublishRelay.create<A>()


    fun wire(): Disposable {
        val disposable = CompositeDisposable()

        disposable += actions.withLatestFrom(state) { action: A, state: S ->
            reducer.reduce(state, action)
        }
            .distinctUntilChanged()
            .doOnError(this::onError)
            .onErrorResumeNext(Observable.empty()) // BUG! this completes the observable
            .subscribe(state::accept)

        disposable += Observable.merge<A>(
            middlewares.map { it.bind(actions, state) }
        )
                .doOnError(this::onError)
                .onErrorResumeNext(Observable.empty()) // BUG! this completes the observable
                .subscribe(actions::accept)

        return disposable
    }

    fun bind(view: MviView<S, A>): Disposable {
        val disposable = CompositeDisposable()
        disposable += state.observeOn(AndroidSchedulers.mainThread()).subscribe(view::render)
        disposable += view.actions.subscribe(actions::accept)
        return disposable
    }

    private fun onError(throwable: Throwable) {
        Log.e("Store", throwable.message, throwable)
        recordException(throwable)
    }
}