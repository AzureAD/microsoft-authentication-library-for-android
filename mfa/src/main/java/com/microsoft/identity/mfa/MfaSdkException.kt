// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa

/** Stable, bounded failure categories. OUTCOME_UNKNOWN must not trigger an automatic retry. */
enum class MfaErrorCode {
    FEATURE_DISABLED, INVALID_ACTIVATION, MISSING_PUSH_REGISTRATION,
    TOKEN_CHANGE_UNSUPPORTED, OPERATION_IN_PROGRESS, UNTRUSTED_ENDPOINT,
    INVALID_NOTIFICATION, INVALID_RESPONSE, UNKNOWN_ACCOUNT, INVALID_SESSION,
    INVALID_NUMBER, UNSUPPORTED_POLICY, SERVICE_REJECTED, VALIDATION_TIMEOUT,
    TRANSPORT_FAILURE, STORAGE_FAILURE, OUTCOME_UNKNOWN
}

/** Safe SDK failure. Original transport/storage exceptions and credential-bearing URLs are omitted. */
class MfaSdkException(
    val code: MfaErrorCode,
    serviceCode: String? = null
) : Exception(code.name) {
    /** Numeric service outcome or a fixed SDK label; arbitrary server text is never exposed. */
    val serviceCode: String? = serviceCode?.takeIf {
        it.matches(Regex("[0-9]{1,10}")) || it in setOf("Rejected", "SOAPFault", "HTTP")
    }
}
