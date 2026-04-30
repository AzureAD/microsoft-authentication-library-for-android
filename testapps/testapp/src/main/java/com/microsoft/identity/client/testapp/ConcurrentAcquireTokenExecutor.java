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
package com.microsoft.identity.client.testapp;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.microsoft.identity.client.IAuthenticationResult;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runs concurrent AcquireTokenSilent calls in a background thread.
 *
 * <p>Multiple instances share a {@link CyclicBarrier} (set via
 * {@link #setSharedBarrier(CyclicBarrier)}) so that all threads fire their
 * silent-token request at the same moment on each iteration wave, maximising
 * contention in the MSAL command dispatcher.</p>
 *
 * <p>Each thread is assigned a scope from {@link #SCOPE_POOL} (cycling by
 * thread-id) so that the CommandDispatcher does not collapse identical
 * in-flight requests.</p>
 */
public class ConcurrentAcquireTokenExecutor {

    /**
     * Scopes rotated across threads to prevent CommandDispatcher from
     * deduplicating requests that have identical parameters.
     */
    private static final String[] SCOPE_POOL = {
            "user.read",
            "user.readbasicprofile",
            "mail.read",
            "calendars.read",
            "contacts.read",
            "files.read",
            "tasks.read",
            "people.read",
            "notes.read",
            "sites.read.all"
    };

    /** Shared barrier – all executor threads wait here before each iteration wave. */
    private static CyclicBarrier sSharedBarrier;

    private final int mThreadId;
    private final int mIterations;
    private final AtomicBoolean mStopped = new AtomicBoolean(false);

    public ConcurrentAcquireTokenExecutor(final int threadId, final int iterations) {
        mThreadId = threadId;
        mIterations = iterations;
    }

    /**
     * Sets the shared {@link CyclicBarrier} used to synchronise all executor
     * threads so they fire their requests simultaneously.
     */
    public static void setSharedBarrier(final CyclicBarrier barrier) {
        sSharedBarrier = barrier;
    }

    /**
     * Returns the scope string assigned to the given thread, cycling through
     * {@link #SCOPE_POOL}.
     */
    public static String getScopeForThread(final int threadId) {
        return SCOPE_POOL[threadId % SCOPE_POOL.length];
    }

    /** Returns the thread-id this executor was created with. */
    public int getThreadId() {
        return mThreadId;
    }

    /**
     * Callback interface for UI progress updates delivered on the main thread.
     */
    public interface IUIUpdateCallback {
        /**
         * Called after each iteration completes (success or error).
         *
         * @param tid            thread id
         * @param successCount   number of successful requests so far
         * @param completedCount total requests attempted so far
         */
        void updateProgress(int tid, int successCount, int completedCount);

        /**
         * Called when the executor has finished all iterations or been stopped.
         *
         * @param tid thread id
         */
        void onStopped(int tid);

        /**
         * Called when a request fails.
         *
         * @param tid     thread id
         * @param message error description
         */
        void onError(int tid, String message);
    }

    /**
     * Starts the concurrent execution in a dedicated background thread.
     *
     * <p>A fresh {@link MsalWrapper} is created from {@code context} and
     * {@code requestOptions} so the executor is self-contained.</p>
     *
     * @param context        application context
     * @param requestOptions current UI request options (must contain a signed-in account)
     * @param callback       UI update receiver
     */
    public void execute(
            final Context context,
            final RequestOptions requestOptions,
            final IUIUpdateCallback callback) {

        new Thread(() -> {
            final RequestOptions scopedOptions = RequestOptions.withDifferentScopes(
                    requestOptions,
                    getScopeForThread(mThreadId));

            MsalWrapper.create(
                    context,
                    Constants.getResourceIdFromConfigFile(requestOptions.getConfigFile()),
                    new INotifyOperationResultCallback<MsalWrapper>() {
                        @Override
                        public void onSuccess(final MsalWrapper msalWrapper) {
                            runIterations(msalWrapper, scopedOptions, callback);
                        }

                        @Override
                        public void showMessage(final String message) {
                            postToMain(() -> {
                                callback.onError(mThreadId, message);
                                callback.onStopped(mThreadId);
                            });
                        }
                    });
        }, "ConcurrentATS-thread-" + mThreadId).start();
    }

    /**
     * Signals the executor to stop after the current in-flight request
     * completes.  Also resets the shared barrier so other waiting threads
     * are unblocked immediately.
     */
    public void stop() {
        mStopped.set(true);
        final CyclicBarrier barrier = sSharedBarrier;
        if (barrier != null) {
            barrier.reset();
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void runIterations(
            final MsalWrapper msalWrapper,
            final RequestOptions scopedOptions,
            final IUIUpdateCallback callback) {

        int successCount = 0;

        for (int i = 0; i < mIterations && !mStopped.get(); i++) {

            // Synchronise all threads so every wave fires simultaneously.
            if (!awaitBarrier(callback)) {
                break;
            }

            if (mStopped.get()) {
                break;
            }

            final CountDownLatch latch = new CountDownLatch(1);
            final boolean[] requestSucceeded = {false};

            msalWrapper.acquireTokenSilent(
                    scopedOptions,
                    new INotifyOperationResultCallback<IAuthenticationResult>() {
                        @Override
                        public void onSuccess(final IAuthenticationResult result) {
                            requestSucceeded[0] = true;
                            latch.countDown();
                        }

                        @Override
                        public void showMessage(final String message) {
                            postToMain(() -> callback.onError(mThreadId, message));
                            latch.countDown();
                        }
                    });

            try {
                final boolean completed = latch.await(2, TimeUnit.MINUTES);
                if (!completed) {
                    final int iterationNumber = i + 1;
                    postToMain(() -> callback.onError(mThreadId,
                            "Request timed out on iteration " + iterationNumber));
                    break;
                }
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            if (requestSucceeded[0]) {
                successCount++;
            }

            final int finalSuccess = successCount;
            final int completedSoFar = i + 1;
            postToMain(() -> callback.updateProgress(mThreadId, finalSuccess, completedSoFar));
        }

        postToMain(() -> callback.onStopped(mThreadId));
    }

    /**
     * Waits on the shared barrier, returning {@code true} on success or
     * {@code false} if the barrier is broken / interrupted.
     */
    private boolean awaitBarrier(final IUIUpdateCallback callback) {
        final CyclicBarrier barrier = sSharedBarrier;
        if (barrier == null) {
            return true;
        }
        try {
            barrier.await();
            return true;
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (final BrokenBarrierException e) {
            // Another thread called stop() – exit cleanly.
            return false;
        }
    }

    private static void postToMain(final Runnable runnable) {
        new Handler(Looper.getMainLooper()).post(runnable);
    }
}
