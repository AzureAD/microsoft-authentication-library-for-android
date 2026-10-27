// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa

import com.microsoft.identity.mfa.internal.MfaHandoffIntentContract
import com.microsoft.identity.mfa.internal.MfaWebMessageReceiver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/** Synthetic boundary/state tests; no service transport, device or activation credentials. */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MfaEnrollmentHandoffTest {
    @Test fun stagesWithoutEnrollingAndConfirmsExactlyOnce() = runTest {
        var calls = 0
        val done = CompletableDeferred<Unit>()
        val handoff = MfaEnrollmentHandoff(backgroundScope, 1000, { testScheduler.currentTime }) {
            calls++
            done.await()
        }
        val receipt = handoff.stage(LINK)
        val id = requireNotNull(receipt.requestId)
        assertEquals(MfaHandoffDisposition.STAGED, receipt.disposition)
        assertEquals(0, calls)
        assertEquals(MfaHandoffPhase.AWAITING_CONFIRMATION, handoff.state.value.phase)
        assertEquals(MfaHandoffDisposition.DUPLICATE, handoff.stage(LINK).disposition)
        assertEquals(MfaHandoffDisposition.BUSY, handoff.stage(otherLink()).disposition)
        assertTrue(handoff.confirm(id))
        assertFalse(handoff.confirm(id))
        assertFalse(handoff.cancel(id))
        runCurrent()
        assertEquals(1, calls)
        assertEquals(MfaHandoffPhase.SUBMITTING, handoff.state.value.phase)
        done.complete(Unit)
        runCurrent()
        assertEquals(MfaHandoffPhase.SUCCEEDED, handoff.state.value.phase)
        assertEquals(id, handoff.state.value.requestId)
        assertEquals(MfaHandoffDisposition.DUPLICATE, handoff.stage(LINK).disposition)
        assertTrue(handoff.acknowledge(id))
        assertEquals(MfaHandoffPhase.IDLE, handoff.state.value.phase)
        handoff.close()
    }

    @Test fun cancelledExpiredAndStaleRequestsCannotSubmitOrReplaceResults() = runTest {
        var calls = 0
        val handoff = MfaEnrollmentHandoff(backgroundScope, 1000, { testScheduler.currentTime }) { calls++ }
        val first = requireNotNull(handoff.stage(LINK).requestId)
        assertTrue(handoff.cancel(first))
        assertFalse(handoff.confirm(first))
        assertEquals(MfaHandoffPhase.CANCELLED, handoff.state.value.phase)
        val second = requireNotNull(handoff.stage(otherLink()).requestId)
        assertFalse(handoff.acknowledge(first))
        assertFalse(handoff.cancel(first))
        runCurrent()
        advanceTimeBy(1001)
        runCurrent()
        assertEquals(MfaHandoffPhase.EXPIRED, handoff.state.value.phase)
        assertFalse(handoff.confirm(second))
        assertEquals(0, calls)
        handoff.close()
    }

    @Test fun errorsAreBoundedAndNeverBecomeRestoredSuccess() = runTest {
        val handoff = MfaEnrollmentHandoff(backgroundScope, 1000, { testScheduler.currentTime }) {
            throw MfaSdkException(MfaErrorCode.VALIDATION_TIMEOUT, "secret-server-text")
        }
        handoff.confirm(requireNotNull(handoff.stage(LINK).requestId))
        runCurrent()
        assertEquals(MfaHandoffPhase.FAILED, handoff.state.value.phase)
        assertEquals(MfaErrorCode.VALIDATION_TIMEOUT, handoff.state.value.errorCode)
        assertFalse(handoff.state.value.toString().contains("secret"))
        handoff.close()
        val unexpected = MfaEnrollmentHandoff(backgroundScope, 1000, { testScheduler.currentTime }) {
            error("secret credential")
        }
        unexpected.confirm(requireNotNull(unexpected.stage(LINK).requestId))
        runCurrent()
        assertEquals(MfaHandoffPhase.OUTCOME_UNKNOWN, unexpected.state.value.phase)
        assertEquals(MfaErrorCode.OUTCOME_UNKNOWN, unexpected.state.value.errorCode)
        assertFalse(unexpected.state.value.toString().contains("secret"))
        unexpected.close()
    }

    @Test fun closingDoesNotCancelSubmittedOperation() = runTest {
        val done = CompletableDeferred<Unit>()
        val handoff = MfaEnrollmentHandoff(backgroundScope, 1000, { testScheduler.currentTime }) { done.await() }
        val id = requireNotNull(handoff.stage(LINK).requestId)
        handoff.confirm(id)
        runCurrent()
        handoff.close()
        assertEquals(MfaHandoffPhase.SUBMITTING, handoff.state.value.phase)
        assertEquals(MfaHandoffDisposition.CLOSED, handoff.stage(otherLink()).disposition)
        done.complete(Unit)
        runCurrent()
        assertEquals(MfaHandoffPhase.SUCCEEDED, handoff.state.value.phase)
    }

    @Test fun scopeCancellationKeepsUnknownOutcomeAndDoesNotReplay() = runTest {
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        val handoff = MfaEnrollmentHandoff(scope, 1000, { testScheduler.currentTime }) {
            throw CancellationException("secret")
        }
        handoff.confirm(requireNotNull(handoff.stage(LINK).requestId))
        runCurrent()
        assertEquals(MfaHandoffPhase.OUTCOME_UNKNOWN, handoff.state.value.phase)
        assertEquals(MfaHandoffDisposition.DUPLICATE, handoff.stage(LINK).disposition)
        scope.cancel()
        handoff.close()
    }

    @Test fun alreadyCancelledScopeCannotRetainOrStageCredentials() = runTest {
        val scope = CoroutineScope(coroutineContext + SupervisorJob().also { it.cancel() })
        val handoff = MfaEnrollmentHandoff(scope, 1000, { testScheduler.currentTime }) {}
        assertEquals(MfaHandoffDisposition.CLOSED, handoff.stage(LINK).disposition)
        handoff.close()
    }

    @Test fun cancellationOfOwningScopeRetainsUnknownObservation() = runTest {
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        val waiting = CompletableDeferred<Unit>()
        val handoff = MfaEnrollmentHandoff(scope, 1000, { testScheduler.currentTime }) { waiting.await() }
        handoff.confirm(requireNotNull(handoff.stage(LINK).requestId))
        runCurrent()
        scope.cancel()
        runCurrent()
        assertEquals(MfaHandoffPhase.OUTCOME_UNKNOWN, handoff.state.value.phase)
        assertEquals(MfaErrorCode.OUTCOME_UNKNOWN, handoff.state.value.errorCode)
        assertEquals(MfaHandoffDisposition.CLOSED, handoff.stage(otherLink()).disposition)
    }

    @Test fun previousSuccessCannotCompleteANewRequest() = runTest {
        val waiting = CompletableDeferred<Unit>()
        var calls = 0
        val handoff = MfaEnrollmentHandoff(backgroundScope, 1000, { testScheduler.currentTime }) {
            if (++calls > 1) waiting.await()
        }
        val first = requireNotNull(handoff.stage(LINK).requestId)
        handoff.confirm(first)
        runCurrent()
        assertEquals(MfaHandoffPhase.SUCCEEDED, handoff.state.value.phase)
        val second = requireNotNull(handoff.stage(otherLink()).requestId)
        assertEquals(MfaHandoffPhase.AWAITING_CONFIRMATION, handoff.state.value.phase)
        assertFalse(handoff.acknowledge(first))
        handoff.confirm(second)
        runCurrent()
        assertEquals(MfaHandoffPhase.SUBMITTING, handoff.state.value.phase)
        waiting.complete(Unit)
        runCurrent()
        assertEquals(second, handoff.state.value.requestId)
        assertEquals(MfaHandoffPhase.SUCCEEDED, handoff.state.value.phase)
        handoff.close()
    }

    @Test fun equivalentQueryOrderIsStillADuplicate() = runTest {
        val handoff = MfaEnrollmentHandoff(backgroundScope, 1000, { testScheduler.currentTime }) {}
        handoff.stage(LINK)
        val reordered = LINK.replace("accountType=mfa&", "") + "&accountType=mfa"
        assertEquals(MfaHandoffDisposition.DUPLICATE, handoff.stage(reordered).disposition)
        handoff.close()
    }

    @Test fun webOriginConfigurationRejectsWildcardsPathsAndCredentialsWithoutEcho() = runTest {
        val handoff = MfaEnrollmentHandoff(backgroundScope, 1000, { testScheduler.currentTime }) {}
        for (origin in listOf("*", "http://mysignins.microsoft.com", "$ORIGIN/", "$ORIGIN:444",
            "https://secret@mysignins.microsoft.com", "$ORIGIN?secret", "$ORIGIN#secret")) {
            try {
                MfaWebMessageReceiver(handoff, setOf(origin))
                fail("Expected invalid configuration")
            } catch (error: MfaSdkException) {
                assertEquals(MfaErrorCode.INVALID_CONFIGURATION, error.code)
                assertFalse(error.toString().contains("secret"))
            }
        }
        handoff.close()
    }

    @Test fun invalidAndOversizeInputsDoNotLeakOrReplacePendingRequest() = runTest {
        val handoff = MfaEnrollmentHandoff(backgroundScope, 1000, { testScheduler.currentTime }) {}
        for (input in listOf("", "secret", LINK + "&code=duplicate", LINK.replace("https:", "http:"),
            LINK.replace("login.microsoftonline.com", "evil.invalid"), "x".repeat(8193),
            LINK.replace("poc.mobileappcommunicator.auth.microsoft.com", "evil.invalid"))) {
            val receipt = handoff.stage(input)
            assertEquals(MfaHandoffDisposition.REJECTED, receipt.disposition)
            assertFalse(receipt.toString().contains("secret"))
            assertEquals(MfaHandoffPhase.IDLE, handoff.state.value.phase)
        }
        handoff.stage(LINK)
        assertFalse(handoff.state.value.toString().contains(CODE))
        handoff.close()
        assertEquals(MfaHandoffPhase.DISPOSED, handoff.state.value.phase)
    }

    @Test fun replayBookkeepingIsFiniteAndFailsClosedAtCapacity() = runTest {
        val handoff = MfaEnrollmentHandoff(backgroundScope, 1000, { testScheduler.currentTime }) {}
        repeat(64) { index ->
            handoff.cancel(requireNotNull(handoff.stage(otherLink(index.toString())).requestId))
        }
        assertEquals(MfaHandoffDisposition.LIMIT_REACHED, handoff.stage(LINK).disposition)
        handoff.close()
    }

    @Test fun intentContractRequiresExactCallbackActionSelectorAndStringExtra() = runTest {
        val handoff = MfaEnrollmentHandoff(backgroundScope, 1000, { testScheduler.currentTime }) {}
        val contract = MfaHandoffIntentContract(handoff, CALLBACK)
        fun receive(action: String? = "android.intent.action.VIEW", selector: Boolean = false,
                    uri: String? = CALLBACK, payload: Any? = LINK) =
            contract.receive(action, selector, uri, payload)
        assertEquals(MfaHandoffDisposition.IGNORED, receive(uri = "other://callback").disposition)
        for (receipt in listOf(receive(action = "other"), receive(selector = true),
            receive(uri = "$CALLBACK?activation=secret"), receive(uri = "$CALLBACK/"),
            receive(uri = "$CALLBACK#secret"), receive(payload = 1), receive(payload = null))) {
            assertEquals(MfaHandoffDisposition.REJECTED, receipt.disposition)
        }
        assertEquals(MfaHandoffDisposition.STAGED, receive().disposition)
        assertEquals(MfaHandoffSource.BROWSER, handoff.state.value.source)
        handoff.close()
    }

    @Test fun webMessagesRequireActualAllowedOriginMainFrameAndExactBoundedSchema() = runTest {
        val handoff = MfaEnrollmentHandoff(backgroundScope, 1000, { testScheduler.currentTime }) {}
        val receiver = MfaWebMessageReceiver(handoff, setOf(ORIGIN))
        val message = """{"version":1,"type":"activation","activationLink":"$LINK"}"""
        assertEquals(MfaHandoffDisposition.REJECTED, receiver.receive(message, "https://evil.invalid", true).disposition)
        assertEquals(MfaHandoffDisposition.REJECTED, receiver.receive(message, ORIGIN, false).disposition)
        for (invalid in listOf("$message trailing", message.replace("\"version\":1", "\"version\":2"),
            message.replace("\"version\":1", "\"version\":1,\"version\":1"),
            message.replace("\"type\"", "\"origin\":\"$ORIGIN\",\"type\""),
            "x".repeat(16385), "{}", "[]", message.replace("\"activationLink\"", "\"unknown\""))) {
            assertEquals(MfaHandoffDisposition.REJECTED, receiver.receive(invalid, ORIGIN, true).disposition)
        }
        assertEquals(MfaHandoffDisposition.STAGED, receiver.receive(message, ORIGIN, true).disposition)
        assertEquals(MfaHandoffSource.WEBVIEW, handoff.state.value.source)
        assertEquals(MfaHandoffDisposition.DUPLICATE, receiver.receive(message, ORIGIN, true).disposition)
        handoff.close()
    }

    @Test fun deepLinkNavigationRequiresMetadataHostContextAndExactDecodedTarget() = runTest {
        var calls = 0
        val handoff = MfaEnrollmentHandoff(backgroundScope, 1000, { testScheduler.currentTime }) { calls++ }
        val adapter = MfaHandoffDeepLinkAdapter(handoff, CALLBACK, "example.test.host", setOf(ORIGIN))
        fun receive(action: String? = "android.intent.action.VIEW", selector: Boolean = false,
                    component: Boolean = false, packageName: String? = "example.test.host",
                    uri: String? = CALLBACK, payload: Any? = LINK, page: String? = "$ORIGIN/security-info",
                    mainFrame: Boolean = true, gesture: Boolean = true, method: String? = "GET",
                    redirect: Boolean = false) =
            adapter.receiveDecoded(action, selector, component, packageName, uri, payload, page,
                mainFrame, gesture, method, redirect)
        val rejected = listOf(
            receive(mainFrame = false), receive(gesture = false), receive(method = "POST"),
            receive(method = null), receive(redirect = true), receive(page = null),
            receive(page = "https://evil.invalid"), receive(page = "http://mysignins.microsoft.com"),
            receive(page = "$ORIGIN:444/security-info"), receive(page = "https://secret@mysignins.microsoft.com"),
            receive(page = "$ORIGIN.evil.invalid"), receive(action = "other"),
            receive(selector = true), receive(component = true), receive(packageName = null),
            receive(packageName = "example.other.host"), receive(uri = "other://activate"),
            receive(uri = "$CALLBACK/"), receive(uri = "$CALLBACK?secret"), receive(uri = "$CALLBACK#secret"),
            receive(payload = 1), receive(payload = null), receive(payload = "x".repeat(8193))
        )
        rejected.forEach {
            assertEquals(MfaHandoffDisposition.REJECTED, it.disposition)
            assertFalse(it.toString().contains("secret"))
        }
        assertEquals(MfaHandoffPhase.IDLE, handoff.state.value.phase)
        assertEquals(MfaHandoffDisposition.STAGED, receive(page = "$ORIGIN:443/security-info?tab=methods#setup").disposition)
        assertEquals(MfaHandoffSource.WEBVIEW, handoff.state.value.source)
        assertEquals(MfaHandoffDisposition.DUPLICATE, receive().disposition)
        assertEquals(0, calls)
        handoff.close()
    }

    @Test fun deepLinkNavigationRejectsInvalidConfigurationWithoutEchoingInput() = runTest {
        val handoff = MfaEnrollmentHandoff(backgroundScope, 1000, { testScheduler.currentTime }) {}
        for ((packageName, origins) in listOf(
            "" to setOf(ORIGIN), "a.b;secret" to setOf(ORIGIN),
            "example.test.host" to emptySet(), "example.test.host" to setOf("*"),
            "example.test.host" to setOf("$ORIGIN/"),
            "example.test.host" to setOf("$ORIGIN?secret")
        )) {
            try {
                MfaHandoffDeepLinkAdapter(handoff, CALLBACK, packageName, origins)
                fail("Expected invalid configuration")
            } catch (error: MfaSdkException) {
                assertEquals(MfaErrorCode.INVALID_CONFIGURATION, error.code)
                assertFalse(error.toString().contains("secret"))
            }
        }
        handoff.close()
    }

    private fun otherLink(code: String = "different-synthetic") = LINK.replace(CODE, code)

    private companion object {
        const val CALLBACK = "mfa-poc-live://activate"
        const val ORIGIN = "https://mysignins.microsoft.com"
        const val CODE = "synthetic-000000"
        const val LINK = "https://login.microsoftonline.com/authenticatorApp/activateAccount" +
            "?accountType=mfa&code=$CODE&url=https%3A%2F%2Fpoc.mobileappcommunicator.auth.microsoft.com"
    }
}
