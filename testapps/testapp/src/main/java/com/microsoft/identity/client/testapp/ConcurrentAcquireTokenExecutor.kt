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
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class ConcurrentAcquireTokenExecutor(
    val threadId: Int,
    val totalCount: Int
) {

    private val randomDelayInMs = (10..50).random().toLong()

    // Flag to track if execution should stop
    private val isStopped = AtomicBoolean(false)

    // Use a dedicated executor for background work
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    interface IUIUpdateCallback {
        fun updateProgress(threadId: Int, successCount: Int, completedCount: Int)
        fun onStopped(threadId: Int)
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
                    // Start the first iteration immediately (no initial sleep)
                    executeAcquireTokenSilent(result!!,
                        0,
                        0,
                        requestOptions,
                        uiCallback)
                }

                override fun showMessage(message: String?) {
                    // do nothing.
                }
            }
        )
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
        msalWrapper.acquireTokenSilent(
            requestOptions,
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

                    // MSAL callbacks already run on main thread - update UI directly
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
                    Thread.sleep(randomDelayInMs)

                    // Post back to main thread to call acquireTokenSilent
                    // (required since MSAL needs to be called from main thread)
                    Handler(Looper.getMainLooper()).post {
                        executeAcquireTokenSilent(
                            msalWrapper,
                            newSuccessCount,
                            newCompletedCount,
                            requestOptions,
                            uiCallback
                        )
                    }
                } catch (e: InterruptedException) {
                    // Interrupted - likely due to stop request
                    Handler(Looper.getMainLooper()).post {
                        uiCallback.onStopped(threadId)
                    }
                }
            }
        }
    }

    fun shutdown() {
        stop()
    }
}