// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.client.testapp

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.microsoft.identity.client.IAuthenticationResult
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class ConcurrentAcquireTokenExecutor(
    val threadId: Int,
    val totalCount: Int
) {

    // Flag to track if execution should stop
    private val isStopped = AtomicBoolean(false)

    // Use a dedicated executor for background work
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    interface IUIUpdateCallback {
        fun updateProgress(threadId: Int, successCount: Int, completedCount: Int)
        fun onStopped(threadId: Int)
        fun onError(threadId: Int, message: String)
    }

    fun execute(context: Context,
                requestOptions: RequestOptions,
                uiCallback: IUIUpdateCallback){
        // MsalWrapper.create callbacks run on main thread, so call it directly
        MsalWrapper.create(
            context.applicationContext,
            Constants.getResourceIdFromConfigFile(requestOptions.configFile),
            object : INotifyOperationResultCallback<MsalWrapper?> {
                override fun onSuccess(result: MsalWrapper?) {
                    if (result != null) {
                        // Wait at the barrier before the first iteration so all
                        // threads start their first request simultaneously.
                        executor.execute {
                            try {
                                sharedBarrier?.await(30, TimeUnit.SECONDS)
                            } catch (e: Exception) {
                                // Barrier broken or timeout — continue anyway
                            }
                            if (!isStopped.get()) {
                                executeAcquireTokenSilent(result,
                                    0,
                                    0,
                                    requestOptions,
                                    uiCallback)
                            }
                        }
                    } else {
                        // Handle the null case appropriately, e.g., notify UI or log error
                        // For now, we'll notify the UI that the operation has stopped
                        uiCallback.onStopped(threadId)
                    }
                }

                override fun showMessage(message: String?) {
                    // do nothing.
                }
            }
        )
    }

    companion object {
        /**
         * Shared barrier across all executor instances.
         * Set by the caller before starting executors.
         * All executors wait at this barrier before each iteration,
         * ensuring all threads fire simultaneously on each wave.
         */
        @JvmStatic
        var sharedBarrier: CyclicBarrier? = null

        /**
         * Pool of legitimate Microsoft Graph scopes.
         * Each thread uses a different scope to bypass calling-side command dedup
         * (CommandDispatcher collapses commands with identical parameters).
         */
        private val SCOPE_POOL = listOf(
            "user.read",
            "user.readbasic.all",
            "mail.read",
            "calendars.read",
            "contacts.read",
            "files.read",
            "files.read.all",
            "people.read",
            "notes.read",
            "tasks.read",
            "sites.read.all",
            "directory.read.all",
            "group.read.all"
        )

        /**
         * Returns a legitimate scope for the given thread ID.
         * Wraps around the pool if threadId exceeds the pool size.
         */
        @JvmStatic
        fun getScopeForThread(threadId: Int): String {
            return SCOPE_POOL[threadId % SCOPE_POOL.size]
        }
    }

    /**
     * Stop the executor and cancel any pending operations
     */
    fun stop() {
        if (isStopped.compareAndSet(false, true)) {
            executor.shutdownNow()
        }
    }

    private fun executeAcquireTokenSilent(msalWrapper: MsalWrapper,
                                          successCount: Int,
                                          completedCount: Int,
                                          requestOptions: RequestOptions,
                                          uiCallback: IUIUpdateCallback) {
        // Use a per-thread scope from the pool to bypass calling-side command dedup.
        val threadOptions = RequestOptions.withDifferentScopes(
            requestOptions,
            getScopeForThread(threadId)
        )
        msalWrapper.acquireTokenSilent(
            threadOptions,
            object : INotifyOperationResultCallback<IAuthenticationResult> {
                override fun onSuccess(result: IAuthenticationResult) {
                    val newSuccessCount = successCount + 1
                    val newCompletedCount = completedCount + 1

                    // MSAL callbacks already run on main thread - update UI directly
                    uiCallback.updateProgress(threadId, newSuccessCount, newCompletedCount)
                    executeNext(
                        msalWrapper,
                        newSuccessCount,
                        newCompletedCount,
                        requestOptions,
                        uiCallback
                    )
                }

                override fun showMessage(message: String?) {
                    val newCompletedCount = completedCount + 1

                    uiCallback.onError(threadId, message ?: "Unknown error")
                    uiCallback.updateProgress(threadId, successCount, newCompletedCount)

                    executeNext(
                        msalWrapper,
                        successCount,
                        newCompletedCount,
                        requestOptions,
                        uiCallback
                    )
                }
            }
        )
    }

    private fun executeNext(
        msalWrapper: MsalWrapper,
        newSuccessCount: Int,
        newCompletedCount: Int,
        requestOptions: RequestOptions,
        uiCallback: IUIUpdateCallback
    ) {
        // Check if stopped
        if (isStopped.get()) {
            Handler(Looper.getMainLooper()).post {
                uiCallback.onStopped(threadId)
            }
            return
        }

        if (newCompletedCount < totalCount) {
            executor.execute {
                try {
                    // If a shared barrier is set, wait for all sibling threads
                    // to reach this point before firing the next request.
                    // This ensures all threads fire each iteration simultaneously.
                    sharedBarrier?.await(30, TimeUnit.SECONDS)
                } catch (e: Exception) {
                    // Barrier broken or timeout — continue anyway
                }

                if (!isStopped.get()) {
                    executeAcquireTokenSilent(
                        msalWrapper,
                        newSuccessCount,
                        newCompletedCount,
                        requestOptions,
                        uiCallback
                    )
                }
            }
        } else {
            // All iterations completed — notify the UI
            Handler(Looper.getMainLooper()).post {
                uiCallback.onStopped(threadId)
            }
        }
    }

}