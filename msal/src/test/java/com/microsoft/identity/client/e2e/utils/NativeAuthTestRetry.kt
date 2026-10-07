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
// furnished to do so, subject to the following conditions:
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
package com.microsoft.identity.client.e2e.utils

/**
 * Retry policy for transient Native Auth E2E failures, independent of the Android E2E test fixture
 * and its shared auth state.
 */
internal object NativeAuthTestRetry {
    /**
     * Three retries allow four attempts, with at most 35 seconds of backoff.
     * Persistent throttling remains a failure rather than a skipped test.
     */
    const val MAX_THROTTLE_RETRIES = 3
    private const val RETRY_BASE_DELAY_MILLIS = 5_000L
    private const val MAX_RETRY_DELAY_MILLIS = 20_000L

    /** Retries eligible failures; [sleeper] receives milliseconds and can record delays in tests. */
    fun <T> retryOperation(
        maxRetries: Int = MAX_THROTTLE_RETRIES,
        sleeper: (Long) -> Unit = { Thread.sleep(it) },
        authFlow: () -> T
    ) {
        var retryCount = 0

        while (true) {
            try {
                authFlow()
                return
            } catch (e: AssertionError) {
                if (!NativeAuthEmailOTPErrorClassifier.isThrottleError(e)) {
                    throw e
                }
                retryOrFail(e, retryCount++, maxRetries, sleeper)
            } catch (e: Exception) {
                retryOrFail(e, retryCount++, maxRetries, sleeper)
            }
        }
    }

    private fun retryOrFail(
        error: Throwable,
        retryCount: Int,
        maxRetries: Int,
        sleeper: (Long) -> Unit
    ) {
        if (retryCount >= maxRetries) {
            throw AssertionError(error.message).apply { initCause(error) }
        }

        // Avoid repeatedly requesting OTPs while the Native Auth test tenant is throttling them.
        sleeper(
            minOf(RETRY_BASE_DELAY_MILLIS * (1L shl retryCount), MAX_RETRY_DELAY_MILLIS)
        )
    }
}
