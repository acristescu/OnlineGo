package io.zenandroid.onlinego

/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import android.util.Log
import androidx.test.espresso.IdlingResource
import io.zenandroid.onlinego.utils.CountingIdlingResource

import java.util.concurrent.atomic.AtomicInteger

/**
 * An simple counter implementation of [IdlingResource] that determines idleness by
 * maintaining an internal counter. When the counter is 0 - it is considered to be idle, when it is
 * non-zero it is not idle. This is very similar to the way a [java.util.concurrent.Semaphore]
 * behaves.
 *
 *
 * This class can then be used to wrap up operations that while in progress should block tests from
 * accessing the UI.
 */
class EspressoIdlingResource : CountingIdlingResource, IdlingResource {

    private val counter = AtomicInteger(0)

    // written from main thread, read from any thread.
    @Volatile
    private var resourceCallback: IdlingResource.ResourceCallback? = null

    override fun getName(): String {
        return "GLOBAL"
    }

    override fun isIdleNow(): Boolean {
        return counter.get() == 0
    }

    override fun registerIdleTransitionCallback(resourceCallback: IdlingResource.ResourceCallback) {
        this.resourceCallback = resourceCallback
    }

    /**
     * Increments the count of in-flight transactions to the resource being monitored.
     */
    override fun increment() {
        val value = counter.getAndIncrement() + 1
        Log.d("EspressoIdlingResource", "incrementing to $value")
    }

    /**
     * Decrements the count of in-flight transactions to the resource being monitored.
     *
     * If this operation results in the counter falling below 0 - an exception is raised.
     *
     * @throws IllegalStateException if the counter is below 0.
     */
    override fun decrement() {
        val counterVal = counter.decrementAndGet()
        Log.d("EspressoIdlingResource", "decrementing to $counterVal")
        if (counterVal == 0) {
            // we've gone from non-zero to zero. That means we're idle now! Tell espresso.
            resourceCallback?.onTransitionToIdle()
        }

        if (counterVal < 0) {
            throw IllegalArgumentException("Counter has been corrupted!")
        }
    }
}