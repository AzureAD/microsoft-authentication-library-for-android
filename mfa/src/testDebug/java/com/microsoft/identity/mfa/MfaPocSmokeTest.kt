// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa

import com.microsoft.identity.mfa.internal.MfaActivationParser
import com.microsoft.identity.mfa.internal.MfaEndpointPolicy
import com.microsoft.identity.mfa.internal.MfaProof
import com.microsoft.identity.mfa.internal.MfaServiceClient
import com.microsoft.identity.mfa.internal.MfaStore
import com.microsoft.identity.mfa.internal.MfaStoredState
import com.microsoft.identity.mfa.internal.MfaTransport
import com.microsoft.identity.mfa.internal.MfaWireRequest
import com.microsoft.identity.mfa.internal.MfaWireResponse
import com.microsoft.identity.mfa.internal.MfaXml
import com.microsoft.identity.mfa.poc.FixtureMfaTransport
import com.microsoft.identity.mfa.poc.MfaPocScenario
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Small debug-only wire/state suite; the same fixture transport powers the manual sample. */
@OptIn(ExperimentalCoroutinesApi::class)
class MfaPocSmokeTest {
    private class MemoryStore : MfaStore {
        var state = MfaStoredState()
        var failWrites = false
        override suspend fun load() = state
        override suspend fun save(state: MfaStoredState) {
            if (failWrites) throw MfaSdkException(MfaErrorCode.STORAGE_FAILURE)
            state.validate()
            this.state = state
        }
    }

    private fun manager(
        store: MemoryStore, transport: MfaTransport, enabled: Boolean = true,
        policyProfile: MfaPolicyProfile = MfaPolicyProfile.CONSERVATIVE
    ) =
        MfaSdkManager(store, MfaServiceClient(transport, "example.poc", "1.0", "test", "test-device"),
            enabled, validationTimeoutMillis = 1000, policyProfile = policyProfile)

    private val activation get() = MfaRegistrationParameters.Activation(
        FixtureMfaTransport.CODE, "https://${FixtureMfaTransport.HOST}")

    private fun registeredStore() = MemoryStore().also {
        it.state = MfaStoredState(token = FixtureMfaTransport.TOKEN, accounts = listOf(
            MfaStoredState.Account(FixtureMfaTransport.ACCOUNT, "Example account",
                FixtureMfaTransport.USERNAME, FixtureMfaTransport.SECRET, FixtureMfaTransport.TENANT,
                FixtureMfaTransport.OBJECT, "https://${FixtureMfaTransport.HOST}/pad",
                FixtureMfaTransport.TOKEN, "existing-dos", "synthetic-group")))
    }

    private fun nestedChallengeTransport(
        fixture: FixtureMfaTransport,
        fields: List<MfaXml>,
        rootFields: List<MfaXml> = emptyList()
    ) = object : MfaTransport {
        override suspend fun send(request: MfaWireRequest): MfaWireResponse {
            val original = fixture.send(request)
            if (request.headers["x-ms-mac-action"] != "PhoneAppAuthentication") return original
            val guid = MfaXml.parse(original.body).child("response")
                .child("phoneAppAuthenticationResponse").required("guid")
            return MfaWireResponse(200, MfaXml("pfpMessage", children = listOf(
                MfaXml("response", children = listOf(
                    MfaXml("status", attributes = mapOf("disposition" to "success")),
                    MfaXml("phoneAppAuthenticationResponse", children = listOf(
                        MfaXml("guid", guid), MfaXml("dosPreventer"),
                        MfaXml("phoneAppInfo", attributes = mapOf("mode" to "standard"), children = fields)
                    ) + rootFields)
                ))
            )).encoded().toByteArray())
        }
    }

    private suspend fun assertCode(code: MfaErrorCode, block: suspend () -> Unit) {
        try {
            block()
            throw AssertionError("Expected $code")
        } catch (e: MfaSdkException) { assertEquals(code, e.code) }
    }

    @Test fun enrollmentWaitsForPushAndDecisionUsesRealWire() = runTest {
        val store = MemoryStore()
        val fixture = FixtureMfaTransport()
        val manager = manager(store, fixture)
        manager.updatePushRegistration(FixtureMfaTransport.TOKEN)
        val registration = async { manager.registerAuthMethod(MfaRegistrationParameters.QrCode(
            "phonefactor://activate_account?code=000000000&url=" +
                "https%3A%2F%2Fpoc.mobileappcommunicator.auth.microsoft.com")) }
        runCurrent()
        assertEquals(MfaEnrollmentStage.AWAITING_VALIDATION, manager.status.value.enrollmentStage)
        assertTrue(manager.registeredAuthMethods().isEmpty())
        assertFalse(registration.isCompleted)
        manager.handlePushNotification(fixture.validationPayload())
        assertEquals(FixtureMfaTransport.ACCOUNT, registration.await().registrationId)
        val payload = fixture.issue()
        val challenge = manager.handlePushNotification(payload).single()
        assertTrue(challenge.requiresNumberMatching)
        assertSame(challenge, manager.handlePushNotification(payload).single())
        assertEquals(MfaDecision.APPROVED, manager.approve(challenge, FixtureMfaTransport.EXPECTED_NUMBER))
        assertCode(MfaErrorCode.INVALID_SESSION) { manager.deny(challenge) }
        val ordinary = manager.handlePushNotification(fixture.issue(false)).single()
        assertEquals(MfaDecision.APPROVED, manager.approve(ordinary))
        val denied = manager.pullPendingNotifications().single()
        assertEquals(MfaDecision.DENIED, manager.deny(denied))
        assertNotEquals(denied.guid, manager.pullPendingNotifications().single().guid)
        assertTrue(fixture.operations.value.containsAll(listOf("ActivateNew",
            "PhoneAppValidateDeviceToken", "ConfirmActivation", "PhoneAppPinValidation",
            "PhoneAppAuthenticationResult", "PhoneAppCheckForAuthentication")))
    }

    @Test fun validationCanArriveBeforeActivateReturnsAndPendingCanResume() = runTest {
        val store = MemoryStore()
        val fixture = FixtureMfaTransport()
        val gate = CompletableDeferred<Unit>()
        val transport = object : MfaTransport {
            override suspend fun send(request: com.microsoft.identity.mfa.internal.MfaWireRequest)
                : com.microsoft.identity.mfa.internal.MfaWireResponse {
                if (request.headers["SOAPAction"]?.endsWith("/ActivateNew") == true) gate.await()
                return fixture.send(request)
            }
        }
        val first = manager(store, transport)
        first.updatePushRegistration(FixtureMfaTransport.TOKEN)
        val operation = async { first.registerAuthMethod(activation) }
        runCurrent()
        first.handlePushNotification(fixture.validationPayload())
        assertTrue(store.state.pending?.validation != null)
        gate.complete(Unit)
        assertEquals(FixtureMfaTransport.ACCOUNT, operation.await().registrationId)
        val restored = manager(store, fixture)
        assertEquals(1, restored.registeredAuthMethods().size)
        restored.updatePushRegistration(FixtureMfaTransport.TOKEN)
        assertCode(MfaErrorCode.TOKEN_CHANGE_UNSUPPORTED) { restored.updatePushRegistration("replacement") }
        assertTrue(restored.status.value.tokenChangeBlocked)
        assertEquals(FixtureMfaTransport.TOKEN, store.state.token)
    }

    @Test fun validationPushPreservesServicePathAndQueryThroughPersistence() = runTest {
        val store = MemoryStore()
        val fixture = FixtureMfaTransport()
        val endpoint = "https://${FixtureMfaTransport.HOST}/pad?route=synthetic/pad"
        val transport = object : MfaTransport {
            override suspend fun send(request: MfaWireRequest): MfaWireResponse {
                if (request.headers["x-ms-mac-action"] == "PhoneAppValidateDeviceToken") {
                    assertEquals(endpoint, request.endpoint.toString())
                }
                return fixture.send(request)
            }
        }
        val manager = manager(store, transport)
        manager.updatePushRegistration(FixtureMfaTransport.TOKEN)
        val registration = async { manager.registerAuthMethod(activation) }
        runCurrent()
        manager.handlePushNotification(fixture.validationPayload() +
            ("url" to "${FixtureMfaTransport.HOST}/pad?route=synthetic"))
        assertEquals(FixtureMfaTransport.ACCOUNT, registration.await().registrationId)
        assertEquals(endpoint, store.state.accounts.single().endpoint)
        store.state.validate()
        val restored = manager(store, transport)
        assertEquals(1, restored.registeredAuthMethods().size)
        assertTrue(restored.pullPendingNotifications().isNotEmpty())
    }

    @Test fun notificationSuffixDoesNotRelaxActivationOrAuthorityChecks() = runTest {
        val host = FixtureMfaTransport.HOST
        assertEquals("https://$host/pad", MfaEndpointPolicy.notification(host).toString())
        assertEquals("https://$host/pad?/pad",
            MfaEndpointPolicy.notification("$host/pad?").toString())
        for (invalid in listOf("$host.evil.invalid/pad?x=1", "evil.invalid/pad?x=1",
            "$host@evil.invalid/pad?x=1", "user@$host/pad?x=1", "$host:80/pad?x=1",
            "$host./pad?x=1", "$host/a/../pad?x=1", "$host/%70ad?x=1",
            "$host/pad#fragment", "$host\\evil/pad?x=1", "https://$host/pad?x=1")) {
            assertCode(MfaErrorCode.UNTRUSTED_ENDPOINT) { MfaEndpointPolicy.notification(invalid) }
        }
        assertCode(MfaErrorCode.UNTRUSTED_ENDPOINT) {
            MfaActivationParser.parse(MfaRegistrationParameters.Activation(
                FixtureMfaTransport.CODE, "https://$host/PfPaWs.asmx?x=1"))
        }
    }

    @Test fun serviceExtensionsAndBlankValidationUsernameUseActivationIdentityInEitherOrder() = runTest {
        for (early in listOf(false, true)) {
            val store = MemoryStore()
            val fixture = FixtureMfaTransport()
            val gate = CompletableDeferred<Unit>()
            if (!early) gate.complete(Unit)
            val transport = object : MfaTransport {
                override suspend fun send(request: MfaWireRequest): MfaWireResponse {
                    if (request.headers["SOAPAction"]?.endsWith("/ActivateNew") == true) gate.await()
                    val response = fixture.send(request)
                    val xml = response.body.toString(Charsets.UTF_8)
                    val adapted = if ("SOAPAction" in request.headers) {
                        xml.replace("</soap:Body>",
                            "<extension xmlns=\"urn:example:extension\"/></soap:Body>")
                    } else if (request.headers["x-ms-mac-action"] == "PhoneAppValidateDeviceToken") {
                        xml.replace("<username>${FixtureMfaTransport.USERNAME}</username>", "<username></username>")
                    } else xml
                    return MfaWireResponse(response.statusCode, adapted.toByteArray())
                }
            }
            val manager = manager(store, transport)
            manager.updatePushRegistration(FixtureMfaTransport.TOKEN)
            val registration = async { manager.registerAuthMethod(activation) }
            runCurrent()
            manager.handlePushNotification(fixture.validationPayload())
            gate.complete(Unit)
            assertEquals(FixtureMfaTransport.ACCOUNT, registration.await().registrationId)
            assertEquals(FixtureMfaTransport.USERNAME, store.state.accounts.single().username)
            assertEquals(MfaEnrollmentStage.REGISTERED, manager.status.value.enrollmentStage)
            assertEquals(1, fixture.operations.value.count { it == "ConfirmActivation" })
        }
    }

    @Test fun soapExtensionsDoNotPermitDuplicateResponsesOrMixedFaults() = runTest {
        for ((extra, error) in listOf(
            "<ActivateNewResponse xmlns=\"${MfaServiceClient.NAMESPACE}\"/>" to MfaErrorCode.INVALID_RESPONSE,
            "<soap:Fault/>" to MfaErrorCode.SERVICE_REJECTED
        )) {
            val fixture = FixtureMfaTransport()
            val transport = object : MfaTransport {
                override suspend fun send(request: MfaWireRequest): MfaWireResponse {
                    val response = fixture.send(request)
                    return MfaWireResponse(200, response.body.toString(Charsets.UTF_8)
                        .replace("</soap:Body>", "$extra</soap:Body>").toByteArray())
                }
            }
            val client = MfaServiceClient(transport, "example.poc", "1.0", "test", "test-device")
            assertCode(error) {
                client.activation("ActivateNew", MfaActivationParser.parse(activation).endpoint,
                    listOf(MfaXml("activationParams", children = listOf(
                        MfaXml("ActivationCode", FixtureMfaTransport.CODE),
                        MfaXml("DeviceToken", FixtureMfaTransport.TOKEN),
                        MfaXml("DeviceName", "test-device"), MfaXml("Version", "1.0"),
                        MfaXml("OathCounter", "1")))))
            }
        }
    }

    @Test fun blankValidationUsernameWithoutGroupKeyCannotCompleteRegistration() = runTest {
        val store = MemoryStore()
        val fixture = FixtureMfaTransport()
        val transport = object : MfaTransport {
            override suspend fun send(request: MfaWireRequest): MfaWireResponse {
                val response = fixture.send(request)
                if (request.headers["x-ms-mac-action"] != "PhoneAppValidateDeviceToken") return response
                return MfaWireResponse(200, response.body.toString(Charsets.UTF_8)
                    .replace("<username>${FixtureMfaTransport.USERNAME}</username>", "<username></username>")
                    .replace("<groupKey>synthetic-group</groupKey>", "<groupKey></groupKey>").toByteArray())
            }
        }
        val manager = manager(store, transport)
        manager.updatePushRegistration(FixtureMfaTransport.TOKEN)
        val registration = async { manager.registerAuthMethod(activation) }
        runCurrent()
        assertCode(MfaErrorCode.INVALID_RESPONSE) { manager.handlePushNotification(fixture.validationPayload()) }
        assertTrue(store.state.accounts.isEmpty())
        assertFalse(fixture.operations.value.contains("ConfirmActivation"))
        registration.cancelAndJoin()
    }

    @Test fun nestedChallengeWithoutRegistrationIdMatchesIdentityAndPreservesDos() = runTest {
        val store = registeredStore()
        val fixture = FixtureMfaTransport()
        val transport = nestedChallengeTransport(fixture, listOf(
            MfaXml("tenantId", FixtureMfaTransport.TENANT),
            MfaXml("azureObjectId", FixtureMfaTransport.OBJECT),
            MfaXml("firstEntropyChallenge", FixtureMfaTransport.EXPECTED_NUMBER),
            MfaXml("returnLocationData", "no")
        ))
        val manager = manager(store, transport)
        val challenge = manager.handlePushNotification(fixture.issue()).single()
        assertTrue(challenge.requiresNumberMatching)
        assertEquals(FixtureMfaTransport.USERNAME, challenge.username)
        assertEquals("existing-dos", store.state.accounts.single().dosPreventer)
        assertEquals(MfaDecision.APPROVED, manager.approve(challenge, FixtureMfaTransport.EXPECTED_NUMBER))
    }

    @Test fun nestedChallengeRejectsConflictingOrAmbiguousIdentity() = runTest {
        for (case in listOf("tenant", "object", "id", "duplicateField", "duplicateAccount")) {
            val store = registeredStore()
            if (case == "duplicateAccount") {
                store.state = store.state.copy(accounts = store.state.accounts +
                    store.state.accounts.single().copy(id = "44444444-4444-4444-8444-444444444444"))
            }
            val fixture = FixtureMfaTransport()
            val fields = listOf(
                MfaXml("tenantId", if (case == "tenant") "other-tenant" else FixtureMfaTransport.TENANT),
                MfaXml("azureObjectId", if (case == "object") "other-object" else FixtureMfaTransport.OBJECT)
            ) + if (case == "id") listOf(MfaXml("phoneAppDetailId", "other-registration")) else emptyList()
            val rootFields = if (case == "duplicateField")
                listOf(MfaXml("tenantId", FixtureMfaTransport.TENANT)) else emptyList()
            val manager = manager(store, nestedChallengeTransport(fixture, fields, rootFields))
            assertCode(if (case == "duplicateField") MfaErrorCode.INVALID_RESPONSE else MfaErrorCode.UNKNOWN_ACCOUNT) {
                manager.handlePushNotification(fixture.issue())
            }
        }
    }

    @Test fun nestedUnsupportedPoliciesCannotBeApproved() = runTest {
        for (policy in listOf(
            MfaXml("returnLocationData", "yes"), MfaXml("isAppLockRequired", "true"),
            MfaXml("pinChangeRequired", "yes"), MfaXml("isSilent", "yes"),
            MfaXml("returnLocationData", "unrecognized"),
            MfaXml("userCredentialPolicy", "policy"), MfaXml("adminPolicies", children = listOf(MfaXml("required")))
        )) {
            val store = registeredStore()
            val fixture = FixtureMfaTransport()
            val transport = nestedChallengeTransport(fixture, listOf(
                MfaXml("tenantId", FixtureMfaTransport.TENANT),
                MfaXml("azureObjectId", FixtureMfaTransport.OBJECT),
                MfaXml("firstEntropyChallenge", FixtureMfaTransport.EXPECTED_NUMBER), policy))
            val manager = manager(store, transport)
            val challenge = manager.handlePushNotification(fixture.issue()).single()
            assertCode(MfaErrorCode.UNSUPPORTED_POLICY) {
                manager.approve(challenge, FixtureMfaTransport.EXPECTED_NUMBER)
            }
            assertFalse(fixture.operations.value.contains("PhoneAppPinValidation"))
            assertEquals(MfaDecision.DENIED, manager.deny(challenge))
        }
    }

    @Test fun encodedPushPolicyAllowsNumberMatchingApproval() = runTest {
        // Synthetic protobuf: primary and companion policies both permit number matching.
        val policy = java.util.Base64.getUrlEncoder().encodeToString(
            byteArrayOf(0x12, 0x04, 0x10, 0x02, 0x28, 0x02,
                0x22, 0x04, 0x10, 0x01, 0x28, 0x02))
        val fixture = FixtureMfaTransport()
        val manager = manager(registeredStore(), nestedChallengeTransport(fixture, listOf(
            MfaXml("tenantId", FixtureMfaTransport.TENANT),
            MfaXml("azureObjectId", FixtureMfaTransport.OBJECT),
            MfaXml("firstEntropyChallenge", FixtureMfaTransport.EXPECTED_NUMBER),
            MfaXml("userCredentialPolicy", policy))))
        val challenge = manager.handlePushNotification(fixture.issue()).single()
        assertTrue(challenge.canApprove)
        assertEquals(MfaDecision.APPROVED,
            manager.approve(challenge, FixtureMfaTransport.EXPECTED_NUMBER))
        assertCode(MfaErrorCode.INVALID_SESSION) { manager.deny(challenge) }
    }

    @Test fun encodedRequiredAppLockBlocksWithoutSubmissionAndAllowsDenial() = runTest {
        val policy = java.util.Base64.getUrlEncoder().encodeToString(
            byteArrayOf(0x12, 0x04, 0x10, 0x01, 0x30, 0x02))
        val store = registeredStore()
        val fixture = FixtureMfaTransport()
        val manager = manager(store, nestedChallengeTransport(fixture, listOf(
            MfaXml("tenantId", FixtureMfaTransport.TENANT),
            MfaXml("azureObjectId", FixtureMfaTransport.OBJECT),
            MfaXml("firstEntropyChallenge", FixtureMfaTransport.EXPECTED_NUMBER),
            MfaXml("userCredentialPolicy", policy))))
        val challenge = manager.handlePushNotification(fixture.issue()).single()
        assertFalse(challenge.canApprove)
        assertEquals(setOf(MfaApprovalBlockReason.APP_LOCK_REQUIRED), challenge.approvalBlockedReasons)
        assertCode(MfaErrorCode.UNSUPPORTED_POLICY) {
            manager.approve(challenge, FixtureMfaTransport.EXPECTED_NUMBER)
        }
        assertTrue(store.state.submitted.isEmpty())
        assertFalse(fixture.operations.value.contains("PhoneAppPinValidation"))
        assertEquals(MfaDecision.DENIED, manager.deny(challenge))
    }

    @Test fun authenticatorProfilePreservesChallengeRestrictionsAndDecisionReplayProtection() = runTest {
        val policy = java.util.Base64.getUrlEncoder().encodeToString(
            byteArrayOf(0x12, 0x04, 0x10, 0x02, 0x28, 0x02, 0x22, 0x02, 0x28, 0x02))
        for (requiresLock in listOf(false, true)) {
            val fixture = FixtureMfaTransport()
            val manager = manager(registeredStore(), nestedChallengeTransport(fixture, listOf(
                MfaXml("tenantId", FixtureMfaTransport.TENANT),
                MfaXml("azureObjectId", FixtureMfaTransport.OBJECT),
                MfaXml("firstEntropyChallenge", FixtureMfaTransport.EXPECTED_NUMBER),
                MfaXml("isAppLockRequired", if (requiresLock) "yes" else "no"),
                MfaXml("userCredentialPolicy", policy))), policyProfile = MfaPolicyProfile.AUTHENTICATOR)
            val challenge = manager.handlePushNotification(fixture.issue()).single()
            assertEquals(!requiresLock, challenge.canApprove)
            if (requiresLock) {
                assertEquals(setOf(MfaApprovalBlockReason.APP_LOCK_REQUIRED), challenge.approvalBlockedReasons)
                assertCode(MfaErrorCode.UNSUPPORTED_POLICY) {
                    manager.approve(challenge, FixtureMfaTransport.EXPECTED_NUMBER)
                }
                assertFalse(fixture.operations.value.contains("PhoneAppPinValidation"))
                assertEquals(MfaDecision.DENIED, manager.deny(challenge))
            } else {
                assertEquals(MfaDecision.APPROVED,
                    manager.approve(challenge, FixtureMfaTransport.EXPECTED_NUMBER))
                assertCode(MfaErrorCode.INVALID_SESSION) { manager.deny(challenge) }
            }
        }
    }

    @Test fun cancelledWaitResumesWithoutReactivating() = runTest {
        val store = MemoryStore()
        val fixture = FixtureMfaTransport()
        val first = manager(store, fixture)
        first.updatePushRegistration(FixtureMfaTransport.TOKEN)
        val waiting = async { first.registerAuthMethod(activation) }
        runCurrent()
        waiting.cancelAndJoin()
        val restored = manager(store, fixture)
        restored.handlePushNotification(fixture.validationPayload())
        restored.registerAuthMethod(activation)
        assertEquals(1, fixture.operations.value.count { it == "ActivateNew" })
        assertEquals(1, restored.registeredAuthMethods().size)
    }

    @Test fun unrelatedValidationReleasesSlotInEitherArrivalOrder() = runTest {
        for (early in listOf(false, true)) {
            val store = MemoryStore()
            val fixture = FixtureMfaTransport()
            val gate = CompletableDeferred<Unit>()
            if (!early) gate.complete(Unit)
            var unrelated = true
            val transport = object : MfaTransport {
                override suspend fun send(request: MfaWireRequest): MfaWireResponse {
                    if (request.headers["SOAPAction"]?.endsWith("/ActivateNew") == true) gate.await()
                    val response = fixture.send(request)
                    return if (unrelated && request.headers["x-ms-mac-action"] == "PhoneAppValidateDeviceToken") {
                        MfaWireResponse(response.statusCode, response.body.toString(Charsets.UTF_8)
                            .replace(FixtureMfaTransport.USERNAME, "unrelated@example.invalid").toByteArray())
                    } else response
                }
            }
            val manager = manager(store, transport)
            manager.updatePushRegistration(FixtureMfaTransport.TOKEN)
            val registration = async { manager.registerAuthMethod(activation) }
            runCurrent()
            if (early) {
                manager.handlePushNotification(fixture.validationPayload())
                gate.complete(Unit)
                runCurrent()
            } else {
                assertCode(MfaErrorCode.INVALID_NOTIFICATION) {
                    manager.handlePushNotification(fixture.validationPayload())
                }
            }
            assertEquals(null, store.state.pending?.validation)
            assertEquals(false, store.state.pending?.validationAttempt)
            assertFalse(registration.isCompleted)
            unrelated = false
            manager.handlePushNotification(fixture.validationPayload())
            assertEquals(FixtureMfaTransport.ACCOUNT, registration.await().registrationId)
            assertEquals(1, fixture.operations.value.count { it == "ActivateNew" })
        }
        val store = MemoryStore()
        val fixture = FixtureMfaTransport()
        val manager = manager(store, fixture)
        manager.updatePushRegistration(FixtureMfaTransport.TOKEN)
        val registration = async { manager.registerAuthMethod(activation) }
        runCurrent()
        fixture.scenario = MfaPocScenario.TRANSPORT_FAILURE
        assertCode(MfaErrorCode.OUTCOME_UNKNOWN) {
            manager.handlePushNotification(fixture.validationPayload())
        }
        fixture.scenario = MfaPocScenario.HAPPY_PATH
        val calls = fixture.operations.value.size
        assertCode(MfaErrorCode.OUTCOME_UNKNOWN) {
            manager.handlePushNotification(fixture.validationPayload())
        }
        assertEquals(calls, fixture.operations.value.size)
        registration.cancelAndJoin()
    }

    @Test fun deviceWidePullUsesUniquelyMatchedOwnedAccount() = runTest {
        val store = MemoryStore()
        val matched = MfaStoredState.Account(FixtureMfaTransport.ACCOUNT, "Matched account",
            FixtureMfaTransport.USERNAME, FixtureMfaTransport.SECRET, FixtureMfaTransport.TENANT,
            FixtureMfaTransport.OBJECT, "https://${FixtureMfaTransport.HOST}/pad",
            FixtureMfaTransport.TOKEN, "synthetic-dos")
        val other = matched.copy(id = "44444444-4444-4444-8444-444444444444",
            username = "other@example.invalid", secret = "MFRGGZDFMZTWQ2LK",
            tenantId = "55555555-5555-4555-8555-555555555555")
        store.state = MfaStoredState(token = FixtureMfaTransport.TOKEN, accounts = listOf(other, matched))
        val fixture = FixtureMfaTransport()
        var expectedProof = ""
        var fetches = 0
        val transport = object : MfaTransport {
            override suspend fun send(request: MfaWireRequest): MfaWireResponse {
                val operation = request.headers["x-ms-mac-action"]
                if (operation == "PhoneAppAuthentication") {
                    val context = MfaXml.parse(request.body).child("request")
                        .child("phoneAppAuthenticationRequest").child("phoneAppContext")
                    assertEquals(expectedProof, context.value("oathCode"))
                    assertEquals(matched.tenantId, request.headers["x-ms-client-tenant-id"])
                    fetches++
                }
                val response = fixture.send(request)
                if (operation == "PhoneAppCheckForAuthentication") {
                    val counter = MfaXml.parse(response.body).child("response")
                        .child("phoneAppCheckForAuthenticationResponse").value("oathCounter").toLong()
                    expectedProof = MfaProof.validation(matched.secret, counter)
                }
                return response
            }
        }
        val results = manager(store, transport).pullPendingNotifications()
        assertEquals(2, fetches)
        assertTrue(results.all { it.username == matched.username })
        store.state = store.state.copy(accounts = listOf(other.copy(username = matched.username), matched))
        assertCode(MfaErrorCode.UNKNOWN_ACCOUNT) {
            manager(store, transport).pullPendingNotifications()
        }
        assertEquals(2, fetches)
    }

    @Test fun unknownActivationAndRejectedActivationAreNotSuccess() = runTest {
        val store = MemoryStore()
        val fixture = FixtureMfaTransport()
        val manager = manager(store, fixture)
        manager.updatePushRegistration(FixtureMfaTransport.TOKEN)
        fixture.scenario = MfaPocScenario.REJECTED_ACTIVATION
        assertCode(MfaErrorCode.SERVICE_REJECTED) { manager.registerAuthMethod(activation) }
        assertEquals(null, store.state.pending)
        fixture.scenario = MfaPocScenario.TRANSPORT_FAILURE
        assertCode(MfaErrorCode.OUTCOME_UNKNOWN) { manager.registerAuthMethod(activation) }
        val calls = fixture.operations.value.size
        fixture.scenario = MfaPocScenario.HAPPY_PATH
        assertCode(MfaErrorCode.OUTCOME_UNKNOWN) { manager.registerAuthMethod(activation) }
        assertEquals(calls, fixture.operations.value.size)
        assertTrue(manager.registeredAuthMethods().isEmpty())
    }

    @Test fun invalidWrongExpiredForeignAndUncertainDecisionsStaySafe() = runTest {
        val store = MemoryStore()
        val fixture = FixtureMfaTransport()
        val manager = manager(store, fixture)
        manager.updatePushRegistration(FixtureMfaTransport.TOKEN)
        val registration = async { manager.registerAuthMethod(activation) }
        runCurrent()
        manager.handlePushNotification(fixture.validationPayload())
        registration.await()
        val challenge = manager.handlePushNotification(fixture.issue()).single()
        val foreign = MfaAuthSession(challenge.guid, challenge.accountName, challenge.username, true)
        assertCode(MfaErrorCode.INVALID_SESSION) { manager.approve(foreign, "42") }
        assertCode(MfaErrorCode.INVALID_NUMBER) { manager.approve(challenge, "-1") }
        store.failWrites = true
        assertCode(MfaErrorCode.STORAGE_FAILURE) { manager.approve(challenge, "42") }
        store.failWrites = false
        assertCode(MfaErrorCode.SERVICE_REJECTED) { manager.approve(challenge, "41") }
        assertCode(MfaErrorCode.INVALID_SESSION) { manager.approve(challenge, "42") }
        val expired = manager.handlePushNotification(fixture.issue()).single()
        fixture.scenario = MfaPocScenario.EXPIRED_REQUEST
        assertCode(MfaErrorCode.SERVICE_REJECTED) { manager.deny(expired) }
        fixture.scenario = MfaPocScenario.HAPPY_PATH
        val unknown = manager.handlePushNotification(fixture.issue()).single()
        fixture.scenario = MfaPocScenario.TRANSPORT_FAILURE
        assertCode(MfaErrorCode.OUTCOME_UNKNOWN) { manager.approve(unknown, "42") }
        assertCode(MfaErrorCode.INVALID_SESSION) { manager.deny(unknown) }
        fixture.scenario = MfaPocScenario.HAPPY_PATH
        assertCode(MfaErrorCode.INVALID_SESSION) {
            manager(store, fixture).handlePushNotification(
                mapOf("type" to "auth", "guid" to unknown.guid, "url" to FixtureMfaTransport.HOST))
        }
    }

    @Test fun parsingProofAndFeatureGateAreBounded() = runTest {
        val expectedEndpoint = "https://${FixtureMfaTransport.HOST}/PfPaWs.asmx"
        for (scheme in listOf("phonefactor", "msauth")) {
            val parsed = MfaActivationParser.parse(MfaRegistrationParameters.QrCode(
                "$scheme://activate_account?code=000000000&url=" +
                    "https%3A%2F%2Fpoc.mobileappcommunicator.auth.microsoft.com"))
            assertEquals(FixtureMfaTransport.CODE, parsed.code)
            assertEquals(expectedEndpoint, parsed.endpoint.toString())
        }
        assertEquals(expectedEndpoint, MfaActivationParser.parse(activation).endpoint.toString())
        assertEquals(expectedEndpoint, MfaActivationParser.parse(MfaRegistrationParameters.Activation(
            FixtureMfaTransport.CODE, "https://${FixtureMfaTransport.HOST}/")).endpoint.toString())
        assertEquals("75A48A19D4CBE100644E8AC1397EEA747A2D33AB",
            MfaProof.validation(FixtureMfaTransport.SECRET, 1))
        assertEquals("a<&", MfaXml.parse(MfaXml("root", children = listOf(
            MfaXml("value", "a<&"))).encoded().toByteArray()).value("value"))
        assertCode(MfaErrorCode.INVALID_RESPONSE) {
            MfaXml.parse("<!DOCTYPE a [<!ENTITY x SYSTEM 'file:///not-read'>]><a>&x;</a>".toByteArray())
        }
        assertCode(MfaErrorCode.INVALID_RESPONSE) {
            MfaXml.parse("<a><b>1</b><b>2</b></a>".toByteArray()).value("b")
        }
        assertCode(MfaErrorCode.UNTRUSTED_ENDPOINT) { MfaEndpointPolicy.validate("https://www.microsoft.com/pad") }
        assertCode(MfaErrorCode.UNTRUSTED_ENDPOINT) {
            MfaEndpointPolicy.validate("https://${FixtureMfaTransport.HOST}.example.invalid/pad")
        }
        assertCode(MfaErrorCode.UNTRUSTED_ENDPOINT) {
            MfaEndpointPolicy.validate("https://${FixtureMfaTransport.HOST}/a/../pad")
        }
        assertCode(MfaErrorCode.UNTRUSTED_ENDPOINT) {
            MfaEndpointPolicy.validate("https://${FixtureMfaTransport.HOST}/%70ad")
        }
        assertCode(MfaErrorCode.INVALID_ACTIVATION) {
            MfaActivationParser.parse(MfaRegistrationParameters.QrCode(
                "phonefactor://activate_account?code=a&code=b&url=x"))
        }
        for (authority in listOf("user@activate_account", "activate_account:443", "activate_account.example")) {
            assertCode(MfaErrorCode.INVALID_ACTIVATION) {
                MfaActivationParser.parse(MfaRegistrationParameters.QrCode(
                    "phonefactor://$authority?code=000000000&url=" +
                        "https%3A%2F%2Fpoc.mobileappcommunicator.auth.microsoft.com"))
            }
        }
        assertCode(MfaErrorCode.FEATURE_DISABLED) {
            manager(MemoryStore(), FixtureMfaTransport(), false).registeredAuthMethods()
        }
    }
}
