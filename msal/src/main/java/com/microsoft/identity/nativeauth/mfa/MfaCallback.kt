// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.nativeauth.mfa

/**
 * Java-friendly completion of an MFA operation.
 *
 * Exactly one terminal callback is delivered on a background dispatcher, never inline on the
 * initiating stack. No specific worker thread or ordering across operations is promised. Move UI
 * updates to the main thread. Process termination and fatal runtime errors are outside this guarantee.
 *
 * SDK failures use [MfaSdkException].
 * Cancellation through [MfaOperation.cancel] reports a [java.util.concurrent.CancellationException]
 * when the operation cooperates by throwing it. A returned success or non-cancellation failure,
 * including a cleanup failure, is not replaced by a cancellation request.
 * Exceptions thrown by application callbacks are not caught and sent to [onError].
 *
 * Warning: This API is experimental. It may be changed in the future without notice.
 * Do not use in production applications.
 */
@ExperimentalMfaApi
interface MfaCallback<T> {
    /** Receives the result, or null for operations with no result (`MfaCallback<Void>` in Java). */
    fun onSuccess(result: T)

    /** Receives the original winning exception without SDK wrapping, reclassification or logging. */
    fun onError(exception: Exception)
}
