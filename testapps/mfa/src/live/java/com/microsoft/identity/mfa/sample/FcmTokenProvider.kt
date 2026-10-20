// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa.sample

import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal object FcmTokenProvider {
    suspend fun currentToken(): String = suspendCancellableCoroutine { continuation ->
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!continuation.isActive) return@addOnCompleteListener
            if (task.isSuccessful && !task.result.isNullOrBlank()) {
                continuation.resume(task.result)
            } else {
                // The Firebase exception may contain configuration details; do not retain/display it.
                continuation.resumeWithException(TokenAcquisitionFailure())
            }
        }
    }
}

internal class TokenAcquisitionFailure : Exception()
