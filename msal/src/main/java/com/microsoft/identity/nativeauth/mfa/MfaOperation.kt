// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.nativeauth.mfa

import java.util.Collections
import java.util.IdentityHashMap
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
     * Cancellation does not undo remote mutations. Required
     * consistency cleanup finishes before cancellation is reported; a cleanup timeout is not success
     * and must retain recovery state. This API-only draft has no remote mutations or cleanup.
     *
     * The suspend operation's outcome is committed after it finishes, including required cleanup,
     * before leaving its work context. A returned success or non-cancellation failure wins even if
     * cancellation was requested earlier. Only cancellation exceptions attributable to this request
     * are reported as that request; unrelated cancellations retain their identity. Once committed,
     * cancellation is a no-op even if callback delivery is queued. Cancelling never cancels the
     * mechanism delivering that terminal callback.
     */
    fun cancel()
}

private fun CancellationException.wrapsRequest(request: CancellationException): Boolean {
    val seen = Collections.newSetFromMap(IdentityHashMap<CancellationException, Boolean>())
    var current: Throwable? = this
    while (current is CancellationException && seen.add(current)) {
        if (current === request) return true
        current = current.cause
    }
    return false
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
    var outcome: Result<T>? = null
    var cancellation: CancellationException? = null

    fun commit(result: Result<T>) {
        synchronized(lock) {
            if (outcome == null) {
                val exception = result.exceptionOrNull()
                val request = cancellation
                outcome = if (exception is CancellationException && request != null &&
                    exception.wrapsRequest(request)
                ) {
                    Result.failure(request)
                } else {
                    result
                }
            }
        }
    }

    scope.launch {
        try {
            withContext(work) {
                val result = try {
                    Result.success(block())
                } catch (exception: Exception) {
                    Result.failure<T>(exception)
                }
                // Commit before cancelled context completion can replace the actual outcome.
                commit(result)
            }
        } catch (exception: Exception) {
            commit(Result.failure(exception))
        } finally {
            work.complete()
        }
        val result = checkNotNull(synchronized(lock) { outcome })
        // Delivery is independent of the cancellable work, and outside its exception boundary.
        scope.launch {
            result.fold(callback::onSuccess) { callback.onError(it as Exception) }
        }
    }
    return MfaOperation {
        val request = synchronized(lock) {
            if (outcome != null || cancellation != null) null
            else CancellationException("MFA operation cancelled.").also { cancellation = it }
        }
        if (request != null) work.cancel(request)
    }
}
