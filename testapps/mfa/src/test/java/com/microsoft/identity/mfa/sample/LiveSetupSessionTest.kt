// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa.sample

import com.microsoft.identity.mfa.MfaRegistrationParameters
import org.junit.Assert.*
import org.junit.Test

class LiveSetupSessionTest {
    private var time = 0L
    private val sessions = LiveSetupSession { time }
    private val input = MfaRegistrationParameters.QrCode("native-only-unit-test-marker")

    @Test
    fun requestRequiresExactUriSourceAndExplicitConfirmation() {
        val request = requireNotNull(sessions.prepare(input, SetupSource.BROWSER))
        assertNull(sessions.consume(request.id))
        assertNull(sessions.prepare(input, SetupSource.EMBEDDED))
        assertFalse(sessions.receive(request.launchUri, SetupSource.EMBEDDED))
        listOf(
            "${request.launchUri}?anything=1", "${request.launchUri}#fragment",
            "${request.launchUri}/", request.launchUri.replace("enroll/", "enroll:80/"),
            request.launchUri.replace("enroll/", "user@enroll/"),
            request.launchUri.replace("enroll/", "foreign/"),
            request.launchUri.replace("mfa-poc-live:", "https:"),
            request.launchUri.replace("/${request.id}", "/%${request.id}"),
            "mfa-poc-live://enroll/${"0".repeat(64)}"
        ).forEach { assertFalse(sessions.receive(it, SetupSource.BROWSER)) }
        assertTrue(sessions.receive(request.launchUri, SetupSource.BROWSER))
        assertFalse(sessions.receive(request.launchUri, SetupSource.BROWSER))
        assertSame(input, sessions.consume(request.id))
        assertNull(sessions.consume(request.id))
        sessions.complete(request.id, true)
        assertEquals(SetupPhase.SUCCEEDED, sessions.current()?.phase)
        assertFalse(sessions.receive(request.launchUri, SetupSource.BROWSER))
    }

    @Test
    fun cancellationAndExpiryDiscardUnsubmittedInput() {
        val request = requireNotNull(sessions.prepare(input, SetupSource.EMBEDDED))
        sessions.receive(request.launchUri, SetupSource.EMBEDDED)
        sessions.cancel(request.id)
        assertNull(sessions.consume(request.id))
        assertEquals(SetupPhase.CANCELLED, sessions.current()?.phase)
        sessions.clear()
        val expired = requireNotNull(sessions.prepare(input, SetupSource.BROWSER))
        time += LiveSetupSession.REQUEST_LIFETIME_MS
        assertFalse(sessions.receive(expired.launchUri, SetupSource.BROWSER))
        assertEquals(SetupPhase.EXPIRED, sessions.current()?.phase)
        assertNull(sessions.consume(expired.id))
        time += LiveSetupSession.RESULT_LIFETIME_MS
        assertNull(sessions.current())
        assertTrue(sessions.canPrepare())
    }

    @Test
    fun clearingOrExpiringPageDoesNotUnlockInFlightEnrollment() {
        val request = requireNotNull(sessions.prepare(input, SetupSource.BROWSER))
        sessions.receive(request.launchUri, SetupSource.BROWSER)
        sessions.consume(request.id)
        sessions.cancel(request.id)
        assertEquals(SetupPhase.SUBMITTING, sessions.current()?.phase)
        time += LiveSetupSession.REQUEST_LIFETIME_MS
        assertEquals(SetupPhase.INTERRUPTED, sessions.current()?.phase)
        sessions.clear()
        assertFalse(sessions.canPrepare())
        assertNull(sessions.prepare(input, SetupSource.EMBEDDED))
        sessions.complete(request.id, false)
        val next = requireNotNull(sessions.prepare(input, SetupSource.EMBEDDED))
        sessions.complete(request.id, true)
        assertEquals(next, sessions.current())
        assertNotEquals(request.id, next.id)
    }

    @Test
    fun readinessAndUncertainFailureNeverCreateSuccessOrReplay() {
        val blocked = requireNotNull(sessions.prepare(input, SetupSource.EMBEDDED))
        sessions.receive(blocked.launchUri, SetupSource.EMBEDDED)
        sessions.cancel(blocked.id, notReady = true)
        assertEquals(SetupPhase.NOT_READY, sessions.current()?.phase)
        assertNull(sessions.consume(blocked.id))
        sessions.clear()
        val failed = requireNotNull(sessions.prepare(input, SetupSource.BROWSER))
        sessions.receive(failed.launchUri, SetupSource.BROWSER)
        sessions.consume(failed.id)
        sessions.complete(failed.id, false)
        sessions.complete(failed.id, true)
        assertEquals(SetupPhase.NOT_CONFIRMED, sessions.current()?.phase)
        assertNull(sessions.consume(failed.id))
        val restarted = LiveSetupSession { time }
        assertFalse(restarted.receive(failed.launchUri, SetupSource.BROWSER))
    }
}
