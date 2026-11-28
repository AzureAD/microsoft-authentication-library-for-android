// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.nativeauth.mfa

import com.microsoft.identity.nativeauth.mfa.MfaTestFixture.Companion.HOST
import com.microsoft.identity.nativeauth.mfa.MfaTestFixture.Companion.PACKAGE
import com.microsoft.identity.nativeauth.mfa.MfaTestFixture.Companion.TOKEN
import com.microsoft.identity.nativeauth.mfa.MfaTestFixture.Companion.NEW_TOKEN
import com.microsoft.identity.nativeauth.mfa.MfaTestFixture.Companion.GUID
import com.microsoft.identity.nativeauth.mfa.MfaTestFixture.Companion.account
import com.microsoft.identity.nativeauth.mfa.MfaTestFixture.Companion.registration
import com.microsoft.identity.nativeauth.mfa.MfaTestFixture.Companion.validation
import com.microsoft.identity.nativeauth.mfa.MfaTestFixture.Companion.auth
import com.microsoft.identity.nativeauth.mfa.internal.*
import java.net.URI
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class MfaSasIntegrationTest {
    @Test
    fun `push lookup survives an unrelated failed account check but pull still fails`() = runBlocking {
        val fixture = MfaTestFixture()
        val store = registeredStore()
        store.state = store.state.copy(accounts = listOf(account("unavailable").copy(dosPreventer = "other"), account()))
        fixture.intercept = { op, fields ->
            if (op == "PhoneAppCheckForAuthentication" && fields.value("dosPreventer") == "other")
                MfaWireResponse(503, byteArrayOf()) else null
        }
        val manager = fixture.manager(store)
        assertEquals(GUID, manager.handlePushNotification(auth()).single().guid)
        assertTrue(error { manager.pullPendingNotifications() }.isGeneralError())
    }

    @Test
    fun `late session fetch does not restore stale missing routing metadata`() = runBlocking {
        val fixture = MfaTestFixture()
        val store = registeredStore()
        val manager = fixture.manager(store)
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val otherGuid = "11111111-1111-4111-8111-111111111112"
        fixture.intercept = { op, fields ->
            if (op == "PhoneAppAuthentication" && fields.child("phoneAppContext").value("guid") == GUID) {
                entered.complete(Unit)
                release.await()
            }
            null
        }
        val delayed = async { manager.handlePushNotification(auth()) }
        withTimeout(5_000) { entered.await() }
        try {
            fixture.responseFields = { _, fields -> fields.filterNot { it.name == "guid" } +
                listOf(MfaXml("guid", otherGuid), MfaXml("routingHint", "updated")) }
            manager.handlePushNotification(auth() + ("guid" to otherGuid))
        } finally {
            fixture.responseFields = { _, fields -> fields }
            release.complete(Unit)
        }
        delayed.await()
        assertEquals("updated", store.state.accounts.single().routingHint)
    }

    @Test
    fun `unknown rotation is not success even for the old token`() = runBlocking {
        val store = registeredStore()
        store.state = store.state.copy(blockedTokenUpdates = setOf(account().id))
        val fixture = MfaTestFixture()
        assertTrue(error { fixture.manager(store).updatePushRegistration(TOKEN) }.isGeneralError())
        assertTrue(fixture.requests.isEmpty())
    }

    @Test
    fun `enrollment at capacity does not start activation or consume a guard`() = runBlocking {
        val store = registeredStore()
        store.state = store.state.copy(accounts = (1..100).map { account("method-$it") })
        val fixture = MfaTestFixture()
        assertTrue(error { fixture.manager(store, timeout = 20).registerAuthMethod(registration()) }.isGeneralError())
        assertTrue(fixture.requests.isEmpty())
        assertTrue(store.state.enrollments.isEmpty())
    }

    @Test
    fun `authority aliases give the same enrollment guard`() {
        fun digest(url: String) = MfaActivationParser.parse(MfaRegistrationParameters.Activation("code", url)).digest
        assertEquals(digest("https://$HOST/path"), digest("https://${HOST.uppercase()}:443/path"))
    }

    @Test
    fun `absolute polling URL retains its path and query`() {
        val url = "https://$HOST/custom?route=1"
        assertEquals(url, MfaEndpointPolicy.notification(url).toString())
        assertTrue(runCatching { MfaEndpointPolicy.notification("http://$HOST/custom") }.isFailure)
    }

    @Test
    fun `confirmed completion survives cancellation of rotation waiter`() = runBlocking {
        val fixture = MfaTestFixture()
        val store = registeredStore()
        val manager = fixture.manager(store)
        val release = CompletableDeferred<Unit>()
        fixture.intercept = { operation, _ ->
            if (operation == "phoneAppCompleteDeviceTokenChangeV2Request") release.await()
            null
        }
        val waiter = async { manager.updatePushRegistration(NEW_TOKEN) }
        fixture.await("phoneAppStartDeviceTokenChangeV2Request")
        val push = async { runCatching { manager.handlePushNotification(validation(true)) } }
        fixture.await("phoneAppCompleteDeviceTokenChangeV2Request")
        waiter.cancelAndJoin()
        release.complete(Unit)
        push.await().getOrThrow()
        assertEquals(NEW_TOKEN, store.state.accounts.single().token)
        assertTrue(store.state.blockedTokenUpdates.isEmpty())
    }

    @Test
    fun `cancelling validation handler does not cancel its independent waiter`() = runBlocking {
        val fixture = MfaTestFixture()
        val manager = fixture.manager(registeredStore())
        fixture.intercept = { operation, _ ->
            if (operation == "phoneAppCompleteDeviceTokenChangeV2Request") CompletableDeferred<Unit>().await()
            null
        }
        val waiter = async { runCatching { manager.updatePushRegistration(NEW_TOKEN) } }
        fixture.await("phoneAppStartDeviceTokenChangeV2Request")
        val push = async { manager.handlePushNotification(validation(true)) }
        fixture.await("phoneAppCompleteDeviceTokenChangeV2Request")
        push.cancelAndJoin()
        assertTrue(waiter.await().exceptionOrNull() is MfaSdkException)
    }

    @Test
    fun `empty store and unrelated push make no service calls`() = runBlocking {
        val fixture = MfaTestFixture()
        val store = MemoryMfaStore()
        val manager = fixture.manager(store)
        assertTrue(manager.registeredAuthMethods().isEmpty())
        assertTrue(manager.pullPendingNotifications().isEmpty())
        assertTrue(manager.handlePushNotification(mapOf("unrelated" to "data")).isEmpty())
        manager.updatePushRegistration(TOKEN)
        manager.updatePushRegistration(TOKEN)
        assertEquals(TOKEN, store.state.token)
        assertTrue(fixture.requests.isEmpty())
        assertTrue(error { manager.updatePushRegistration("invalid token") }.isGeneralError())
    }

    @Test
    fun `enrollment needs a token and completes only after confirmation and save`() = runBlocking {
        val fixture = MfaTestFixture()
        val store = MemoryMfaStore()
        val manager = fixture.manager(store)
        assertTrue(error { manager.registerAuthMethod(registration()) }.isPushTokenMissing())
        manager.updatePushRegistration(TOKEN)
        val enrolled = async { manager.registerAuthMethod(registration()) }
        fixture.await("ActivateNew")
        assertTrue(manager.registeredAuthMethods().isEmpty())
        assertTrue(manager.handlePushNotification(validation()).isEmpty())
        val result = withTimeout(5_000) { enrolled.await() }
        assertEquals(account().display(), result)
        assertEquals(listOf(result), fixture.manager(store).registeredAuthMethods())
        assertEquals(listOf("ActivateNew", "PhoneAppValidateDeviceToken", "ConfirmActivation"), operations(fixture))
        assertTrue(store.state.enrollments.isNotEmpty())
    }

    @Test
    fun `early validation executes while activation response is outstanding`() = runBlocking {
        val fixture = MfaTestFixture()
        val release = CompletableDeferred<Unit>()
        fixture.intercept = { operation, _ ->
            if (operation == "ActivateNew") release.await()
            null
        }
        val manager = fixture.manager()
        manager.updatePushRegistration(TOKEN)
        val enrollment = async { manager.registerAuthMethod(registration()) }
        fixture.await("ActivateNew")
        manager.handlePushNotification(validation())
        assertFalse(enrollment.isCompleted)
        assertTrue(manager.registeredAuthMethods().isEmpty())
        release.complete(Unit)
        assertEquals(account().display(), withTimeout(5_000) { enrollment.await() })
    }

    @Test
    fun `wrong validation identity cannot confirm registration`() = runBlocking {
        val fixture = MfaTestFixture()
        fixture.intercept = { operation, _ ->
            if (operation == "PhoneAppValidateDeviceToken") fixture.phone(operation, listOf(
                MfaXml("username", "other@example.invalid"), MfaXml("accountName", "Other"),
                MfaXml("dosPreventer", "synthetic-dos"))) else null
        }
        val manager = fixture.manager()
        manager.updatePushRegistration(TOKEN)
        val enrollment = async { runCatching { manager.registerAuthMethod(registration()) } }
        fixture.await("ActivateNew")
        manager.handlePushNotification(validation())
        assertTrue(withTimeout(5_000) { enrollment.await() }.exceptionOrNull() is MfaSdkException)
        assertFalse(operations(fixture).contains("ConfirmActivation"))
        assertTrue(manager.registeredAuthMethods().isEmpty())
    }

    @Test
    fun `empty validation username with unrelated group cannot confirm registration`() = runBlocking {
        val fixture = MfaTestFixture()
        fixture.intercept = { operation, _ ->
            if (operation == "PhoneAppValidateDeviceToken") fixture.phone(operation, listOf(
                MfaXml("username"), MfaXml("groupKey", "unrelated-group"),
                MfaXml("accountName", "Other"), MfaXml("dosPreventer", "synthetic-dos"))) else null
        }
        val manager = fixture.manager()
        manager.updatePushRegistration(TOKEN)
        val enrollment = async { runCatching { manager.registerAuthMethod(registration()) } }
        fixture.await("ActivateNew")
        manager.handlePushNotification(validation())
        assertTrue(withTimeout(5_000) { enrollment.await() }.exceptionOrNull() is MfaSdkException)
        assertFalse(operations(fixture).contains("ConfirmActivation"))
        assertTrue(manager.registeredAuthMethods().isEmpty())
    }

    @Test
    fun `cancelled activation cannot be replayed and pending enrollment is not restored`() = runBlocking {
        val fixture = MfaTestFixture()
        fixture.intercept = { operation, _ ->
            if (operation == "ActivateNew") CompletableDeferred<Unit>().await()
            null
        }
        val store = MemoryMfaStore()
        val manager = fixture.manager(store)
        manager.updatePushRegistration(TOKEN)
        val enrollment = async { manager.registerAuthMethod(registration()) }
        fixture.await("ActivateNew")
        enrollment.cancelAndJoin()
        val restored = fixture.manager(store)
        assertTrue(restored.registeredAuthMethods().isEmpty())
        assertTrue(error { restored.registerAuthMethod(registration()) }.isGeneralError())
        assertTrue(error { restored.handlePushNotification(validation()) }.isGeneralError())
        assertEquals(1, fixture.requests.size)
    }

    @Test
    fun `failed save after confirmation never reports registration`() = runBlocking {
        val fixture = MfaTestFixture()
        val store = MemoryMfaStore()
        store.beforeSave = { if (it.accounts.isNotEmpty()) throw IllegalStateException("sensitive storage detail") }
        val manager = fixture.manager(store)
        manager.updatePushRegistration(TOKEN)
        val enrollment = async { runCatching { manager.registerAuthMethod(registration()) } }
        fixture.await("ActivateNew")
        manager.handlePushNotification(validation())
        val failure = withTimeout(5_000) { enrollment.await() }.exceptionOrNull() as MfaSdkException
        assertTrue(failure.isGeneralError())
        assertFalse(failure.toString().contains("sensitive"))
        assertTrue(store.state.accounts.isEmpty())
        assertTrue(error { manager.registerAuthMethod(registration()) }.isGeneralError())
    }

    @Test
    fun `push retrieves bound session and submits number matching once`() = runBlocking {
        val fixture = MfaTestFixture()
        val store = registeredStore()
        val manager = fixture.manager(store)
        val session = manager.handlePushNotification(auth()).single()
        assertEquals(account().id, session.registrationId)
        assertEquals(account().username, session.username)
        assertTrue(session.requiresNumberMatching)
        assertSame(session, manager.handlePushNotification(auth()).single())
        assertTrue(error { manager.respond(session, MfaAction.Approve()) }.isInvalidCode())
        assertTrue(error { manager.respond(session, MfaAction.Approve("not-a-number")) }.isInvalidCode())
        assertEquals(MfaDecision.APPROVED, manager.respond(session, MfaAction.Approve("42")))
        assertTrue(error { manager.respond(session, MfaAction.Deny) }.isInvalidChallenge())
        assertTrue(error { fixture.manager(store).handlePushNotification(auth()) }.isInvalidChallenge())
        assertEquals(1, operations(fixture).count { it == "PhoneAppPinValidation" })
    }

    @Test
    fun `pull uses registration proof and supplied routing without endpoint discovery`() = runBlocking {
        val fixture = MfaTestFixture()
        val account = account().copy(replicationScope = "synthetic-scope",
            endpoint = "https://$HOST/service?route=synthetic")
        val manager = fixture.manager(MemoryMfaStore(MfaStoredState(token = TOKEN, accounts = listOf(account))))
        val session = manager.pullPendingNotifications().single()
        assertEquals(account.id, session.registrationId)
        assertEquals(URI(account.endpoint), fixture.requests.first().endpoint)
        assertEquals(listOf("PhoneAppCheckForAuthentication", "PhoneAppAuthentication"), operations(fixture))
        assertEquals(MfaDecision.DENIED, manager.respond(session, MfaAction.Deny))
        assertEquals("PhoneAppAuthenticationResult", operations(fixture).last())
    }

    @Test
    fun `mismatched account unsupported and ordinary policies fail retrieval`() = runBlocking {
        val fixture = MfaTestFixture()
        val manager = fixture.manager(registeredStore())
        fixture.fetchedAccount = account().copy(objectId = "other-object")
        assertTrue(error { manager.handlePushNotification(auth()) }.isGeneralError())
        fixture.fetchedAccount = account()
        fixture.numberMatching = false
        assertTrue(error { manager.handlePushNotification(auth()) }.isGeneralError())
        fixture.numberMatching = true
        fixture.credentialPolicy = "malformed"
        assertTrue(error { manager.handlePushNotification(auth()) }.isGeneralError())
        assertFalse(operations(fixture).any { it in setOf("PhoneAppPinValidation", "PhoneAppAuthenticationResult") })
    }

    @Test
    fun `authoritative policy changes block decision before submission`() = runBlocking {
        val fixture = MfaTestFixture()
        val store = registeredStore()
        val manager = fixture.manager(store)
        val session = manager.handlePushNotification(auth()).single()
        fixture.numberMatching = false
        assertTrue(error { manager.respond(session, MfaAction.Approve("42")) }.isGeneralError())
        assertTrue(store.state.submitted.isEmpty())
        assertFalse(operations(fixture).contains("PhoneAppPinValidation"))
    }

    @Test
    fun `session retrieval requires matching method identity even for the same user`() = runBlocking {
        for (methodId in listOf(null, "", "another-method")) {
            val fixture = MfaTestFixture()
            fixture.responseFields = { operation, fields ->
                if (operation == "PhoneAppAuthentication") fields.filterNot { it.name == "phoneAppDetailId" } +
                    (methodId?.let { listOf(MfaXml("phoneAppDetailId", it)) } ?: emptyList()) else fields
            }
            val manager = fixture.manager(registeredStore())
            assertTrue(error { manager.handlePushNotification(auth()) }.isGeneralError())
            assertFalse(operations(fixture).any {
                it in setOf("PhoneAppPinValidation", "PhoneAppAuthenticationResult")
            })
        }
    }

    @Test
    fun `pre-decision refresh requires the original method identity`() = runBlocking {
        for (methodId in listOf(null, "", "another-method")) {
            val fixture = MfaTestFixture()
            val store = registeredStore()
            val manager = fixture.manager(store)
            val session = manager.handlePushNotification(auth()).single()
            fixture.responseFields = { operation, fields ->
                if (operation == "PhoneAppAuthentication") fields.filterNot { it.name == "phoneAppDetailId" } +
                    (methodId?.let { listOf(MfaXml("phoneAppDetailId", it)) } ?: emptyList()) else fields
            }
            assertTrue(error { manager.respond(session, MfaAction.Approve("42")) }.isGeneralError())
            assertTrue(store.state.submitted.isEmpty())
            assertFalse(operations(fixture).contains("PhoneAppPinValidation"))
        }
    }

    @Test
    fun `missing empty or affirmative policy flags never authorize sessions or decisions`() = runBlocking {
        for (flag in listOf("returnLocationData", "isSilent", "isAppLockRequired", "pinChangeRequired")) {
            for (value in listOf(null, "", "yes")) {
                val fixture = MfaTestFixture()
                val store = registeredStore()
                val manager = fixture.manager(store)
                val session = manager.handlePushNotification(auth()).single()
                fixture.responseFields = { operation, fields ->
                    if (operation == "PhoneAppAuthentication") fields.filterNot { it.name == flag } +
                        (value?.let { listOf(MfaXml(flag, it)) } ?: emptyList()) else fields
                }
                assertTrue(error { fixture.manager(store).handlePushNotification(auth()) }.isGeneralError())
                assertTrue(error { manager.respond(session, MfaAction.Deny) }.isGeneralError())
                assertFalse(operations(fixture).any {
                    it in setOf("PhoneAppPinValidation", "PhoneAppAuthenticationResult")
                })
            }
        }
    }

    @Test
    fun `wrong number service rejection never returns denied or authorizes retry`() = runBlocking {
        val fixture = MfaTestFixture()
        fixture.decisionCode = "7"
        val manager = fixture.manager(registeredStore())
        val session = manager.handlePushNotification(auth()).single()
        assertTrue(error { manager.respond(session, MfaAction.Approve("42")) }.isGeneralError())
        assertTrue(error { manager.respond(session, MfaAction.Approve("42")) }.isInvalidChallenge())
    }

    @Test
    fun `cancellation after decision submission preserves durable replay guard`() = runBlocking {
        val fixture = MfaTestFixture()
        fixture.intercept = { operation, _ ->
            if (operation == "PhoneAppPinValidation") CompletableDeferred<Unit>().await()
            null
        }
        val store = registeredStore()
        val manager = fixture.manager(store)
        val session = manager.handlePushNotification(auth()).single()
        val response = async { manager.respond(session, MfaAction.Approve("42")) }
        fixture.await("PhoneAppPinValidation")
        response.cancelAndJoin()
        assertTrue(store.state.submitted.contains(GUID))
        assertTrue(error { manager.respond(session, MfaAction.Deny) }.isInvalidChallenge())
    }

    @Test
    fun `V2 token update validates HMAC and persists previous token before success`() = runBlocking {
        val fixture = MfaTestFixture()
        val store = registeredStore()
        val manager = fixture.manager(store)
        val update = async { manager.updatePushRegistration(NEW_TOKEN) }
        fixture.await("phoneAppStartDeviceTokenChangeV2Request")
        assertEquals(NEW_TOKEN, store.state.token)
        assertEquals(TOKEN, store.state.accounts.single().token)
        manager.handlePushNotification(validation(rotation = true))
        withTimeout(5_000) { update.await() }
        assertEquals(NEW_TOKEN, store.state.accounts.single().token)
        assertEquals(TOKEN, store.state.accounts.single().previousToken)
        assertTrue(store.state.blockedTokenUpdates.isEmpty())
        val count = fixture.requests.size
        manager.updatePushRegistration(NEW_TOKEN)
        assertEquals(count, fixture.requests.size)
        assertEquals(listOf("phoneAppStartDeviceTokenChangeV2Request", "phoneAppCompleteDeviceTokenChangeV2Request"),
            operations(fixture))
    }

    @Test
    fun `early V2 validation does not commit before start acknowledgment`() = runBlocking {
        val fixture = MfaTestFixture()
        val release = CompletableDeferred<Unit>()
        fixture.intercept = { operation, _ ->
            if (operation == "phoneAppStartDeviceTokenChangeV2Request") release.await()
            null
        }
        val store = registeredStore()
        val manager = fixture.manager(store)
        val update = async { manager.updatePushRegistration(NEW_TOKEN) }
        fixture.await("phoneAppStartDeviceTokenChangeV2Request")
        manager.handlePushNotification(validation(true))
        assertFalse(update.isCompleted)
        assertEquals(TOKEN, store.state.accounts.single().token)
        release.complete(Unit)
        withTimeout(5_000) { update.await() }
        assertEquals(NEW_TOKEN, store.state.accounts.single().token)
    }

    @Test
    fun `rotation skips unchanged accounts and preserves partial per-account progress`() = runBlocking {
        val fixture = MfaTestFixture()
        val first = account()
        val second = account("method-2").copy(objectId = "different-object")
        val unchanged = account("method-3").copy(token = NEW_TOKEN)
        val store = MemoryMfaStore(MfaStoredState(token = TOKEN, accounts = listOf(first, second, unchanged)))
        val manager = fixture.manager(store)
        val update = async { runCatching { manager.updatePushRegistration(NEW_TOKEN) } }
        fixture.await("phoneAppStartDeviceTokenChangeV2Request")
        manager.handlePushNotification(validation(true))
        fixture.await("phoneAppStartDeviceTokenChangeV2Request")
        fixture.fetchedAccount = second
        assertTrue(error { manager.handlePushNotification(validation(true)) }.isGeneralError())
        fixture.completionAccountResult = "Rejected"
        assertTrue(error { manager.handlePushNotification(validation(true) + ("guid" to
            "11111111-1111-4111-8111-111111111112")) }.isGeneralError())
        assertTrue(withTimeout(5_000) { update.await() }.exceptionOrNull() is MfaSdkException)
        assertEquals(listOf(NEW_TOKEN, TOKEN, NEW_TOKEN), store.state.accounts.map { it.token })
        assertEquals(setOf(second.id), store.state.blockedTokenUpdates)
        store.state.validate()
        val count = fixture.requests.size
        assertTrue(error { fixture.manager(store).updatePushRegistration(NEW_TOKEN) }.isGeneralError())
        assertEquals(count, fixture.requests.size)
    }

    @Test
    fun `rotation cancellation and malformed validation never update token or replay`() = runBlocking {
        val fixture = MfaTestFixture()
        val store = registeredStore()
        val manager = fixture.manager(store)
        val update = async { manager.updatePushRegistration(NEW_TOKEN) }
        fixture.await("phoneAppStartDeviceTokenChangeV2Request")
        assertTrue(error { manager.handlePushNotification(validation(true) + ("tenantId" to "wrong-tenant")) }.isGeneralError())
        assertTrue(error { manager.handlePushNotification(validation(true) + ("oathCounter" to "-1")) }.isGeneralError())
        update.cancelAndJoin()
        assertEquals(TOKEN, store.state.accounts.single().token)
        assertEquals(setOf(account().id), store.state.blockedTokenUpdates)
        assertTrue(error { fixture.manager(store).updatePushRegistration(NEW_TOKEN) }.isGeneralError())
        assertEquals(1, fixture.requests.size)
    }

    @Test
    fun `validation-only HTTP 200 never creates a registration decision or token update`() = runBlocking {
        val fixture = MfaTestFixture()
        val validationOnly = fixture.wire(MfaXml("eeidContractValidation", attributes = mapOf(
            "mode" to "validation-only", "contractValid" to "true", "workflowExecuted" to "false")))
        fixture.intercept = { _, _ -> validationOnly }
        val empty = fixture.manager()
        empty.updatePushRegistration(TOKEN)
        assertTrue(error { empty.registerAuthMethod(registration()) }.isGeneralError())
        assertTrue(empty.registeredAuthMethods().isEmpty())
        val store = registeredStore()
        val manager = fixture.manager(store)
        assertTrue(error { manager.updatePushRegistration(NEW_TOKEN) }.isGeneralError())
        assertEquals(TOKEN, store.state.accounts.single().token)
        fixture.intercept = { _, _ -> null }
        val session = manager.handlePushNotification(auth()).single()
        fixture.intercept = { operation, _ -> if (operation == "PhoneAppPinValidation") validationOnly else null }
        assertTrue(error { manager.respond(session, MfaAction.Approve("42")) }.isGeneralError())
        assertTrue(error { manager.respond(session, MfaAction.Deny) }.isInvalidChallenge())
    }

    @Test
    fun `wire parsing rejects transport-only success malformed duplicate and hostile XML`() = runBlocking {
        val fixture = MfaTestFixture()
        val client = MfaServiceClient(fixture, PACKAGE, "1", "16", "Synthetic")
        val invalid = listOf(
            "<pfpMessage><response><status disposition=\"success\"/></response></pfpMessage>",
            "<pfpMessage><response><status disposition=\"success\"/><status disposition=\"failure\"/></response></pfpMessage>",
            "<pfpMessage><response><status xmlns=\"urn:unexpected\" disposition=\"success\"/>" +
                "<phoneAppCheckForAuthenticationResponse/></response></pfpMessage>",
            "<!DOCTYPE a [<!ENTITY x SYSTEM 'file:///sensitive'>]><a>&x;</a>",
            "<pfpMessage><response><status disposition=\"success\"/><phoneAppCheckForAuthenticationResponse>" +
                "<checkForAuthenticationResult>1</checkForAuthenticationResult>" +
                "<eeidContractValidation workflowExecuted=\"false\"/>" +
                "</phoneAppCheckForAuthenticationResponse></response></pfpMessage>"
        )
        for (body in invalid) {
            fixture.intercept = { _, _ -> MfaWireResponse(200, body.toByteArray()) }
            assertTrue(runCatching { client.phoneApp("PhoneAppCheckForAuthentication",
                URI("https://$HOST/pad"), emptyList()) }.exceptionOrNull() is MfaProtocolException)
        }
        for (status in listOf(204, 301, 400, 401, 500)) {
            fixture.intercept = { _, _ -> MfaWireResponse(status, ByteArray(0)) }
            assertTrue(runCatching { client.phoneApp("PhoneAppCheckForAuthentication",
                URI("https://$HOST/pad"), emptyList()) }.exceptionOrNull() is MfaProtocolException)
        }
        assertTrue(runCatching { MfaXml.parse(ByteArray(MfaXml.LIMIT + 1)) }.isFailure)
    }

    @Test
    fun `routing preserves query and rejects non-allowlisted or ambiguous URLs`() {
        val url = "https://$HOST/service/PfPaWs.asmx?route=synthetic&test=false"
        assertEquals(url, MfaActivationParser.parse(MfaRegistrationParameters.Activation("code", url)).endpoint.toString())
        assertEquals("https://$HOST/service?route=synthetic",
            MfaEndpointPolicy.notification("$HOST/service?route=synthetic").toString())
        assertEquals("https://$HOST/pad?route=synthetic",
            MfaEndpointPolicy.notification("$HOST?route=synthetic").toString())
        for (endpoint in listOf("http://$HOST/pad", "https://evil.invalid/pad",
            "https://$HOST.evil.invalid/pad", "https://user@$HOST/pad", "https://$HOST/a/../pad",
            "https://$HOST/%2e%2e/pad", "https://$HOST:444/pad", "https://$HOST/pad#fragment")) {
            assertTrue(endpoint, runCatching { MfaEndpointPolicy.service(endpoint) }.isFailure)
        }
    }

    @Test
    fun `callback preserves committed registration when cancellation occurs during save`() = runBlocking {
        val fixture = MfaTestFixture()
        val store = MemoryMfaStore()
        val manager = fixture.manager(store)
        manager.updatePushRegistration(TOKEN)
        val completed = CompletableDeferred<Result<MfaRegistrationResult>>()
        val operation = manager.registerAuthMethod(registration(), object : MfaCallback<MfaRegistrationResult> {
            override fun onSuccess(result: MfaRegistrationResult) { completed.complete(Result.success(result)) }
            override fun onError(exception: Exception) { completed.complete(Result.failure(exception)) }
        })
        fixture.await("ActivateNew")
        store.beforeSave = { if (it.accounts.isNotEmpty()) operation.cancel() }
        manager.handlePushNotification(validation())
        assertEquals(account().display(), withTimeout(5_000) { completed.await() }.getOrThrow())
        assertEquals(account().display(), store.state.accounts.single().display())
    }

    @Test
    fun `validation timeout and service rejection retain conservative update guard`() = runBlocking {
        val fixture = MfaTestFixture()
        val store = registeredStore()
        val manager = fixture.manager(store, timeout = 25)
        assertTrue(error { manager.updatePushRegistration(NEW_TOKEN) }.isGeneralError())
        assertEquals(TOKEN, store.state.accounts.single().token)
        assertTrue(store.state.blockedTokenUpdates.contains(account().id))
        val failure = error { manager.updatePushRegistration(NEW_TOKEN) }
        assertTrue(failure.isGeneralError())
        assertEquals(1, fixture.requests.size)
    }

    @Test
    fun `duplicate concurrent decisions submit only once`() = runBlocking {
        val fixture = MfaTestFixture()
        val manager = fixture.manager(registeredStore())
        val session = manager.handlePushNotification(auth()).single()
        val first = async { runCatching { manager.respond(session, MfaAction.Approve("42")) } }
        val second = async { runCatching { manager.respond(session, MfaAction.Deny) } }
        val results = listOf(first.await(), second.await())
        assertEquals(1, results.count { it.isSuccess })
        assertTrue((results.single { it.isFailure }.exceptionOrNull() as MfaSdkException).isInvalidChallenge())
        assertEquals(1, operations(fixture).count {
            it in setOf("PhoneAppPinValidation", "PhoneAppAuthenticationResult")
        })
    }

    @Test
    fun `wire failure correlation matches operation request without sensitive text`() = runBlocking {
        val fixture = MfaTestFixture()
        fixture.intercept = { _, _ -> throw IllegalStateException("credential-bearing detail") }
        val manager = fixture.manager(registeredStore())
        val failure = error { manager.pullPendingNotifications() }
        assertEquals(fixture.requests.single().headers["client-request-id"], failure.correlationId)
        assertFalse(failure.toString().contains("credential-bearing"))
    }

    private fun registeredStore() = MemoryMfaStore(MfaStoredState(token = TOKEN, accounts = listOf(account())))

    private fun operations(fixture: MfaTestFixture): List<String> = fixture.requests.map {
        it.headers["x-ms-mac-action"] ?: MfaXml.parse(it.body).child("Body").children.single().name
    }

    private suspend fun error(block: suspend () -> Any?): MfaSdkException {
        val result = runCatching { block() }.exceptionOrNull()
        assertTrue("Expected a sanitized MFA failure, got $result", result is MfaSdkException)
        assertNull(result?.cause)
        return result as MfaSdkException
    }
}
