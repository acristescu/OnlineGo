package io.zenandroid.onlinego.utils

class NOOPIdlingResource : CountingIdlingResource {

    override fun increment() {
    }

    override fun decrement() {
    }
}