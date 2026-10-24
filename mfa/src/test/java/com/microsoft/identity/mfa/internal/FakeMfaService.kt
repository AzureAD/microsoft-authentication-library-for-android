// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.mfa.internal

import com.microsoft.identity.mfa.MfaDecision
import com.microsoft.identity.mfa.MfaRegistrationResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Test-only scripted responses. No timers, background scopes, real service behavior, or IO. */
internal class FakeMfaService : MfaService {
    val activationStarted = CompletableDeferred<Unit>()
    val activationResponse = CompletableDeferred<MfaServiceOutcome<MfaEnrollmentMaterial>>()
    val validationStarted = CompletableDeferred<Unit>()
    val validationResponse = CompletableDeferred<MfaServiceOutcome<MfaValidationReceipt>>()
    var decisionResponse: MfaServiceOutcome<MfaDecision> = MfaServiceOutcome.NotSupported
    var numberResponse: MfaServiceOutcome<MfaDecision> = MfaServiceOutcome.NotSupported
    private val decisions = mutableListOf<MfaDecisionRequest>()
    private val numbers = mutableListOf<MfaNumberRequest>()
    val decisionRequests: List<MfaDecisionRequest> get() = decisions.toList()
    val numberRequests: List<MfaNumberRequest> get() = numbers.toList()

    override suspend fun activate(
        request: MfaActivationRequest
    ): MfaServiceOutcome<MfaEnrollmentMaterial> {
        currentCoroutineContext().ensureActive()
        activationStarted.complete(Unit)
        return activationResponse.await()
    }

    override suspend fun validateDevice(
        request: MfaValidationRequest
    ): MfaServiceOutcome<MfaValidationReceipt> {
        currentCoroutineContext().ensureActive()
        validationStarted.complete(Unit)
        return validationResponse.await()
    }

    override suspend fun confirmActivation(
        request: MfaConfirmationRequest
    ): MfaServiceOutcome<MfaRegistrationResult> {
        currentCoroutineContext().ensureActive()
        return MfaServiceOutcome.NotSupported
    }

    override suspend fun fetchChallenge(
        request: MfaChallengeRequest
    ): MfaServiceOutcome<MfaChallengeEnvelope> {
        currentCoroutineContext().ensureActive()
        return MfaServiceOutcome.NotSupported
    }

    override suspend fun submitDecision(request: MfaDecisionRequest): MfaServiceOutcome<MfaDecision> {
        currentCoroutineContext().ensureActive()
        decisions.add(request)
        return decisionResponse
    }

    override suspend fun submitNumber(request: MfaNumberRequest): MfaServiceOutcome<MfaDecision> {
        currentCoroutineContext().ensureActive()
        numbers.add(request)
        return numberResponse
    }
}
