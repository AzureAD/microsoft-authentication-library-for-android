// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.mfa

/** Enrollment progress, not an authentication decision or evidence of completed sign-in. */
enum class MfaEnrollmentStage {
    /** No enrollment is in progress. */
    IDLE,

    /** Enrollment is being activated. */
    ACTIVATING,

    /** Enrollment awaits a validation notification forwarded by the host. */
    AWAITING_VALIDATION,

    /** Enrollment is being confirmed with the service. */
    CONFIRMING,

    /** Enrollment has been confirmed and persisted locally. */
    REGISTERED
}

/**
 * Non-sensitive status for setup and progress presentation.
 *
 * The API-review manager exposes only an inert initial value; it implements no lifecycle transitions.
 * @property enrollmentStage Current enrollment stage.
 * @property tokenReady Whether a push token is available for enrollment.
 * @property tokenChangeBlocked Whether a push-token change is currently blocked.
 */
data class MfaSdkStatus(
    val enrollmentStage: MfaEnrollmentStage,
    val tokenReady: Boolean,
    val tokenChangeBlocked: Boolean
)
