//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.client.msal.automationapp.testpass.broker.concurrent

import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Drives a barrier-synchronized concurrent `AcquireTokenSilent` stress run.
 *
 * Each of [iterations] waves releases all [threadCount] threads from a
 * [CyclicBarrier] at once, so the broker sees [threadCount] truly simultaneous
 * requests. A wave only advances once every request has called back.
 *
 * Callers must make each wave's requests distinct (e.g. [scopesForThread]) or
 * the dispatcher de-duplicates the identical in-flight commands.
 */
object ConcurrentAcquireTokenSilentHelper {

    /**
     * One distinct delegated scope per thread, so concurrent commands aren't
     * de-duplicated. All are silently satisfiable once the device is WPJ'd.
     */
    val SCOPE_POOL = arrayOf(
        "User.Read",
        "AccessReview.Read.All",
        "PeopleSettings.Read.All",
        "AdministrativeUnit.Read.All",
        "UserAuthenticationMethod.Read",
        "Sites.Search.All",
        "User-Phone.ReadWrite.All",
        "Organization.Read.All",
        "AgentCollection.Read.All",
        "Place.Read.All",
        "Application.Read.All",
        "Agreement.Read.All",
        "TermStore.Read.All",
        "User-Mail.ReadWrite.All",
        "User-LifeCycleInfo.Read.All"
    )

    fun scopesForThread(threadIndex: Int): List<String> =
        listOf(SCOPE_POOL[threadIndex % SCOPE_POOL.size])

    /** `allCompleted` is true only if every wave finished within the timeout. */
    data class StressResult(val allCompleted: Boolean, val errors: List<String>)

    /** Issues one request; must call `done.countDown()` exactly once. */
    fun interface SilentTokenRequester {
        fun request(threadIndex: Int, iteration: Int, done: CountDownLatch, errors: MutableList<String>)
    }

    fun run(
        threadCount: Int,
        iterations: Int,
        perWaveTimeoutSec: Long,
        requester: SilentTokenRequester,
    ): StressResult {
        require(threadCount > 0) { "threadCount must be > 0" }
        require(iterations > 0) { "iterations must be > 0" }
        require(perWaveTimeoutSec > 0) { "perWaveTimeoutSec must be > 0" }

        val errors = Collections.synchronizedList(ArrayList<String>())
        val stopped = AtomicBoolean(false)
        val allThreadsDone = CountDownLatch(threadCount)

        // The last thread to reach the barrier installs the wave's shared latch
        // (counting down from threadCount) before any thread is released.
        val waveLatch = AtomicReference<CountDownLatch>()
        val barrier = CyclicBarrier(threadCount) { waveLatch.set(CountDownLatch(threadCount)) }

        for (t in 0 until threadCount) {
            Thread({
                try {
                    for (iter in 0 until iterations) {
                        if (stopped.get()) break

                        try {
                            barrier.await(perWaveTimeoutSec, TimeUnit.SECONDS)
                        } catch (interrupted: InterruptedException) {
                            Thread.currentThread().interrupt()
                            break
                        } catch (barrierBroken: Exception) {
                            if (!stopped.get()) errors.add("Thread $t barrier broke at wave $iter: $barrierBroken")
                            break
                        }
                        if (stopped.get()) break

                        val done = waveLatch.get()
                        try {
                            requester.request(t, iter, done, errors)
                        } catch (dispatchError: Throwable) {
                            errors.add("Thread $t iter $iter dispatch failed: $dispatchError")
                            done.countDown()
                        }

                        try {
                            if (!done.await(perWaveTimeoutSec, TimeUnit.SECONDS)) {
                                // First to time out aborts the run and frees any
                                // threads already parked at the next barrier.
                                if (stopped.compareAndSet(false, true)) {
                                    errors.add("Wave $iter timed out after ${perWaveTimeoutSec}s")
                                    barrier.reset()
                                }
                                break
                            }
                        } catch (interrupted: InterruptedException) {
                            Thread.currentThread().interrupt()
                            break
                        }
                    }
                } finally {
                    allThreadsDone.countDown()
                }
            }, "ConcurrentATS-$t").apply { isDaemon = true }.start()
        }

        val allCompleted = allThreadsDone.await(iterations.toLong() * perWaveTimeoutSec, TimeUnit.SECONDS)
        return StressResult(allCompleted && !stopped.get(), ArrayList(errors))
    }
}
