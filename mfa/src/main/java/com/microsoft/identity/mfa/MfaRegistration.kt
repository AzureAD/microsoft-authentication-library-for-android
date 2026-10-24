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

/**
 * Independent snapshots of enrollment and host-provided push-token availability.
 *
 * A token being available does not establish enrollment. This model implements no state machine
 * and construction does not change SDK or service state.
 * @property enrollmentState Last known enrollment state.
 * @property pushTokenState Last known push-token state; contains no token.
 */
class MfaSdkStatus(
    val enrollmentState: MfaEnrollmentState,
    val pushTokenState: MfaPushTokenState
)

/** Enrollment knowledge, independent of push-token availability. */
enum class MfaEnrollmentState {
    /** No confirmed enrollment. */
    NOT_ENROLLED,
    /** Activation is pending. */
    ACTIVATING,
    /** An independently delivered validation challenge is pending. */
    AWAITING_VALIDATION,
    /** Enrollment acknowledgement is pending. */
    CONFIRMING,
    /** Enrollment was confirmed by the service. */
    ENROLLED,
    /** Recovery is required before continuing. */
    RECOVERY_REQUIRED,
    /** Enrollment outcome cannot currently be established. */
    UNKNOWN
}

/** Availability and update progress of a host-owned push token, not enrollment proof. */
enum class MfaPushTokenState {
    /** No host token is available. */
    UNAVAILABLE,
    /** A host token is available; this does not imply service acceptance. */
    AVAILABLE,
    /** A token change awaits submission. */
    UPDATE_PENDING,
    /** A token update is in progress. */
    UPDATING,
    /** A token update failed. */
    UPDATE_FAILED,
    /** Token state or update outcome is unknown. */
    UNKNOWN
}
