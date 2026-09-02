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
import com.microsoft.identity.client.AuthenticationResultAdapter
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.e2e.shadows.ShadowAndroidSdkStorageEncryptionManager
import com.microsoft.identity.client.e2e.tests.PublicClientApplicationAbstractTest
import com.microsoft.identity.client.exception.MsalClientException
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.common.java.commands.BaseCommand
import com.microsoft.identity.common.java.commands.ICommandResult
import com.microsoft.identity.common.java.controllers.CommandDispatcher
import com.microsoft.identity.common.java.controllers.CommandResult
import com.microsoft.identity.common.java.exception.BaseException
import com.microsoft.identity.common.java.logging.DiagnosticContext
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignUpV2StartCommandParameters
import com.microsoft.identity.common.java.nativeauth.controllers.results.INativeAuthCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2CommandResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationState
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2LinkRelation
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2RequiredAttribute
import com.microsoft.identity.common.java.nativeauth.providers.v2.NativeAuthV2FlowScenario
import com.microsoft.identity.common.java.result.FinalizableResultFuture
import com.microsoft.identity.common.java.result.ILocalAuthenticationResult
import com.microsoft.identity.common.java.util.ResultFuture
import com.microsoft.identity.common.nativeauth.internal.commands.NativeAuthV2SignInAfterSignUpCommand
import com.microsoft.identity.common.nativeauth.internal.commands.NativeAuthV2SignUpStartCommand
import com.microsoft.identity.common.nativeauth.internal.commands.NativeAuthV2SubmitAttributesCommand
import com.microsoft.identity.common.nativeauth.internal.commands.NativeAuthV2SubmitCodeCommand
import com.microsoft.identity.nativeauth.INativeAuthPublicClientApplication
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplication
import com.microsoft.identity.nativeauth.UserAttributes
import com.microsoft.identity.nativeauth.parameters.NativeAuthSignUpParameters
import com.microsoft.identity.nativeauth.statemachine.NativeAuthFlowScenarioV2
import com.microsoft.identity.nativeauth.statemachine.errors.ErrorTypes
import com.microsoft.identity.nativeauth.statemachine.errors.NativeAuthErrorV2
import com.microsoft.identity.nativeauth.statemachine.errors.SignUpErrorV2
import com.microsoft.identity.nativeauth.statemachine.errors.SubmitAttributesErrorV2
import com.microsoft.identity.nativeauth.statemachine.errors.SubmitCodeErrorV2
import com.microsoft.identity.nativeauth.statemachine.errors.SubmitPasswordErrorV2
import com.microsoft.identity.nativeauth.statemachine.results.NativeAuthResultV2
import com.microsoft.identity.nativeauth.statemachine.states.AttributesInvalidStateV2
import com.microsoft.identity.nativeauth.statemachine.states.AttributesRequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.CodeRequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.PasswordRequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.SignInAfterSignUpStateV2
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
import org.junit.Assert.assertNotSame
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
 * Parity coverage for Native Auth V2 sign-up. Every scenario is driven through mocked Common
 * command results, so the whole public sign-up surface — the [INativeAuthPublicClientApplication.signUpV2]
 * entry and each follow-up state ([CodeRequiredStateV2], [AttributesRequiredStateV2],
 * [AttributesInvalidStateV2], [PasswordRequiredStateV2], [SignInAfterSignUpStateV2]) — is exercised
 * without a service.
 *
 * The scenarios mirror the in-scope iOS V2 sign-up cases and the server traces in
 * `AI Docs/signup.txt`: email one-time-code verification, deferred password and attributes,
 * user-already-exists, attribute-validation errors (including a password-policy violation surfaced
 * as an invalid password), browser-required, and the explicit sign-in-after-sign-up completion.
 */
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowAndroidSdkStorageEncryptionManager::class])
class NativeAuthV2SignUpTest : PublicClientApplicationAbstractTest() {

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
    // signUpV2 entry
    // -----------------------------------------------------------------------------------------

    @Test
    fun signUpV2RejectsBlankUsernameWithoutIssuingACommand() = runTest {
        mockkObject(DiagnosticContext.INSTANCE)
        every { DiagnosticContext.INSTANCE.threadCorrelationId } returns correlationId
        try {
            val result = application.signUpV2(signUpParameters(username = " "))
            assertTrue(result is SignUpErrorV2)
            val error = result as SignUpErrorV2
            assertTrue(error.isInvalidUsername())
            assertEquals(NativeAuthFlowScenarioV2.SIGN_UP, error.scenario)
            assertEquals(correlationId, error.correlationId)
        } finally {
            unmockkObject(DiagnosticContext.INSTANCE)
        }
    }

    @Test
    fun signUpV2WithPasswordSurfacesCodeRequired() = runTest {
        enqueueResult(
            NativeAuthV2CommandResult.CodeRequired(
                correlationId,
                createContinuationState(),
                6,
                "u***@contoso.com",
                "email"
            ),
            NativeAuthV2SignUpStartCommand::class
        )

        val result = application.signUpV2(signUpParameters())

        assertTrue(result is NativeAuthResultV2.CodeRequired)
        result as NativeAuthResultV2.CodeRequired
        assertEquals(NativeAuthFlowScenarioV2.SIGN_UP, result.scenario)
        assertEquals(6, result.codeLength)
        assertEquals("u***@contoso.com", result.sentTo)
        assertNull(result.nextState.continuationToken)
    }

    @Test
    fun signUpV2WithoutPasswordSurfacesPasswordRequired() = runTest {
        enqueueResult(
            NativeAuthV2CommandResult.PasswordRequired(correlationId, createContinuationState()),
            NativeAuthV2SignUpStartCommand::class
        )

        val result = application.signUpV2(signUpParameters(password = null))

        assertTrue(result is NativeAuthResultV2.PasswordRequired)
        assertEquals(
            NativeAuthFlowScenarioV2.SIGN_UP,
            (result as NativeAuthResultV2.PasswordRequired).scenario
        )
    }

    @Test
    fun signUpV2SurfacesAttributesRequiredWithMappedAttributes() = runTest {
        enqueueResult(
            NativeAuthV2CommandResult.AttributesRequired(
                correlationId,
                createContinuationState(),
                listOf(NativeAuthV2RequiredAttribute("city", "string", true))
            ),
            NativeAuthV2SignUpStartCommand::class
        )

        val result = application.signUpV2(signUpParameters())

        assertTrue(result is NativeAuthResultV2.AttributesRequired)
        result as NativeAuthResultV2.AttributesRequired
        assertEquals(1, result.requiredAttributes.size)
        assertEquals("city", result.requiredAttributes.single().attributeName)
    }

    @Test
    fun signUpV2SurfacesRetryableAttributesInvalid() = runTest {
        enqueueResult(
            NativeAuthV2CommandResult.AttributesInvalid(
                correlationId,
                createContinuationState(),
                listOf("city"),
                "attribute_validation_failed",
                "AADSTS1002027: attribute validation failed.",
                listOf(1002027)
            ),
            NativeAuthV2SignUpStartCommand::class
        )

        val result = application.signUpV2(signUpParameters())

        assertTrue(result is NativeAuthResultV2.AttributesInvalid)
        assertEquals(
            listOf("city"),
            (result as NativeAuthResultV2.AttributesInvalid).invalidAttributes
        )
    }

    @Test
    fun signUpV2WithExistingAccountIsUserAlreadyExists() = runTest {
        enqueueResult(
            NativeAuthV2CommandResult.UserAlreadyExists(
                correlationId,
                "user_already_exists",
                "AADSTS1003037: account already exists.",
                listOf(1003037)
            ),
            NativeAuthV2SignUpStartCommand::class
        )

        val result = application.signUpV2(signUpParameters()) as SignUpErrorV2

        assertTrue(result.isUserAlreadyExists())
        assertEquals(listOf(1003037), result.errorCodes)
        assertEquals(NativeAuthFlowScenarioV2.SIGN_UP, result.scenario)
    }

    @Test
    fun signUpV2MapsBrowserRequiredToSignUpError() = runTest {
        enqueueResult(
            INativeAuthCommandResult.Redirect(correlationId, "browser"),
            NativeAuthV2SignUpStartCommand::class
        )

        val result = application.signUpV2(signUpParameters()) as SignUpErrorV2

        assertTrue(result.isBrowserRequired())
    }

    @Test
    fun signUpV2MapsNotImplementedToNativeAuthError() = runTest {
        enqueueResult(
            NativeAuthV2CommandResult.NotImplemented(correlationId, "unsupported", "unsupported"),
            NativeAuthV2SignUpStartCommand::class
        )

        val result = application.signUpV2(signUpParameters())

        assertTrue(result is NativeAuthErrorV2)
        assertTrue((result as NativeAuthErrorV2).isNotImplemented())
    }

    @Test
    fun signUpV2PropagatesCancellationRatherThanReportingAnAuthError() = runTest {
        enqueueCancellation(NativeAuthV2SignUpStartCommand::class)

        try {
            application.signUpV2(signUpParameters())
            fail("Expected CancellationException")
        } catch (_: CancellationException) {
            // Expected: cancellation is not an authentication failure.
        }
    }

    @Test
    fun signUpV2ClearsTheOwnedPasswordCopyOnCompletionAndCancellationWhileLeavingTheCallersBufferIntact() = runTest {
        var capturedOnCompletion: CharArray? = null
        enqueueResult(
            NativeAuthV2CommandResult.PasswordRequired(correlationId, createContinuationState()),
            NativeAuthV2SignUpStartCommand::class
        ) { command ->
            capturedOnCompletion = (command.parameters as SignUpV2StartCommandParameters).password
        }

        val callerPassword = "Password123!".toCharArray()
        application.signUpV2(signUpParameters(password = callerPassword))

        // signUpV2 submits a distinct, owned copy -- never the caller's own array -- and wipes that
        // copy on exit, so the caller's buffer stays intact while the owned copy is zeroed. A
        // password is never stored in a Parcelable state or in the opaque continuation state.
        assertNotSame(callerPassword, capturedOnCompletion)
        assertEquals("Password123!", String(callerPassword))
        assertPasswordCleared(capturedOnCompletion!!)

        var capturedOnCancellation: CharArray? = null
        enqueueCancellation(NativeAuthV2SignUpStartCommand::class) { command ->
            capturedOnCancellation = (command.parameters as SignUpV2StartCommandParameters).password
        }
        val secondCallerPassword = "Password123!".toCharArray()
        try {
            application.signUpV2(signUpParameters(password = secondCallerPassword))
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
    fun signUpV2RejectsWhenAnAccountIsAlreadySignedIn() = runTest {
        mockkObject(NativeAuthPublicClientApplication.Companion)
        every {
            NativeAuthPublicClientApplication.getCurrentAccountInternal(any())
        } returns mockk<com.microsoft.identity.client.IAccount>()
        try {
            val result = application.signUpV2(signUpParameters())
            assertTrue(result is SignUpErrorV2)
            assertEquals(ErrorTypes.CLIENT_EXCEPTION, (result as SignUpErrorV2).errorType)
            assertTrue(result.exception is MsalClientException)
        } finally {
            unmockkObject(NativeAuthPublicClientApplication.Companion)
        }
    }

    @Test
    fun signUpV2CallbackAndSuspendSurfacesAgree() = runTest {
        enqueueResult(
            NativeAuthV2CommandResult.PasswordRequired(correlationId, createContinuationState()),
            NativeAuthV2SignUpStartCommand::class
        )
        val suspendResult = application.signUpV2(signUpParameters(password = null))

        enqueueResult(
            NativeAuthV2CommandResult.PasswordRequired(correlationId, createContinuationState()),
            NativeAuthV2SignUpStartCommand::class
        )
        val future = ResultFuture<NativeAuthResultV2>()
        application.signUpV2(
            signUpParameters(password = null),
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
    // Code verification (email one-time code)
    // -----------------------------------------------------------------------------------------

    @Test
    fun submitCodeCompletesSignUpThroughSignInAfterSignUp() = runTest {
        val state = codeRequiredState()
        enqueueResult(
            NativeAuthV2CommandResult.SignInAfterSignUpRequired(correlationId, createContinuationState()),
            NativeAuthV2SubmitCodeCommand::class
        )

        val result = state.submitCode("123456")

        assertTrue(result is NativeAuthResultV2.SignInAfterSignUpRequired)
        assertEquals(
            NativeAuthFlowScenarioV2.SIGN_UP,
            (result as NativeAuthResultV2.SignInAfterSignUpRequired).scenario
        )
    }

    @Test
    fun submitCodeCanSurfaceFurtherAttributesOrPassword() = runTest {
        val state = codeRequiredState()

        enqueueResult(
            NativeAuthV2CommandResult.AttributesRequired(
                correlationId,
                createContinuationState(),
                listOf(NativeAuthV2RequiredAttribute("city", "string", true))
            ),
            NativeAuthV2SubmitCodeCommand::class
        )
        assertTrue(state.submitCode("123456") is NativeAuthResultV2.AttributesRequired)

        enqueueResult(
            NativeAuthV2CommandResult.PasswordRequired(correlationId, createContinuationState()),
            NativeAuthV2SubmitCodeCommand::class
        )
        assertTrue(state.submitCode("123456") is NativeAuthResultV2.PasswordRequired)
    }

    @Test
    fun submitCodeWithIncorrectCodeIsRetryableInvalidCode() = runTest {
        val state = codeRequiredState()
        enqueueResult(
            NativeAuthV2CommandResult.IncorrectCode(
                correlationId,
                "invalid_grant",
                "AADSTS50181: invalid code.",
                "invalidOneTimeCode",
                listOf(50181)
            ),
            NativeAuthV2SubmitCodeCommand::class
        )

        val result = state.submitCode("000000") as SubmitCodeErrorV2

        assertTrue(result.isInvalidCode())
    }

    // -----------------------------------------------------------------------------------------
    // Deferred attributes
    // -----------------------------------------------------------------------------------------

    @Test
    fun submitAttributesCompletesSignUp() = runTest {
        val state = attributesRequiredState()
        enqueueResult(
            NativeAuthV2CommandResult.SignInAfterSignUpRequired(correlationId, createContinuationState()),
            NativeAuthV2SubmitAttributesCommand::class
        )

        val result = state.submitAttributes(UserAttributes.Builder().build())

        assertTrue(result is NativeAuthResultV2.SignInAfterSignUpRequired)
    }

    @Test
    fun submitAttributesWithInvalidValueIsRetryableAttributesInvalid() = runTest {
        val state = attributesRequiredState()
        enqueueResult(
            NativeAuthV2CommandResult.AttributesInvalid(
                correlationId,
                createContinuationState(),
                listOf("city"),
                "attribute_validation_failed",
                "AADSTS1002027: attribute validation failed.",
                listOf(1002027)
            ),
            NativeAuthV2SubmitAttributesCommand::class
        )

        val result = state.submitAttributes(UserAttributes.Builder().build())

        assertTrue(result is NativeAuthResultV2.AttributesInvalid)
        // The retryable state must accept another submit-attributes attempt against the same state.
        assertTrue(
            (result as NativeAuthResultV2.AttributesInvalid).nextState is AttributesInvalidStateV2
        )
    }

    @Test
    fun submitAttributesMapsUserAlreadyExists() = runTest {
        val state = attributesRequiredState()
        enqueueResult(
            NativeAuthV2CommandResult.UserAlreadyExists(
                correlationId,
                "user_already_exists",
                "AADSTS1003037: account already exists.",
                listOf(1003037)
            ),
            NativeAuthV2SubmitAttributesCommand::class
        )

        val result = state.submitAttributes(UserAttributes.Builder().build()) as SignUpErrorV2

        assertTrue(result.isUserAlreadyExists())
    }

    @Test
    fun submitAttributesMapsBrowserRequired() = runTest {
        val state = attributesRequiredState()
        enqueueResult(
            INativeAuthCommandResult.Redirect(correlationId, "browser"),
            NativeAuthV2SubmitAttributesCommand::class
        )

        val result = state.submitAttributes(UserAttributes.Builder().build()) as SubmitAttributesErrorV2

        assertTrue(result.isBrowserRequired())
    }

    @Test
    fun attributesInvalidStateAcceptsRetryThatCompletesSignUp() = runTest {
        val invalidState = attributesInvalidState()
        enqueueResult(
            NativeAuthV2CommandResult.SignInAfterSignUpRequired(correlationId, createContinuationState()),
            NativeAuthV2SubmitAttributesCommand::class
        )

        val result = invalidState.submitAttributes(UserAttributes.Builder().build())

        assertTrue(result is NativeAuthResultV2.SignInAfterSignUpRequired)
    }

    // -----------------------------------------------------------------------------------------
    // Deferred password (sign-up scenario)
    // -----------------------------------------------------------------------------------------

    @Test
    fun submitPasswordCompletesSignUp() = runTest {
        val state = signUpPasswordRequiredState()
        enqueueResult(
            NativeAuthV2CommandResult.SignInAfterSignUpRequired(correlationId, createContinuationState()),
            NativeAuthV2SubmitAttributesCommand::class
        )

        val result = state.submitPassword("Password123!".toCharArray())

        assertTrue(result is NativeAuthResultV2.SignInAfterSignUpRequired)
    }

    @Test
    fun submitPasswordWithPolicyViolationIsRetryableInvalidPassword() = runTest {
        val state = signUpPasswordRequiredState()
        // A sign-up password-policy violation comes back as an attribute-validation error on the
        // `password` attribute; the deferred password state maps it to a same-state invalid-password
        // error so the app can retry.
        enqueueResult(
            NativeAuthV2CommandResult.AttributesInvalid(
                correlationId,
                createContinuationState(),
                listOf("password"),
                "attribute_validation_failed",
                "AADSTS1002027: password policy violation.",
                listOf(1002027)
            ),
            NativeAuthV2SubmitAttributesCommand::class
        )

        val result = state.submitPassword("weak".toCharArray()) as SubmitPasswordErrorV2

        assertTrue(result.isInvalidPassword())
    }

    // -----------------------------------------------------------------------------------------
    // Sign-in after sign-up
    // -----------------------------------------------------------------------------------------

    @Test
    fun signInAfterSignUpCompletesAndReturnsAccount() = runTest {
        val signInState = signInAfterSignUpState()
        val localResult = mockk<ILocalAuthenticationResult>()
        val authenticationResult = mockk<IAuthenticationResult>()
        every { authenticationResult.account } returns mockk<IAccount>()
        mockkStatic(AuthenticationResultAdapter::class)
        try {
            every { AuthenticationResultAdapter.adapt(localResult) } returns authenticationResult
            enqueueResult(
                NativeAuthV2CommandResult.Complete(correlationId, localResult, null, null),
                NativeAuthV2SignInAfterSignUpCommand::class
            )

            val complete = signInState.signIn() as NativeAuthResultV2.Complete

            assertEquals(correlationId, complete.resultValue.correlationId)
        } finally {
            unmockkStatic(AuthenticationResultAdapter::class)
        }
    }

    @Test
    fun signInAfterSignUpMapsErrorsAndCancellation() = runTest {
        val signInState = signInAfterSignUpState()

        enqueueResult(
            INativeAuthCommandResult.APIError(
                error = "api_error",
                errorDescription = "boom",
                errorCodes = errorCodes,
                correlationId = correlationId
            ),
            NativeAuthV2SignInAfterSignUpCommand::class
        )
        assertEquals(errorCodes, (signInState.signIn() as NativeAuthErrorV2).errorCodes)

        enqueueResult(
            INativeAuthCommandResult.Redirect(correlationId, "browser"),
            NativeAuthV2SignInAfterSignUpCommand::class
        )
        assertTrue((signInState.signIn() as NativeAuthErrorV2).isBrowserRequired())

        enqueueCancellation(NativeAuthV2SignInAfterSignUpCommand::class)
        try {
            signInState.signIn()
            fail("Expected CancellationException")
        } catch (_: CancellationException) {
            // Expected.
        }
    }

    // -----------------------------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------------------------

    private fun signUpParameters(
        username: String = this.username,
        password: CharArray? = "Password123!".toCharArray(),
        attributes: UserAttributes? = null
    ): NativeAuthSignUpParameters = NativeAuthSignUpParameters(username).also {
        it.password = password
        it.attributes = attributes
    }

    private suspend fun codeRequiredState(): CodeRequiredStateV2 {
        enqueueResult(
            NativeAuthV2CommandResult.CodeRequired(
                correlationId,
                createContinuationState(),
                6,
                "u***@contoso.com",
                "email"
            ),
            NativeAuthV2SignUpStartCommand::class
        )
        return (application.signUpV2(signUpParameters()) as NativeAuthResultV2.CodeRequired).nextState
    }

    private suspend fun attributesRequiredState(): AttributesRequiredStateV2 {
        enqueueResult(
            NativeAuthV2CommandResult.AttributesRequired(
                correlationId,
                createContinuationState(),
                listOf(NativeAuthV2RequiredAttribute("city", "string", true))
            ),
            NativeAuthV2SignUpStartCommand::class
        )
        return (application.signUpV2(signUpParameters()) as NativeAuthResultV2.AttributesRequired).nextState
    }

    private suspend fun attributesInvalidState(): AttributesInvalidStateV2 {
        enqueueResult(
            NativeAuthV2CommandResult.AttributesInvalid(
                correlationId,
                createContinuationState(),
                listOf("city"),
                "attribute_validation_failed",
                "AADSTS1002027: attribute validation failed.",
                listOf(1002027)
            ),
            NativeAuthV2SignUpStartCommand::class
        )
        return (application.signUpV2(signUpParameters()) as NativeAuthResultV2.AttributesInvalid).nextState
    }

    private suspend fun signUpPasswordRequiredState(): PasswordRequiredStateV2 {
        enqueueResult(
            NativeAuthV2CommandResult.PasswordRequired(correlationId, createContinuationState()),
            NativeAuthV2SignUpStartCommand::class
        )
        return (application.signUpV2(signUpParameters(password = null)) as
            NativeAuthResultV2.PasswordRequired).nextState
    }

    private suspend fun signInAfterSignUpState(): SignInAfterSignUpStateV2 {
        enqueueResult(
            NativeAuthV2CommandResult.SignInAfterSignUpRequired(correlationId, createContinuationState()),
            NativeAuthV2SubmitCodeCommand::class
        )
        return (codeRequiredState().submitCode("123456") as
            NativeAuthResultV2.SignInAfterSignUpRequired).nextState
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

    private fun assertPasswordCleared(password: CharArray) {
        password.forEach { assertEquals('\u0000', it) }
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
            NativeAuthV2LinkRelation.SIGN_UP.value,
            NativeAuthV2FlowScenario.SIGN_UP,
            emptySet<String>()
        ) as NativeAuthV2ContinuationState
    }

    private companion object {
        const val correlationId = "correlation-id"
        val errorCodes = listOf(1002027)
    }
}
