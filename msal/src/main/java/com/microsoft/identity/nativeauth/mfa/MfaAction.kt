// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.nativeauth.mfa

/** Explicit user decision to submit through [MfaSdkManager.respond], not an acknowledged outcome. */
sealed class MfaAction {
    /**
     * Approves the session using its authoritative approval policy.
     * @property number User-entered matching number, or null only for permitted ordinary approval.
     * Missing or malformed required input must not downgrade number matching. Never log this value.
     */
    data class Approve(val number: String?) : MfaAction() {
        /** Omits the entered matching number. */
        override fun toString(): String = "Approve(<redacted>)"
    }

    /** Denies the session without requesting or validating a number. */
    object Deny : MfaAction()
}
