// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.client.tokenprovider

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * Java-friendly host token acquisition. Adapt with [TokenProvider.from].
 *
 * The SDK invokes this provider on a background dispatcher. Move interactive UI work to the main
 * thread and honor [TokenProviderParameters.allowsInteraction]. Return promptly with a cancellation
 * handle, or null if acquisition cannot be cancelled; complete on any thread, including synchronously.
 * The adapter resumes SDK work on its background dispatcher and ignores duplicate or late completions
 * after cancellation.
 */
fun interface CallbackTokenProvider {
    /**
     * Acquires a token under the same requirements as [TokenProvider.getAccessToken].
     * Thrown exceptions and [Callback.onError] preserve exception identity. Never log token data.
     * @return A best-effort cancellation handle, or null when cancellation is unsupported.
     * A null handle does not complete acquisition; the provider must still invoke [Callback].
     * SDK cancellation ignores subsequent callbacks even when there is no handle.
     */
    @Throws(Exception::class)
    fun getAccessToken(parameters: TokenProviderParameters, callback: Callback): Cancellation?

    /** Terminal result of host-owned acquisition; the first completion wins. */
    interface Callback {
        /** Supplies actual authenticated-account metadata, not copies of request hints. */
        fun onSuccess(result: TokenProviderResult)

        /** Supplies the original provider exception, including user dismissal of sign-in. */
        fun onError(exception: Exception)
    }

    /** Feature-neutral best-effort cancellation of host-owned token acquisition. */
    fun interface Cancellation {
        /**
         * Requests cancellation without promising UI dismissal or rollback.
         * Must be thread-safe, idempotent, non-blocking and non-throwing; may be called on any thread.
         */
        fun cancel()
    }
}

@JvmSynthetic
internal fun adaptTokenProvider(
    provider: CallbackTokenProvider,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
): TokenProvider = object : TokenProvider {
    override suspend fun getAccessToken(parameters: TokenProviderParameters): TokenProviderResult =
        withContext(dispatcher) {
            suspendCancellableCoroutine<Result<TokenProviderResult>> { continuation ->
                val completed = AtomicBoolean()
                val cancelled = CallbackTokenProvider.Cancellation { }
                val handle = AtomicReference<CallbackTokenProvider.Cancellation?>()
                continuation.invokeOnCancellation {
                    completed.set(true)
                    handle.getAndSet(cancelled)?.cancel()
                }
                val callback = object : CallbackTokenProvider.Callback {
                    override fun onSuccess(result: TokenProviderResult) {
                        if (completed.compareAndSet(false, true)) continuation.resume(Result.success(result))
                    }

                    override fun onError(exception: Exception) {
                        if (completed.compareAndSet(false, true)) continuation.resume(Result.failure(exception))
                    }
                }
                if (continuation.isActive) {
                    try {
                        val returned = provider.getAccessToken(parameters, callback)
                        // Cancellation may precede publication of the provider's handle.
                        if (!handle.compareAndSet(null, returned)) returned?.cancel()
                    } catch (exception: Exception) {
                        callback.onError(exception)
                    }
                }
            }
        }.getOrThrow()
}
