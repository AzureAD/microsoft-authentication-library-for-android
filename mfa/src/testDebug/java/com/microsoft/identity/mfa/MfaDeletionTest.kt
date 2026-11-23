// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa

import com.microsoft.identity.mfa.internal.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class MfaDeletionTest {
    private val id = "11111111-1111-1111-1111-111111111111"
    private val tenant = "22222222-2222-2222-2222-222222222222"
    private val oid = "33333333-3333-3333-3333-333333333333"
    private fun token() = MfaDeletionToken("opaque", tenant, oid,
        "https://login.microsoftonline.com/$tenant", Long.MAX_VALUE, "Bearer",
        listOf("UserAuthenticationMethod.ReadWrite"))

    private inner class Fixture(var status: Int = 204) {
        var state = MfaStoredState(accounts = listOf(account(id), account("other")))
        var calls = 0
        var invalidated = false
        var failSave = false
        var graph: suspend () -> Int = { status }
        val controller = MfaDeletionController(Mutex(), { state }, {
            if (failSave) throw MfaSdkException(MfaErrorCode.STORAGE_FAILURE)
            state = it
        }, { invalidated = true }, MfaGraphTransport { request, _ ->
            assertEquals("https://graph.microsoft.com/v1.0/users/$oid/authentication/microsoftAuthenticatorMethods/$id", request)
            calls++
            graph()
        })
        fun account(key: String) = MfaStoredState.Account(key, "Test", "test@example.invalid",
            "unused", tenant, oid, "https://example.invalid", "push", "dos")
        suspend fun delete(provider: MfaDeletionTokenProvider = MfaDeletionTokenProvider { token() }) =
            controller.delete(id, provider, true) {}
    }

    @Test fun onlyConfirmedRemoteAbsenceRemovesSelectedAccount() = runTest {
        for (status in listOf(204, 404)) {
            val f = Fixture(status)
            f.delete()
            assertEquals(listOf("other"), f.state.accounts.map { it.id })
            assertTrue(f.invalidated)
        }
        for (status in listOf(200, 401, 403, 429, 500, 503)) {
            val f = Fixture(status)
            val error = failure { f.delete() }
            assertEquals(status, error.httpStatus)
            assertEquals(2, f.state.accounts.size)
            assertFalse(f.invalidated)
        }
    }

    @Test fun rejectsUntrustedTokenMetadataBeforeDelete() = runTest {
        val valid = token()
        for (bad in listOf(valid.copy(accessToken = ""), valid.copy(accessToken = "bad\r\nvalue"),
            valid.copy(tenantId = oid), valid.copy(objectId = tenant),
            valid.copy(authority = "https://login.microsoftonline.com.evil.invalid/$tenant"),
            valid.copy(authority = "https://login.microsoftonline.com/$oid"),
            valid.copy(expiresOnMillis = 0), valid.copy(authenticationScheme = "PoP"),
            valid.copy(grantedScopes = listOf("User.Read")))) {
            val f = Fixture()
            assertEquals(MfaDeletionError.INVALID_TOKEN,
                failure { f.delete(MfaDeletionTokenProvider { bad }) }.code)
            assertEquals(0, f.calls)
            assertEquals(2, f.state.accounts.size)
        }
    }

    @Test fun cleanupFailureRetriesWithoutProviderOrSecondDelete() = runTest {
        val f = Fixture()
        f.failSave = true
        assertEquals(MfaDeletionError.LOCAL_CLEANUP_REQUIRED, failure { f.delete() }.code)
        assertTrue(f.invalidated)
        assertTrue(f.controller.isRemotelyDeleted(id))
        f.failSave = false
        f.delete(MfaDeletionTokenProvider { error("Must not acquire again") })
        assertEquals(1, f.calls)
        assertEquals(listOf("other"), f.state.accounts.map { it.id })
    }

    @Test fun providerFailureAndUnknownTransportOutcomeRetainRegistration() = runTest {
        val f = Fixture()
        assertEquals(MfaDeletionError.PROVIDER_FAILED,
            failure { f.delete(MfaDeletionTokenProvider { error("sensitive") }) }.code)
        assertEquals(0, f.calls)
        f.graph = { throw java.io.IOException("sensitive") }
        assertEquals(MfaDeletionError.OUTCOME_UNKNOWN, failure { f.delete() }.code)
        assertEquals(2, f.state.accounts.size)
    }

    @Test fun approvalsStayAvailableDuringAcquisitionAndDeleteWaitsForResponse() = runTest {
        val f = Fixture()
        val acquisition = CompletableDeferred<Unit>()
        val response = CompletableDeferred<Unit>()
        val deletion = launch { f.delete(MfaDeletionTokenProvider { acquisition.await(); token() }) }
        yield()
        val approval = launch { f.controller.respond(id) { response.await() } }
        yield()
        acquisition.complete(Unit)
        yield()
        assertEquals(0, f.calls)
        response.complete(Unit)
        approval.join()
        deletion.join()
        assertEquals(1, f.calls)
    }

    @Test fun cancelBeforeDispatchRetainsAndAllowsRetry() = runTest {
        val f = Fixture()
        val deletion = launch { f.delete(MfaDeletionTokenProvider { awaitCancellation() }) }
        yield()
        deletion.cancelAndJoin()
        assertEquals(0, f.calls)
        assertEquals(2, f.state.accounts.size)
        f.delete()
        assertEquals(1, f.calls)
    }

    @Test fun duplicateDeletionAndCancelledApprovalWaitCannotDispatch() = runTest {
        val f = Fixture()
        val response = CompletableDeferred<Unit>()
        val approval = launch { f.controller.respond(id) { response.await() } }
        yield()
        val deletion = launch { f.delete() }
        yield()
        assertEquals(MfaDeletionError.OPERATION_IN_PROGRESS, failure { f.delete() }.code)
        deletion.cancelAndJoin()
        assertEquals(0, f.calls)
        assertEquals(2, f.state.accounts.size)
        response.complete(Unit)
        approval.join()
        f.delete()
        assertEquals(1, f.calls)
    }

    @Test fun unknownOutcomeThen404RetryRemovesOnlySelectedMethod() = runTest {
        val f = Fixture()
        f.graph = { throw java.io.IOException() }
        assertEquals(MfaDeletionError.OUTCOME_UNKNOWN, failure { f.delete() }.code)
        f.graph = { 404 }
        f.delete()
        assertEquals(listOf("other"), f.state.accounts.map { it.id })
    }

    @Test fun committedCleanupFailureSurvivesCancellationAndRemainsRecoverable() = runTest {
        val f = Fixture()
        f.failSave = true
        val dispatched = CompletableDeferred<Unit>()
        val response = CompletableDeferred<Unit>()
        f.graph = { dispatched.complete(Unit); response.await(); 204 }
        val deletion = launch {
            assertEquals(MfaDeletionError.LOCAL_CLEANUP_REQUIRED, failure { f.delete() }.code)
        }
        dispatched.await()
        deletion.cancel()
        response.complete(Unit)
        deletion.join()
        assertTrue(f.controller.isRemotelyDeleted(id))
        f.failSave = false
        f.delete(MfaDeletionTokenProvider { error("Should be cleanup-only") })
        assertEquals(1, f.calls)
        assertEquals(listOf("other"), f.state.accounts.map { it.id })
    }

    @Test fun cancellationAfterDispatchCannotUndoConfirmedCleanup() = runTest {
        val f = Fixture()
        val dispatched = CompletableDeferred<Unit>()
        val response = CompletableDeferred<Unit>()
        f.graph = { dispatched.complete(Unit); response.await(); 204 }
        val deletion = launch { f.delete() }
        dispatched.await()
        deletion.cancel()
        response.complete(Unit)
        deletion.join()
        assertEquals(listOf("other"), f.state.accounts.map { it.id })
        assertTrue(f.invalidated)
    }

    private suspend fun failure(block: suspend () -> Unit): MfaDeletionException {
        try { block(); throw AssertionError("Expected deletion failure") }
        catch (e: MfaDeletionException) { return e }
    }
}
