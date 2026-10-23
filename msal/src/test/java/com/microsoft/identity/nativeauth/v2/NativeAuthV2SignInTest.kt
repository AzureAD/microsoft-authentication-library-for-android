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
import android.os.Parcel
import androidx.test.core.app.ApplicationProvider
import com.microsoft.identity.client.AuthenticationResultAdapter
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.e2e.shadows.ShadowAndroidSdkStorageEncryptionManager
import com.microsoft.identity.client.e2e.tests.PublicClientApplicationAbstractTest
import com.microsoft.identity.client.exception.MsalClientException
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.common.java.controllers.CommandDispatcher
import com.microsoft.identity.common.java.commands.BaseCommand
import com.microsoft.identity.common.java.commands.ICommandResult
import com.microsoft.identity.common.java.controllers.CommandResult
import com.microsoft.identity.common.java.eststelemetry.PublicApiId
import com.microsoft.identity.common.java.exception.BaseException
import com.microsoft.identity.common.java.logging.DiagnosticContext
import com.microsoft.identity.common.java.nativeauth.commands.parameters.NativeAuthV2ResendCodeCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.NativeAuthV2SignInAfterSignUpCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.NativeAuthV2SubmitCodeCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.NativeAuthV2SubmitPasswordCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInV2StartCommandParameters
import com.microsoft.identity.common.java.nativeauth.controllers.results.INativeAuthCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2CommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2ResendCodeCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2SelectMFAMethodCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2SignInStartCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2SignInSubmitCodeCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2SubmitMFAChallengeCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2SubmitPasswordCommandResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2AuthMethod
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationState
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationStateTestFactory
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2LinkRelation
import com.microsoft.identity.common.java.nativeauth.providers.v2.NativeAuthV2FlowScenario
import com.microsoft.identity.common.java.result.FinalizableResultFuture
import com.microsoft.identity.common.java.result.ILocalAuthenticationResult
import com.microsoft.identity.common.nativeauth.internal.commands.NativeAuthV2ResendCodeCommand
import com.microsoft.identity.common.nativeauth.internal.commands.NativeAuthV2SelectMFAMethodCommand
import com.microsoft.identity.common.nativeauth.internal.commands.NativeAuthV2SignInSubmitCodeCommand
import com.microsoft.identity.common.nativeauth.internal.commands.NativeAuthV2SignInStartCommand
import com.microsoft.identity.common.nativeauth.internal.commands.NativeAuthV2SubmitMFAChallengeCommand
import com.microsoft.identity.common.nativeauth.internal.commands.NativeAuthV2SubmitPasswordCommand
import com.microsoft.identity.common.nativeauth.internal.commands.NativeAuthV2EnrollCommand
import com.microsoft.identity.common.nativeauth.internal.commands.NativeAuthV2ActivateCommand
import com.microsoft.identity.common.nativeauth.internal.controllers.v2.NativeAuthV2FlowController
import com.microsoft.identity.common.java.nativeauth.commands.parameters.NativeAuthV2EnrollCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.NativeAuthV2ActivateCommandParameters
import com.microsoft.identity.nativeauth.statemachine.states.StrongAuthRegistrationRequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.StrongAuthVerificationRequiredStateV2
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import io.mockk.slot
import io.mockk.spyk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.util.concurrent.CountDownLatch
import com.microsoft.identity.common.nativeauth.internal.commands.NativeAuthV2ResetPasswordStartCommand
import com.microsoft.identity.common.nativeauth.internal.commands.NativeAuthV2SelectResetPasswordMethodCommand
import com.microsoft.identity.common.nativeauth.internal.commands.NativeAuthV2ResetPasswordSubmitCodeCommand
import com.microsoft.identity.common.nativeauth.internal.commands.NativeAuthV2SubmitNewPasswordCommand
import com.microsoft.identity.common.nativeauth.internal.commands.NativeAuthV2SignUpStartCommand
import com.microsoft.identity.common.nativeauth.internal.commands.NativeAuthV2SignUpSubmitCodeCommand
import com.microsoft.identity.common.nativeauth.internal.commands.NativeAuthV2SubmitAttributesCommand
import com.microsoft.identity.common.java.nativeauth.commands.parameters.NativeAuthV2SubmitAttributesCommandParameters
import com.microsoft.identity.nativeauth.AuthMethod
import com.microsoft.identity.nativeauth.UserAttributes
import com.microsoft.identity.nativeauth.parameters.NativeAuthSignUpParameters
import com.microsoft.identity.nativeauth.parameters.NativeAuthResetPasswordParameters
import com.microsoft.identity.nativeauth.statemachine.states.NewPasswordRequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.ResetPasswordMethodRequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.AttributesRequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.AttributesInvalidStateV2
import com.microsoft.identity.nativeauth.INativeAuthPublicClientApplication
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplication
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplicationConfiguration
import com.microsoft.identity.nativeauth.parameters.NativeAuthSignInParameters
import com.microsoft.identity.nativeauth.statemachine.NativeAuthFlowScenarioV2
import com.microsoft.identity.nativeauth.statemachine.errors.ErrorTypes
import com.microsoft.identity.nativeauth.statemachine.errors.NativeAuthErrorV2
import com.microsoft.identity.nativeauth.statemachine.results.NativeAuthResultV2
import com.microsoft.identity.nativeauth.statemachine.states.Callback
import com.microsoft.identity.nativeauth.statemachine.states.CodeRequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.MFARequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.MFAVerificationRequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.PasswordRequiredStateV2
import com.microsoft.identity.common.java.util.ResultFuture
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
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
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.seconds

/**
 * Parity coverage for Native Auth V2 sign-in: email one-time-code or password first factor,
 * followed by the existing MFA flow when required.
 * Strong-auth enrollment interruptions also cover their shared sign-up and reset continuations.
 *
 * Every scenario is driven through mocked Common command results, so the whole public surface is
 * exercised without a service. The live end-to-end equivalents are gated separately on a
 * V2-capable slice.
 *
 * The scenarios mirror the in-scope iOS V2 cases: email-code and password success, unknown user,
 * invalid entry and deferred passwords, invalid OTP recovery, MFA transitions, browser-required,
 * and the Android V1 existing-account rejection.
 */
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowAndroidSdkStorageEncryptionManager::class])
class NativeAuthV2SignInTest : PublicClientApplicationAbstractTest() {

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

    // -----------------------------------------------------------------------------------------
    // Password first factor
    // -----------------------------------------------------------------------------------------

    @Test
    fun signInV2MapsStrongAuthRegistrationWithOfferedMethods() = runTest {
        val state = createContinuationState()
        enqueueResult(
            NativeAuthV2CommandResult.StrongAuthRegistrationRequired(
                correlationId, state,
                listOf(NativeAuthV2AuthMethod("sms-1", "sms", null))
            ),
            NativeAuthV2SignInStartCommand::class
        )

        val result = application.signInV2(signInParameters()) as NativeAuthResultV2.StrongAuthRegistrationRequired
        assertEquals(NativeAuthFlowScenarioV2.SIGN_IN, result.scenario)
        assertEquals("sms-1", result.authMethods.single().id)
        assertEquals("sms", result.authMethods.single().challengeChannel)
        assertEquals(state, result.nextState.continuationState)
    }

    @Test
    fun enrollmentUsesRealCommandAndOfferedChannelAfterParcel() = runTest(timeout = 60.seconds) {
        mockkConstructor(NativeAuthV2FlowController::class)
        try {
            val captured = slot<NativeAuthV2EnrollCommandParameters>()
            val next = createContinuationState("enrolled-correlation")
            val commonResult = NativeAuthV2CommandResult.StrongAuthVerificationRequired(
                next.correlationId, next, 7, "***1234"
            )
            every { anyConstructed<NativeAuthV2FlowController>().enroll(capture(captured)) } returns commonResult
            enqueueResult(commonResult, NativeAuthV2EnrollCommand::class) { command ->
                assertEquals(PublicApiId.NATIVE_AUTH_JIT_CHALLENGE_AUTH_METHOD, command.publicApiId)
                assertEquals(commonResult, command.execute())
            }
            val registration = strongAuthRegistration()
            val restored = parcelRoundTrip(registration.nextState, StrongAuthRegistrationRequiredStateV2.CREATOR)
            assertEquals(registration.authMethods.single().id, restored.authMethods.single().id)
            assertEquals(registration.authMethods.single().challengeChannel, restored.authMethods.single().challengeChannel)
            assertEquals(correlationId, restored.continuationState?.correlationId)
            // Existing Native Auth serialization omits the inherited PCA configuration/runtime.
            restored.config.mergeConfiguration(registration.nextState.config)
            restored.config.appContext = registration.nextState.config.appContext
            restored.config.oAuth2TokenCache = registration.nextState.config.oAuth2TokenCache
            // The caller cannot change the offered channel by copying the selected method.
            val selected = registration.authMethods.single().copy(challengeChannel = "email")
            val raw = restored.selectAuthMethod(selected, "+1 555 1234")
            assertTrue("Enrollment failed: ${(raw as? NativeAuthErrorV2)?.exception}", raw is NativeAuthResultV2.StrongAuthVerificationRequired)
            val result = raw as NativeAuthResultV2.StrongAuthVerificationRequired
            assertEquals("sms", result.channel)
            assertEquals(7, result.codeLength)
            assertEquals("***1234", result.sentTo)
            assertEquals(next, result.nextState.continuationState)
            assertEquals("sms-1", captured.captured.methodId)
            assertEquals("+1 555 1234", captured.captured.contact)
            assertEquals(correlationId, captured.captured.continuationState.correlationId)
            assertEquals(correlationId, captured.captured.tokenParameters.correlationId)
            assertEquals(restored.config.clientId, captured.captured.tokenParameters.clientId)
        } finally {
            unmockkConstructor(NativeAuthV2FlowController::class)
        }
    }

    @Test
    fun enrollmentRejectsBlankContactAndUnofferedMethodWithoutCommand() = runTest {
        val registration = strongAuthRegistration()
        for (contact in listOf(null, "", "  ")) {
            val error = registration.nextState.selectAuthMethod(registration.authMethods.single(), contact) as NativeAuthErrorV2
            assertTrue(error.isInvalidInput())
        }
        val error = registration.nextState.selectAuthMethod(
            registration.authMethods.single().copy(id = "not-offered"), "user@example.com"
        ) as NativeAuthErrorV2
        assertTrue(error.isInvalidInput())
        verify(exactly = 0) {
            CommandDispatcher.submitSilentReturningFuture(match { it is NativeAuthV2EnrollCommand })
        }
    }

    @Test
    fun activationUsesRealCommandAndDoesNotValidateOtpFormatAfterParcel() = runTest(timeout = 60.seconds) {
        mockkConstructor(NativeAuthV2FlowController::class)
        try {
            val captured = slot<NativeAuthV2ActivateCommandParameters>()
            val incorrect = NativeAuthV2CommandResult.IncorrectCode(correlationId, "invalidGrant", "Invalid code", "invalidOneTimeCode")
            every { anyConstructed<NativeAuthV2FlowController>().activate(capture(captured)) } returns incorrect
            val state = strongAuthVerification()
            val restored = parcelRoundTrip(state, StrongAuthVerificationRequiredStateV2.CREATOR)
            assertEquals("sms", restored.selectedChannel)
            restored.config.mergeConfiguration(state.config)
            restored.config.appContext = state.config.appContext
            restored.config.oAuth2TokenCache = state.config.oAuth2TokenCache
            enqueueResult(incorrect, NativeAuthV2ActivateCommand::class) { command ->
                assertEquals(PublicApiId.NATIVE_AUTH_JIT_SUBMIT_CHALLENGE, command.publicApiId)
                assertEquals(incorrect, command.execute())
            }
            val error = restored.submitChallenge(" not-a-numeric-code ") as NativeAuthErrorV2
            assertTrue("Activation failed: ${error.exception}", error.isInvalidChallenge())
            assertEquals(" not-a-numeric-code ", captured.captured.code)
            assertEquals(restored.continuationState, captured.captured.continuationState)
            enqueueResult(
                NativeAuthV2CommandResult.MFARequired(correlationId, createContinuationState(), emptyList()),
                NativeAuthV2ActivateCommand::class
            )
            assertTrue(restored.submitChallenge("retry") is NativeAuthResultV2.MFARequired)
        } finally {
            unmockkConstructor(NativeAuthV2FlowController::class)
        }
    }

    @Test
    fun jitResendRetainsScenarioAndChannelAfterParcel() = runTest(timeout = 60.seconds) {
        val original = strongAuthVerification()
        for (scenario in listOf(NativeAuthFlowScenarioV2.SIGN_IN, NativeAuthFlowScenarioV2.SIGN_UP, NativeAuthFlowScenarioV2.RESET_PASSWORD)) {
            for (channel in listOf("sms", "email")) {
                val state = StrongAuthVerificationRequiredStateV2(
                    correlationId, scenario, original.config, original.continuationState, channel)
                val restored = parcelRoundTrip(state, StrongAuthVerificationRequiredStateV2.CREATOR)
                restored.config.mergeConfiguration(state.config)
                restored.config.appContext = state.config.appContext
                restored.config.oAuth2TokenCache = state.config.oAuth2TokenCache
                val fresh = NativeAuthV2ContinuationStateTestFactory.create(
                    "resent", scenario = NativeAuthV2FlowScenario.valueOf(scenario.name))
                val common = NativeAuthV2CommandResult.StrongAuthVerificationRequired("resent", fresh, 8, "")
                enqueueResult(common, NativeAuthV2ResendCodeCommand::class) { command ->
                    assertEquals(PublicApiId.NATIVE_AUTH_JIT_RESEND_CHALLENGE, command.publicApiId)
                    assertSame(restored.continuationState,
                        (command.parameters as NativeAuthV2ResendCodeCommandParameters).continuationState)
                }
                val raw = restored.resendChallenge()
                assertTrue("Resend failed for $scenario/$channel: ${(raw as? NativeAuthErrorV2)?.exception}",
                    raw is NativeAuthResultV2.StrongAuthVerificationRequired)
                val result = raw as NativeAuthResultV2.StrongAuthVerificationRequired
                assertSame(fresh, result.nextState.continuationState)
                assertEquals(scenario, result.scenario)
                assertEquals(scenario, result.nextState.scenario)
                assertEquals(channel, result.channel)
                assertEquals(channel, result.nextState.selectedChannel)
                assertEquals("", result.sentTo)
                assertEquals(8, result.codeLength)
                enqueueResult(
                    NativeAuthV2CommandResult.IncorrectCode("resent", "invalidGrant", "Retry", "invalidOneTimeCode"),
                    NativeAuthV2ActivateCommand::class
                ) { command ->
                    assertSame(fresh, (command.parameters as NativeAuthV2SignInAfterSignUpCommandParameters).continuationState)
                }
                val activationError = result.nextState.submitChallenge("retry") as NativeAuthErrorV2
                assertTrue("Activation after resend failed for $scenario/$channel: ${activationError.errorMessage}; ${activationError.exception}",
                    activationError.isInvalidChallenge())
                enqueueResult(common, NativeAuthV2ResendCodeCommand::class) { command ->
                    assertSame(fresh, (command.parameters as NativeAuthV2ResendCodeCommandParameters).continuationState)
                }
                assertTrue(result.nextState.resendChallenge() is NativeAuthResultV2.StrongAuthVerificationRequired)
            }
        }
    }

    @Test
    fun jitResendPreservesErrorsAndRejectsUnexpectedSuccess() = runTest {
        val state = strongAuthVerification()
        enqueueResult(
            INativeAuthCommandResult.APIError(
                correlationId = correlationId, error = "invalidRequest", errorDescription = "Try later",
                traceId = "trace", timestamp = "time", errorCodes = listOf(42), subError = "serviceSpecific"
            ),
            NativeAuthV2ResendCodeCommand::class
        )
        val error = state.resendChallenge() as NativeAuthErrorV2
        assertEquals("invalidRequest", error.error)
        assertEquals("serviceSpecific", error.subError)
        assertEquals("trace", error.traceId)
        assertEquals("time", error.timestamp)
        assertEquals(listOf(42), error.errorCodes)
        enqueueResult(INativeAuthCommandResult.Redirect(correlationId, "Use browser"), NativeAuthV2ResendCodeCommand::class)
        assertTrue((state.resendChallenge() as NativeAuthErrorV2).isBrowserRequired())
        enqueueResult(emailCodeRequiredResult(), NativeAuthV2ResendCodeCommand::class)
        assertEquals(ErrorTypes.INVALID_STATE, (state.resendChallenge() as NativeAuthErrorV2).errorType)
    }

    @Test
    fun jitResendReturnsExplicitClientFailureAndPropagatesCancellation() = runTest {
        val state = strongAuthVerification()
        val failure = com.microsoft.identity.common.java.exception.ClientException(
            com.microsoft.identity.common.java.exception.ClientException.MISSING_PARAMETER,
            "Native Auth V2 continuation state is missing the required 'resend' relation.")
        every {
            CommandDispatcher.submitSilentReturningFuture(ofType<NativeAuthV2ResendCodeCommand>())
        } throws failure
        val result = state.resendChallenge() as NativeAuthErrorV2
        assertEquals(ErrorTypes.CLIENT_EXCEPTION, result.errorType)
        assertSame(failure, result.exception)
        enqueueCancellation(NativeAuthV2ResendCodeCommand::class)
        try {
            state.resendChallenge()
            fail("Expected cancellation")
        } catch (_: CancellationException) {
            // Cancellation must not be converted into an authentication error.
        }
    }

    @Test
    fun jitResendCallbackReturnsFreshStateAndMissingStateDoesNotDispatch() = runTest {
        val state = strongAuthVerification()
        val missing = StrongAuthVerificationRequiredStateV2(correlationId, state.scenario, state.config)
        val missingResult = missing.resendChallenge() as NativeAuthErrorV2
        assertEquals(ErrorTypes.INVALID_STATE, missingResult.errorType)
        verify(exactly = 0) {
            CommandDispatcher.submitSilentReturningFuture(ofType<NativeAuthV2ResendCodeCommand>())
        }
        val fresh = createContinuationState("callback-resend")
        enqueueResult(
            NativeAuthV2CommandResult.StrongAuthVerificationRequired("callback-resend", fresh, 7, "updated"),
            NativeAuthV2ResendCodeCommand::class
        )
        val callbackResult = ResultFuture<NativeAuthResultV2>()
        state.resendChallenge(object : StrongAuthVerificationRequiredStateV2.ResendChallengeCallback {
            override fun onResult(result: NativeAuthResultV2) = callbackResult.setResult(result)
            override fun onError(exception: BaseException) = callbackResult.setException(exception)
        })
        val result = callbackResult.get(10, TimeUnit.SECONDS) as NativeAuthResultV2.StrongAuthVerificationRequired
        assertSame(fresh, result.nextState.continuationState)
        assertEquals("sms", result.channel)
        assertEquals("updated", result.sentTo)
    }

    @Test
    fun activationMapsEveryOriginatingFlowSuccessor() = runTest {
        val original = strongAuthVerification()
        val state = createContinuationState("next-correlation")
        val successors = listOf(
            NativeAuthV2CommandResult.MFARequired(correlationId, state, emptyList()) to NativeAuthResultV2.MFARequired::class,
            NativeAuthV2CommandResult.NewPasswordRequired(correlationId, state) to NativeAuthResultV2.NewPasswordRequired::class,
            NativeAuthV2CommandResult.SignInAfterResetPasswordRequired(correlationId, state) to NativeAuthResultV2.SignInAfterResetPasswordRequired::class,
            NativeAuthV2CommandResult.SignInAfterSignUpRequired(correlationId, state) to NativeAuthResultV2.SignInAfterSignUpRequired::class,
            NativeAuthV2CommandResult.PasswordRequired(correlationId, state) to NativeAuthResultV2.PasswordRequired::class,
            NativeAuthV2CommandResult.AttributesRequired(correlationId, state, emptyList()) to NativeAuthResultV2.AttributesRequired::class,
            NativeAuthV2CommandResult.AttributesInvalid(correlationId, state, listOf("city"), "invalidRequest", "Invalid city") to NativeAuthResultV2.AttributesInvalid::class,
            NativeAuthV2CommandResult.CodeRequired(correlationId, state, 8, "***", "email") to NativeAuthResultV2.CodeRequired::class,
            NativeAuthV2CommandResult.StrongAuthRegistrationRequired(correlationId, state, emptyList()) to NativeAuthResultV2.StrongAuthRegistrationRequired::class
        )
        for (scenario in listOf(NativeAuthFlowScenarioV2.SIGN_IN, NativeAuthFlowScenarioV2.SIGN_UP, NativeAuthFlowScenarioV2.RESET_PASSWORD)) {
            val verification = StrongAuthVerificationRequiredStateV2(
                correlationId, scenario, original.config, original.continuationState, "sms"
            )
            for ((common, expected) in successors) {
                enqueueResult(common, NativeAuthV2ActivateCommand::class)
                val result = verification.submitChallenge("otp")
                assertTrue("Expected $expected but got ${result.javaClass}", expected.java.isInstance(result))
                assertEquals(scenario, result.scenario)
                val next = (result as com.microsoft.identity.nativeauth.statemachine.results.Result.SuccessResult).nextState as
                    com.microsoft.identity.nativeauth.statemachine.states.NativeAuthBaseStateV2
                assertSame(state, next.continuationState)
                assertEquals(scenario, next.scenario)
            }
        }
    }

    @Test
    fun passwordResetMfaResumesResetWithoutSigningIn() = runTest {
        val config = strongAuthVerification().config
        val reset = NativeAuthFlowScenarioV2.RESET_PASSWORD
        val state = NativeAuthV2ContinuationStateTestFactory.afterPasswordUpdate(
            NativeAuthV2ContinuationStateTestFactory.create(correlationId))
        val method = NativeAuthV2AuthMethod("sms-id", "sms", "***00")
        enqueueResult(
            NativeAuthV2CommandResult.MFARequired(correlationId, state, listOf(method)),
            NativeAuthV2SubmitNewPasswordCommand::class
        )
        val mfa = NewPasswordRequiredStateV2(state, reset, config)
            .submitNewPassword("Password123!".toCharArray()) as NativeAuthResultV2.MFARequired
        assertEquals(reset, mfa.scenario)
        assertSame(state, mfa.nextState.continuationState)
        enqueueResult(
            NativeAuthV2CommandResult.MFAVerificationRequired(correlationId, state, 6, "***00", "sms"),
            NativeAuthV2SelectMFAMethodCommand::class
        )
        val verification = mfa.nextState.selectAuthMethod(mfa.authMethods.single())
            as NativeAuthResultV2.MFAVerificationRequired
        assertEquals(reset, verification.scenario)
        assertSame(state, verification.nextState.continuationState)
        enqueueResult(
            NativeAuthV2CommandResult.SignInAfterResetPasswordRequired(correlationId, state),
            NativeAuthV2SubmitMFAChallengeCommand::class
        )
        val completed = verification.nextState.submitChallenge("123456")
            as NativeAuthResultV2.SignInAfterResetPasswordRequired
        assertEquals(reset, completed.scenario)
        assertSame(state, completed.nextState.continuationState)
        verify(exactly = 0) {
            CommandDispatcher.submitSilentReturningFuture(match {
                it is com.microsoft.identity.common.nativeauth.internal.commands.NativeAuthV2SignInAfterResetPasswordCommand
            })
        }
    }

    @Test
    fun passwordResetMfaCanResumePasswordCollection() = runTest {
        val config = strongAuthVerification().config
        val state = NativeAuthV2ContinuationStateTestFactory.create(correlationId)
        enqueueResult(
            NativeAuthV2CommandResult.NewPasswordRequired(correlationId, state),
            NativeAuthV2SubmitMFAChallengeCommand::class
        )
        val result = MFAVerificationRequiredStateV2(state, NativeAuthFlowScenarioV2.RESET_PASSWORD, config)
            .submitChallenge("123456") as NativeAuthResultV2.NewPasswordRequired
        assertEquals(NativeAuthFlowScenarioV2.RESET_PASSWORD, result.scenario)
        assertSame(state, result.nextState.continuationState)
    }

    @Test
    fun activationMapsTypedErrorsAndRetainsRedactedDiagnostics() = runTest {
        val state = strongAuthVerification()
        val cases = listOf(
            NativeAuthV2CommandResult.UserAlreadyExists(
                correlationId, "invalidRequest", "Account exists", listOf(42), "userAlreadyExists",
                traceId = "exists-trace", timestamp = "exists-time"
            ) to "exists",
            NativeAuthV2CommandResult.PasswordResetFailed(correlationId, "invalidRequest", "Reset failed") to "reset",
            NativeAuthV2CommandResult.IncorrectCode(
                correlationId, "invalidGrant", "Invalid OTP", "invalidOneTimeCode", listOf(7),
                traceId = "code-trace", timestamp = "code-time"
            ) to "code",
            INativeAuthCommandResult.Redirect(
                correlationId, "Use browser", errorCodes = listOf(42),
                traceId = "browser-trace", timestamp = "browser-time"
            ) to "browser",
            INativeAuthCommandResult.APIError(
                correlationId = correlationId, error = "invalidRequest", errorDescription = "Service error",
                traceId = "api-trace", timestamp = "api-time"
            ) to "api"
        )
        for ((common, kind) in cases) {
            enqueueResult(common, NativeAuthV2ActivateCommand::class)
            val error = state.submitChallenge("sensitive-otp") as NativeAuthErrorV2
            assertEquals(correlationId, error.correlationId)
            assertEquals(common.traceId, error.traceId)
            assertEquals(common.timestamp, error.timestamp)
            when (kind) {
                "exists" -> assertTrue(error.isUserAlreadyExists())
                "reset" -> assertTrue(error.isPasswordResetFailed())
                "code" -> assertTrue(error.isInvalidChallenge())
                "browser" -> {
                    assertTrue(error.isBrowserRequired())
                    assertEquals(listOf(42), error.errorCodes)
                }
                "api" -> assertEquals("invalidRequest", error.error)
            }
            assertFalse(error.toString().contains("sensitive-otp"))
        }
    }

    @Test
    fun strongAuthCommandsPropagateCancellation() = runTest {
        val registration = strongAuthRegistration()
        enqueueCancellation(NativeAuthV2EnrollCommand::class)
        try {
            registration.nextState.selectAuthMethod(registration.authMethods.single(), "user@example.com")
            fail("Expected cancellation")
        } catch (_: CancellationException) { }
        val state = strongAuthVerification()
        enqueueCancellation(NativeAuthV2ActivateCommand::class)
        try {
            state.submitChallenge("otp")
            fail("Expected cancellation")
        } catch (_: CancellationException) { }
    }

    @Test
    fun registrationInterruptsEveryAffectedEntryAndContinuationOperation() = runTest {
        val config = strongAuthRegistration().nextState.config
        val state = createContinuationState()
        val method = AuthMethod("email-1", "oob", null, "email")
        val attributes = UserAttributes.Builder().city("Redmond").build()
        suspend fun check(
            scenario: NativeAuthFlowScenarioV2,
            commandClass: KClass<out BaseCommand<*>>,
            operation: suspend () -> NativeAuthResultV2
        ) {
            enqueueResult(
                NativeAuthV2CommandResult.StrongAuthRegistrationRequired(
                    correlationId, state, listOf(NativeAuthV2AuthMethod("sms-1", "sms", null))
                ), commandClass
            )
            val result = operation()
            assertTrue("$commandClass returned ${result.javaClass}", result is NativeAuthResultV2.StrongAuthRegistrationRequired)
            result as NativeAuthResultV2.StrongAuthRegistrationRequired
            assertEquals(scenario, result.scenario)
            assertEquals(scenario, result.nextState.scenario)
            assertSame(state, result.nextState.continuationState)
            assertEquals("sms-1", result.authMethods.single().id)
        }
        check(NativeAuthFlowScenarioV2.SIGN_IN, NativeAuthV2SignInStartCommand::class) {
            application.signInV2(signInParameters())
        }
        check(NativeAuthFlowScenarioV2.SIGN_UP, NativeAuthV2SignUpStartCommand::class) {
            application.signUpV2(NativeAuthSignUpParameters(username))
        }
        check(NativeAuthFlowScenarioV2.RESET_PASSWORD, NativeAuthV2ResetPasswordStartCommand::class) {
            application.resetPasswordV2(NativeAuthResetPasswordParameters(username))
        }
        for (scenario in listOf(NativeAuthFlowScenarioV2.SIGN_IN, NativeAuthFlowScenarioV2.SIGN_UP, NativeAuthFlowScenarioV2.RESET_PASSWORD)) {
            val code = CodeRequiredStateV2(state, scenario, config)
            val commandClass = when (scenario) {
                NativeAuthFlowScenarioV2.SIGN_UP -> NativeAuthV2SignUpSubmitCodeCommand::class
                NativeAuthFlowScenarioV2.RESET_PASSWORD -> NativeAuthV2ResetPasswordSubmitCodeCommand::class
                else -> NativeAuthV2SignInSubmitCodeCommand::class
            }
            check(scenario, commandClass) { code.submitCode("otp") }
            check(scenario, NativeAuthV2ResendCodeCommand::class) { code.resendCode() }
        }
        check(NativeAuthFlowScenarioV2.RESET_PASSWORD, NativeAuthV2SelectResetPasswordMethodCommand::class) {
            ResetPasswordMethodRequiredStateV2(state, listOf(method), config).selectAuthMethod(method)
        }
        // Common also returns this result from password-update polling through the same command.
        check(NativeAuthFlowScenarioV2.RESET_PASSWORD, NativeAuthV2SubmitNewPasswordCommand::class) {
            NewPasswordRequiredStateV2(state, NativeAuthFlowScenarioV2.RESET_PASSWORD, config).submitNewPassword("Password123!".toCharArray())
        }
        check(NativeAuthFlowScenarioV2.SIGN_IN, NativeAuthV2SubmitPasswordCommand::class) {
            PasswordRequiredStateV2(state, NativeAuthFlowScenarioV2.SIGN_IN, config).submitPassword("Password123!".toCharArray())
        }
        check(NativeAuthFlowScenarioV2.SIGN_UP, NativeAuthV2SubmitAttributesCommand::class) {
            PasswordRequiredStateV2(state, NativeAuthFlowScenarioV2.SIGN_UP, config).submitPassword("Password123!".toCharArray())
        }
        check(NativeAuthFlowScenarioV2.SIGN_UP, NativeAuthV2SubmitAttributesCommand::class) {
            AttributesRequiredStateV2(state, NativeAuthFlowScenarioV2.SIGN_UP, config).submitAttributes(attributes)
        }
        check(NativeAuthFlowScenarioV2.SIGN_UP, NativeAuthV2SubmitAttributesCommand::class) {
            AttributesInvalidStateV2(state, NativeAuthFlowScenarioV2.SIGN_UP, config).submitAttributes(attributes)
        }
        check(NativeAuthFlowScenarioV2.SIGN_IN, NativeAuthV2SelectMFAMethodCommand::class) {
            MFARequiredStateV2(state, listOf(method), NativeAuthFlowScenarioV2.SIGN_IN, config).selectAuthMethod(method)
        }
        val mfa = MFAVerificationRequiredStateV2(state, NativeAuthFlowScenarioV2.SIGN_IN, config)
        check(NativeAuthFlowScenarioV2.SIGN_IN, NativeAuthV2SubmitMFAChallengeCommand::class) { mfa.submitChallenge("otp") }
        check(NativeAuthFlowScenarioV2.SIGN_IN, NativeAuthV2ResendCodeCommand::class) { mfa.resendChallenge() }
    }

    @Test
    fun activationResumesSignUpPasswordRecollectionThroughSubmitAttributes() = runTest {
        val config = strongAuthRegistration().nextState.config
        val commonState = NativeAuthV2ContinuationStateTestFactory.create(
            correlationId = correlationId, entryRelation = NativeAuthV2LinkRelation.SIGN_UP,
            scenario = NativeAuthV2FlowScenario.SIGN_UP
        )
        val state = StrongAuthVerificationRequiredStateV2(
            correlationId, NativeAuthFlowScenarioV2.SIGN_UP, config, commonState, "email"
        )
        enqueueResult(NativeAuthV2CommandResult.PasswordRequired(correlationId, commonState), NativeAuthV2ActivateCommand::class)
        val password = state.submitChallenge("otp") as NativeAuthResultV2.PasswordRequired
        assertEquals(NativeAuthFlowScenarioV2.SIGN_UP, password.scenario)
        var submittedPassword: String? = null
        enqueueResult(
            NativeAuthV2CommandResult.SignInAfterSignUpRequired(correlationId, commonState),
            NativeAuthV2SubmitAttributesCommand::class
        ) { command ->
            val parameters = command.parameters as NativeAuthV2SubmitAttributesCommandParameters
            submittedPassword = parameters.password?.let { String(it) }
            assertSame(commonState, parameters.continuationState)
        }
        val callerPassword = "Password123!".toCharArray()
        val next = password.nextState.submitPassword(callerPassword)
        assertTrue(next is NativeAuthResultV2.SignInAfterSignUpRequired)
        assertEquals("Password123!", submittedPassword)
        assertEquals("Password123!", String(callerPassword))
        assertEquals(NativeAuthFlowScenarioV2.SIGN_UP, next.scenario)
    }

    @Test
    fun strongAuthCallbacksReturnSameSuccessStatesAsSuspendApis() = runTest {
        val registration = strongAuthRegistration()
        enqueueResult(
            NativeAuthV2CommandResult.StrongAuthVerificationRequired(correlationId, createContinuationState(), 6, "***"),
            NativeAuthV2EnrollCommand::class
        )
        val enrolled = ResultFuture<NativeAuthResultV2>()
        registration.nextState.selectAuthMethod(registration.authMethods.single(), "user@example.com",
            object : StrongAuthRegistrationRequiredStateV2.SelectAuthMethodCallback {
                override fun onResult(result: NativeAuthResultV2) = enrolled.setResult(result)
                override fun onError(exception: BaseException) = enrolled.setException(exception)
            }
        )
        val verification = enrolled.get(10, TimeUnit.SECONDS) as NativeAuthResultV2.StrongAuthVerificationRequired
        assertEquals("sms", verification.channel)
        enqueueResult(
            NativeAuthV2CommandResult.MFARequired(correlationId, createContinuationState(), emptyList()),
            NativeAuthV2ActivateCommand::class
        )
        val activated = ResultFuture<NativeAuthResultV2>()
        verification.nextState.submitChallenge("otp", object : StrongAuthVerificationRequiredStateV2.SubmitChallengeCallback {
            override fun onResult(result: NativeAuthResultV2) = activated.setResult(result)
            override fun onError(exception: BaseException) = activated.setException(exception)
        })
        assertTrue(activated.get(10, TimeUnit.SECONDS) is NativeAuthResultV2.MFARequired)
    }

    @Test
    fun activationMapsInlineCompletionAndRejectsMissingAuthenticationResult() = runTest {
        val state = strongAuthVerification()
        val localResult = mockk<ILocalAuthenticationResult>()
        val authenticationResult = mockk<IAuthenticationResult>()
        every { authenticationResult.account } returns mockk<IAccount>()
        mockkStatic(AuthenticationResultAdapter::class)
        try {
            every { AuthenticationResultAdapter.adapt(localResult) } returns authenticationResult
            enqueueResult(
                NativeAuthV2CommandResult.Complete(correlationId, localResult, null, null),
                NativeAuthV2ActivateCommand::class
            )
            val result = state.submitChallenge("otp") as NativeAuthResultV2.Complete
            assertEquals(NativeAuthFlowScenarioV2.SIGN_IN, result.scenario)
            assertEquals(correlationId, result.resultValue.correlationId)
            enqueueResult(
                NativeAuthV2CommandResult.Complete(correlationId, null, null, null),
                NativeAuthV2ActivateCommand::class
            )
            assertTrue(state.submitChallenge("otp") is NativeAuthErrorV2)
        } finally {
            unmockkStatic(AuthenticationResultAdapter::class)
        }
    }

    @Test
    fun strongAuthClientExceptionsHaveFixedRedactedMessages() = runTest {
        val registration = strongAuthRegistration()
        val verification = strongAuthVerification()
        every {
            CommandDispatcher.submitSilentReturningFuture(match { it is NativeAuthV2EnrollCommand || it is NativeAuthV2ActivateCommand })
        } throws IllegalStateException("sensitive-contact-and-otp")
        val errors = listOf(
            registration.nextState.selectAuthMethod(registration.authMethods.single(), "sensitive-contact"),
            verification.submitChallenge("sensitive-otp")
        )
        errors.forEach {
            val error = it as NativeAuthErrorV2
            assertEquals(ErrorTypes.CLIENT_EXCEPTION, error.errorType)
            assertFalse(error.errorMessage.orEmpty().contains("sensitive"))
            assertFalse(error.toString().contains("sensitive"))
            assertTrue(error.exception is IllegalStateException)
        }
    }

    @Test
    fun strongAuthCallbacksCancelPendingFuturesWithoutDeliveringResults() = runTest(timeout = 60.seconds) {
        val registration = strongAuthRegistration()
        val verification = strongAuthVerification()
        val enrollCallback = mockk<StrongAuthRegistrationRequiredStateV2.SelectAuthMethodCallback>(relaxed = true)
        val activateCallback = mockk<StrongAuthVerificationRequiredStateV2.SubmitChallengeCallback>(relaxed = true)
        val resendCallback = mockk<StrongAuthVerificationRequiredStateV2.ResendChallengeCallback>(relaxed = true)
        mockkObject(NativeAuthPublicClientApplication.Companion)
        try {
            for (commandClass in listOf(NativeAuthV2EnrollCommand::class, NativeAuthV2ActivateCommand::class, NativeAuthV2ResendCodeCommand::class)) {
                val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
                val waiting = CountDownLatch(1)
                val future = spyk(FinalizableResultFuture<CommandResult<Any>>())
                every { NativeAuthPublicClientApplication.pcaScope } returns scope
                every { future.get() } answers {
                    waiting.countDown()
                    callOriginal()
                }
                every {
                    CommandDispatcher.submitSilentReturningFuture(match {
                        commandClass.java.isInstance(it)
                    })
                } returns future
                try {
                    when (commandClass) {
                        NativeAuthV2EnrollCommand::class -> registration.nextState.selectAuthMethod(
                            registration.authMethods.single(), "user@example.com", enrollCallback
                        )
                        NativeAuthV2ActivateCommand::class -> verification.submitChallenge("otp", activateCallback)
                        NativeAuthV2ResendCodeCommand::class -> verification.resendChallenge(resendCallback)
                    }
                    assertTrue(waiting.await(10, TimeUnit.SECONDS))
                } finally {
                    scope.cancel()
                    scope.coroutineContext[Job]?.join()
                }
                verify(exactly = 1) { future.cancelSignal() }
            }
            verify(exactly = 0) { enrollCallback.onResult(any()) }
            verify(exactly = 0) { enrollCallback.onError(any()) }
            verify(exactly = 0) { activateCallback.onResult(any()) }
            verify(exactly = 0) { activateCallback.onError(any()) }
            verify(exactly = 0) { resendCallback.onResult(any()) }
            verify(exactly = 0) { resendCallback.onError(any()) }
        } finally {
            unmockkObject(NativeAuthPublicClientApplication.Companion)
        }
    }

    private suspend fun strongAuthRegistration(): NativeAuthResultV2.StrongAuthRegistrationRequired {
        enqueueResult(
            NativeAuthV2CommandResult.StrongAuthRegistrationRequired(
                correlationId, createContinuationState(), listOf(NativeAuthV2AuthMethod("sms-1", "sms", null))
            ), NativeAuthV2SignInStartCommand::class
        )
        return application.signInV2(signInParameters()) as NativeAuthResultV2.StrongAuthRegistrationRequired
    }

    private suspend fun strongAuthVerification(): StrongAuthVerificationRequiredStateV2 {
        val registration = strongAuthRegistration()
        enqueueResult(
            NativeAuthV2CommandResult.StrongAuthVerificationRequired(correlationId, createContinuationState(), 6, "***"),
            NativeAuthV2EnrollCommand::class
        )
        return (registration.nextState.selectAuthMethod(registration.authMethods.single(), "user@example.com") as
            NativeAuthResultV2.StrongAuthVerificationRequired).nextState
    }

    @Test
    fun signInV2RejectsBlankUsernameWithoutIssuingACommand() = runTest {
        mockkObject(DiagnosticContext.INSTANCE)
        every { DiagnosticContext.INSTANCE.threadCorrelationId } returns correlationId
        try {
            val result = application.signInV2(signInParameters(username = " "))
            assertTrue(result is NativeAuthErrorV2)
            val error = result as NativeAuthErrorV2
            assertTrue(error.isInvalidUsername())
            assertEquals(NativeAuthFlowScenarioV2.SIGN_IN, error.scenario)
            assertEquals(correlationId, error.correlationId)
        } finally {
            unmockkObject(DiagnosticContext.INSTANCE)
        }
    }

    @Test
    fun signInV2WithoutPasswordReturnsEmailCodeRequiredState() = runTest {
        enqueueResult(
            NativeAuthV2CommandResult.CodeRequired(
                correlationId = correlationId,
                continuationState = createContinuationState(),
                codeLength = 8,
                challengeTargetLabel = "u***@contoso.com",
                challengeChannel = "email"
            ),
            NativeAuthV2SignInStartCommand::class
        )

        val result = application.signInV2(NativeAuthSignInParameters(username))

        assertTrue(result is NativeAuthResultV2.CodeRequired)
        result as NativeAuthResultV2.CodeRequired
        assertEquals(NativeAuthFlowScenarioV2.SIGN_IN, result.scenario)
        assertEquals(8, result.codeLength)
        assertEquals("u***@contoso.com", result.sentTo)
        assertEquals("email", result.channel)
        assertEquals(correlationId, result.nextState.correlationId)
    }

    @Test
    fun signInV2MapsPasswordRequiredWhenCommonSelectsPassword() = runTest {
        enqueueResult(
            NativeAuthV2CommandResult.PasswordRequired(correlationId, createContinuationState()),
            NativeAuthV2SignInStartCommand::class
        )

        val result = application.signInV2(NativeAuthSignInParameters(username))

        assertTrue(result is NativeAuthResultV2.PasswordRequired)
        assertEquals(NativeAuthFlowScenarioV2.SIGN_IN, result.scenario)
    }

    @Test
    fun signInV2WithWrongEntryPasswordIsInvalidCredentials() = runTest {
        enqueueResult(
            NativeAuthV2CommandResult.InvalidCredentials(
                correlationId,
                "invalid_grant",
                "AADSTS50126: invalid username or password.",
                "invalidUserNameOrPassword",
                errorCodes,
                traceId = "password-trace",
                timestamp = "password-time"
            ),
            NativeAuthV2SignInStartCommand::class
        )

        val result = application.signInV2(signInParameters(password = "WrongPassword!".toCharArray()))

        assertTrue(result is NativeAuthErrorV2)
        result as NativeAuthErrorV2
        assertTrue(result.isInvalidCredentials())
        assertFalse(result.isUserNotFound())
        assertEquals(errorCodes, result.errorCodes)
        assertEquals("invalidUserNameOrPassword", result.subError)
        assertEquals("password-trace", result.traceId)
        assertEquals("password-time", result.timestamp)
        assertEquals(NativeAuthFlowScenarioV2.SIGN_IN, result.scenario)
        // A recoverable-looking error must not hand back a state to continue from.
        assertNull((result as NativeAuthResultV2).let { (it as? NativeAuthResultV2.PasswordRequired)?.nextState })
    }

    @Test
    fun signInV2WithUnknownUserIsUserNotFound() = runTest {
        enqueueResult(
            NativeAuthV2CommandResult.UserNotFound(
                correlationId,
                "invalid_grant",
                "AADSTS50034: user not found.",
                errorCodes
            ),
            NativeAuthV2SignInStartCommand::class
        )

        val result = application.signInV2(signInParameters()) as NativeAuthErrorV2

        assertTrue(result.isUserNotFound())
        assertFalse(result.isInvalidCredentials())
        assertEquals(errorCodes, result.errorCodes)
    }

    @Test
    fun signInV2MapsBrowserRequiredToSignInError() = runTest {
        enqueueResult(
            INativeAuthCommandResult.Redirect(correlationId, "browser"),
            NativeAuthV2SignInStartCommand::class
        )

        val result = application.signInV2(signInParameters()) as NativeAuthErrorV2

        assertTrue(result.isBrowserRequired())
    }

    @Test
    fun signInV2MapsUnsupportedStartResultToInvalidState() = runTest {
        val unsupportedResult = mockk<NativeAuthV2SignInStartCommandResult>()
        every { unsupportedResult.correlationId } returns correlationId
        enqueueResult(unsupportedResult, NativeAuthV2SignInStartCommand::class)

        val result = application.signInV2(signInParameters()) as NativeAuthErrorV2

        assertEquals(ErrorTypes.INVALID_STATE, result.errorType)
        assertEquals(correlationId, result.correlationId)
        assertEquals(NativeAuthFlowScenarioV2.SIGN_IN, result.scenario)
    }

    @Test
    fun signInV2StateMappingsRejectUnsupportedCommonResultsAsInvalidState() = runBlocking {
        val codeState = codeRequiredState()
        enqueueResult(
            unsupportedResult<NativeAuthV2SignInSubmitCodeCommandResult>(),
            NativeAuthV2SignInSubmitCodeCommand::class
        )
        val submitCodeResult = codeState.submitCode("123456") as NativeAuthErrorV2
        assertEquals(
            submitCodeResult.exception?.stackTraceToString(),
            ErrorTypes.INVALID_STATE,
            submitCodeResult.errorType
        )

        val passwordState = passwordRequiredState()
        enqueueResult(
            unsupportedResult<NativeAuthV2SubmitPasswordCommandResult>(),
            NativeAuthV2SubmitPasswordCommand::class
        )
        assertEquals(
            ErrorTypes.INVALID_STATE,
            (passwordState.submitPassword("Password!".toCharArray()) as NativeAuthErrorV2).errorType
        )

        val mfaState = mfaRequiredState()
        enqueueResult(
            unsupportedResult<NativeAuthV2SelectMFAMethodCommandResult>(),
            NativeAuthV2SelectMFAMethodCommand::class
        )
        assertEquals(
            ErrorTypes.INVALID_STATE,
            (mfaState.selectAuthMethod(mfaState.authMethods.single()) as NativeAuthErrorV2).errorType
        )

        val verificationState = mfaVerificationState(mfaState)
        enqueueResult(
            unsupportedResult<NativeAuthV2SubmitMFAChallengeCommandResult>(),
            NativeAuthV2SubmitMFAChallengeCommand::class
        )
        assertEquals(
            ErrorTypes.INVALID_STATE,
            (verificationState.submitChallenge("123456") as NativeAuthErrorV2).errorType
        )

        enqueueResult(
            unsupportedResult<NativeAuthV2ResendCodeCommandResult>(),
            NativeAuthV2ResendCodeCommand::class
        )
        assertEquals(
            ErrorTypes.INVALID_STATE,
            (verificationState.resendChallenge() as NativeAuthErrorV2).errorType
        )
    }

    @Test
    fun signInV2ClearsTheOwnedPasswordCopyOnCompletionAndCancellationWhileLeavingTheCallersBufferIntact() = runTest(timeout = 60.seconds) {
        var capturedOnCompletion: CharArray? = null
        enqueueResult(
            NativeAuthV2CommandResult.InvalidCredentials(
                correlationId,
                "invalid_grant",
                "invalid",
                "invalidUserNameOrPassword"
            ),
            NativeAuthV2SignInStartCommand::class
        ) { command ->
            capturedOnCompletion = (command.parameters as SignInV2StartCommandParameters).password
        }

        val callerPassword = "Password123!".toCharArray()
        application.signInV2(signInParameters(password = callerPassword))

        // The command must have received a distinct, owned copy -- never the caller's own array.
        assertNotSame(callerPassword, capturedOnCompletion)
        assertEquals("Password123!", String(callerPassword))
        assertPasswordCleared(capturedOnCompletion!!)

        var capturedOnCancellation: CharArray? = null
        enqueueCancellation(NativeAuthV2SignInStartCommand::class) { command ->
            capturedOnCancellation = (command.parameters as SignInV2StartCommandParameters).password
        }
        val secondCallerPassword = "Password123!".toCharArray()
        try {
            application.signInV2(signInParameters(password = secondCallerPassword))
            fail("Expected CancellationException")
        } catch (_: CancellationException) {
            // Expected.
        }

        // The owned copy must be cleared even when the command is cancelled, while the caller's
        // own buffer -- which it never handed over -- remains untouched.
        assertEquals("Password123!", String(secondCallerPassword))
        assertPasswordCleared(capturedOnCancellation!!)
    }

    @Test
    fun signInV2CallbackSnapshotsThePasswordSynchronouslyBeforeLaunch() = runTest {
        var capturedPassword: CharArray? = null
        var capturedScopes: List<String>? = null
        var capturedClaims: String? = null
        enqueueResult(
            NativeAuthV2CommandResult.PasswordRequired(correlationId, createContinuationState()),
            NativeAuthV2SignInStartCommand::class
        ) { command ->
            val commandParameters = command.parameters as SignInV2StartCommandParameters
            capturedPassword = commandParameters.password?.copyOf()
            capturedScopes = commandParameters.scopes
            capturedClaims = commandParameters.claimsRequestJson
        }

        val callerPassword = "Password123!".toCharArray()
        val future = ResultFuture<NativeAuthResultV2>()
        val scopes = mutableListOf("User.Read")
        val claims = com.microsoft.identity.client.claims.ClaimsRequest().also { it.requestClaimInIdToken("auth_time", null) }
        val expectedClaims = com.microsoft.identity.client.claims.ClaimsRequest.getJsonStringFromClaimsRequest(claims)
        application.signInV2(
            signInParameters(password = callerPassword, scopes = scopes).also { it.claimsRequest = claims },
            object : NativeAuthPublicClientApplication.NativeAuthV2Callback {
                override fun onResult(result: NativeAuthResultV2) = future.setResult(result)
                override fun onError(exception: BaseException) = future.setException(exception)
            }
        )
        // Mutate the caller's own buffer immediately after the call returns, before the launched
        // coroutine has necessarily had a chance to run. If the snapshot were taken lazily (inside
        // the coroutine body, as opposed to synchronously before launch), this mutation could race
        // with -- and change -- what actually gets submitted to Common.
        callerPassword.fill('X')
        scopes.clear()
        claims.requestClaimInIdToken("changed", null)

        future.get(10, TimeUnit.SECONDS)

        assertEquals("Password123!", String(capturedPassword!!))
        assertEquals(listOf("User.Read"), capturedScopes)
        assertEquals(expectedClaims, capturedClaims)
    }

    @Test
    fun signInV2PropagatesCancellationRatherThanReportingAnAuthError() = runTest {
        enqueueCancellation(NativeAuthV2SignInStartCommand::class)

        try {
            application.signInV2(signInParameters())
            fail("Expected CancellationException")
        } catch (_: CancellationException) {
            // Expected: cancellation is not an authentication failure.
        }
    }

    @Test
    fun signInV2RejectsSignInWhenAnAccountIsAlreadySignedIn() = runTest {
        mockkObject(NativeAuthPublicClientApplication.Companion)
        every {
            NativeAuthPublicClientApplication.getCurrentAccountInternal(any())
        } returns mockk<com.microsoft.identity.client.IAccount>()
        try {
            // Android V1 behaviour is preserved: V2 rejects sign-in while an account is signed in,
            // and does not implement the iOS repeated/switch-account behaviour.
            val suspendResult = application.signInV2(signInParameters())
            assertTrue(suspendResult is NativeAuthErrorV2)
            assertEquals(ErrorTypes.CLIENT_EXCEPTION, (suspendResult as NativeAuthErrorV2).errorType)
            val cause = suspendResult.exception
            assertTrue(cause is MsalClientException)
            assertEquals("An account is already signed in.", cause?.message)

            val callbackResult = ResultFuture<NativeAuthResultV2>()
            application.signInV2(
                signInParameters(),
                object : NativeAuthPublicClientApplication.NativeAuthV2Callback {
                    override fun onResult(result: NativeAuthResultV2) = callbackResult.setResult(result)
                    override fun onError(exception: BaseException) =
                        callbackResult.setException(IllegalStateException("Expected callback.onResult", exception))
                }
            )
            val delivered = callbackResult.get(10, TimeUnit.SECONDS)
            assertTrue(delivered is NativeAuthErrorV2)
            assertEquals(ErrorTypes.CLIENT_EXCEPTION, (delivered as NativeAuthErrorV2).errorType)
        } finally {
            unmockkObject(NativeAuthPublicClientApplication.Companion)
        }
    }

    @Test
    fun signInV2CallbackAndSuspendSurfacesAgree() = runTest {
        enqueueResult(
            emailCodeRequiredResult(),
            NativeAuthV2SignInStartCommand::class
        )
        val suspendResult = application.signInV2(signInParameters(password = null))

        enqueueResult(
            emailCodeRequiredResult(),
            NativeAuthV2SignInStartCommand::class
        )
        val future = ResultFuture<NativeAuthResultV2>()
        application.signInV2(
            signInParameters(password = null),
            object : NativeAuthPublicClientApplication.NativeAuthV2Callback {
                override fun onResult(result: NativeAuthResultV2) = future.setResult(result)
                override fun onError(exception: BaseException) = future.setException(exception)
            }
        )
        val callbackResult = future.get(30, TimeUnit.SECONDS)

        assertTrue(suspendResult is NativeAuthResultV2.CodeRequired)
        assertTrue(callbackResult is NativeAuthResultV2.CodeRequired)
        assertEquals(suspendResult.scenario, callbackResult.scenario)
    }

    // -----------------------------------------------------------------------------------------
    // Email OTP first factor
    // -----------------------------------------------------------------------------------------

    @Test
    fun submitFirstFactorEmailCodeWithWrongCodeIsRecoverable() = runTest {
        val state = codeRequiredState()
        enqueueResult(
            NativeAuthV2CommandResult.IncorrectCode(
                correlationId,
                "invalidGrant",
                "AADSTS50184: invalid code.",
                "invalidOneTimeCode",
                errorCodes
            ),
            NativeAuthV2SignInSubmitCodeCommand::class
        )

        val result = state.submitCode("00000000") as NativeAuthErrorV2

        assertTrue(result.isInvalidCode())
        assertEquals("invalidOneTimeCode", result.subError)
        assertEquals(NativeAuthFlowScenarioV2.SIGN_IN, result.scenario)
        verify {
            CommandDispatcher.submitSilentReturningFuture(
                match {
                    it is NativeAuthV2SignInSubmitCodeCommand &&
                        it.publicApiId == PublicApiId.NATIVE_AUTH_V2_SIGN_IN_SUBMIT_CODE &&
                        (it.parameters as NativeAuthV2SubmitCodeCommandParameters).code == "00000000"
                }
            )
        }
    }

    @Test
    fun submitFirstFactorEmailCodeCanTransitionToExistingMFAFlow() = runTest {
        val state = codeRequiredState()
        enqueueResult(
            NativeAuthV2CommandResult.MFARequired(
                correlationId,
                createContinuationState(),
                listOf(NativeAuthV2AuthMethod("email-2", "email", "u***@contoso.com"))
            ),
            NativeAuthV2SignInSubmitCodeCommand::class
        )

        val result = state.submitCode("12345678") as NativeAuthResultV2.MFARequired

        assertEquals(NativeAuthFlowScenarioV2.SIGN_IN, result.scenario)
        assertEquals(1, result.authMethods.size)
        assertEquals("email-2", result.authMethods.single().id)
        assertEquals("email", result.authMethods.single().challengeChannel)
    }

    @Test
    fun submitFirstFactorEmailCodeCanTransitionToSmsMFA() = runTest {
        val state = codeRequiredState()
        enqueueResult(
            NativeAuthV2CommandResult.MFARequired(
                correlationId,
                createContinuationState(),
                listOf(NativeAuthV2AuthMethod("sms-1", "sms", "+X XXX XXX 34"))
            ),
            NativeAuthV2SignInSubmitCodeCommand::class
        )

        val result = state.submitCode("12345678") as NativeAuthResultV2.MFARequired

        assertEquals(NativeAuthFlowScenarioV2.SIGN_IN, result.scenario)
        assertEquals("sms-1", result.authMethods.single().id)
        assertEquals("sms", result.authMethods.single().challengeChannel)
    }

    @Test
    fun submitFirstFactorEmailCodeCanCompleteSignIn() = runTest {
        val state = codeRequiredState()
        val localResult = mockk<ILocalAuthenticationResult>()
        val authenticationResult = mockk<IAuthenticationResult>()
        every { authenticationResult.account } returns mockk<IAccount>()
        mockkStatic(AuthenticationResultAdapter::class)
        try {
            every { AuthenticationResultAdapter.adapt(localResult) } returns authenticationResult
            enqueueResult(
                NativeAuthV2CommandResult.Complete(correlationId, localResult, null, null),
                NativeAuthV2SignInSubmitCodeCommand::class
            )

            val result = state.submitCode("12345678") as NativeAuthResultV2.Complete

            assertEquals(correlationId, result.resultValue.correlationId)
            assertEquals(NativeAuthFlowScenarioV2.SIGN_IN, result.scenario)
        } finally {
            unmockkStatic(AuthenticationResultAdapter::class)
        }
    }

    @Test
    fun resendFirstFactorEmailCodeReturnsRefreshedCodeRequiredState() = runTest {
        val state = codeRequiredState()
        val refreshedState = createContinuationState(correlationId = "resend-correlation-id")
        enqueueResult(
            NativeAuthV2CommandResult.CodeRequired(
                correlationId = "resend-correlation-id",
                continuationState = refreshedState,
                codeLength = 8,
                challengeTargetLabel = "n***@contoso.com",
                challengeChannel = "email"
            ),
            NativeAuthV2ResendCodeCommand::class
        )

        val result = state.resendCode() as NativeAuthResultV2.CodeRequired

        assertEquals(8, result.codeLength)
        assertEquals("n***@contoso.com", result.sentTo)
        assertEquals("email", result.channel)
        assertEquals("resend-correlation-id", result.nextState.correlationId)
        assertTrue(result.nextState !== state)
        verify {
            CommandDispatcher.submitSilentReturningFuture(
                match {
                    it is NativeAuthV2ResendCodeCommand &&
                        it.publicApiId == PublicApiId.NATIVE_AUTH_V2_SIGN_IN_RESEND_CODE &&
                        (it.parameters as NativeAuthV2ResendCodeCommandParameters)
                            .continuationState === state.continuationState
                }
            )
        }
    }

    // -----------------------------------------------------------------------------------------
    // Deferred password submission
    // -----------------------------------------------------------------------------------------

    @Test
    fun submitPasswordWithWrongPasswordIsInvalidCredentialsNotPasswordPolicy() = runTest {
        val state = passwordRequiredState()
        enqueueResult(
            NativeAuthV2CommandResult.IncorrectPassword(
                correlationId,
                "invalid_grant",
                "AADSTS50126: invalid username or password.",
                "invalidUserNameOrPassword",
                errorCodes,
                traceId = "password-trace",
                timestamp = "password-time"
            ),
            NativeAuthV2SubmitPasswordCommand::class
        )

        val result = state.submitPassword("WrongPassword!".toCharArray())

        assertTrue(result is NativeAuthErrorV2)
        result as NativeAuthErrorV2
        assertTrue(result.isInvalidCredentials())
        assertFalse(result.isInvalidPassword())
        assertEquals(errorCodes, result.errorCodes)
        assertEquals("password-trace", result.traceId)
        assertEquals("password-time", result.timestamp)
    }

    @Test
    fun submitPasswordRejectsAnEmptyPasswordWithoutIssuingACommand() = runTest {
        val state = passwordRequiredState()

        val result = state.submitPassword(CharArray(0)) as NativeAuthErrorV2

        assertTrue(result.isInvalidInput())
        assertFalse(result.isInvalidPassword())
    }

    @Test
    fun submitPasswordClearsTheOwnedPasswordCopyOnCompletionAndCancellationWhileLeavingTheCallersBufferIntact() = runTest(timeout = 60.seconds) {
        val state = passwordRequiredState()
        var capturedOnCompletion: CharArray? = null
        enqueueResult(
            INativeAuthCommandResult.Redirect(correlationId, "browser"),
            NativeAuthV2SubmitPasswordCommand::class
        ) { command ->
            capturedOnCompletion = (command.parameters as NativeAuthV2SubmitPasswordCommandParameters).password
        }

        val callerPassword = "Password123!".toCharArray()
        state.submitPassword(callerPassword)

        // The command must have received a distinct, owned copy -- never the caller's own array.
        assertNotSame(callerPassword, capturedOnCompletion)
        assertEquals("Password123!", String(callerPassword))
        assertPasswordCleared(capturedOnCompletion!!)

        var capturedOnCancellation: CharArray? = null
        enqueueCancellation(NativeAuthV2SubmitPasswordCommand::class) { command ->
            capturedOnCancellation = (command.parameters as NativeAuthV2SubmitPasswordCommandParameters).password
        }
        val secondCallerPassword = "Password123!".toCharArray()
        try {
            state.submitPassword(secondCallerPassword)
            fail("Expected CancellationException")
        } catch (_: CancellationException) {
            // Expected.
        }

        // The owned copy must be cleared even when the command is cancelled, while the caller's
        // own buffer -- which it never handed over -- remains untouched.
        assertEquals("Password123!", String(secondCallerPassword))
        assertPasswordCleared(capturedOnCancellation!!)
    }

    @Test
    fun submitPasswordCallbackSnapshotsThePasswordSynchronouslyBeforeLaunch() = runTest {
        val state = passwordRequiredState()
        var capturedPassword: CharArray? = null
        enqueueResult(
            INativeAuthCommandResult.Redirect(correlationId, "browser"),
            NativeAuthV2SubmitPasswordCommand::class
        ) { command ->
            capturedPassword = (command.parameters as NativeAuthV2SubmitPasswordCommandParameters).password.copyOf()
        }

        val callerPassword = "Password123!".toCharArray()
        val future = ResultFuture<NativeAuthResultV2>()
        state.submitPassword(
            callerPassword,
            object : PasswordRequiredStateV2.SubmitPasswordCallback {
                override fun onResult(result: NativeAuthResultV2) = future.setResult(result)
                override fun onError(exception: BaseException) = future.setException(exception)
            }
        )
        // Mutate the caller's own buffer immediately after the call returns, before the launched
        // coroutine has necessarily had a chance to run, for the same reason as the signInV2
        // callback variant above.
        callerPassword.fill('X')

        future.get(10, TimeUnit.SECONDS)

        assertEquals("Password123!", String(capturedPassword!!))
    }

    @Test
    fun submitPasswordPropagatesCancellation() = runTest {
        val state = passwordRequiredState()
        enqueueCancellation(NativeAuthV2SubmitPasswordCommand::class)

        try {
            state.submitPassword("Password123!".toCharArray())
            fail("Expected CancellationException")
        } catch (_: CancellationException) {
            // Expected.
        }
    }

    @Test
    fun submitPasswordMapsApiErrorAndBrowserRequired() = runTest {
        val state = passwordRequiredState()

        enqueueResult(
            INativeAuthCommandResult.Redirect(correlationId, "browser"),
            NativeAuthV2SubmitPasswordCommand::class
        )
        assertTrue((state.submitPassword("Password123!".toCharArray()) as NativeAuthErrorV2).isBrowserRequired())

        enqueueResult(
            INativeAuthCommandResult.APIError("api_error", "API failed", correlationId = correlationId, errorCodes = errorCodes),
            NativeAuthV2SubmitPasswordCommand::class
        )
        val apiError = state.submitPassword("Password123!".toCharArray()) as NativeAuthErrorV2
        assertEquals(errorCodes, apiError.errorCodes)
        assertFalse(apiError.isInvalidPassword())
    }

    @Test
    fun submitPasswordCallbackDeliversRecoverableErrorThroughOnResult() = runTest {
        val state = passwordRequiredState()
        enqueueResult(
            NativeAuthV2CommandResult.IncorrectPassword(
                correlationId,
                "invalid_grant",
                "invalid",
                "invalidUserNameOrPassword"
            ),
            NativeAuthV2SubmitPasswordCommand::class
        )

        val future = ResultFuture<NativeAuthResultV2>()
        state.submitPassword(
            "WrongPassword!".toCharArray(),
            object : PasswordRequiredStateV2.SubmitPasswordCallback {
                override fun onResult(result: NativeAuthResultV2) = future.setResult(result)
                override fun onError(exception: BaseException) =
                    future.setException(IllegalStateException("Expected callback.onResult", exception))
            }
        )

        val result = future.get(30, TimeUnit.SECONDS)
        assertTrue((result as NativeAuthErrorV2).isInvalidCredentials())
        assertFalse(result.isInvalidPassword())
    }

    // -----------------------------------------------------------------------------------------
    // Email and SMS OTP MFA
    // -----------------------------------------------------------------------------------------

    @Test
    fun signInV2WithPasswordCanTransitionToMFARequiredWithServerMethods() = runTest {
        enqueueResult(
            NativeAuthV2CommandResult.MFARequired(
                correlationId,
                createContinuationState(),
                listOf(NativeAuthV2AuthMethod("email-1", "email", "u***@contoso.com"))
            ),
            NativeAuthV2SignInStartCommand::class
        )

        val result = application.signInV2(signInParameters()) as NativeAuthResultV2.MFARequired

        assertEquals(1, result.authMethods.size)
        val method = result.authMethods.single()
        assertEquals("email-1", method.id)
        assertEquals("email", method.challengeChannel)
        assertEquals("oob", method.challengeType)
        assertEquals("u***@contoso.com", method.loginHint)
        assertEquals(NativeAuthFlowScenarioV2.SIGN_IN, result.scenario)
    }

    @Test
    fun signInV2WithPasswordCanTransitionToSmsMFA() = runTest {
        enqueueResult(
            NativeAuthV2CommandResult.MFARequired(
                correlationId,
                createContinuationState(),
                listOf(NativeAuthV2AuthMethod("sms-1", "sms", "+X XXX XXX 34"))
            ),
            NativeAuthV2SignInStartCommand::class
        )

        val result = application.signInV2(signInParameters()) as NativeAuthResultV2.MFARequired
        val method = result.authMethods.single()

        assertEquals("sms-1", method.id)
        assertEquals("sms", method.challengeChannel)
        assertEquals("sms", method.challengeType)
        assertEquals("+X XXX XXX 34", method.loginHint)
    }

    @Test
    fun selectAuthMethodTransitionsToMFAVerificationRequired() = runTest {
        val state = mfaRequiredState()
        enqueueResult(
            NativeAuthV2CommandResult.MFAVerificationRequired(
                correlationId,
                createContinuationState(),
                6,
                "u***@contoso.com",
                "email"
            ),
            NativeAuthV2SelectMFAMethodCommand::class
        )

        val result = state.selectAuthMethod(state.authMethods.single()) as NativeAuthResultV2.MFAVerificationRequired

        assertEquals(6, result.codeLength)
        assertTrue("Code length must be positive", result.codeLength > 0)
        assertEquals("u***@contoso.com", result.sentTo)
        assertEquals("email", result.channel)
        assertEquals(NativeAuthFlowScenarioV2.SIGN_IN, result.scenario)
    }

    @Test
    fun selectSmsAuthMethodTransitionsToMFAVerificationRequired() = runTest {
        val state = mfaRequiredState(
            methods = listOf(NativeAuthV2AuthMethod("sms-1", "sms", "+X XXX XXX 34"))
        )
        enqueueResult(
            NativeAuthV2CommandResult.MFAVerificationRequired(
                correlationId,
                createContinuationState(),
                7,
                "+X XXX XXX 34",
                "sms"
            ),
            NativeAuthV2SelectMFAMethodCommand::class
        )

        val result = state.selectAuthMethod(state.authMethods.single()) as
            NativeAuthResultV2.MFAVerificationRequired

        assertEquals(7, result.codeLength)
        assertEquals("+X XXX XXX 34", result.sentTo)
        assertEquals("sms", result.channel)
        assertEquals(NativeAuthFlowScenarioV2.SIGN_IN, result.scenario)
    }

    @Test
    fun selectAuthMethodRejectsAMethodTheServerDidNotOffer() = runTest {
        val state = mfaRequiredState()

        val result = state.selectAuthMethod(
            com.microsoft.identity.nativeauth.AuthMethod("not-offered", "oob", null, "email")
        )

        assertTrue(result is NativeAuthErrorV2)
        assertEquals(ErrorTypes.INVALID_STATE, (result as NativeAuthErrorV2).errorType)
    }

    @Test
    fun selectAuthMethodRejectsAnUnknownChannelWithoutIssuingACommand() = runTest {
        val state = mfaRequiredState(
            methods = listOf(NativeAuthV2AuthMethod("voice-1", "voice", "+1***4567"))
        )

        val result = state.selectAuthMethod(state.authMethods.single())

        assertTrue(result is NativeAuthErrorV2)
        val error = result as NativeAuthErrorV2
        assertTrue(error.isNotImplemented())
        assertFalse(error.isAuthMethodBlocked())
        assertFalse(error.isBrowserRequired())
    }

    @Test
    fun selectAuthMethodMapsApiError() = runTest {
        val state = mfaRequiredState()
        enqueueResult(
            INativeAuthCommandResult.APIError(
                error = "accessDenied",
                errorDescription = "blocked",
                correlationId = correlationId,
                errorCodes = errorCodes
            ),
            NativeAuthV2SelectMFAMethodCommand::class
        )

        val result = state.selectAuthMethod(state.authMethods.single()) as NativeAuthErrorV2

        assertFalse(result.isAuthMethodBlocked())
        assertEquals("accessDenied", result.error)
        assertEquals("blocked", result.errorMessage)
        assertEquals(errorCodes, result.errorCodes)
    }

    @Test
    fun submitChallengeWithWrongCodeIsRecoverableAndAllowsAFreshChallenge() = runTest {
        val mfaState = mfaRequiredState()
        val verificationState = mfaVerificationState(mfaState)

        enqueueResult(
            NativeAuthV2CommandResult.IncorrectCode(
                correlationId,
                "invalidGrant",
                "AADSTS50184: invalid code.",
                "invalidOneTimeCode",
                errorCodes
            ),
            NativeAuthV2SubmitMFAChallengeCommand::class
        )
        val wrongCode = verificationState.submitChallenge("000000") as NativeAuthErrorV2
        assertTrue(wrongCode.isInvalidChallenge())
        assertEquals("invalidOneTimeCode", wrongCode.subError)

        val refreshedContinuationState = createContinuationState(correlationId = "resend-correlation-id")
        val resendFuture = FinalizableResultFuture<CommandResult<Any>>()
        resendFuture.setResult(
            CommandResult(
                ICommandResult.ResultStatus.COMPLETED,
                NativeAuthV2CommandResult.CodeRequired(
                    correlationId = "resend-correlation-id",
                    continuationState = refreshedContinuationState,
                    codeLength = 8,
                    challengeTargetLabel = "n***@contoso.com",
                    challengeChannel = "email"
                ) as Any,
                "resend-correlation-id"
            )
        )
        every {
            CommandDispatcher.submitSilentReturningFuture(
                match {
                    it is NativeAuthV2ResendCodeCommand &&
                        it.publicApiId == PublicApiId.NATIVE_AUTH_V2_SIGN_IN_RESEND_MFA_CHALLENGE &&
                        (it.parameters as NativeAuthV2ResendCodeCommandParameters).continuationState === verificationState.continuationState
                }
            )
        } returns resendFuture

        val fresh = verificationState.resendChallenge()
        assertTrue(fresh is NativeAuthResultV2.MFAVerificationRequired)
        fresh as NativeAuthResultV2.MFAVerificationRequired
        assertEquals(8, fresh.codeLength)
        assertEquals("n***@contoso.com", fresh.sentTo)
        assertEquals("email", fresh.channel)
        assertEquals("resend-correlation-id", fresh.nextState.correlationId)
        assertTrue(fresh.nextState !== verificationState)
        assertTrue(fresh.nextState.continuationState === refreshedContinuationState)

        // Retrying on the state instance the caller already holds also works.
        enqueueResult(
            NativeAuthV2CommandResult.IncorrectCode(
                correlationId,
                "invalidGrant",
                "invalid",
                "invalidOneTimeCode"
            ),
            NativeAuthV2SubmitMFAChallengeCommand::class
        )
        assertTrue((verificationState.submitChallenge("111111") as NativeAuthErrorV2).isInvalidChallenge())
    }

    @Test
    fun submitChallengeSendsEmptyAndShortCodesToServiceAndRemainsRetryable() = runTest {
        val verificationState = mfaVerificationState(mfaRequiredState())

        listOf("", "1").forEach { code ->
            var submittedCode: String? = null
            enqueueResult(
                NativeAuthV2CommandResult.IncorrectCode(correlationId, "invalidGrant", "invalid", "invalidOneTimeCode"),
                NativeAuthV2SubmitMFAChallengeCommand::class
            ) { command ->
                submittedCode = (command.parameters as com.microsoft.identity.common.java.nativeauth.commands.parameters.NativeAuthV2SubmitMFAChallengeCommandParameters).code
            }
            val result = verificationState.submitChallenge(code) as NativeAuthErrorV2
            assertTrue(result.isInvalidChallenge())
            assertTrue(result.isInvalidCode())
            assertEquals(code, submittedCode)
        }
    }

    @Test
    fun submitChallengeMapsNotImplementedAndBrowserRequired() = runTest {
        val verificationState = mfaVerificationState(mfaRequiredState())

        enqueueResult(
            INativeAuthCommandResult.Redirect(correlationId, "browser"),
            NativeAuthV2SubmitMFAChallengeCommand::class
        )
        assertTrue((verificationState.submitChallenge("123456") as NativeAuthErrorV2).isBrowserRequired())

        enqueueResult(
            NativeAuthV2CommandResult.NotImplemented(correlationId, "not_implemented", "nope"),
            NativeAuthV2SubmitMFAChallengeCommand::class
        )
        assertTrue((verificationState.submitChallenge("123456") as NativeAuthErrorV2).isNotImplemented())
    }

    @Test
    fun signInCompletionWithoutAuthenticationResultUsesScenarioNeutralDiagnostic() = runTest {
        val expectedMessage =
            "Native Auth V2 flow completed but no authentication result was returned."

        val passwordState = passwordRequiredState()
        enqueueResult(
            NativeAuthV2CommandResult.Complete(correlationId, null, null, null),
            NativeAuthV2SubmitPasswordCommand::class
        )
        assertEquals(
            expectedMessage,
            (passwordState.submitPassword("Password123!".toCharArray()) as NativeAuthErrorV2).errorMessage
        )

        val verificationState = mfaVerificationState(mfaRequiredState())
        enqueueResult(
            NativeAuthV2CommandResult.Complete(correlationId, null, null, null),
            NativeAuthV2SubmitMFAChallengeCommand::class
        )
        assertEquals(
            expectedMessage,
            (verificationState.submitChallenge("123456") as NativeAuthErrorV2).errorMessage
        )
    }

    @Test
    fun submitChallengePropagatesCancellation() = runTest {
        val verificationState = mfaVerificationState(mfaRequiredState())
        enqueueCancellation(NativeAuthV2SubmitMFAChallengeCommand::class)

        try {
            verificationState.submitChallenge("123456")
            fail("Expected CancellationException")
        } catch (_: CancellationException) {
            // Expected.
        }
    }

    @Test
    fun mfaVerificationCallbackDeliversRecoverableErrorThroughOnResult() = runTest {
        val verificationState = mfaVerificationState(mfaRequiredState())
        enqueueResult(
            NativeAuthV2CommandResult.IncorrectCode(
                correlationId,
                "invalidGrant",
                "invalid",
                "invalidOneTimeCode"
            ),
            NativeAuthV2SubmitMFAChallengeCommand::class
        )
        val future = ResultFuture<NativeAuthResultV2>()
        verificationState.submitChallenge(
            "000000",
            object : MFAVerificationRequiredStateV2.SubmitChallengeCallback {
                override fun onResult(result: NativeAuthResultV2) = future.setResult(result)
                override fun onError(exception: BaseException) =
                    future.setException(IllegalStateException("Expected callback.onResult", exception))
            }
        )

        assertTrue((future.get(30, TimeUnit.SECONDS) as NativeAuthErrorV2).isInvalidChallenge())
    }

    @Test
    fun resendChallengeCallbackDeliversARefreshedVerificationStateThroughOnResult() = runTest {
        val verificationState = mfaVerificationState(mfaRequiredState())
        val refreshedContinuationState = createContinuationState(correlationId = "callback-resend-correlation-id")
        enqueueResult(
            NativeAuthV2CommandResult.CodeRequired(
                correlationId = "callback-resend-correlation-id",
                continuationState = refreshedContinuationState,
                codeLength = 7,
                challengeTargetLabel = "c***@contoso.com",
                challengeChannel = "email"
            ),
            NativeAuthV2ResendCodeCommand::class
        )

        val future = ResultFuture<NativeAuthResultV2>()
        verificationState.resendChallenge(
            object : MFAVerificationRequiredStateV2.ResendChallengeCallback {
                override fun onResult(result: NativeAuthResultV2) = future.setResult(result)
                override fun onError(exception: BaseException) =
                    future.setException(IllegalStateException("Expected callback.onResult", exception))
            }
        )

        val result = future.get(30, TimeUnit.SECONDS)
        assertTrue(result is NativeAuthResultV2.MFAVerificationRequired)
        result as NativeAuthResultV2.MFAVerificationRequired
        assertEquals(7, result.codeLength)
        assertEquals("c***@contoso.com", result.sentTo)
        assertEquals("email", result.channel)
        assertTrue(result.nextState.continuationState === refreshedContinuationState)
    }

    @Test
    fun resendChallengeMapsNotImplementedRedirectAndApiErrorThenPropagatesCancellation() = runTest(timeout = 60.seconds) {
        val verificationState = mfaVerificationState(mfaRequiredState())

        enqueueResult(
            INativeAuthCommandResult.Redirect(correlationId, "browser"),
            NativeAuthV2ResendCodeCommand::class
        )
        assertTrue((verificationState.resendChallenge() as NativeAuthErrorV2).isBrowserRequired())

        enqueueResult(
            NativeAuthV2CommandResult.NotImplemented(correlationId, "not_implemented", "nope"),
            NativeAuthV2ResendCodeCommand::class
        )
        assertTrue((verificationState.resendChallenge() as NativeAuthErrorV2).isNotImplemented())

        enqueueResult(
            INativeAuthCommandResult.APIError("api_error", "API failed", correlationId = correlationId, errorCodes = errorCodes),
            NativeAuthV2ResendCodeCommand::class
        )
        val apiError = verificationState.resendChallenge() as NativeAuthErrorV2
        assertEquals(errorCodes, apiError.errorCodes)

        enqueueCancellation(NativeAuthV2ResendCodeCommand::class)
        try {
            verificationState.resendChallenge()
            fail("Expected CancellationException")
        } catch (_: CancellationException) {
            // Expected: cancellation is not an authentication failure.
        }
    }

    // -----------------------------------------------------------------------------------------
    // State restoration
    // -----------------------------------------------------------------------------------------

    @Test
    fun signInStatesSurviveParcelRestorationWithoutExposingSecrets() = runTest {
        val passwordState = passwordRequiredState()
        val mfaState = mfaRequiredState()
        val verificationState = mfaVerificationState(mfaState)

        val restoredPassword = parcelRoundTrip(passwordState, PasswordRequiredStateV2.CREATOR)
        val restoredMfa = parcelRoundTrip(mfaState, MFARequiredStateV2.CREATOR)
        val restoredVerification = parcelRoundTrip(verificationState, MFAVerificationRequiredStateV2.CREATOR)

        listOf(restoredPassword, restoredMfa, restoredVerification).forEach { restored ->
            assertEquals(correlationId, restored.correlationId)
            assertEquals(NativeAuthFlowScenarioV2.SIGN_IN, restored.scenario)
            assertEquals(listOf("scope"), restored.continuationState?.scopesForTokenRequest())
            // The opaque DTO must reveal nothing, even after restoration.
            assertFalse(restored.continuationState.toString().contains("opaque-token"))
        }

        // The offered methods must survive restoration so selection can still be validated.
        assertEquals(mfaState.authMethods, restoredMfa.authMethods)

        // Validation against the restored method set still happens before any command is issued,
        // so a stale selection fails deterministically on a restored state too.
        val staleSelection = restoredMfa.selectAuthMethod(
            com.microsoft.identity.nativeauth.AuthMethod("not-offered", "oob", null, "email")
        )
        assertTrue(staleSelection is NativeAuthErrorV2)
        assertEquals(ErrorTypes.INVALID_STATE, (staleSelection as NativeAuthErrorV2).errorType)
    }

    @Test
    fun submitPasswordMapsNotImplemented() = runTest {
        val state = passwordRequiredState()
        enqueueResult(
            NativeAuthV2CommandResult.NotImplemented(correlationId, "unsupported", "Password is unavailable."),
            NativeAuthV2SubmitPasswordCommand::class
        )

        val result = state.submitPassword("Password123!".toCharArray()) as NativeAuthErrorV2

        assertTrue(result.isNotImplemented())
        assertEquals("unsupported", result.error)
        assertEquals("Password is unavailable.", result.errorMessage)
        assertEquals(correlationId, result.correlationId)
        assertEquals(NativeAuthFlowScenarioV2.SIGN_IN, result.scenario)
    }

    @Test
    fun selectAuthMethodMapsNotImplemented() = runTest {
        val state = mfaRequiredState()
        enqueueResult(
            NativeAuthV2CommandResult.NotImplemented(correlationId, "unsupported", "MFA is unavailable."),
            NativeAuthV2SelectMFAMethodCommand::class
        )

        val result = state.selectAuthMethod(state.authMethods.single()) as NativeAuthErrorV2

        assertTrue(result.isNotImplemented())
        assertEquals("unsupported", result.error)
        assertEquals("MFA is unavailable.", result.errorMessage)
        assertEquals(correlationId, result.correlationId)
        assertEquals(NativeAuthFlowScenarioV2.SIGN_IN, result.scenario)
    }

    @Test
    fun selectAuthMethodPreservesBrowserRedirectDiagnostics() = runTest {
        val state = mfaRequiredState()
        enqueueResult(
            INativeAuthCommandResult.Redirect(
                correlationId = correlationId,
                redirectReason = "browser",
                traceId = "mfa-trace",
                timestamp = "mfa-time",
                errorCodes = errorCodes
            ),
            NativeAuthV2SelectMFAMethodCommand::class
        )

        val result = state.selectAuthMethod(state.authMethods.single()) as NativeAuthErrorV2

        assertTrue(result.isBrowserRequired())
        assertEquals("mfa-trace", result.traceId)
        assertEquals("mfa-time", result.timestamp)
        assertEquals(errorCodes, result.errorCodes)
        assertEquals(correlationId, result.correlationId)
    }

    @Test
    fun signInStatesPreserveAllApiErrorDiagnostics() = runTest {
        val passwordState = passwordRequiredState()
        val mfaState = mfaRequiredState()
        val verificationState = mfaVerificationState(mfaState)
        val failure = IllegalStateException("service failure")
        val details = listOf(mapOf("name" to "email", "reason" to "invalid"))
        val apiError = INativeAuthCommandResult.APIError(
            error = "service_error",
            errorDescription = "Verification failed.",
            correlationId = correlationId,
            traceId = "service-trace",
            timestamp = "service-time",
            errorCodes = errorCodes,
            subError = "verification_failed",
            attributeValidationDetails = details,
            exception = failure
        )
        enqueueResult(apiError, NativeAuthV2SubmitPasswordCommand::class)
        enqueueResult(apiError, NativeAuthV2SelectMFAMethodCommand::class)
        enqueueResult(apiError, NativeAuthV2SubmitMFAChallengeCommand::class)

        val results = listOf(
            passwordState.submitPassword("Password123!".toCharArray()),
            mfaState.selectAuthMethod(mfaState.authMethods.single()),
            verificationState.submitChallenge("123456")
        )

        results.forEach { result ->
            assertTrue(result is NativeAuthErrorV2)
            result as NativeAuthErrorV2
            assertEquals("service_error", result.error)
            assertEquals("Verification failed.", result.errorMessage)
            assertEquals(correlationId, result.correlationId)
            assertEquals(NativeAuthFlowScenarioV2.SIGN_IN, result.scenario)
            assertEquals("service-trace", result.traceId)
            assertEquals("service-time", result.timestamp)
            assertEquals(errorCodes, result.errorCodes)
            assertEquals("verification_failed", result.subError)
            assertEquals(details, result.attributeValidationDetails)
            assertSame(failure, result.exception)
        }
    }

    @Test
    fun submitPasswordMapsClientExceptionAndClearsOwnedBuffer() = runTest {
        val state = passwordRequiredState()
        val password = "Password123!".toCharArray()
        val failure = IllegalStateException("dispatcher failed")
        lateinit var ownedPassword: CharArray
        every {
            CommandDispatcher.submitSilentReturningFuture(ofType<NativeAuthV2SubmitPasswordCommand>())
        } answers {
            ownedPassword = (firstArg<NativeAuthV2SubmitPasswordCommand>().parameters as NativeAuthV2SubmitPasswordCommandParameters).password
            throw failure
        }

        val result = state.submitPassword(password) as NativeAuthErrorV2

        assertEquals(ErrorTypes.CLIENT_EXCEPTION, result.errorType)
        assertSame(failure, result.exception)
        assertEquals(correlationId, result.correlationId)
        assertEquals(NativeAuthFlowScenarioV2.SIGN_IN, result.scenario)
        assertPasswordCleared(ownedPassword)
        assertEquals("Password123!", String(password))
    }

    @Test
    fun selectAuthMethodMapsClientException() = runTest {
        val state = mfaRequiredState()
        val failure = IllegalStateException("dispatcher failed")
        every {
            CommandDispatcher.submitSilentReturningFuture(ofType<NativeAuthV2SelectMFAMethodCommand>())
        } throws failure

        val result = state.selectAuthMethod(state.authMethods.single()) as NativeAuthErrorV2

        assertEquals(ErrorTypes.CLIENT_EXCEPTION, result.errorType)
        assertSame(failure, result.exception)
        assertEquals(correlationId, result.correlationId)
        assertEquals(NativeAuthFlowScenarioV2.SIGN_IN, result.scenario)
    }

    @Test
    fun submitChallengeMapsClientException() = runTest {
        val state = mfaVerificationState(mfaRequiredState())
        val failure = IllegalStateException("dispatcher failed")
        every {
            CommandDispatcher.submitSilentReturningFuture(ofType<NativeAuthV2SubmitMFAChallengeCommand>())
        } throws failure

        val result = state.submitChallenge("123456") as NativeAuthErrorV2

        assertEquals(ErrorTypes.CLIENT_EXCEPTION, result.errorType)
        assertSame(failure, result.exception)
        assertEquals(correlationId, result.correlationId)
        assertEquals(NativeAuthFlowScenarioV2.SIGN_IN, result.scenario)
    }

    @Test
    fun selectAuthMethodPropagatesCancellation() = runTest {
        val state = mfaRequiredState()
        enqueueCancellation(NativeAuthV2SelectMFAMethodCommand::class)

        try {
            state.selectAuthMethod(state.authMethods.single())
            fail("Expected CancellationException")
        } catch (_: CancellationException) {
            // Cancellation must not be converted to an authentication error.
        }
    }

    // -----------------------------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------------------------

    private fun signInParameters(
        password: CharArray? = "Password123!".toCharArray(),
        scopes: List<String>? = null,
        username: String = this.username
    ): NativeAuthSignInParameters = NativeAuthSignInParameters(username).also {
        it.password = password
        it.scopes = scopes
    }

    private fun emailCodeRequiredResult(
        correlationId: String = NativeAuthV2SignInTest.correlationId,
        continuationState: NativeAuthV2ContinuationState = createContinuationState(correlationId)
    ): NativeAuthV2CommandResult.CodeRequired =
        NativeAuthV2CommandResult.CodeRequired(
            correlationId = correlationId,
            continuationState = continuationState,
            codeLength = 8,
            challengeTargetLabel = "u***@contoso.com",
            challengeChannel = "email"
        )

    private suspend fun codeRequiredState(): CodeRequiredStateV2 {
        enqueueResult(
            emailCodeRequiredResult(),
            NativeAuthV2SignInStartCommand::class
        )
        return (application.signInV2(signInParameters(password = null)) as
            NativeAuthResultV2.CodeRequired).nextState
    }

    private suspend fun passwordRequiredState(): PasswordRequiredStateV2 {
        enqueueResult(
            NativeAuthV2CommandResult.PasswordRequired(correlationId, createContinuationState()),
            NativeAuthV2SignInStartCommand::class
        )
        return (application.signInV2(signInParameters(password = null)) as
            NativeAuthResultV2.PasswordRequired).nextState
    }

    private suspend fun mfaRequiredState(
        methods: List<NativeAuthV2AuthMethod> =
            listOf(NativeAuthV2AuthMethod("email-1", "email", "u***@contoso.com"))
    ): MFARequiredStateV2 {
        enqueueResult(
            NativeAuthV2CommandResult.MFARequired(correlationId, createContinuationState(), methods),
            NativeAuthV2SignInStartCommand::class
        )
        return (application.signInV2(signInParameters()) as NativeAuthResultV2.MFARequired).nextState
    }

    private suspend fun mfaVerificationState(state: MFARequiredStateV2): MFAVerificationRequiredStateV2 {
        enqueueResult(
            NativeAuthV2CommandResult.MFAVerificationRequired(
                correlationId,
                createContinuationState(),
                6,
                "u***@contoso.com",
                "email"
            ),
            NativeAuthV2SelectMFAMethodCommand::class
        )
        return (state.selectAuthMethod(state.authMethods.single()) as
            NativeAuthResultV2.MFAVerificationRequired).nextState
    }

    private fun <T : com.microsoft.identity.nativeauth.statemachine.states.NativeAuthBaseStateV2> parcelRoundTrip(
        state: T,
        creator: android.os.Parcelable.Creator<T>
    ): T {
        val parcel = Parcel.obtain()
        val restoredParcel = Parcel.obtain()
        try {
            state.writeToParcel(parcel, 0)
            val bytes = parcel.marshall()
            restoredParcel.unmarshall(bytes, 0, bytes.size)
            restoredParcel.setDataPosition(0)
            return creator.createFromParcel(restoredParcel)
        } finally {
            parcel.recycle()
            restoredParcel.recycle()
        }
    }

    private fun assertPasswordCleared(password: CharArray) {
        password.forEach { assertEquals('\u0000', it) }
    }

    private fun enqueueResult(
        result: INativeAuthCommandResult,
        commandClass: KClass<out BaseCommand<*>>,
        onCommand: (BaseCommand<*>) -> Unit = {}
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
        } answers {
            onCommand(firstArg())
            future
        }
    }

    private fun enqueueCancellation(
        commandClass: KClass<out BaseCommand<*>>,
        onCommand: (BaseCommand<*>) -> Unit = {}
    ) {
        every {
            CommandDispatcher.submitSilentReturningFuture(
                match { commandClass.java.isInstance(it) }
            )
        } answers {
            onCommand(firstArg())
            throw CancellationException("cancelled")
        }
    }

    private inline fun <reified T : INativeAuthCommandResult> unsupportedResult(): T =
        mockk<T>().also {
            every { it.correlationId } returns correlationId
            every { it.toString() } returns "UnsupportedCommonResult"
            every { it.toUnsanitizedString() } returns "UnsupportedCommonResult"
            every { it.containsPii() } returns false
        }

    private fun createContinuationState(
        correlationId: String = NativeAuthV2SignInTest.correlationId
    ): NativeAuthV2ContinuationState =
        NativeAuthV2ContinuationStateTestFactory.create(
            correlationId = correlationId,
            entryRelation = NativeAuthV2LinkRelation.SIGN_IN,
            scenario = NativeAuthV2FlowScenario.SIGN_IN
        )

    private companion object {
        const val correlationId = "correlation-id"
        val errorCodes = listOf(50126)
    }
}
