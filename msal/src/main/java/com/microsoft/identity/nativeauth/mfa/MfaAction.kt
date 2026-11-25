// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.nativeauth.mfa

/**
 * Explicit user decision to submit through [MfaSdkManager.respond], not an acknowledged outcome.
 *
 * Warning: This API is experimental. It may be changed in the future without notice.
 * Do not use in production applications.
 */
@ExperimentalMfaApi
sealed class MfaAction {
    /**
     * Approves the session using its authoritative approval policy.
     *
     * Warning: This API is experimental. It may be changed in the future without notice.
     * Do not use in production applications.
     * @property number User-entered matching number, or null only for permitted ordinary approval.
     * Missing or malformed required input must not downgrade number matching. Never log this value.
     */
    @ExperimentalMfaApi
    class Approve @JvmOverloads constructor(val number: String? = null) : MfaAction() {
        override fun equals(other: Any?): Boolean = other is Approve && number == other.number

        override fun hashCode(): Int = number?.hashCode() ?: 0

        /** Omits the entered matching number. */
        override fun toString(): String = "Approve(<redacted>)"
    }

    /**
     * Denies the session without requesting or validating a number.
     *
     * Warning: This API is experimental. It may be changed in the future without notice.
     * Do not use in production applications.
     */
    @ExperimentalMfaApi
    object Deny : MfaAction()
}
