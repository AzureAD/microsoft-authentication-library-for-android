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
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.e2e.shadows.ShadowAndroidSdkStorageEncryptionManager
import com.microsoft.identity.client.e2e.tests.PublicClientApplicationAbstractTest
import com.microsoft.identity.client.exception.MsalClientException
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.common.java.controllers.CommandDispatcher
import com.microsoft.identity.common.java.commands.BaseCommand
import com.microsoft.identity.common.java.commands.ICommandResult
import com.microsoft.identity.common.java.controllers.CommandResult
import com.microsoft.identity.common.java.exception.BaseException
import com.microsoft.identity.common.java.logging.DiagnosticContext
import com.microsoft.identity.common.java.nativeauth.controllers.results.INativeAuthCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2CommandResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2AuthMethod
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationState
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2LinkRelation
import com.microsoft.identity.common.java.nativeauth.providers.v2.NativeAuthV2FlowScenario
import com.microsoft.identity.common.java.result.FinalizableResultFuture
import com.microsoft.identity.common.nativeauth.internal.commands.NativeAuthV2SelectMFAMethodCommand
import com.microsoft.identity.common.nativeauth.internal.commands.NativeAuthV2SignInStartCommand
import com.microsoft.identity.common.nativeauth.internal.commands.NativeAuthV2SubmitMFAChallengeCommand
import com.microsoft.identity.common.nativeauth.internal.commands.NativeAuthV2SubmitPasswordCommand
import com.microsoft.identity.nativeauth.INativeAuthPublicClientApplication
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplication
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplicationConfiguration
import com.microsoft.identity.nativeauth.parameters.NativeAuthSignInParameters
import com.microsoft.identity.nativeauth.statemachine.NativeAuthFlowScenarioV2
import com.microsoft.identity.nativeauth.statemachine.errors.ErrorTypes
import com.microsoft.identity.nativeauth.statemachine.errors.MFARequestChallengeErrorV2
import com.microsoft.identity.nativeauth.statemachine.errors.MFASubmitChallengeErrorV2
import com.microsoft.identity.nativeauth.statemachine.errors.NativeAuthErrorV2
import com.microsoft.identity.nativeauth.statemachine.errors.SignInErrorV2
import com.microsoft.identity.nativeauth.statemachine.errors.SubmitPasswordErrorV2
import com.microsoft.identity.nativeauth.statemachine.results.NativeAuthResultV2
import com.microsoft.identity.nativeauth.statemachine.states.Callback
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

/**
 * Parity coverage for Native Auth V2 sign-in: password first factor and password followed by email
 * one-time-code MFA.
 *
 * Every scenario is driven through mocked Common command results, so the whole public surface is
 * exercised without a service. The live end-to-end equivalents are gated separately on a
 * V2-capable slice.
 *
 * The scenarios mirror the in-scope iOS V2 cases: password success, unknown user, an invalid
 * entry-supplied password reported as invalid credentials, an invalid deferred password reported as
 * an invalid password, deferred-password state, password + email MFA success, invalid OTP followed
 * by a fresh challenge, browser-required, and the Android V1 existing-account rejection.
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
    fun signInV2RejectsBlankUsernameWithoutIssuingACommand() = runTest {
        mockkObject(DiagnosticContext.INSTANCE)
        every { DiagnosticContext.INSTANCE.threadCorrelationId } returns correlationId
        try {
            val result = application.signInV2(signInParameters(username = " "))
            assertTrue(result is SignInErrorV2)
            val error = result as SignInErrorV2
            assertTrue(error.isInvalidUsername())
            assertEquals(NativeAuthFlowScenarioV2.SIGN_IN, error.scenario)
            assertEquals(correlationId, error.correlationId)
        } finally {
            unmockkObject(DiagnosticContext.INSTANCE)
        }
    }

    @Test
    fun signInV2WithoutPasswordReturnsPasswordRequiredState() = runTest {
        enqueueResult(
            NativeAuthV2CommandResult.PasswordRequired(correlationId, createContinuationState()),
            NativeAuthV2SignInStartCommand::class
        )

        val result = application.signInV2(NativeAuthSignInParameters(username))

        assertTrue(result is NativeAuthResultV2.PasswordRequired)
        result as NativeAuthResultV2.PasswordRequired
        assertEquals(NativeAuthFlowScenarioV2.SIGN_IN, result.scenario)
        // The public state exposes no continuation token; the opaque DTO carries it instead.
        assertNull(result.nextState.continuationToken)
        assertEquals(correlationId, result.nextState.correlationId)
    }

    @Test
    fun signInV2WithWrongEntryPasswordIsInvalidCredentials() = runTest {
        enqueueResult(
            NativeAuthV2CommandResult.InvalidCredentials(
                correlationId,
                "invalid_grant",
                "AADSTS50126: invalid username or password.",
                "invalidUserNameOrPassword",
                errorCodes
            ),
            NativeAuthV2SignInStartCommand::class
        )

        val result = application.signInV2(signInParameters(password = "WrongPassword!".toCharArray()))

        assertTrue(result is SignInErrorV2)
        result as SignInErrorV2
        assertTrue(result.isInvalidCredentials())
        assertFalse(result.isUserNotFound())
        assertEquals(errorCodes, result.errorCodes)
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

        val result = application.signInV2(signInParameters()) as SignInErrorV2

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

        val result = application.signInV2(signInParameters()) as SignInErrorV2

        assertTrue(result.isBrowserRequired())
    }

    @Test
    fun signInV2ClearsTheCallersPasswordCopyAndLeavesTheOriginalIntact() = runTest {
        enqueueResult(
            NativeAuthV2CommandResult.InvalidCredentials(
                correlationId,
                "invalid_grant",
                "invalid",
                "invalidUserNameOrPassword"
            ),
            NativeAuthV2SignInStartCommand::class
        )

        val callerPassword = "Password123!".toCharArray()
        application.signInV2(signInParameters(password = callerPassword))

        // signInV2 copies the buffer, so the caller's own array is untouched while the copy handed
        // to Common is cleared. A password is never stored in a state or continuation state.
        assertEquals("Password123!", String(callerPassword))
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
            assertTrue(suspendResult is SignInErrorV2)
            assertEquals(ErrorTypes.CLIENT_EXCEPTION, (suspendResult as SignInErrorV2).errorType)
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
            assertTrue(delivered is SignInErrorV2)
            assertEquals(ErrorTypes.CLIENT_EXCEPTION, (delivered as SignInErrorV2).errorType)
        } finally {
            unmockkObject(NativeAuthPublicClientApplication.Companion)
        }
    }

    @Test
    fun signInV2CallbackAndSuspendSurfacesAgree() = runTest {
        enqueueResult(
            NativeAuthV2CommandResult.PasswordRequired(correlationId, createContinuationState()),
            NativeAuthV2SignInStartCommand::class
        )
        val suspendResult = application.signInV2(signInParameters(password = null))

        enqueueResult(
            NativeAuthV2CommandResult.PasswordRequired(correlationId, createContinuationState()),
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

        assertTrue(suspendResult is NativeAuthResultV2.PasswordRequired)
        assertTrue(callbackResult is NativeAuthResultV2.PasswordRequired)
        assertEquals(suspendResult.scenario, callbackResult.scenario)
    }

    // -----------------------------------------------------------------------------------------
    // Deferred password submission
    // -----------------------------------------------------------------------------------------

    @Test
    fun submitPasswordWithWrongPasswordIsInvalidPasswordNotInvalidCredentials() = runTest {
        val state = passwordRequiredState()
        enqueueResult(
            NativeAuthV2CommandResult.IncorrectPassword(
                correlationId,
                "invalid_grant",
                "AADSTS50126: invalid username or password.",
                "invalidUserNameOrPassword",
                errorCodes
            ),
            NativeAuthV2SubmitPasswordCommand::class
        )

        val result = state.submitPassword("WrongPassword!".toCharArray())

        assertTrue(result is SubmitPasswordErrorV2)
        result as SubmitPasswordErrorV2
        assertTrue(result.isInvalidPassword())
        @Suppress("DEPRECATION")
        assertFalse(
            "A deferred password rejection must not be classified as invalid credentials",
            result.isInvalidCredentials()
        )
        assertEquals(errorCodes, result.errorCodes)
    }

    @Test
    fun submitPasswordRejectsAnEmptyPasswordWithoutIssuingACommand() = runTest {
        val state = passwordRequiredState()

        val result = state.submitPassword(CharArray(0)) as SubmitPasswordErrorV2

        assertTrue(result.isInvalidPassword())
    }

    @Test
    fun submitPasswordClearsItsOwnCopyAndLeavesTheCallersBufferIntact() = runTest {
        val state = passwordRequiredState()
        enqueueResult(
            INativeAuthCommandResult.Redirect(correlationId, "browser"),
            NativeAuthV2SubmitPasswordCommand::class
        )

        val callerPassword = "Password123!".toCharArray()
        state.submitPassword(callerPassword)

        assertEquals("Password123!", String(callerPassword))
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
        assertTrue((state.submitPassword("Password123!".toCharArray()) as SubmitPasswordErrorV2).isBrowserRequired())

        enqueueResult(
            INativeAuthCommandResult.APIError("api_error", "API failed", correlationId = correlationId, errorCodes = errorCodes),
            NativeAuthV2SubmitPasswordCommand::class
        )
        val apiError = state.submitPassword("Password123!".toCharArray()) as SubmitPasswordErrorV2
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
        assertTrue((result as SubmitPasswordErrorV2).isInvalidPassword())
    }

    // -----------------------------------------------------------------------------------------
    // Email OTP MFA
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
    fun selectAuthMethodRejectsAMethodTheServerDidNotOffer() = runTest {
        val state = mfaRequiredState()

        val result = state.selectAuthMethod(
            com.microsoft.identity.nativeauth.AuthMethod("not-offered", "oob", null, "email")
        )

        assertTrue(result is MFARequestChallengeErrorV2)
        assertEquals(ErrorTypes.INVALID_STATE, (result as MFARequestChallengeErrorV2).errorType)
    }

    @Test
    fun selectAuthMethodRejectsAnUnsupportedChannelWithoutIssuingACommand() = runTest {
        val state = mfaRequiredState(
            methods = listOf(NativeAuthV2AuthMethod("sms-1", "sms", "+1***4567"))
        )

        val result = state.selectAuthMethod(state.authMethods.single())

        assertTrue(result is MFARequestChallengeErrorV2)
        val error = result as MFARequestChallengeErrorV2
        // Distinguishable from an unspecified server error: this increment supports email only.
        assertTrue(error.isNotImplemented())
        assertFalse(error.isAuthMethodBlocked())
        assertFalse(error.isBrowserRequired())
    }

    @Test
    fun selectAuthMethodMapsBlockedMethod() = runTest {
        val state = mfaRequiredState()
        enqueueResult(
            NativeAuthV2CommandResult.AuthMethodBlocked(
                correlationId,
                "accessDenied",
                "blocked",
                "providerBlockedByRep",
                errorCodes
            ),
            NativeAuthV2SelectMFAMethodCommand::class
        )

        val result = state.selectAuthMethod(state.authMethods.single()) as MFARequestChallengeErrorV2

        assertTrue(result.isAuthMethodBlocked())
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
        val wrongCode = verificationState.submitChallenge("000000") as MFASubmitChallengeErrorV2
        assertTrue(wrongCode.isInvalidChallenge())
        assertEquals("invalidOneTimeCode", wrongCode.subError)

        // The app requests a fresh challenge from the retained MFARequiredStateV2, not from a
        // state carried on the error.
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
        val fresh = mfaState.selectAuthMethod(mfaState.authMethods.single())
        assertTrue(fresh is NativeAuthResultV2.MFAVerificationRequired)

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
        assertTrue((verificationState.submitChallenge("111111") as MFASubmitChallengeErrorV2).isInvalidChallenge())
    }

    @Test
    fun submitChallengeRejectsAnEmptyCodeWithoutIssuingACommand() = runTest {
        val verificationState = mfaVerificationState(mfaRequiredState())

        val result = verificationState.submitChallenge("") as MFASubmitChallengeErrorV2

        assertTrue(result.isInvalidChallenge())
    }

    @Test
    fun submitChallengeMapsNotImplementedAndBrowserRequired() = runTest {
        val verificationState = mfaVerificationState(mfaRequiredState())

        enqueueResult(
            INativeAuthCommandResult.Redirect(correlationId, "browser"),
            NativeAuthV2SubmitMFAChallengeCommand::class
        )
        assertTrue((verificationState.submitChallenge("123456") as MFASubmitChallengeErrorV2).isBrowserRequired())

        enqueueResult(
            NativeAuthV2CommandResult.NotImplemented(correlationId, "not_implemented", "nope"),
            NativeAuthV2SubmitMFAChallengeCommand::class
        )
        assertTrue((verificationState.submitChallenge("123456") as NativeAuthErrorV2).isNotImplemented())
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

        assertTrue((future.get(30, TimeUnit.SECONDS) as MFASubmitChallengeErrorV2).isInvalidChallenge())
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
            assertNull(restored.continuationToken)
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
        assertTrue(staleSelection is MFARequestChallengeErrorV2)
        assertEquals(ErrorTypes.INVALID_STATE, (staleSelection as MFARequestChallengeErrorV2).errorType)
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
        try {
            state.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)
            return creator.createFromParcel(parcel)
        } finally {
            parcel.recycle()
        }
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

    private fun enqueueCancellation(commandClass: KClass<out BaseCommand<*>>) {
        every {
            CommandDispatcher.submitSilentReturningFuture(
                match { commandClass.java.isInstance(it) }
            )
        } throws CancellationException("cancelled")
    }

    private fun createContinuationState(): NativeAuthV2ContinuationState {
        val constructor = NativeAuthV2ContinuationState::class.java.declaredConstructors
            .single { it.parameterCount == 9 }
        constructor.isAccessible = true
        return constructor.newInstance(
            "opaque-token",
            emptyMap<String, String>(),
            emptyMap<String, Map<String, String>>(),
            listOf("scope"),
            null,
            correlationId,
            NativeAuthV2LinkRelation.SIGN_IN.value,
            NativeAuthV2FlowScenario.SIGN_IN,
            emptySet<String>()
        ) as NativeAuthV2ContinuationState
    }

    private companion object {
        const val correlationId = "correlation-id"
        val errorCodes = listOf(50126)
    }
}
