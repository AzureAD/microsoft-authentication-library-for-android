// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa.sample

import com.microsoft.identity.mfa.MfaDeletionError
import com.microsoft.identity.mfa.MfaDeletionException
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resumeWithException

internal class DeletionCallbackBridge {
    private val outstanding = AtomicBoolean(false)

    suspend fun <T> await(start: ((Result<T>) -> Unit) -> Unit): T =
        suspendCancellableCoroutine { continuation ->
            if (!outstanding.compareAndSet(false, true)) {
                continuation.resumeWithException(MfaDeletionException(MfaDeletionError.OPERATION_IN_PROGRESS))
                return@suspendCancellableCoroutine
            }
            val completed = AtomicBoolean(false)
            val complete: (Result<T>) -> Unit = { result ->
                if (completed.compareAndSet(false, true)) {
                    outstanding.set(false)
                    if (continuation.isActive) continuation.resumeWith(result)
                }
            }
            // MSAL returns void, not a cancellable operation. Hold the guard until its callback;
            // cancelled continuations ignore late tokens and cannot advance to DELETE.
            try { start(complete) }
            catch (_: Exception) { complete(Result.failure(MfaDeletionException(MfaDeletionError.PROVIDER_FAILED))) }
        }
}
