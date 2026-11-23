// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.nativeauth.mfa.internal

import com.microsoft.identity.nativeauth.mfa.MfaErrorType
import com.microsoft.identity.nativeauth.mfa.MfaSdkException
import java.util.UUID
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

internal enum class MfaErrorCode {
    INVALID_ACTIVATION, MISSING_PUSH_REGISTRATION, OPERATION_IN_PROGRESS, UNTRUSTED_ENDPOINT,
    INVALID_NOTIFICATION, INVALID_RESPONSE, UNKNOWN_ACCOUNT, INVALID_SESSION, INVALID_NUMBER,
    UNSUPPORTED_POLICY, SERVICE_REJECTED, VALIDATION_TIMEOUT, TRANSPORT_FAILURE,
    STORAGE_FAILURE, OUTCOME_UNKNOWN, INVALID_CONFIGURATION
}

internal class MfaProtocolException(val code: MfaErrorCode, val serviceCode: String? = null) :
    Exception(code.name, null)

internal class MfaCorrelation(val id: String) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<MfaCorrelation>
}

internal suspend fun <T> mfaOperation(block: suspend () -> T): T {
    currentCoroutineContext().ensureActive()
    val correlation = MfaCorrelation(UUID.randomUUID().toString())
    var completed: Result<T>? = null
    try {
        withContext(Dispatchers.IO + correlation) {
            completed = try {
                Result.success(block())
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                val type = when ((error as? MfaProtocolException)?.code) {
                    MfaErrorCode.MISSING_PUSH_REGISTRATION -> MfaErrorType.PUSH_TOKEN_MISSING
                    MfaErrorCode.INVALID_SESSION -> MfaErrorType.INVALID_CHALLENGE
                    MfaErrorCode.INVALID_NUMBER -> MfaErrorType.INVALID_CODE
                    else -> MfaErrorType.GENERAL_ERROR
                }
                Result.failure(MfaSdkException.create(type,
                    (error as? MfaProtocolException)?.code?.name ?: "MFA operation failed.",
                    correlation.id))
            }
        }
    } catch (error: CancellationException) {
        // A committed outcome must not be replaced while returning from the work dispatcher.
        if (completed == null) throw error
    }
    return checkNotNull(completed).getOrThrow()
}
