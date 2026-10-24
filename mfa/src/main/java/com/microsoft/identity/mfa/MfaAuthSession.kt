// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.mfa

import java.util.Collections

/**
 * Immutable challenge snapshot constructed by the SDK, not an approval or authorization grant.
 *
 * The host owns rendering and must not infer service success from a button press or local model.
 * No expected number-matching answer is exposed. Expiry and policy must be checked again when a
 * future operation is submitted; an empty block set is not service authorization.
 * @property sessionHandle Opaque SDK session handle; not service routing material.
 * @property registrationId Opaque registration identity owning this session.
 * @property displayMetadata Untrusted, display-only account information.
 * @property expiresAtEpochMillis Expiration time in milliseconds since the Unix epoch (UTC).
 * @property requiresNumberMatching Whether a separate number submission is required.
 */
class MfaAuthSession internal constructor(
    val sessionHandle: String,
    val registrationId: String,
    val displayMetadata: MfaDisplayMetadata,
    val expiresAtEpochMillis: Long,
    val requiresNumberMatching: Boolean,
    approvalBlockedReasons: Set<MfaApprovalBlockReason>
) {
    /** Defensive, immutable snapshot of requirements that prevent approval. */
    val approvalBlockedReasons: Set<MfaApprovalBlockReason> =
        Collections.unmodifiableSet(LinkedHashSet(approvalBlockedReasons))

    /** Omits session identifiers and user display data. */
    override fun toString(): String = "MfaAuthSession(<redacted>)"
}

/** Bounded reasons the client cannot satisfy an approval policy; no raw policy payload. */
enum class MfaApprovalBlockReason {
    /** The requested authentication mode is unsupported. */
    UNSUPPORTED_MODE,
    /** MFA is disabled by policy. */
    MFA_DISABLED,
    /** Required device verification is unsupported. */
    DEVICE_VERIFICATION_REQUIRED,
    /** A separate application approval PIN is required. */
    APPROVAL_PIN_REQUIRED,
    /** Required application context cannot be rendered. */
    APP_CONTEXT_REQUIRED,
    /** Required location capabilities cannot be satisfied. */
    LOCATION_REQUIRED,
    /** Number matching is required but required challenge data is unavailable. */
    NUMBER_MATCHING_DATA_MISSING,
    /** An administrator requirement cannot be satisfied. */
    ADMIN_POLICY,
    /** Policy requirements are unknown. */
    UNKNOWN_POLICY,
    /** Policy cannot be interpreted unambiguously. */
    MALFORMED_POLICY
}

/**
 * Acknowledged service decision only, never a local user action or a request to approve.
 *
 * A rejected or unknown submission must not be represented by either value.
 */
enum class MfaDecision {
    /** The service acknowledged approval. */
    APPROVED,
    /** The service acknowledged denial. */
    DENIED
}
