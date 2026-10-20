// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa

/** Enrollment progress. AWAITING_VALIDATION requires an independently delivered notification. */
enum class MfaEnrollmentStage { IDLE, ACTIVATING, AWAITING_VALIDATION, CONFIRMING, REGISTERED }

/** Safe state for rendering setup/progress; excludes tokens, activation inputs and service bodies. */
data class MfaSdkStatus(
    val enrollmentStage: MfaEnrollmentStage,
    val tokenReady: Boolean,
    val tokenChangeBlocked: Boolean
)
