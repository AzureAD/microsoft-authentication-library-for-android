// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.nativeauth

import androidx.test.core.app.ApplicationProvider
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.claims.ClaimsRequest
import com.microsoft.identity.client.e2e.shadows.ShadowAndroidSdkStorageEncryptionManager
import com.microsoft.identity.client.e2e.tests.PublicClientApplicationAbstractTest
import com.microsoft.identity.common.java.commands.BaseCommand
import com.microsoft.identity.common.java.commands.ICommandResult
import com.microsoft.identity.common.java.controllers.CommandDispatcher
import com.microsoft.identity.common.java.controllers.CommandResult
import com.microsoft.identity.common.java.exception.BaseException
import com.microsoft.identity.common.java.nativeauth.commands.parameters.BaseSignInTokenCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInStartCommandParameters
import com.microsoft.identity.common.java.nativeauth.controllers.results.SignInCommandResult
import com.microsoft.identity.common.java.result.FinalizableResultFuture
import com.microsoft.identity.common.nativeauth.internal.commands.SignInStartCommand
import com.microsoft.identity.common.nativeauth.internal.commands.SignInWithContinuationTokenCommand
import com.microsoft.identity.nativeauth.parameters.NativeAuthSignInContinuationParameters
import com.microsoft.identity.nativeauth.parameters.NativeAuthSignInParameters
import com.microsoft.identity.nativeauth.statemachine.errors.SignInContinuationError
import com.microsoft.identity.nativeauth.statemachine.errors.SignInError
import com.microsoft.identity.nativeauth.statemachine.results.SignInResult
import com.microsoft.identity.nativeauth.statemachine.states.SignInContinuationState
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowAndroidSdkStorageEncryptionManager::class])
class NativeAuthV1InputSnapshotTest : PublicClientApplicationAbstractTest() {
    private lateinit var application: INativeAuthPublicClientApplication
    private lateinit var callbackScope: CoroutineScope
    private val username = "user@example.com"
    private val correlation = "test-correlation"
    private val claimsJson = """{"id_token":{"auth_time":{"essential":true}}}"""

    override fun getConfigFilePath() = "src/test/res/raw/native_auth_native_only_test_config.json"

    @Before
    override fun setup() {
        application = PublicClientApplication.createNativeAuthPublicClientApplication(
            ApplicationProvider.getApplicationContext(), File(getConfigFilePath()))
        CommandDispatcher.getSilentExecutorPoolSize()
        mockkStatic(CommandDispatcher::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(CommandDispatcher::class)
    }

    @Test
    fun initialCallbackSnapshotsInputsBeforeLaunch() = withCallbackScope {
        val password = "original".toCharArray()
        val scopes = mutableListOf("scope")
        val parameters = NativeAuthSignInParameters(username).apply {
            this.password = password
            this.scopes = scopes
            claimsRequest = claims()
        }
        val expectedClaims = ClaimsRequest.getJsonStringFromClaimsRequest(parameters.claimsRequest)
        enqueue(false) { command ->
            val sent = command.parameters as SignInStartCommandParameters
            assertEquals(expectedClaims, sent.claimsRequestJson)
            assertEquals(listOf("scope"), sent.scopes)
            assertArrayEquals("original".toCharArray(), sent.password)
        }
        val callback = CallbackResult()

        application.signIn(parameters, callback)
        assertFalse(callback.result.isCompleted)
        parameters.claimsRequest!!.idTokenClaimsRequested.clear()
        parameters.claimsRequest = ClaimsRequest()
        scopes.clear()
        parameters.scopes = emptyList()
        password.fill('x')
        parameters.password = "replacement".toCharArray()

        assertTrue(callback.result.await() is SignInResult.MFARequired)
        assertEquals(1, callback.calls.get())
        verify(exactly = 1) {
            CommandDispatcher.submitSilentReturningFuture(match { it is SignInStartCommand })
        }
    }

    @Test
    fun continuationCallbackSnapshotsInputsBeforeLaunch() = withCallbackScope {
        val scopes = mutableListOf("scope")
        val parameters = NativeAuthSignInContinuationParameters().apply {
            this.scopes = scopes
            claimsRequest = claims()
        }
        val expectedClaims = ClaimsRequest.getJsonStringFromClaimsRequest(parameters.claimsRequest)
        enqueue(true) { command ->
            val sent = command.parameters as BaseSignInTokenCommandParameters
            assertEquals(expectedClaims, sent.claimsRequestJson)
            assertEquals(listOf("scope"), sent.scopes)
        }
        val callback = CallbackResult()

        continuation().signIn(parameters, callback)
        assertFalse(callback.result.isCompleted)
        parameters.claimsRequest!!.idTokenClaimsRequested.clear()
        parameters.claimsRequest = ClaimsRequest()
        scopes.clear()
        parameters.scopes = emptyList()

        assertTrue(callback.result.await() is SignInResult.MFARequired)
        assertEquals(1, callback.calls.get())
        verify(exactly = 1) {
            CommandDispatcher.submitSilentReturningFuture(match { it is SignInWithContinuationTokenCommand })
        }
    }

    @Test
    fun nullOptionalInputsRemainNullForCallbacksAndSuspendCalls() = withCallbackScope {
        for (isContinuation in listOf(false, true)) {
            enqueue(isContinuation) { command ->
                val sent = command.parameters as BaseSignInTokenCommandParameters
                assertNull(sent.claimsRequestJson)
                assertNull(sent.scopes)
                if (sent is SignInStartCommandParameters) assertNull(sent.password)
            }
            val callback = CallbackResult()
            signIn(isContinuation, null, callback)
            assertTrue(callback.result.await() is SignInResult.MFARequired)
            assertEquals(1, callback.calls.get())
            val result = if (isContinuation) {
                continuation().signIn(NativeAuthSignInContinuationParameters())
            } else {
                application.signIn(NativeAuthSignInParameters(username))
            }
            assertTrue(result is SignInResult.MFARequired)
        }
    }

    @Test
    fun snapshotFailuresAreDeliveredOnceWithOriginalException() = withCallbackScope {
        val requestClaims = claims()
        val failure = IllegalStateException("Cannot snapshot claims")
        mockkStatic(ClaimsRequest::class)
        try {
            every { ClaimsRequest.getJsonStringFromClaimsRequest(requestClaims) } throws failure
            for (isContinuation in listOf(false, true)) {
                val callback = CallbackResult()
                signIn(isContinuation, requestClaims, callback)
                assertFalse(callback.result.isCompleted)
                val result = callback.result.await()
                val exception = if (isContinuation) {
                    (result as SignInContinuationError).exception
                } else {
                    (result as SignInError).exception
                }
                assertSame(failure, exception)
                assertEquals(1, callback.calls.get())
            }
            verify(exactly = 0) { CommandDispatcher.submitSilentReturningFuture(any()) }
        } finally {
            unmockkStatic(ClaimsRequest::class)
        }
    }

    @Test
    fun snapshotCancellationIsNotConvertedToAnErrorResult() = withCallbackScope {
        val requestClaims = claims()
        val cancellation = CancellationException("Cancelled")
        mockkStatic(ClaimsRequest::class)
        try {
            every { ClaimsRequest.getJsonStringFromClaimsRequest(requestClaims) } throws cancellation
            for (isContinuation in listOf(false, true)) {
                val callback = CallbackResult()
                try {
                    signIn(isContinuation, requestClaims, callback)
                    fail("Cancellation must propagate")
                } catch (exception: CancellationException) {
                    assertSame(cancellation, exception)
                }
                assertEquals(0, callback.calls.get())
            }
            verify(exactly = 0) { CommandDispatcher.submitSilentReturningFuture(any()) }
        } finally {
            unmockkStatic(ClaimsRequest::class)
        }
    }

    @Test
    fun cancelledCallbackScopeDoesNotDispatchOrDeliverResults() = withCallbackScope {
        val initial = CallbackResult()
        val continued = CallbackResult()
        signIn(false, claims(), initial)
        signIn(true, claims(), continued)

        callbackScope.cancel()
        runCurrent()

        assertEquals(0, initial.calls.get())
        assertEquals(0, continued.calls.get())
        verify(exactly = 0) { CommandDispatcher.submitSilentReturningFuture(any()) }
    }

    private fun withCallbackScope(block: suspend TestScope.() -> Unit) = runTest(timeout = 60.seconds) {
        callbackScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val applicationScope = NativeAuthPublicClientApplication.pcaScope
        mockkObject(applicationScope)
        try {
            every { applicationScope.coroutineContext } returns callbackScope.coroutineContext
            block()
        } finally {
            callbackScope.cancel()
            unmockkObject(applicationScope)
        }
    }

    private fun claims() = ClaimsRequest.getClaimsRequestFromJsonString(claimsJson)

    private fun signIn(isContinuation: Boolean, claims: ClaimsRequest?, callback: CallbackResult) {
        if (isContinuation) {
            continuation().signIn(NativeAuthSignInContinuationParameters().apply {
                claimsRequest = claims
            }, callback)
        } else {
            application.signIn(NativeAuthSignInParameters(username).apply {
                claimsRequest = claims
            }, callback)
        }
    }

    private fun continuation(): SignInContinuationState {
        val configField = NativeAuthPublicClientApplication::class.java.getDeclaredField("nativeAuthConfig")
        configField.isAccessible = true
        val config = configField.get(application) as NativeAuthPublicClientApplicationConfiguration
        return SignInContinuationState("continuation", correlation, username, config)
    }

    private fun enqueue(isContinuation: Boolean, onCommand: (BaseCommand<*>) -> Unit) {
        val future = FinalizableResultFuture<CommandResult<Any>>()
        future.setResult(CommandResult(ICommandResult.ResultStatus.COMPLETED,
            SignInCommandResult.MFARequired(correlation, "mfa", emptyList()) as Any, correlation))
        every {
            CommandDispatcher.submitSilentReturningFuture(match {
                if (isContinuation) it is SignInWithContinuationTokenCommand else it is SignInStartCommand
            })
        } answers {
            onCommand(firstArg())
            future
        }
    }

    private class CallbackResult : NativeAuthPublicClientApplication.SignInCallback,
        SignInContinuationState.SignInContinuationCallback {
        val result = CompletableDeferred<SignInResult>()
        val calls = AtomicInteger()

        override fun onResult(result: SignInResult) {
            calls.incrementAndGet()
            this.result.complete(result)
        }

        override fun onError(exception: BaseException) {
            calls.incrementAndGet()
            result.completeExceptionally(exception)
        }
    }
}
