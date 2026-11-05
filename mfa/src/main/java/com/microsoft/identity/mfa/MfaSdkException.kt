// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.mfa

/**
 * Bounded SDK failure without raw service content, user data, or an underlying exception message.
 *
 * Coroutine cancellation is not an SDK failure and must propagate as cancellation. An unknown
 * outcome may have taken effect remotely; do not blindly retry it.
 * @property category Safe, typed failure category.
 */
class MfaSdkException(val category: MfaErrorCategory) : Exception(category.name, null)

/** Stable failure distinctions without arbitrary server strings or user input. */
enum class MfaErrorCategory {
    /** Activation or other input is invalid. */
    INVALID_INPUT,
    /** Session identity or context is invalid. */
    INVALID_SESSION,
    /** The scoped account is unknown. */
    UNKNOWN_ACCOUNT,
    /** Required policy is unsupported. */
    UNSUPPORTED_POLICY,
    /** Required client or service capability is unsupported. */
    UNSUPPORTED_CAPABILITY,
    /** The input or session has expired. */
    EXPIRED,
    /** The supplied number is invalid. */
    INVALID_NUMBER,
    /** The service explicitly rejected the operation. */
    SERVICE_REJECTION,
    /** Transport failed; this alone does not prove a mutating operation did not take effect. */
    TRANSPORT_FAILURE,
    /** Local storage failed. */
    STORAGE_FAILURE,
    /** Another conflicting operation is in progress. */
    OPERATION_IN_PROGRESS,
    /** Recovery is required. */
    RECOVERY_REQUIRED,
    /** The service outcome cannot be established; reconciliation is needed before retry. */
    OUTCOME_UNKNOWN,
    /** The operation or service is unavailable. */
    UNAVAILABLE
}
