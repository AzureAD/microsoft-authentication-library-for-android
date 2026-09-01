//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.nativeauth.v2

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.AuthenticationResultAdapter
import com.microsoft.identity.client.e2e.shadows.ShadowAndroidSdkStorageEncryptionManager
import com.microsoft.identity.client.e2e.tests.PublicClientApplicationAbstractTest
import com.microsoft.identity.client.exception.MsalClientException
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.common.java.commands.BaseCommand
import com.microsoft.identity.common.java.commands.ICommandResult
import com.microsoft.identity.common.java.controllers.CommandDispatcher
import com.microsoft.identity.common.java.controllers.CommandResult
import com.microsoft.identity.common.java.exception.BaseException
import com.microsoft.identity.common.java.logging.DiagnosticContext
import com.microsoft.identity.common.java.nativeauth.controllers.results.INativeAuthCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2CommandResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationState
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2LinkRelation
import com.microsoft.identity.common.java.nativeauth.providers.v2.NativeAuthV2FlowScenario
import com.microsoft.identity.common.java.result.FinalizableResultFuture
import com.microsoft.identity.common.java.result.ILocalAuthenticationResult
import com.microsoft.identity.common.java.util.ResultFuture
import com.microsoft.identity.common.nativeauth.internal.commands.NativeAuthV2ResendCodeCommand
import com.microsoft.identity.common.nativeauth.internal.commands.NativeAuthV2ResetPasswordStartCommand
import com.microsoft.identity.common.nativeauth.internal.commands.NativeAuthV2SignInAfterResetPasswordCommand
import com.microsoft.identity.common.nativeauth.internal.commands.NativeAuthV2SubmitCodeCommand
import com.microsoft.identity.common.nativeauth.internal.commands.NativeAuthV2SubmitNewPasswordCommand
import com.microsoft.identity.nativeauth.INativeAuthPublicClientApplication
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplication
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplicationConfiguration
import com.microsoft.identity.nativeauth.parameters.NativeAuthResetPasswordParameters
import com.microsoft.identity.nativeauth.parameters.NativeAuthSignInParameters
import com.microsoft.identity.nativeauth.parameters.NativeAuthSignUpParameters
import com.microsoft.identity.nativeauth.statemachine.errors.ErrorTypes
import com.microsoft.identity.nativeauth.statemachine.errors.NativeAuthErrorV2
import com.microsoft.identity.nativeauth.statemachine.errors.ResetPasswordErrorV2
import com.microsoft.identity.nativeauth.statemachine.errors.SubmitCodeErrorV2
import com.microsoft.identity.nativeauth.statemachine.errors.SubmitNewPasswordErrorV2
import com.microsoft.identity.nativeauth.statemachine.NativeAuthFlowScenarioV2
import com.microsoft.identity.nativeauth.statemachine.results.NativeAuthResultV2
import com.microsoft.identity.nativeauth.statemachine.states.AttributesInvalidStateV2
import com.microsoft.identity.nativeauth.statemachine.states.AttributesRequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.CodeRequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.MFARequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.MFAVerificationRequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.NewPasswordRequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.PasswordRequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.SignInAfterResetPasswordStateV2
import com.microsoft.identity.nativeauth.statemachine.states.StrongAuthRegistrationRequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.StrongAuthVerificationRequiredStateV2
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertSame
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowAndroidSdkStorageEncryptionManager::class])
class NativeAuthV2InterfaceKotlinTest : PublicClientApplicationAbstractTest() {

    private lateinit var context: Context
    private lateinit var application: INativeAuthPublicClientApplication
    private val username = "user@email.com"

    override fun getConfigFilePath() = "src/test/res/raw/native_auth_native_only_test_config.json"

    @Before
    override fun setup() {
        context = ApplicationProvider.getApplicationContext()
        val configFile = File(getConfigFilePath())
        try {
            application = PublicClientApplication.createNativeAuthPublicClientApplication(context, configFile)
        } catch (e: MsalException) {
            fail(e.message)
        }
        CommandDispatcher.getSilentExecutorPoolSize()
        mockkStatic(CommandDispatcher::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(CommandDispatcher::class)
    }

    @Test
    fun signUpV2ReturnsNotImplemented() = runTest {
        val result = application.signUpV2(NativeAuthSignUpParameters(username = username))
        val error = assertNotImplemented(result, NativeAuthFlowScenarioV2.SIGN_UP)
        assertEquals(NativeAuthFlowScenarioV2.SIGN_UP, error.scenario)
    }

    @Test
    fun resetPasswordV2MapsCodeRequiredWithoutNetwork() = runTest {
        val continuationState = createContinuationState()
        enqueueResult(
            NativeAuthV2CommandResult.CodeRequired(
                correlationId = correlationId,
                continuationState = continuationState,
                codeLength = 6,
                challengeTargetLabel = "a***@example.com",
                challengeChannel = "email"
            ),
            NativeAuthV2ResetPasswordStartCommand::class
        )

        val result = application.resetPasswordV2(NativeAuthResetPasswordParameters(username = username))

        assertTrue(result is NativeAuthResultV2.CodeRequired)
        result as NativeAuthResultV2.CodeRequired
        assertEquals(6, result.codeLength)
        assertEquals("a***@example.com", result.sentTo)
        assertEquals("email", result.channel)
        assertNull(result.nextState.continuationToken)
        assertEquals(correlationId, result.nextState.correlationId)
    }

    @Test
    fun resetPasswordV2PreservesServerErrorsAndCancellation() = runTest {
        enqueueResult(
            NativeAuthV2CommandResult.UserNotFound(
                correlationId,
                "user_not_found",
                "No account",
                errorCodes
            ),
            NativeAuthV2ResetPasswordStartCommand::class
        )
        val userNotFound = application.resetPasswordV2(NativeAuthResetPasswordParameters(username))
        assertTrue(userNotFound is ResetPasswordErrorV2)
        assertEquals(errorCodes, (userNotFound as ResetPasswordErrorV2).errorCodes)

        enqueueResult(
            INativeAuthCommandResult.APIError(
                "api_error",
                "API failed",
                correlationId = correlationId,
                errorCodes = errorCodes
            ),
            NativeAuthV2ResetPasswordStartCommand::class
        )
        val apiError = application.resetPasswordV2(NativeAuthResetPasswordParameters(username))
        assertEquals(errorCodes, (apiError as ResetPasswordErrorV2).errorCodes)

        enqueueCancellation(NativeAuthV2ResetPasswordStartCommand::class)
        assertCancellation {
            application.resetPasswordV2(NativeAuthResetPasswordParameters(username))
        }
    }

    @Test
    fun resetPasswordV2UsesDiagnosticCorrelationIdForBlankUsername() = runTest {
        mockkObject(DiagnosticContext.INSTANCE)
        every { DiagnosticContext.INSTANCE.threadCorrelationId } returns correlationId
        try {
            val error = application.resetPasswordV2(
                NativeAuthResetPasswordParameters(username = " ")
            ) as ResetPasswordErrorV2

            assertTrue(error.isInvalidUsername())
            assertEquals(correlationId, error.correlationId)
        } finally {
            unmockkObject(DiagnosticContext.INSTANCE)
        }
    }

    @Test
    fun resetPasswordV2SurfacesSignedInPreconditionAsResultToSuspendAndCallback() = runTest {
        mockkObject(NativeAuthPublicClientApplication.Companion)
        every {
            NativeAuthPublicClientApplication.getCurrentAccountInternal(any())
        } returns mockk<IAccount>()
        try {
            // The precondition must be surfaced through the V2 result contract, not thrown. This
            // matches the V1 flows, whose guard sits inside the try/catch that converts it to an
            // error result.
            val suspendResult = application.resetPasswordV2(NativeAuthResetPasswordParameters(username))
            assertTrue(suspendResult is ResetPasswordErrorV2)
            assertEquals(ErrorTypes.CLIENT_EXCEPTION, (suspendResult as ResetPasswordErrorV2).errorType)
            val suspendCause = suspendResult.exception
            assertTrue(suspendCause is MsalClientException)
            assertEquals("An account is already signed in.", suspendCause?.message)

            val callbackResult = ResultFuture<NativeAuthResultV2>()
            application.resetPasswordV2(
                NativeAuthResetPasswordParameters(username),
                object : NativeAuthPublicClientApplication.NativeAuthV2Callback {
                    override fun onResult(result: NativeAuthResultV2) {
                        callbackResult.setResult(result)
                    }

                    override fun onError(exception: BaseException) {
                        callbackResult.setException(
                            IllegalStateException("Expected callback.onResult", exception)
                        )
                    }
                }
            )
            val result = callbackResult.get(10, TimeUnit.SECONDS)
            assertTrue(result is ResetPasswordErrorV2)
            assertEquals(ErrorTypes.CLIENT_EXCEPTION, (result as ResetPasswordErrorV2).errorType)
            val callbackCause = result.exception
            assertTrue(callbackCause is MsalClientException)
            assertEquals("An account is already signed in.", callbackCause?.message)
        } finally {
            unmockkObject(NativeAuthPublicClientApplication.Companion)
        }
    }

    @Test
    fun submitCodeAndResendCodeMapAllResultKinds() = runTest {
        val state = codeRequiredState()

        enqueueResult(
            NativeAuthV2CommandResult.IncorrectCode(
                correlationId,
                "invalid_code",
                "Wrong code",
                subError,
                errorCodes
            ),
            NativeAuthV2SubmitCodeCommand::class
        )
        val incorrectCode = state.submitCode("123456") as SubmitCodeErrorV2
        assertTrue(incorrectCode.isInvalidCode())
        assertEquals(errorCodes, incorrectCode.errorCodes)
        assertEquals(subError, incorrectCode.subError)

        enqueueResult(
            NativeAuthV2CommandResult.NewPasswordRequired(correlationId, createContinuationState()),
            NativeAuthV2SubmitCodeCommand::class
        )
        assertTrue(state.submitCode("123456") is NativeAuthResultV2.NewPasswordRequired)

        enqueueResult(
            INativeAuthCommandResult.Redirect(correlationId, "browser"),
            NativeAuthV2SubmitCodeCommand::class
        )
        assertTrue((state.submitCode("123456") as SubmitCodeErrorV2).isBrowserRequired())

        enqueueResult(
            NativeAuthV2CommandResult.NotImplemented(correlationId, "unsupported", "unsupported"),
            NativeAuthV2SubmitCodeCommand::class
        )
        assertTrue((state.submitCode("123456") as NativeAuthErrorV2).isNotImplemented())

        enqueueResult(apiError(), NativeAuthV2SubmitCodeCommand::class)
        assertEquals(errorCodes, (state.submitCode("123456") as SubmitCodeErrorV2).errorCodes)

        enqueueResult(
            NativeAuthV2CommandResult.Complete(correlationId, null, null, null),
            NativeAuthV2SubmitCodeCommand::class
        )
        assertTrue(state.submitCode("123456") is NativeAuthErrorV2)

        enqueueResult(
            NativeAuthV2CommandResult.CodeRequired(
                correlationId,
                createContinuationState(),
                8,
                "phone",
                "sms"
            ),
            NativeAuthV2ResendCodeCommand::class
        )
        val resent = state.resendCode() as NativeAuthResultV2.CodeRequired
        assertEquals(8, resent.codeLength)
        assertEquals("phone", resent.sentTo)

        enqueueResult(apiError(), NativeAuthV2ResendCodeCommand::class)
        assertEquals(errorCodes, (state.resendCode() as NativeAuthErrorV2).errorCodes)

        enqueueResult(
            INativeAuthCommandResult.Redirect(correlationId, "browser"),
            NativeAuthV2ResendCodeCommand::class
        )
        assertTrue((state.resendCode() as NativeAuthErrorV2).isBrowserRequired())

        enqueueResult(
            NativeAuthV2CommandResult.NotImplemented(correlationId, "unsupported", "unsupported"),
            NativeAuthV2ResendCodeCommand::class
        )
        assertTrue((state.resendCode() as NativeAuthErrorV2).isNotImplemented())

        enqueueResult(
            NativeAuthV2CommandResult.Complete(correlationId, null, null, null),
            NativeAuthV2ResendCodeCommand::class
        )
        assertTrue(state.resendCode() is NativeAuthErrorV2)

        enqueueCancellation(NativeAuthV2SubmitCodeCommand::class)
        assertCancellation { state.submitCode("123456") }
        enqueueCancellation(NativeAuthV2ResendCodeCommand::class)
        assertCancellation { state.resendCode() }
    }

    @Test
    fun submitCodeRejectsEmptyInputWithRetryableInvalidCodeError() = runTest {
        val state = codeRequiredState()

        val error = state.submitCode("") as SubmitCodeErrorV2

        assertTrue(error.isInvalidCode())
    }

    @Test
    fun codeOperationsWithoutContinuationStateReturnInvalidState() = runTest {
        val state = CodeRequiredStateV2(
            null,
            correlationId,
            NativeAuthFlowScenarioV2.RESET_PASSWORD,
            NativeAuthPublicClientApplicationConfiguration()
        )

        val submitError = state.submitCode("123456") as NativeAuthErrorV2
        val resendError = state.resendCode() as NativeAuthErrorV2

        assertEquals(ErrorTypes.INVALID_STATE, submitError.errorType)
        assertEquals("The continuation state is unavailable. Restart the flow.", submitError.errorMessage)
        assertEquals(ErrorTypes.INVALID_STATE, resendError.errorType)
        assertEquals("The continuation state is unavailable. Restart the flow.", resendError.errorMessage)
    }

    @Test
    fun submitNewPasswordMapsAllResultKinds() = runTest {
        val codeState = codeRequiredState()
        enqueueResult(
            NativeAuthV2CommandResult.NewPasswordRequired(correlationId, createContinuationState()),
            NativeAuthV2SubmitCodeCommand::class
        )
        val state =
            (codeState.submitCode("123456") as NativeAuthResultV2.NewPasswordRequired).nextState

        enqueueResult(
            NativeAuthV2CommandResult.PasswordNotAccepted(
                correlationId,
                "invalid_password",
                "Rejected",
                subError,
                errorCodes
            ),
            NativeAuthV2SubmitNewPasswordCommand::class
        )
        val rejected = state.submitNewPassword("Password!".toCharArray()) as SubmitNewPasswordErrorV2
        assertTrue(rejected.isInvalidPassword())
        assertEquals(errorCodes, rejected.errorCodes)
        assertEquals(subError, rejected.subError)

        enqueueResult(
            NativeAuthV2CommandResult.PasswordResetFailed(correlationId, "reset_failed", "Failed"),
            NativeAuthV2SubmitNewPasswordCommand::class
        )
        assertTrue(
            (state.submitNewPassword("Password!".toCharArray()) as SubmitNewPasswordErrorV2)
                .isPasswordResetFailed()
        )

        enqueueResult(
            NativeAuthV2CommandResult.SignInAfterResetPasswordRequired(
                correlationId,
                createContinuationState()
            ),
            NativeAuthV2SubmitNewPasswordCommand::class
        )
        assertTrue(
            state.submitNewPassword("Password!".toCharArray()) is
                NativeAuthResultV2.SignInAfterResetPasswordRequired
        )

        enqueueResult(apiError(), NativeAuthV2SubmitNewPasswordCommand::class)
        assertEquals(
            errorCodes,
            (state.submitNewPassword("Password!".toCharArray()) as SubmitNewPasswordErrorV2).errorCodes
        )

        enqueueResult(
            INativeAuthCommandResult.Redirect(correlationId, "browser"),
            NativeAuthV2SubmitNewPasswordCommand::class
        )
        assertTrue(
            (state.submitNewPassword("Password!".toCharArray()) as SubmitNewPasswordErrorV2)
                .isBrowserRequired()
        )

        enqueueResult(
            NativeAuthV2CommandResult.NotImplemented(correlationId, "unsupported", "unsupported"),
            NativeAuthV2SubmitNewPasswordCommand::class
        )
        assertTrue(
            (state.submitNewPassword("Password!".toCharArray()) as NativeAuthErrorV2)
                .isNotImplemented()
        )

        enqueueResult(
            NativeAuthV2CommandResult.Complete(correlationId, null, null, null),
            NativeAuthV2SubmitNewPasswordCommand::class
        )
        assertTrue(state.submitNewPassword("Password!".toCharArray()) is NativeAuthErrorV2)

        enqueueCancellation(NativeAuthV2SubmitNewPasswordCommand::class)
        assertCancellation {
            state.submitNewPassword("Password!".toCharArray())
        }
    }

    @Test
    fun submitNewPasswordRejectsEmptyInputWithRetryableInvalidPasswordError() = runTest {
        val codeState = codeRequiredState()
        enqueueResult(
            NativeAuthV2CommandResult.NewPasswordRequired(correlationId, createContinuationState()),
            NativeAuthV2SubmitCodeCommand::class
        )
        val state =
            (codeState.submitCode("123456") as NativeAuthResultV2.NewPasswordRequired).nextState

        val error = state.submitNewPassword(charArrayOf()) as SubmitNewPasswordErrorV2

        assertTrue(error.isInvalidPassword())
    }

    /**
     * Regression test for password cleanup ownership: the caller-provided array must be overwritten
     * even when the request fails or is cancelled before Common's interactor starts processing,
     * because Common's own clearing only runs once the interactor receives the request.
     */
    @Test
    fun submitNewPasswordClearsCallerPasswordWhenItFailsBeforeCommonProcessesIt() = runBlocking {
        val codeState = codeRequiredState()
        enqueueResult(
            NativeAuthV2CommandResult.NewPasswordRequired(correlationId, createContinuationState()),
            NativeAuthV2SubmitCodeCommand::class
        )
        val state =
            (codeState.submitCode("123456") as NativeAuthResultV2.NewPasswordRequired).nextState

        // Dispatcher submission fails before Common ever receives the request.
        every {
            CommandDispatcher.submitSilentReturningFuture(
                match { NativeAuthV2SubmitNewPasswordCommand::class.java.isInstance(it) }
            )
        } throws IllegalStateException("dispatcher unavailable")
        val dispatchFailurePassword = "Password!".toCharArray()
        val error = state.submitNewPassword(dispatchFailurePassword) as SubmitNewPasswordErrorV2
        assertEquals(ErrorTypes.CLIENT_EXCEPTION, error.errorType)
        assertPasswordCleared(dispatchFailurePassword)

        // Cancellation before Common starts processing.
        enqueueCancellation(NativeAuthV2SubmitNewPasswordCommand::class)
        val cancelledPassword = "Password!".toCharArray()
        assertCancellation { state.submitNewPassword(cancelledPassword) }
        assertPasswordCleared(cancelledPassword)

        // Null-continuation fallback returns before any command parameters are created.
        val notImplementedPassword = "Password!".toCharArray()
        val notImplemented = NewPasswordRequiredStateV2(
            "continuation-token",
            correlationId,
            NativeAuthFlowScenarioV2.RESET_PASSWORD,
            NativeAuthPublicClientApplicationConfiguration()
        ).submitNewPassword(notImplementedPassword)
        assertTrue((notImplemented as NativeAuthErrorV2).isNotImplemented())
        assertPasswordCleared(notImplementedPassword)
    }

    @Test
    fun signInAfterResetMapsErrorsAndCancellation() = runBlocking {
        val signInState = signInAfterResetState()

        val localResult = mockk<ILocalAuthenticationResult>()
        val authenticationResult = mockk<IAuthenticationResult>()
        every { authenticationResult.account } returns mockk<IAccount>()
        mockkStatic(AuthenticationResultAdapter::class)
        try {
            every { AuthenticationResultAdapter.adapt(localResult) } returns authenticationResult
            enqueueResult(
                NativeAuthV2CommandResult.Complete(correlationId, localResult, null, null),
                NativeAuthV2SignInAfterResetPasswordCommand::class
            )
            val complete = signInState.signIn() as NativeAuthResultV2.Complete
            assertEquals(correlationId, complete.resultValue.correlationId)
        } finally {
            unmockkStatic(AuthenticationResultAdapter::class)
        }

        enqueueResult(apiError(), NativeAuthV2SignInAfterResetPasswordCommand::class)
        assertEquals(errorCodes, (signInState.signIn() as NativeAuthErrorV2).errorCodes)

        enqueueResult(
            INativeAuthCommandResult.Redirect(correlationId, "browser"),
            NativeAuthV2SignInAfterResetPasswordCommand::class
        )
        assertTrue((signInState.signIn() as NativeAuthErrorV2).isBrowserRequired())

        enqueueResult(
            NativeAuthV2CommandResult.Complete(correlationId, null, null, null),
            NativeAuthV2SignInAfterResetPasswordCommand::class
        )
        assertTrue(signInState.signIn() is NativeAuthErrorV2)

        enqueueCancellation(NativeAuthV2SignInAfterResetPasswordCommand::class)
        assertCancellation { signInState.signIn() }
    }

    @Test
    fun v2SuspendCommandsCancelPendingCommandAndHttpSignal() = runBlocking {
        val codeState = codeRequiredState()
        val passwordState = NewPasswordRequiredStateV2(
            createContinuationState(),
            NativeAuthFlowScenarioV2.RESET_PASSWORD,
            codeState.config
        )
        val signInState = SignInAfterResetPasswordStateV2(
            createContinuationState(),
            NativeAuthFlowScenarioV2.RESET_PASSWORD,
            codeState.config
        )

        assertCommandWaitCancellation(NativeAuthV2ResetPasswordStartCommand::class) {
            application.resetPasswordV2(NativeAuthResetPasswordParameters(username))
        }
        assertCommandWaitCancellation(NativeAuthV2SubmitCodeCommand::class) {
            codeState.submitCode("123456")
        }
        assertCommandWaitCancellation(NativeAuthV2ResendCodeCommand::class) {
            codeState.resendCode()
        }
        assertCommandWaitCancellation(NativeAuthV2SubmitNewPasswordCommand::class) {
            passwordState.submitNewPassword("Password!".toCharArray())
        }
        assertCommandWaitCancellation(NativeAuthV2SignInAfterResetPasswordCommand::class) {
            signInState.signIn()
        }
    }

    @Test
    fun signInV2RoutesExceptionToOnError() {
        assertRoutesToOnError { future ->
            application.signInV2(NativeAuthSignInParameters(username = username), throwingCallback(future))
        }
    }

    @Test
    fun signUpV2RoutesExceptionToOnError() {
        assertRoutesToOnError { future ->
            application.signUpV2(NativeAuthSignUpParameters(username = username), throwingCallback(future))
        }
    }

    @Test
    fun resetPasswordV2RoutesExceptionToOnError() {
        assertRoutesToOnError { future ->
            application.resetPasswordV2(NativeAuthResetPasswordParameters(username = username), throwingCallback(future))
        }
    }

    /**
     * Drives the error path of the application-level V2 callback overloads: when result delivery
     * throws an [MsalException], the `catch` block must route it to the callback's `onError`. This
     * covers the `catch`/`onError` branch of the callback overloads.
     */
    private fun assertRoutesToOnError(action: (ResultFuture<NativeAuthResultV2>) -> Unit) {
        val future = ResultFuture<NativeAuthResultV2>()
        action(future)
        try {
            future.get(30, TimeUnit.SECONDS)
            fail("Expected the exception to be routed to onError")
        } catch (e: ExecutionException) {
            assertTrue(e.cause is MsalClientException)
        }
    }

    private fun throwingCallback(future: ResultFuture<NativeAuthResultV2>): NativeAuthPublicClientApplication.NativeAuthV2Callback {
        return object : NativeAuthPublicClientApplication.NativeAuthV2Callback {
            override fun onResult(result: NativeAuthResultV2): Unit = throw MsalClientException("test_error", "boom")
            override fun onError(exception: BaseException) = future.setException(exception)
        }
    }

    @Test
    fun flowStateStepsReturnNotImplemented() = runTest {
        val config = NativeAuthPublicClientApplicationConfiguration()
        val scenario = NativeAuthFlowScenarioV2.SIGN_IN
        val attributes = com.microsoft.identity.nativeauth.UserAttributes.Builder().city("city").build()
        val authMethod = com.microsoft.identity.nativeauth.AuthMethod("id", "oob", null, "email")

        val codeRequired = CodeRequiredStateV2("continuation-token", "correlation-id", scenario, config)
        assertInvalidState(codeRequired.submitCode("1234"))
        assertInvalidState(codeRequired.resendCode())

        assertInvalidState(PasswordRequiredStateV2("continuation-token", "correlation-id", scenario, config).submitPassword("password".toCharArray()))
        assertNotImplemented(NewPasswordRequiredStateV2("continuation-token", "correlation-id", scenario, config).submitNewPassword("password".toCharArray()))
        assertNotImplemented(SignInAfterResetPasswordStateV2("continuation-token", "correlation-id", scenario, config).signIn())
        assertNotImplemented(AttributesRequiredStateV2("continuation-token", "correlation-id", scenario, config).submitAttributes(attributes))
        assertNotImplemented(AttributesInvalidStateV2("continuation-token", "correlation-id", scenario, config).submitAttributes(attributes))
        assertInvalidState(MFARequiredStateV2("continuation-token", "correlation-id", scenario, config).selectAuthMethod(authMethod))
        assertInvalidState(MFAVerificationRequiredStateV2("continuation-token", "correlation-id", scenario, config).submitChallenge("challenge"))
        assertInvalidState(MFAVerificationRequiredStateV2("continuation-token", "correlation-id", scenario, config).resendChallenge())
        assertNotImplemented(StrongAuthRegistrationRequiredStateV2("continuation-token", "correlation-id", scenario, config).selectAuthMethod(authMethod))
        assertNotImplemented(StrongAuthVerificationRequiredStateV2("continuation-token", "correlation-id", scenario, config).submitChallenge("challenge"))
    }

    @Test
    fun signInAfterResetPasswordRequiredIsResult() {
        val state = SignInAfterResetPasswordStateV2(
            continuationToken = "continuation-token",
            correlationId = "correlation-id",
            scenario = NativeAuthFlowScenarioV2.RESET_PASSWORD,
            config = NativeAuthPublicClientApplicationConfiguration()
        )

        val result = NativeAuthResultV2.SignInAfterResetPasswordRequired(
            nextState = state,
            scenario = NativeAuthFlowScenarioV2.RESET_PASSWORD
        )

        assertTrue(result is NativeAuthResultV2)
        assertEquals("signInAfterResetPassword", exhaustiveWhen(result))
    }

    @Test
    fun errorIsResultAndNotBrowserRequired() {
        val error = NativeAuthErrorV2(
            errorType = "not_implemented",
            errorMessage = "This is not implemented yet",
            correlationId = "correlation-id",
            scenario = NativeAuthFlowScenarioV2.UNKNOWN
        )
        assertTrue(error is NativeAuthResultV2)
        assertTrue(error.isNotImplemented())
        assertFalse(error.isBrowserRequired())
    }

    private fun assertNotImplemented(
        result: NativeAuthResultV2,
        expectedScenario: NativeAuthFlowScenarioV2 = NativeAuthFlowScenarioV2.SIGN_IN
    ): NativeAuthErrorV2 {
        assertEquals(expectedScenario, result.scenario)
        assertTrue(result is NativeAuthErrorV2)
        val error = result as NativeAuthErrorV2
        assertTrue(error.isNotImplemented())
        return error
    }

    private fun assertInvalidState(result: NativeAuthResultV2) {
        assertTrue(result is NativeAuthErrorV2)
        assertEquals(ErrorTypes.INVALID_STATE, (result as NativeAuthErrorV2).errorType)
    }

    private suspend fun codeRequiredState(): CodeRequiredStateV2 {
        enqueueResult(
            NativeAuthV2CommandResult.CodeRequired(
                correlationId,
                createContinuationState(),
                6,
                "email",
                "email"
            ),
            NativeAuthV2ResetPasswordStartCommand::class
        )
        return (application.resetPasswordV2(NativeAuthResetPasswordParameters(username)) as
            NativeAuthResultV2.CodeRequired).nextState
    }

    private suspend fun signInAfterResetState(): SignInAfterResetPasswordStateV2 {
        val codeState = codeRequiredState()
        enqueueResult(
            NativeAuthV2CommandResult.NewPasswordRequired(correlationId, createContinuationState()),
            NativeAuthV2SubmitCodeCommand::class
        )
        val passwordState =
            (codeState.submitCode("123456") as NativeAuthResultV2.NewPasswordRequired).nextState
        enqueueResult(
            NativeAuthV2CommandResult.SignInAfterResetPasswordRequired(
                correlationId,
                createContinuationState()
            ),
            NativeAuthV2SubmitNewPasswordCommand::class
        )
        return (passwordState.submitNewPassword("Password!".toCharArray()) as
            NativeAuthResultV2.SignInAfterResetPasswordRequired).nextState
    }

    private fun apiError() = INativeAuthCommandResult.APIError(
        "api_error",
        "API failed",
        correlationId = correlationId,
        errorCodes = errorCodes
    )

    private fun assertPasswordCleared(password: CharArray) {
        assertTrue(
            "The caller-provided password array was not overwritten",
            password.all { it == '\u0000' }
        )
    }

    private suspend fun assertCancellation(block: suspend () -> Unit) {
        try {
            block()
            fail("Expected CancellationException")
        } catch (_: CancellationException) {
            // Expected.
        }
    }

    private fun enqueueCancellation(commandClass: KClass<out BaseCommand<*>>) {
        every {
            CommandDispatcher.submitSilentReturningFuture(
                match { commandClass.java.isInstance(it) }
            )
        } throws CancellationException("cancelled")
    }

    private suspend fun assertCommandWaitCancellation(
        commandClass: KClass<out BaseCommand<*>>,
        block: suspend () -> Unit
    ) {
        val waitStarted = CountDownLatch(1)
        val future = object : FinalizableResultFuture<CommandResult<Any>>() {
            override fun get(): CommandResult<Any> {
                waitStarted.countDown()
                return super.get()
            }
        }
        every {
            CommandDispatcher.submitSilentReturningFuture(
                match { commandClass.java.isInstance(it) }
            )
        } returns future

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val job = scope.launch { block() }
        assertTrue("Future.get() was not entered", waitStarted.await(10, TimeUnit.SECONDS))
        job.cancel()
        val completedPromptly = withTimeoutOrNull(1_000) {
            job.join()
            true
        } ?: false
        val signalCancelled = future.cancellationSignal.isCancelled

        if (!completedPromptly) {
            future.setResult(
                CommandResult(
                    ICommandResult.ResultStatus.COMPLETED,
                    NativeAuthV2CommandResult.NotImplemented(
                        correlationId,
                        "cancelled",
                        "cancelled"
                    ) as Any,
                    correlationId
                )
            )
            job.join()
        }
        scope.cancel()

        assertTrue("Coroutine remained blocked in Future.get()", completedPromptly)
        assertTrue("HTTP cancellation signal was not cancelled", signalCancelled)
    }

    private fun enqueueResult(
        result: INativeAuthCommandResult,
        commandClass: KClass<out BaseCommand<*>>
    ) {
        val future = FinalizableResultFuture<CommandResult<Any>>()
        future.setResult(
            CommandResult(
                ICommandResult.ResultStatus.COMPLETED,
                result as Any,
                result.correlationId
            )
        )
        every {
            CommandDispatcher.submitSilentReturningFuture(
                match { commandClass.java.isInstance(it) }
            )
        } returns future
    }

    private fun createContinuationState(): NativeAuthV2ContinuationState {
        val constructor = NativeAuthV2ContinuationState::class.java.declaredConstructors
            .single { it.parameterCount == 8 }
        constructor.isAccessible = true
        return constructor.newInstance(
            "opaque-token",
            emptyMap<String, String>(),
            emptyMap<String, Map<String, String>>(),
            listOf("scope"),
            null,
            correlationId,
            NativeAuthV2LinkRelation.RESET_PASSWORD.value,
            NativeAuthV2FlowScenario.RESET_PASSWORD
        ) as NativeAuthV2ContinuationState
    }

    @Suppress("unused")
    private fun exhaustiveWhen(result: NativeAuthResultV2): String = when (result) {
        is NativeAuthResultV2.Complete -> "complete"
        is NativeAuthResultV2.CodeRequired -> "code"
        is NativeAuthResultV2.PasswordRequired -> "password"
        is NativeAuthResultV2.NewPasswordRequired -> "newPassword"
        is NativeAuthResultV2.SignInAfterResetPasswordRequired -> "signInAfterResetPassword"
        is NativeAuthResultV2.AttributesRequired -> "attributes"
        is NativeAuthResultV2.AttributesInvalid -> "attributesInvalid"
        is NativeAuthResultV2.MFARequired -> "mfa"
        is NativeAuthResultV2.MFAVerificationRequired -> "mfaVerification"
        is NativeAuthResultV2.StrongAuthRegistrationRequired -> "strongAuthRegistration"
        is NativeAuthResultV2.StrongAuthVerificationRequired -> "strongAuthVerification"
        is NativeAuthErrorV2 -> "error"
        else -> "unknown"
    }

    private companion object {
        const val correlationId = "correlation-id"
        const val subError = "sub-error"
        val errorCodes = listOf(50001, 50002)
    }
}
