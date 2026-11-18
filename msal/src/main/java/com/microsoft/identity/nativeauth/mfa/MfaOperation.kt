// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.nativeauth.mfa

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Java cancellation control for one callback-based MFA operation, without exposing a coroutine Job. */
fun interface MfaOperation {
    /**
     * Requests cooperative cancellation. Thread-safe, idempotent and non-blocking.
     *
     * Cancellation does not undo remote mutations or guarantee that provider UI closes. Required
     * consistency cleanup finishes before cancellation is reported; a cleanup timeout is not success
     * and must retain recovery state. This API-only draft has no remote mutations or cleanup.
     *
     * Cancellation competes with committing the result after the suspend operation finishes,
     * including required cleanup. Once committed, cancellation is a no-op even if callback delivery
     * is queued. Cancelling never cancels the mechanism delivering that terminal callback.
     */
    fun cancel()
}

@JvmSynthetic
internal fun <T> launchMfaOperation(
    callback: MfaCallback<T>,
    context: CoroutineContext = Dispatchers.IO,
    block: suspend () -> T
): MfaOperation {
    val scope = CoroutineScope(context)
    val work = Job()
    val lock = Any()
    var committed = false
    var cancellation: CancellationException? = null
    scope.launch {
        val result = try {
            withContext(work) {
                // Carry failures as values across coroutine boundaries to preserve exception identity.
                try {
                    Result.success(block())
                } catch (exception: Exception) {
                    Result.failure<T>(exception)
                }
            }
        } catch (exception: Exception) {
            Result.failure<T>(exception)
        } finally {
            work.complete()
        }
        val outcome = synchronized(lock) {
            committed = true
            cancellation?.let { Result.failure<T>(it) } ?: result
        }
        // Delivery is independent of the cancellable work, and outside its exception boundary.
        scope.launch {
            outcome.fold(callback::onSuccess) { callback.onError(it as Exception) }
        }
    }
    return MfaOperation {
        val request = synchronized(lock) {
            if (committed || cancellation != null) null
            else CancellationException("MFA operation cancelled.").also { cancellation = it }
        }
        if (request != null) work.cancel(request)
    }
}
