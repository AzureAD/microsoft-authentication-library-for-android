// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.mfa.internal

import com.microsoft.identity.mfa.MfaApprovalBlockReason
import com.microsoft.identity.mfa.MfaAuthSession
import com.microsoft.identity.mfa.MfaDecision
import com.microsoft.identity.mfa.MfaDisplayMetadata
import com.microsoft.identity.mfa.MfaEnrollmentState
import com.microsoft.identity.mfa.MfaPushTokenState
import com.microsoft.identity.mfa.MfaRegistrationParameters
import com.microsoft.identity.mfa.MfaSdkStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MfaServiceTest {
    private val marker = "synthetic-sensitive-marker"
    private val operation = MfaOperationContext("operation", "attempt", "installation", 1, 2)
    private val account = MfaAccountIdentity("environment", "tenant", "object", "registration")
    private val context = MfaRegistrationContext(operation, account, MfaCredentialHandle(marker))
    private val parameters = MfaRegistrationParameters.QrCode(marker)
    private val activation = MfaActivationRequest(operation, parameters)
    private val validation = MfaValidationRequest(context, marker)
    private val receipt = MfaValidationReceipt(context, marker)
    private val display = MfaDisplayMetadata(marker, marker)
    private val material = MfaEnrollmentMaterial(
        context, MfaCredentialMaterial(marker.toByteArray()), display
    )
    private val decision = MfaDecisionRequest(context, marker, MfaDecisionIntent.APPROVE)
    private val number = MfaNumberRequest(context, marker, "42")

    @Test
    fun validationCanCompleteWhileActivationRemainsDelayed() = runTest {
        val service = FakeMfaService()
        val activationCall = async { service.activate(activation) }
        val validationCall = async { service.validateDevice(validation) }
        runCurrent()
        service.activationStarted.await()
        service.validationStarted.await()
        assertFalse(activationCall.isCompleted)
        assertFalse(validationCall.isCompleted)

        val earlyValidation = MfaServiceOutcome.Acknowledged(receipt)
        service.validationResponse.complete(earlyValidation)
        runCurrent()
        assertSame(earlyValidation, validationCall.await())
        assertFalse(activationCall.isCompleted)

        val activated = MfaServiceOutcome.Acknowledged(material)
        service.activationResponse.complete(activated)
        runCurrent()
        assertSame(activated, activationCall.await())
        assertSame(context, activated.value.context)
    }

    @Test
    fun acknowledgedRejectedUnknownAndUnavailableAreDistinct() = runTest {
        val service = FakeMfaService()
        MfaDecision.values().forEach { acknowledged ->
            service.decisionResponse = MfaServiceOutcome.Acknowledged(acknowledged)
            val result = service.submitDecision(decision)
            assertEquals(acknowledged, (result as MfaServiceOutcome.Acknowledged<MfaDecision>).value)
        }
        val rejected = MfaServiceOutcome.Rejected(MfaServiceRejection.SERVICE_REJECTION)
        service.decisionResponse = rejected
        assertSame(rejected, service.submitDecision(decision))
        assertEquals(MfaServiceRejection.SERVICE_REJECTION, rejected.reason)

        service.decisionResponse = MfaServiceOutcome.Unknown
        assertSame(MfaServiceOutcome.Unknown, service.submitDecision(decision))
        service.decisionResponse = MfaServiceOutcome.Unavailable
        assertSame(MfaServiceOutcome.Unavailable, service.submitDecision(decision))
    }

    @Test
    fun numberSubmissionNeverFallsBackToOrdinaryApproval() = runTest {
        val service = FakeMfaService()
        service.decisionResponse = MfaServiceOutcome.Acknowledged(MfaDecision.APPROVED)
        assertSame(MfaServiceOutcome.NotSupported, service.submitNumber(number))
        assertTrue(service.decisionRequests.isEmpty())
        assertEquals(listOf(number), service.numberRequests)

        val invalidNumber = MfaServiceOutcome.Rejected(MfaServiceRejection.INVALID_NUMBER)
        service.numberResponse = invalidNumber
        assertSame(invalidNumber, service.submitNumber(number))
        service.numberResponse = MfaServiceOutcome.Unknown
        assertSame(MfaServiceOutcome.Unknown, service.submitNumber(number))
        service.numberResponse = MfaServiceOutcome.Acknowledged(MfaDecision.APPROVED)
        assertEquals(
            MfaDecision.APPROVED,
            (service.submitNumber(number) as MfaServiceOutcome.Acknowledged<MfaDecision>).value
        )
        assertTrue(service.decisionRequests.isEmpty())

        service.submitDecision(decision)
        assertEquals(listOf(decision), service.decisionRequests)
        assertEquals(4, service.numberRequests.size)
    }

    @Test
    fun cancellingActivationDoesNotCancelIndependentValidationOrInventAnOutcome() = runTest {
        val service = FakeMfaService()
        var completed = false
        val activationCall = launch {
            service.activate(activation)
            completed = true
        }
        val validationCall = async { service.validateDevice(validation) }
        runCurrent()
        service.activationStarted.await()
        activationCall.cancelAndJoin()
        assertTrue(activationCall.isCancelled)
        assertFalse(completed)
        assertFalse(service.activationResponse.isCancelled)
        service.validationResponse.complete(MfaServiceOutcome.Acknowledged(receipt))
        assertSame(
            receipt,
            (validationCall.await() as MfaServiceOutcome.Acknowledged<MfaValidationReceipt>).value
        )
    }

    @Test
    fun cancellingValidationPropagatesWithoutCancellingActivation() = runTest {
        val service = FakeMfaService()
        val activationCall = async { service.activate(activation) }
        val validationCall = async { service.validateDevice(validation) }
        runCurrent()
        validationCall.cancelAndJoin()
        assertTrue(validationCall.isCancelled)
        assertFalse(activationCall.isCompleted)
        assertFalse(service.validationResponse.isCancelled)
        service.activationResponse.complete(MfaServiceOutcome.Unknown)
        assertSame(MfaServiceOutcome.Unknown, activationCall.await())
    }

    @Test
    fun alreadyCancelledCallerCannotSubmitOrReceiveUnsupportedAsAnOutcome() = runTest {
        val service = FakeMfaService()
        val caller = launch {
            currentCoroutineContext().cancel()
            try {
                service.submitDecision(decision)
                fail("Cancellation must propagate")
            } catch (_: CancellationException) {
                // Expected; no response has been returned.
            }
            try {
                service.unregister(context)
                fail("Cancellation must propagate")
            } catch (_: CancellationException) {
                // Default optional operations preserve cancellation too.
            }
        }
        caller.join()
        assertTrue(caller.isCancelled)
        assertTrue(service.decisionRequests.isEmpty())
    }

    @Test
    fun optionalCapabilitiesDefaultFalseAndUnscriptedOperationsAreUnsupported() = runTest {
        val service: MfaService = FakeMfaService()
        val capabilities = service.capabilities
        assertFalse(capabilities.pushTokenUpdates)
        assertFalse(capabilities.unregistration)
        assertFalse(capabilities.reconciliation)
        assertFalse(capabilities.richPush)
        assertFalse(capabilities.challengePull)
        assertSame(MfaServiceOutcome.NotSupported, service.pullChallenges(context))
        assertSame(
            MfaServiceOutcome.NotSupported,
            service.updatePushToken(MfaPushTokenRequest(context, MfaPushToken(marker)))
        )
        assertSame(MfaServiceOutcome.NotSupported, service.unregister(context))
        assertSame(
            MfaServiceOutcome.NotSupported,
            service.reconcile(MfaReconciliationRequest(context, "previous-operation"))
        )
        assertSame(
            MfaServiceOutcome.NotSupported,
            service.confirmActivation(MfaConfirmationRequest(context, receipt))
        )
        assertSame(
            MfaServiceOutcome.NotSupported,
            service.fetchChallenge(MfaChallengeRequest(context, marker))
        )
        assertSame(MfaServiceOutcome.NotSupported, service.submitDecision(decision))
        assertSame(MfaServiceOutcome.NotSupported, service.submitNumber(number))
    }

    @Test
    fun identityIncludesAccountScopeAndOperationGenerationsNotUsername() {
        assertEquals(account, account.copy())
        assertEquals(
            5,
            setOf(
                account,
                account.copy(environment = "other"),
                account.copy(tenantId = "other"),
                account.copy(objectId = "other"),
                account.copy(registrationId = "other")
            ).size
        )
        assertNotEquals(operation, operation.copy(installationGeneration = 2))
        assertNotEquals(operation, operation.copy(registrationGeneration = 3))
        assertNotEquals(operation, operation.copy(installationId = "other"))
        assertNotEquals(operation, operation.copy(attemptId = "retry"))
        assertNotEquals(operation, operation.copy(operationId = "new-operation"))
        assertFalse(MfaAccountIdentity::class.java.declaredFields.any { it.name == "username" })
    }

    @Test
    fun materialAndChallengeCollectionsAreDefensiveSnapshots() {
        val source = byteArrayOf(1, 2, 3)
        val credential = MfaCredentialMaterial(source)
        source.fill(0)
        val exported = credential.copyBytes()
        assertArrayEquals(byteArrayOf(1, 2, 3), exported)
        exported.fill(0)
        assertArrayEquals(byteArrayOf(1, 2, 3), credential.copyBytes())

        val reasons = setOf(MfaApprovalBlockReason.UNKNOWN_POLICY)
        val session = MfaAuthSession(marker, marker, display, 1000, true, reasons)
        val envelope = MfaChallengeEnvelope(context, session)
        val sourceChallenges = mutableListOf(envelope)
        val batch = MfaChallengeBatch(sourceChallenges)
        sourceChallenges.clear()
        assertEquals(listOf(envelope), batch.challenges)
        assertThrows(UnsupportedOperationException::class.java) {
            (batch.challenges as MutableList<MfaChallengeEnvelope>).clear()
        }
    }

    @Test
    fun requestsCredentialsAndNestedOutcomesNeverRenderSensitiveMarkers() {
        val sensitiveOperation = MfaOperationContext(marker, marker, marker, 1, 1)
        val sensitiveAccount = MfaAccountIdentity(marker, marker, marker, marker)
        val sensitiveContext = MfaRegistrationContext(
            sensitiveOperation, sensitiveAccount, MfaCredentialHandle(marker)
        )
        val session = MfaAuthSession(marker, marker, display, 1000, true, emptySet())
        val envelope = MfaChallengeEnvelope(sensitiveContext, session)
        val models = listOf(
            sensitiveOperation, sensitiveAccount, sensitiveContext, context.credential,
            material.material, activation, validation, receipt, material, decision,
            MfaNumberRequest(context, marker, marker),
            MfaPushToken(marker),
            MfaPushTokenRequest(context, MfaPushToken(marker)),
            MfaConfirmationRequest(context, receipt),
            MfaChallengeRequest(context, marker),
            envelope, MfaChallengeBatch(listOf(envelope)),
            MfaReconciliationRequest(context, marker),
            MfaReconciliationResult(
                MfaSdkStatus(MfaEnrollmentState.UNKNOWN, MfaPushTokenState.UNKNOWN), null
            ),
            MfaServiceOutcome.Acknowledged(material),
            MfaServiceOutcome.Rejected(MfaServiceRejection.INVALID_NUMBER),
            MfaServiceOutcome.Unknown, MfaServiceOutcome.NotSupported, MfaServiceOutcome.Unavailable
        )
        models.forEach { assertFalse(it.toString().contains(marker)) }
        listOf(
            MfaCredentialHandle::class.java, MfaCredentialMaterial::class.java,
            MfaPushToken::class.java, MfaNumberRequest::class.java
        ).forEach { type ->
            assertFalse(type.methods.any { it.name == "copy" || it.name == "component1" })
        }
    }
}
