// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.nativeauth.mfa

/**
 * SDK-issued sign-in request. Pass this original object to the manager that returned it.
 *
 * Every returned session supports approval and denial, subject to service validation and session
 * validity. Unsupported policy fails retrieval rather than producing a partially actionable session.
 * The expected matching number is never exposed.
 * @property guid Service-issued sign-in request identifier, not sufficient to reconstruct a session.
 * @property registrationId SDK-validated identifier linking this session to [MfaRegistrationResult].
 * Use this linkage instead of matching account names; it does not replace [guid].
 * @property accountName Registered account's display label. Do not log it.
 * @property username Registered account's username. Do not log it.
 * @property requiresNumberMatching Whether approval requires a user-entered number. False permits
 * ordinary approval only when authoritative policy and the backend support it.
 */
class MfaAuthSession private constructor(
    val guid: String,
    val registrationId: String,
    val accountName: String,
    val username: String,
    val requiresNumberMatching: Boolean
) {
    /** Omits session identifiers and account information. */
    override fun toString(): String = "MfaAuthSession(<redacted>)"

    internal companion object {
        @JvmSynthetic
        internal fun create(
            guid: String,
            registrationId: String,
            accountName: String,
            username: String,
            requiresNumberMatching: Boolean
        ): MfaAuthSession = MfaAuthSession(
            guid, registrationId, accountName, username, requiresNumberMatching
        )
    }
}
