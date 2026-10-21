// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa

/** An opaque challenge issued by this manager, never the expected number-matching answer. */
class MfaAuthSession internal constructor(
    val guid: String,
    val accountName: String,
    val username: String,
    val requiresNumberMatching: Boolean,
    blockedReasons: Set<MfaApprovalBlockReason> = emptySet()
) {
    /** Requirements this experimental client cannot satisfy; contains no policy payload. */
    val approvalBlockedReasons: Set<MfaApprovalBlockReason> =
        java.util.Collections.unmodifiableSet(blockedReasons.toSet())

    /** Whether approval is supported locally; the service still decides the final outcome. */
    val canApprove: Boolean get() = approvalBlockedReasons.isEmpty()
}

/** Bounded reasons why this experimental client must not submit an approval. */
enum class MfaApprovalBlockReason {
    /** The challenge or credential policy does not allow ordinary MFA push. */
    UNSUPPORTED_MODE,
    /** The credential policy disables MFA. */
    MFA_DISABLED,
    /** Device verification is required but is not implemented by this PoC. */
    APP_LOCK_REQUIRED,
    /** An application approval PIN, distinct from the browser number, is required. */
    APPROVAL_PIN_REQUIRED,
    /** A legacy PIN change is required. */
    PIN_CHANGE_REQUIRED,
    /** Required application context is not rendered by this PoC. */
    APP_CONTEXT_REQUIRED,
    /** Required location context is not rendered by this PoC. */
    LOCATION_CONTEXT_REQUIRED,
    /** Location collection is required. */
    LOCATION_REQUIRED,
    /** This PoC only supports interactive decisions. */
    SILENT_REQUEST,
    /** Policy requires number matching but the challenge does not provide it. */
    NUMBER_MATCHING_DATA_MISSING,
    /** An additional administrator policy cannot be enforced by this PoC. */
    ADMIN_POLICY,
    /** The policy includes an unknown or indeterminate requirement. */
    UNKNOWN_POLICY,
    /** Policy data cannot be decoded unambiguously within the configured limits. */
    MALFORMED_POLICY
}
