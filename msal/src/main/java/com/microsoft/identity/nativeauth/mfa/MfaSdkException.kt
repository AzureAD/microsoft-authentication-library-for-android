// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.nativeauth.mfa

/**
 * SDK-created MFA failure shared by all manager operations, independent of Native Auth errors.
 *
 * Exactly one classifier is true. Use its documented caller action, not diagnostic text or service
 * codes. Recovery does not automatically authorize replay; failure or cancellation does not prove
 * rollback. Coroutine cancellation propagates separately and unchanged.
 *
 * SDK errors never embed raw underlying errors, responses, claims, notification payloads or credentials.
 *
 * @property errorMessage Optional bounded, sanitized SDK-controlled diagnostic, also exposed as
 * the inherited [message]. Do not parse it for control flow.
 * @property correlationId SDK-generated operation correlation ID, including for local failures.
 * Not a server-generated error field or an authentication-session GUID.
 */
class MfaSdkException private constructor(
    private val type: MfaErrorType,
    val errorMessage: String?,
    val correlationId: String
) : Exception(errorMessage, null) {
    /**
     * A required push token has not been supplied. Supply it through updatePushRegistration.
     * Invalid token data, storage failures and unsuccessful rotation do not qualify.
     */
    fun isPushTokenMissing(): Boolean = type == MfaErrorType.PUSH_TOKEN_MISSING

    /**
     * The explicitly requested registration is absent locally. Reload registeredAuthMethods.
     * An empty list is successful; storage failures and in-flight context mismatches do not qualify.
     */
    fun isRegistrationNotFound(): Boolean = type == MfaErrorType.REGISTRATION_NOT_FOUND

    /**
     * An authoritative outcome establishes that the session is unusable. Discard it.
     * Includes a known completed or replay-blocked SDK session, not malformed or untrusted context.
     */
    fun isInvalidChallenge(): Boolean = type == MfaErrorType.INVALID_CHALLENGE

    /**
     * The matching number is invalid and the same session remains usable. Request the number again.
     * Pre-submission format failures can qualify; service rejections require confirmed continuation
     * semantics. An uncertain submission outcome is a general error, not permission to retry.
     */
    fun isInvalidCode(): Boolean = type == MfaErrorType.INVALID_CODE

    /**
     * No supported application recovery is established. End the operation and record safe diagnostics.
     * Includes protocol, storage, configuration, busy/partial-write and uncertain-outcome failures,
     * unclassified service errors and internal notification/account-association failures.
     * Do not automatically retry; this classification does not establish permanent failure.
     */
    fun isGeneralError(): Boolean = type == MfaErrorType.GENERAL_ERROR

    internal companion object {
        @JvmSynthetic
        internal fun create(
            type: MfaErrorType,
            errorMessage: String?,
            correlationId: String
        ): MfaSdkException = MfaSdkException(type, errorMessage, correlationId)
    }
}

internal enum class MfaErrorType {
    PUSH_TOKEN_MISSING,
    REGISTRATION_NOT_FOUND,
    INVALID_CHALLENGE,
    INVALID_CODE,
    GENERAL_ERROR
}
