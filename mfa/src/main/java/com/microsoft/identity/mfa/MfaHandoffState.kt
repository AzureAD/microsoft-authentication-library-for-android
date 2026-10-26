// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa

/** Request-local lifecycle. SUCCEEDED requires this request's registerAuthMethod return. */
enum class MfaHandoffPhase {
    IDLE, AWAITING_CONFIRMATION, SUBMITTING, SUCCEEDED, FAILED,
    CANCELLED, EXPIRED, OUTCOME_UNKNOWN, DISPOSED
}

/** Entry point, not proof of user identity or browser authentication. */
enum class MfaHandoffSource { DIRECT, BROWSER, WEBVIEW }

/**
 * Credential-free observation state. [requestId] is SDK-generated correlation, not a credential.
 * Errors expose only an SDK category; account details remain available from the manager separately.
 */
data class MfaHandoffState(
    val phase: MfaHandoffPhase = MfaHandoffPhase.IDLE,
    val requestId: String? = null,
    val source: MfaHandoffSource? = null,
    val errorCode: MfaErrorCode? = null
)

/** Receipt for an input attempt; an unsuccessful attempt never overwrites an active request. */
enum class MfaHandoffDisposition { STAGED, DUPLICATE, BUSY, IGNORED, REJECTED, CLOSED, LIMIT_REACHED }

/** Credential-free input receipt. STAGED means confirmation is required, never service success. */
data class MfaHandoffReceipt(
    val disposition: MfaHandoffDisposition,
    val requestId: String? = null,
    val errorCode: MfaErrorCode? = null
)
