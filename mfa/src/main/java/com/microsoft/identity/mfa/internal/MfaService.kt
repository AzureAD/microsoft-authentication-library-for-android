// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.mfa.internal

import com.microsoft.identity.mfa.MfaDecision
import com.microsoft.identity.mfa.MfaRegistrationResult
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * Replaceable client abstraction, not SAS endpoint names or a wire protocol.
 *
 * Implementations must preserve caller coroutine cancellation, never own a detached scope, and
 * never map cancellation to a rejection or acknowledgement. Cancellation of a mutating call does
 * not prove the service did nothing; later orchestration must reconcile uncertain outcomes.
 *
 * Requests carry generation/scope identities so future orchestration can reject stale results.
 * This seam performs no orchestration, storage, trust validation, or service calls itself.
 */
internal interface MfaService {
    val capabilities: MfaServiceCapabilities get() = MfaServiceCapabilities()

    suspend fun activate(request: MfaActivationRequest): MfaServiceOutcome<MfaEnrollmentMaterial>

    suspend fun validateDevice(request: MfaValidationRequest): MfaServiceOutcome<MfaValidationReceipt>

    suspend fun confirmActivation(
        request: MfaConfirmationRequest
    ): MfaServiceOutcome<MfaRegistrationResult>

    suspend fun fetchChallenge(request: MfaChallengeRequest): MfaServiceOutcome<MfaChallengeEnvelope>

    suspend fun submitDecision(request: MfaDecisionRequest): MfaServiceOutcome<MfaDecision>

    /** Only this path accepts a number; it must never delegate to ordinary approve/deny. */
    suspend fun submitNumber(request: MfaNumberRequest): MfaServiceOutcome<MfaDecision>

    suspend fun pullChallenges(
        context: MfaRegistrationContext
    ): MfaServiceOutcome<MfaChallengeBatch> = unsupported()

    suspend fun updatePushToken(request: MfaPushTokenRequest): MfaServiceOutcome<Unit> = unsupported()

    suspend fun unregister(context: MfaRegistrationContext): MfaServiceOutcome<Unit> = unsupported()

    suspend fun reconcile(
        request: MfaReconciliationRequest
    ): MfaServiceOutcome<MfaReconciliationResult> = unsupported()
}

private suspend fun unsupported(): MfaServiceOutcome<Nothing> {
    currentCoroutineContext().ensureActive()
    return MfaServiceOutcome.NotSupported
}
