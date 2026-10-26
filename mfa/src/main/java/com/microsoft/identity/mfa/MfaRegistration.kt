// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.mfa

/**
 * Sensitive activation bootstrap input supplied by the host.
 *
 * The host owns QR capture and input lifetime. Do not log these values; strings cannot be cleared
 * from memory. Construction does not parse, validate, enroll, or contact a service. An activation
 * URL is bootstrap input, not permission to override future service routing or trust validation.
 */
sealed class MfaRegistrationParameters private constructor() {
    /**
     * QR contents captured by the host.
     * @property text Sensitive, unparsed QR text.
     */
    class QrCode(val text: String) : MfaRegistrationParameters()

    /**
     * Explicit activation input.
     * @property code Sensitive activation code.
     * @property url Sensitive activation bootstrap URL; not an arbitrary service endpoint override.
     */
    class Activation(val code: String, val url: String) : MfaRegistrationParameters()

    /** Returns a bounded representation without activation input. */
    final override fun toString(): String = "MfaRegistrationParameters(<redacted>)"
}

/**
 * Host-rendered display data, never an account key or authorization evidence.
 *
 * Treat display strings as untrusted text, not HTML or routing information.
 * @property accountName Account label for display only.
 * @property username User label for display only; not a stable account identifier.
 */
class MfaDisplayMetadata(val accountName: String, val username: String) {
    /** Omits potentially identifying display text. */
    override fun toString(): String = "MfaDisplayMetadata(<redacted>)"
}

/**
 * Confirmed enrollment result, created internally after service acknowledgement.
 * @property registrationId Opaque registration identity, not a credential or service address.
 * @property displayMetadata Display-only account information.
 */
class MfaRegistrationResult internal constructor(
    val registrationId: String,
    val displayMetadata: MfaDisplayMetadata
) {
    /** Omits account and registration identifiers. */
    override fun toString(): String = "MfaRegistrationResult(<redacted>)"
}
