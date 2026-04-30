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
package com.microsoft.identity.client.msal.automationapp.testpass.broker.concurrent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Reusable helper that drives a concurrent {@code AcquireTokenSilent} stress run.
 *
 * <p>This class encapsulates the {@link CyclicBarrier}/{@link CountDownLatch}
 * orchestration that would otherwise be duplicated in every concurrent stress
 * test. Callers supply a {@link SilentTokenRequester} that issues one MSAL
 * call per (thread, iteration) slot; the helper manages thread creation,
 * synchronization, error collection, and timeout enforcement.</p>
 *
 * <p>The scope pool ({@link #THREAD_SCOPES}) mirrors the design used in the
 * msaltestapp's {@code ConcurrentAcquireTokenExecutor}: each thread is
 * assigned a distinct scope set so that the MSAL {@code CommandDispatcher}
 * cannot collapse concurrent in-flight requests that would otherwise be
 * identical.</p>
 */
public final class ConcurrentAcquireTokenSilentHelper {

    private ConcurrentAcquireTokenSilentHelper() { /* utility class */ }

    /**
     * Scope combinations rotated per-thread to prevent the
     * {@code CommandDispatcher} from deduplicating concurrent requests that
     * share identical parameters.
     *
     * <p>The array contains exactly 13 entries, matching the default value of
     * the {@code concurrent_count} field in the msaltestapp UI.</p>
     */
    public static final String[][] THREAD_SCOPES = {
            {"User.read"},
            {"User.read", "profile"},
            {"User.read", "openid"},
            {"User.read", "email"},
            {"User.read", "offline_access"},
            {"User.read", "profile", "openid"},
            {"User.read", "profile", "email"},
            {"User.read", "openid", "email"},
            {"User.read", "openid", "offline_access"},
            {"User.read", "email", "offline_access"},
            {"User.read", "profile", "openid", "email"},
            {"User.read", "profile", "offline_access"},
            {"User.read", "openid", "email", "offline_access"}
    };

    /**
     * Returns a mutable {@link List} of scopes assigned to {@code threadIndex},
     * cycling through {@link #THREAD_SCOPES} when
     * {@code threadIndex >= THREAD_SCOPES.length}.
     */
    public static List<String> scopesForThread(final int threadIndex) {
        return new ArrayList<>(
                Arrays.asList(THREAD_SCOPES[threadIndex % THREAD_SCOPES.length]));
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Encapsulates the outcome of a completed (or timed-out) stress run.
     */
    public static final class StressResult {
        /** {@code true} if every thread finished all iterations within the timeout. */
        public final boolean allCompleted;
        /** Immutable snapshot of error messages recorded during the run. */
        public final List<String> errors;

        StressResult(final boolean allCompleted, final List<String> errors) {
            this.allCompleted = allCompleted;
            this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
        }
    }

    /**
     * Callback supplied by test code to issue one silent-token request.
     *
     * <p>Implementations <strong>must</strong> call
     * {@code done.countDown()} exactly once – whether the request succeeds,
     * fails, or is dispatched asynchronously – so the helper can advance to the
     * next iteration wave.</p>
     */
    public interface SilentTokenRequester {
        /**
         * Issues one silent-token request.
         *
         * @param threadIndex zero-based index of the issuing thread
         * @param iteration   zero-based iteration number within the thread
         * @param done        latch that must be counted down exactly once
         * @param errors      thread-safe list; append a message on failure
         */
        void request(int threadIndex, int iteration, CountDownLatch done, List<String> errors);
    }

    /**
     * Runs a concurrent silent-token stress scenario.
     *
     * <p>Launches {@code threadCount} threads. On each of {@code iterations}
     * waves, every thread waits at a {@link CyclicBarrier} before calling
     * {@code requester.request(...)}, ensuring all requests are dispatched
     * simultaneously. All {@code threadCount} callbacks for the wave must
     * arrive within {@code perWaveTimeoutSec} seconds; if any wave exceeds
     * this deadline the run is aborted and an error is recorded.</p>
     *
     * <p>The {@code done} latch passed to
     * {@link SilentTokenRequester#request} is a <em>shared</em> wave latch
     * that counts from {@code threadCount} down to zero, so callers must
     * still call {@code done.countDown()} exactly once per request.</p>
     *
     * @param threadCount       number of concurrent threads
     * @param iterations        number of iteration waves per thread
     * @param perWaveTimeoutSec maximum seconds for all {@code threadCount}
     *                          callbacks in one wave to complete
     * @param totalTimeoutSec   seconds to wait for all threads to finish all
     *                          iterations (safety backstop)
     * @param requester         supplies one MSAL call per (thread, iteration)
     * @return the {@link StressResult} containing the completion flag and any errors
     * @throws InterruptedException if the calling thread is interrupted while awaiting
     *                              completion
     */
    public static StressResult run(
            final int threadCount,
            final int iterations,
            final long perWaveTimeoutSec,
            final long totalTimeoutSec,
            final SilentTokenRequester requester) throws InterruptedException {

        final AtomicBoolean stopped = new AtomicBoolean(false);
        final AtomicReference<CountDownLatch> currentWaveLatch = new AtomicReference<>();
        final List<String> errors = Collections.synchronizedList(new ArrayList<>());

        // Barrier action: executed by the last arriving thread before any is released.
        // Creates a fresh shared latch for the wave that is about to start.
        final CyclicBarrier waveBarrier = new CyclicBarrier(threadCount,
                () -> currentWaveLatch.set(new CountDownLatch(threadCount)));

        final CountDownLatch allThreadsDone = new CountDownLatch(threadCount);

        for (int t = 0; t < threadCount; t++) {
            final int threadIndex = t;
            new Thread(() -> {
                try {
                    for (int iter = 0; iter < iterations; iter++) {
                        if (stopped.get()) {
                            break;
                        }

                        // Synchronise all threads so every wave fires together.
                        try {
                            waveBarrier.await();
                        } catch (final Exception barrierEx) {
                            if (!stopped.get()) {
                                errors.add("Thread " + threadIndex
                                        + " barrier failed at wave " + iter
                                        + ": " + barrierEx.getMessage());
                            }
                            break;
                        }

                        if (stopped.get()) {
                            break;
                        }

                        // All threads in this wave share the same latch (counts
                        // from threadCount to 0). Each thread fires its request
                        // and counts down once via its callback.
                        final CountDownLatch waveDone = currentWaveLatch.get();
                        final int currentIter = iter;

                        requester.request(threadIndex, currentIter, waveDone, errors);

                        // Wait for every callback in this wave before starting the next.
                        try {
                            if (!waveDone.await(perWaveTimeoutSec, TimeUnit.SECONDS)) {
                                // Only the first thread to detect the timeout logs + stops.
                                if (stopped.compareAndSet(false, true)) {
                                    errors.add("Wave " + currentIter + " timed out after "
                                            + perWaveTimeoutSec + "s");
                                    // Unblock any threads already waiting at the next barrier.
                                    waveBarrier.reset();
                                }
                                break;
                            }
                        } catch (final InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                } finally {
                    allThreadsDone.countDown();
                }
            }, "ConcurrentATS-" + threadIndex).start();
        }

        final boolean allCompleted = allThreadsDone.await(totalTimeoutSec, TimeUnit.SECONDS);
        return new StressResult(allCompleted, errors);
    }
}
