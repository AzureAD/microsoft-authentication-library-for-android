// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.mfa

/**
 * Display metadata for a service-confirmed, locally persisted registration.
 *
 * No activation material or credentials are returned. This value is not proof of current
 * server-side validity.
 * @property registrationId Opaque authentication-method identifier, not a user or tenant ID.
 * @property accountName Service-provided display label, not a unique identifier or guaranteed email.
 * May contain personal information; do not log it.
 */
data class MfaRegistrationResult(val registrationId: String, val accountName: String) {
    /** Omits registration identifiers and account information. */
    override fun toString(): String = "MfaRegistrationResult(<redacted>)"
}
