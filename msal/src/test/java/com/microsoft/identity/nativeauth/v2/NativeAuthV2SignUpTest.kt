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
import com.microsoft.identity.common.java.eststelemetry.PublicApiId
import com.microsoft.identity.common.java.exception.BaseException
import com.microsoft.identity.common.java.exception.ConnectionError
import com.microsoft.identity.common.java.logging.DiagnosticContext
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignUpV2StartCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.NativeAuthV2SelectAuthMethodCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.NativeAuthV2SubmitAttributesCommandParameters
import com.microsoft.identity.common.java.nativeauth.controllers.results.INativeAuthCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2CommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2SignInAfterSignUpCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2SignUpStartCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2SignUpSubmitCodeCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2SubmitAttributesCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.SignUpCommandResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationState
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationStateTestFactory
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2AuthMethod
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2LinkRelation
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2RequiredAttribute
import com.microsoft.identity.common.java.nativeauth.providers.v2.NativeAuthV2FlowScenario
import com.microsoft.identity.common.java.result.FinalizableResultFuture
import com.microsoft.identity.common.java.result.ILocalAuthenticationResult
import com.microsoft.identity.common.java.util.ResultFuture
import com.microsoft.identity.common.nativeauth.internal.commands.NativeAuthV2ResendCodeCommand
import com.microsoft.identity.common.nativeauth.internal.commands.NativeAuthV2SignInAfterSignUpCommand
import com.microsoft.identity.common.nativeauth.internal.commands.NativeAuthV2SignUpSubmitCodeCommand
import com.microsoft.identity.common.nativeauth.internal.commands.NativeAuthV2SignUpStartCommand
import com.microsoft.identity.common.nativeauth.internal.commands.NativeAuthV2SelectAuthMethodCommand
import com.microsoft.identity.nativeauth.statemachine.states.AuthMethodSelectionRequiredStateV2
import com.microsoft.identity.nativeauth.AuthMethod
import com.microsoft.identity.common.nativeauth.internal.commands.NativeAuthV2EnrollCommand
import com.microsoft.identity.common.nativeauth.internal.commands.NativeAuthV2ActivateCommand
import com.microsoft.identity.common.nativeauth.internal.commands.NativeAuthV2SignInSubmitCodeCommand
import com.microsoft.identity.common.nativeauth.internal.commands.NativeAuthV2SubmitPasswordCommand
import com.microsoft.identity.common.nativeauth.internal.commands.NativeAuthV2SubmitAttributesCommand
import com.microsoft.identity.nativeauth.INativeAuthPublicClientApplication
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplication
import com.microsoft.identity.nativeauth.UserAttributes
import com.microsoft.identity.nativeauth.parameters.NativeAuthSignUpParametersV2
import com.microsoft.identity.nativeauth.statemachine.NativeAuthFlowScenarioV2
import com.microsoft.identity.nativeauth.statemachine.errors.ErrorTypes
import com.microsoft.identity.nativeauth.statemachine.errors.NativeAuthErrorV2
import com.microsoft.identity.nativeauth.statemachine.results.NativeAuthResultV2
import com.microsoft.identity.nativeauth.statemachine.states.AttributesRequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.CodeRequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.ExistingAccountCodeRequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.ExistingAccountPasswordRequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.PasswordRequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.SignInAfterSignUpStateV2
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
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

/**
 * Parity coverage for Native Auth V2 sign-up. Every scenario is driven through mocked Common
 * command results, so the whole public sign-up surface — the [INativeAuthPublicClientApplication.signUpV2]
 * entry and each follow-up state ([CodeRequiredStateV2], [AttributesRequiredStateV2],
 * [PasswordRequiredStateV2], [SignInAfterSignUpStateV2]) — is exercised
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
            assertTrue(result is NativeAuthErrorV2)
            val error = result as NativeAuthErrorV2
            assertTrue(error.isInvalidUsername())
            assertEquals(NativeAuthFlowScenarioV2.SIGN_UP, error.scenario)
            assertEquals(correlationId, error.correlationId)
        } finally {
            unmockkObject(DiagnosticContext.INSTANCE)
        }
    }

    @Test
    fun signUpV2MapsTypedInvalidAttributesError() = runTest {
        enqueueResult(
            SignUpCommandResult.InvalidAttributes(
                error = "invalid_attributes",
                errorDescription = "Reserved attributes were supplied.",
                invalidAttributes = listOf("email"),
                correlationId = correlationId
            ),
            NativeAuthV2SignUpStartCommand::class
        )

        val result = application.signUpV2(signUpParameters()) as NativeAuthErrorV2

        assertTrue(result.isInvalidAttributes())
    }

    @Test
    fun signUpV2RetainsRequiredAttributeDiagnosticsInError() = runTest {
        enqueueResult(
            NativeAuthV2CommandResult.AttributesInvalid(
                correlationId, createContinuationState(), listOf("city"),
                "invalidRequest", "attribute validation", listOf(1002027),
                subError = "attributeValidationError",
                traceId = "attribute-trace",
                timestamp = "attribute-time",
                attributeValidationDetails = listOf(mapOf("code" to "attributeRequired", "attributeIds" to listOf("city")))
            ),
            NativeAuthV2SignUpStartCommand::class
        )
        val error = application.signUpV2(signUpParameters()) as NativeAuthErrorV2
        assertTrue(error.isMissingRequiredAttributes())
        assertTrue(error.isInvalidAttributes())
        assertFalse(error.isInvalidPassword())
        assertEquals("attributeValidationError", error.subError)
        assertEquals(listOf(1002027), error.errorCodes)
        assertEquals("attribute-trace", error.traceId)
        assertEquals("attribute-time", error.timestamp)
        assertFalse(error.isSuccess())
    }

    @Test
    fun signUpV2PreservesUnknownServiceDiagnostics() = runTest {
        val exception = IllegalStateException("test diagnostic")
        enqueueResult(
            INativeAuthCommandResult.APIError(
                "futureError", "service diagnostic", correlationId = correlationId,
                errorCodes = listOf(1234), exception = exception, subError = "futureSubError",
                traceId = "service-trace", timestamp = "service-time"
            ),
            NativeAuthV2SignUpStartCommand::class
        )
        val error = application.signUpV2(signUpParameters()) as NativeAuthErrorV2
        assertTrue(error.isGeneralError())
        assertEquals("futureError", error.error)
        assertEquals("service diagnostic", error.errorMessage)
        assertEquals("futureSubError", error.subError)
        assertEquals(listOf(1234), error.errorCodes)
        assertEquals(exception, error.exception)
        assertEquals("service-trace", error.traceId)
        assertEquals("service-time", error.timestamp)
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
    }

    @Test
    fun signUpV2WithoutPasswordSurfacesPasswordRequired() = runTest {
        enqueueResult(
            NativeAuthV2CommandResult.PasswordRequired(correlationId, createContinuationState()),
            NativeAuthV2SignUpStartCommand::class
        )

        val result = application.signUpV2(signUpParameters())

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
    fun signUpV2SurfacesAttributesInvalidError() = runTest {
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

        assertTrue(result is NativeAuthErrorV2)
        assertTrue((result as NativeAuthErrorV2).isInvalidAttributes())
        assertFalse(result.isSuccess())
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

        val result = application.signUpV2(signUpParameters()) as NativeAuthErrorV2

        assertTrue(result.isUserAlreadyExists())
        assertEquals(listOf(1003037), result.errorCodes)
        assertEquals(NativeAuthFlowScenarioV2.SIGN_UP, result.scenario)
    }

    @Test
    fun signUpV2MapsBrowserRequiredToSignUpError() = runTest {
        enqueueResult(
            INativeAuthCommandResult.Redirect(
                correlationId, "browser", errorCodes = listOf(123),
                traceId = "browser-trace", timestamp = "browser-time"
            ),
            NativeAuthV2SignUpStartCommand::class
        )

        val result = application.signUpV2(signUpParameters()) as NativeAuthErrorV2

        assertTrue(result.isBrowserRequired())
        assertEquals(listOf(123), result.errorCodes)
        assertEquals("browser-trace", result.traceId)
        assertEquals("browser-time", result.timestamp)
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
    fun signUpV2MappingsRejectUnsupportedCommonResultsAsInvalidState() = runBlocking {
        enqueueResult(
            unsupportedResult<NativeAuthV2SignUpStartCommandResult>(),
            NativeAuthV2SignUpStartCommand::class
        )
        assertEquals(
            ErrorTypes.INVALID_STATE,
            (application.signUpV2(signUpParameters()) as NativeAuthErrorV2).errorType
        )

        val codeState = codeRequiredState()
        enqueueResult(
            unsupportedResult<NativeAuthV2SignUpSubmitCodeCommandResult>(),
            NativeAuthV2SignUpSubmitCodeCommand::class
        )
        assertEquals(
            ErrorTypes.INVALID_STATE,
            (codeState.submitCode("123456") as NativeAuthErrorV2).errorType
        )

        val attributesState = attributesRequiredState()
        enqueueResult(
            unsupportedResult<NativeAuthV2SubmitAttributesCommandResult>(),
            NativeAuthV2SubmitAttributesCommand::class
        )
        assertEquals(
            ErrorTypes.INVALID_STATE,
            (attributesState.submitAttributes(UserAttributes.Builder().build()) as NativeAuthErrorV2).errorType
        )

        val passwordState = signUpPasswordRequiredState()
        enqueueResult(
            unsupportedResult<NativeAuthV2SubmitAttributesCommandResult>(),
            NativeAuthV2SubmitAttributesCommand::class
        )
        assertEquals(
            ErrorTypes.INVALID_STATE,
            (passwordState.submitPassword("Password!".toCharArray()) as NativeAuthErrorV2).errorType
        )

        val signInState = signInAfterSignUpState()
        enqueueResult(
            unsupportedResult<NativeAuthV2SignInAfterSignUpCommandResult>(),
            NativeAuthV2SignInAfterSignUpCommand::class
        )
        assertEquals(
            ErrorTypes.INVALID_STATE,
            (signInState.signIn() as NativeAuthErrorV2).errorType
        )
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
    fun signUpV2ParametersAndContinuationDoNotRetainPasswords() {
        listOf(NativeAuthSignUpParametersV2::class.java, SignUpV2StartCommandParameters::class.java).forEach { type ->
            assertFalse(type.declaredFields.any { it.name.contains("password", ignoreCase = true) })
        }
        assertFalse(NativeAuthV2ContinuationState::class.java.declaredFields.any {
            it.name.startsWith("pendingSignUpPassword") || it.type == CharArray::class.java
        })
        assertFalse(NativeAuthSignUpParametersV2::class.java.methods.any {
            it.name in setOf("getPassword", "setPassword")
        })
        val v1 = com.microsoft.identity.nativeauth.parameters.NativeAuthSignUpParameters(username)
        val password = "V1-password".toCharArray()
        v1.password = password
        assertSame(password, v1.password)
    }

    @Test
    fun signUpV2CallbackSnapshotsMutableParametersSynchronouslyBeforeLaunch() = runTest {
        var capturedUsername: String? = null
        var capturedAttributes: Map<String, String>? = null
        var capturedScopes: List<String>? = null
        var capturedClaims: String? = null
        enqueueResult(
            NativeAuthV2CommandResult.PasswordRequired(correlationId, createContinuationState()),
            NativeAuthV2SignUpStartCommand::class
        ) { command ->
            val commandParameters = command.parameters as SignUpV2StartCommandParameters
            capturedUsername = commandParameters.username
            capturedAttributes = commandParameters.attributes?.toMap()
            capturedScopes = commandParameters.scopes
            capturedClaims = commandParameters.claimsRequestJson
        }

        val attributesBuilder = UserAttributes.Builder().city("Redmond")
        val parameters = signUpParameters(
            username = username,
            attributes = attributesBuilder.build()
        )
        val scopes = mutableListOf("User.Read")
        val claims = com.microsoft.identity.client.claims.ClaimsRequest().also { it.requestClaimInIdToken("auth_time", null) }
        val expectedClaims = com.microsoft.identity.client.claims.ClaimsRequest.getJsonStringFromClaimsRequest(claims)
        parameters.scopes = scopes
        parameters.claimsRequest = claims
        val future = ResultFuture<NativeAuthResultV2>()
        application.signUpV2(
            parameters,
            object : NativeAuthPublicClientApplication.NativeAuthV2Callback {
                override fun onResult(result: NativeAuthResultV2) = future.setResult(result)
                override fun onError(exception: BaseException) = future.setException(exception)
            }
        )
        attributesBuilder.city("Seattle")
        parameters.attributes = null
        scopes.clear()
        claims.requestClaimInIdToken("changed", null)
        parameters.claimsRequest = null

        future.get(10, TimeUnit.SECONDS)

        assertEquals(username, capturedUsername)
        assertEquals(mapOf("city" to "Redmond"), capturedAttributes)
        assertEquals(listOf("User.Read"), capturedScopes)
        assertEquals(expectedClaims, capturedClaims)
    }

    @Test
    fun signUpV2RejectsWhenAnAccountIsAlreadySignedIn() = runTest {
        mockkObject(NativeAuthPublicClientApplication.Companion)
        every {
            NativeAuthPublicClientApplication.getCurrentAccountInternal(any())
        } returns mockk<com.microsoft.identity.client.IAccount>()
        try {
            val result = application.signUpV2(signUpParameters())
            assertTrue(result is NativeAuthErrorV2)
            assertEquals(ErrorTypes.CLIENT_EXCEPTION, (result as NativeAuthErrorV2).errorType)
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
        val suspendResult = application.signUpV2(signUpParameters())

        enqueueResult(
            NativeAuthV2CommandResult.PasswordRequired(correlationId, createContinuationState()),
            NativeAuthV2SignUpStartCommand::class
        )
        val future = ResultFuture<NativeAuthResultV2>()
        application.signUpV2(
            signUpParameters(),
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
            NativeAuthV2SignUpSubmitCodeCommand::class
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
            NativeAuthV2SignUpSubmitCodeCommand::class
        )
        assertTrue(state.submitCode("123456") is NativeAuthResultV2.AttributesRequired)

        enqueueResult(
            NativeAuthV2CommandResult.PasswordRequired(correlationId, createContinuationState()),
            NativeAuthV2SignUpSubmitCodeCommand::class
        )
        assertTrue(state.submitCode("123456") is NativeAuthResultV2.PasswordRequired)
    }

    @Test
    fun submitCodeReturnsAttributeErrorWithServiceDiagnostics() = runTest {
        val state = codeRequiredState()
        val collectionState = createContinuationState()
        val details = listOf(mapOf(
            "code" to "attributeValidationFailed", "attributeIds" to listOf("displayName")))
        enqueueResult(
            NativeAuthV2CommandResult.AttributesInvalid(
                correlationId, collectionState, listOf("displayName"), "invalidRequest", "Attribute rejected",
                listOf(1002027), "attributeValidationError", details, "attribute-trace", "attribute-time"
            ),
            NativeAuthV2SignUpSubmitCodeCommand::class
        )
        val raw = state.submitCode("123456")
        assertTrue(raw is NativeAuthErrorV2)
        val error = raw as NativeAuthErrorV2
        assertEquals(NativeAuthFlowScenarioV2.SIGN_UP, error.scenario)
        assertTrue(error.isInvalidAttributes())
        assertFalse(error.isSuccess())
        assertEquals("invalidRequest", error.error)
        assertEquals("Attribute rejected", error.errorMessage)
        assertEquals(correlationId, error.correlationId)
        assertEquals("attributeValidationError", error.subError)
        assertEquals(listOf(1002027), error.errorCodes)
        assertEquals(details, error.attributeValidationDetails)
        assertEquals("attribute-trace", error.traceId)
        assertEquals("attribute-time", error.timestamp)
        verify(exactly = 0) {
            CommandDispatcher.submitSilentReturningFuture(match { it is NativeAuthV2SubmitAttributesCommand })
        }
    }

    @Test
    fun attributeValidationFailuresReturnErrorsAcrossSignUpSteps() = runTest {
        val details = listOf(mapOf(
            "code" to "attributeValidationFailed", "attributeIds" to listOf("city")))
        for ((commandClass, submit) in signUpAttributeSubmissionSteps()) {
            enqueueResult(NativeAuthV2CommandResult.AttributesInvalid(
                correlationId, createContinuationState(), listOf("city"), "invalidRequest",
                "City rejected", errorCodes, "attributeValidationError", details,
                "attribute-trace", "attribute-time"
            ), commandClass)
            val error = submit() as NativeAuthErrorV2
            assertTrue(error.isInvalidAttributes())
            assertFalse(error.isInvalidPassword())
            assertFalse(error.isSuccess())
            assertEquals(NativeAuthFlowScenarioV2.SIGN_UP, error.scenario)
            assertEquals(correlationId, error.correlationId)
            assertEquals("invalidRequest", error.error)
            assertEquals("City rejected", error.errorMessage)
            assertEquals("attributeValidationError", error.subError)
            assertEquals(errorCodes, error.errorCodes)
            assertEquals(details, error.attributeValidationDetails)
            assertEquals("attribute-trace", error.traceId)
            assertEquals("attribute-time", error.timestamp)
        }
    }

    @Test
    fun attributeValidationFailureCallbackReturnsErrorWithoutState() = runTest {
        val state = attributesRequiredState()
        val details = listOf(mapOf(
            "code" to "attributeValidationFailed", "attributeIds" to listOf("city")))
        enqueueResult(NativeAuthV2CommandResult.AttributesInvalid(
            correlationId, createContinuationState(), listOf("city"), "invalidRequest",
            "City rejected", attributeValidationDetails = details
        ), NativeAuthV2SubmitAttributesCommand::class)
        val future = ResultFuture<NativeAuthResultV2>()
        state.submitAttributes(UserAttributes.Builder().city("Invalid").build(),
            object : AttributesRequiredStateV2.SubmitAttributesCallback {
                override fun onResult(result: NativeAuthResultV2) = future.setResult(result)
                override fun onError(exception: BaseException) = future.setException(exception)
            })
        val error = future.get(10, TimeUnit.SECONDS) as NativeAuthErrorV2
        assertTrue(error.isInvalidAttributes())
        assertFalse(error.isSuccess())
        assertEquals(details, error.attributeValidationDetails)
    }

    @Test
    fun passwordPolicyFailuresFromAttributeSubmissionReturnInvalidPasswordAcrossSignUpSteps() = runTest {
        val steps = signUpAttributeSubmissionSteps()
        for (invalidNames in listOf(listOf("password"), listOf("password", "displayName"))) {
            val details = listOf(mapOf(
                "code" to "passwordPolicyViolation", "attributeIds" to listOf("password")))
            for ((commandClass, submit) in steps) {
                enqueueResult(
                    NativeAuthV2CommandResult.AttributesInvalid(
                        correlationId, createContinuationState(), invalidNames, "invalidRequest",
                        "Password rejected", errorCodes, "attributeValidationError", details,
                        "policy-trace", "policy-time"
                    ), commandClass
                )
                val result = submit()
                assertTrue(result.toString(), result is NativeAuthErrorV2)
                val error = result as NativeAuthErrorV2
                assertTrue(error.isInvalidPassword())
                assertFalse(error.isSuccess())
                assertEquals(NativeAuthFlowScenarioV2.SIGN_UP, error.scenario)
                assertEquals(correlationId, error.correlationId)
                assertEquals("invalidRequest", error.error)
                assertEquals("Password rejected", error.errorMessage)
                assertEquals("attributeValidationError", error.subError)
                assertEquals(details, error.attributeValidationDetails)
                assertEquals(errorCodes, error.errorCodes)
                assertEquals("policy-trace", error.traceId)
                assertEquals("policy-time", error.timestamp)
            }
        }
    }

    @Test
    fun attributeSubmissionTransportFailuresReturnNormalErrorsAcrossSignUpSteps() = runTest {
        val steps = signUpAttributeSubmissionSteps()
        val failure = ConnectionError.getClientException(IOException("Connection interrupted"))
        for ((commandClass, submit) in steps) {
            every {
                CommandDispatcher.submitSilentReturningFuture(match { commandClass.java.isInstance(it) })
            } throws failure
            val error = submit() as NativeAuthErrorV2
            assertEquals(ErrorTypes.CLIENT_EXCEPTION, error.errorType)
            assertEquals(NativeAuthFlowScenarioV2.SIGN_UP, error.scenario)
            assertSame(failure, error.exception)
            assertFalse(error.isInvalidPassword())
            assertFalse(error.isSuccess())
        }
    }

    @Test
    fun submitCodePreservesExistingUserClassificationAndDiagnostics() = runTest {
        val state = codeRequiredState()
        val details = listOf(mapOf("code" to "userAlreadyExists", "attributeIds" to listOf("email")))
        enqueueResult(
            NativeAuthV2CommandResult.UserAlreadyExists(
                correlationId, "invalidRequest", "Account exists", listOf(1002029),
                "accountConflict", details, "exists-trace", "exists-time"
            ),
            NativeAuthV2SignUpSubmitCodeCommand::class
        )
        val error = state.submitCode("123456") as NativeAuthErrorV2
        assertTrue(error.isUserAlreadyExists())
        assertEquals(NativeAuthFlowScenarioV2.SIGN_UP, error.scenario)
        assertEquals(correlationId, error.correlationId)
        assertEquals("invalidRequest", error.error)
        assertEquals("Account exists", error.errorMessage)
        assertEquals(listOf(1002029), error.errorCodes)
        assertEquals("accountConflict", error.subError)
        assertEquals(details, error.attributeValidationDetails)
        assertEquals("exists-trace", error.traceId)
        assertEquals("exists-time", error.timestamp)
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
                listOf(50181),
                traceId = "code-trace",
                timestamp = "code-time"
            ),
            NativeAuthV2SignUpSubmitCodeCommand::class
        )

        val result = state.submitCode("000000") as NativeAuthErrorV2

        assertTrue(result.isInvalidCode())
        assertEquals(listOf(50181), result.errorCodes)
        assertEquals("code-trace", result.traceId)
        assertEquals("code-time", result.timestamp)
    }

    @Test
    fun resendCodeUsesSignUpPublicApiId() = runTest {
        val signUpState = codeRequiredState()
        var signUpPublicApiId: String? = null
        enqueueResult(
            NativeAuthV2CommandResult.CodeRequired(
                correlationId,
                createContinuationState(),
                6,
                "u***@contoso.com",
                "email"
            ),
            NativeAuthV2ResendCodeCommand::class
        ) { signUpPublicApiId = it.publicApiId }

        assertTrue(signUpState.resendCode() is NativeAuthResultV2.CodeRequired)
        assertEquals(PublicApiId.NATIVE_AUTH_V2_SIGN_UP_RESEND_CODE, signUpPublicApiId)
    }

    @Test
    fun resendCodeRejectsUnknownScenarioWithoutDispatching() = runTest {
        val signUpState = codeRequiredState()
        val state = CodeRequiredStateV2(
            createContinuationState(),
            NativeAuthFlowScenarioV2.UNKNOWN,
            signUpState.config
        )

        val result = state.resendCode()

        assertTrue(result is NativeAuthErrorV2)
        assertEquals(ErrorTypes.INVALID_STATE, (result as NativeAuthErrorV2).errorType)
        verify(exactly = 0) {
            CommandDispatcher.submitSilentReturningFuture(
                match { it is NativeAuthV2ResendCodeCommand }
            )
        }
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
    fun submitAttributesWithInvalidValueReturnsAttributesInvalidError() = runTest {
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

        assertTrue(result is NativeAuthErrorV2)
        assertTrue((result as NativeAuthErrorV2).isInvalidAttributes())
        assertFalse(result.isSuccess())
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

        val result = state.submitAttributes(UserAttributes.Builder().build()) as NativeAuthErrorV2

        assertTrue(result.isUserAlreadyExists())
    }

    @Test
    fun submitAttributesMapsBrowserRequired() = runTest {
        val state = attributesRequiredState()
        enqueueResult(
            INativeAuthCommandResult.Redirect(correlationId, "browser"),
            NativeAuthV2SubmitAttributesCommand::class
        )

        val result = state.submitAttributes(UserAttributes.Builder().build()) as NativeAuthErrorV2

        assertTrue(result.isBrowserRequired())
    }

    @Test
    fun submitAttributesMapsTypedInvalidAttributesError() = runTest {
        val state = attributesRequiredState()
        enqueueResult(
            SignUpCommandResult.InvalidAttributes(
                error = "invalid_attributes",
                errorDescription = "Reserved attributes were supplied.",
                invalidAttributes = listOf("password"),
                correlationId = correlationId
            ),
            NativeAuthV2SubmitAttributesCommand::class
        )

        val result = state.submitAttributes(
            UserAttributes.Builder().build()
        ) as NativeAuthErrorV2

        assertTrue(result.isInvalidAttributes())
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

        val result = state.submitPassword("weak".toCharArray()) as NativeAuthErrorV2

        assertTrue(result.isInvalidPassword())
    }

    @Test
    fun submitPasswordWithNonPasswordValidationErrorReturnsAttributesInvalidError() = runTest {
        val state = signUpPasswordRequiredState()
        enqueueResult(
            NativeAuthV2CommandResult.AttributesInvalid(
                correlationId,
                createContinuationState(),
                listOf("displayName"),
                "attribute_validation_failed",
                "AADSTS1002027: attribute validation failed.",
                listOf(1002027)
            ),
            NativeAuthV2SubmitAttributesCommand::class
        )

        val result = state.submitPassword("Password123!".toCharArray()) as NativeAuthErrorV2
        assertTrue(result.isInvalidAttributes())
        assertFalse(result.isInvalidPassword())
    }

    @Test
    fun submitPasswordWithMixedValidationErrorsReturnsAttributesInvalidError() = runTest {
        val state = signUpPasswordRequiredState()
        enqueueResult(
            NativeAuthV2CommandResult.AttributesInvalid(
                correlationId,
                createContinuationState(),
                listOf("password", "displayName"),
                "attribute_validation_failed",
                "AADSTS1002027: attribute validation failed.",
                listOf(1002027)
            ),
            NativeAuthV2SubmitAttributesCommand::class
        )

        val result = state.submitPassword("weak".toCharArray()) as NativeAuthErrorV2
        assertTrue(result.isInvalidAttributes())
        assertFalse(result.isInvalidPassword())
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

    @Test
    fun existingAccountMethodSelection_resumesBothCredentialPaths() = runTest {
        val state = NativeAuthV2ContinuationStateTestFactory.create(
            correlationId, scenario = NativeAuthV2FlowScenario.SIGN_IN)
        val methods = listOf(NativeAuthV2AuthMethod("email-id", "email", "***"),
            NativeAuthV2AuthMethod("password-id", "password", null))
        enqueueResult(NativeAuthV2CommandResult.AuthMethodSelectionRequired(
            correlationId, state, methods), NativeAuthV2SignUpStartCommand::class)
        val selection = application.signUpV2(signUpParameters()) as NativeAuthResultV2.AuthMethodSelectionRequired
        assertEquals(NativeAuthFlowScenarioV2.SIGN_IN, selection.scenario)
        assertEquals(methods.map { it.id }, selection.authMethods.map { it.id })
        assertSame(state, selection.nextState.continuationState)
        verify(exactly = 0) {
            CommandDispatcher.submitSilentReturningFuture(match { it is NativeAuthV2SelectAuthMethodCommand })
        }

        enqueueResult(NativeAuthV2CommandResult.ExistingAccountCodeRequired(
            correlationId, state, 6, "***", "email"), NativeAuthV2SelectAuthMethodCommand::class) {
            val parameters = it.parameters as NativeAuthV2SelectAuthMethodCommandParameters
            assertEquals(methods.first().id, parameters.methodId)
            assertSame(state, parameters.continuationState)
            assertEquals(correlationId, parameters.correlationId)
            assertEquals(state.scopesForTokenRequest(), parameters.scopes)
            assertEquals(state.claimsRequestJsonForTokenRequest(), parameters.claimsRequestJson)
        }
        // Resolve by offered ID, never trust caller-modified method metadata.
        val email = selection.nextState.selectAuthMethod(
            selection.authMethods.first().copy(challengeChannel = "password", loginHint = "changed"))
            as NativeAuthResultV2.ExistingAccountCodeRequired
        enqueueResult(NativeAuthV2CommandResult.MFARequired(
            correlationId, state, emptyList()), NativeAuthV2SignInSubmitCodeCommand::class)
        assertTrue(email.nextState.submitCode("123456") is NativeAuthResultV2.MFARequired)

        enqueueResult(NativeAuthV2CommandResult.ExistingAccountPasswordRequired(
            correlationId, state), NativeAuthV2SelectAuthMethodCommand::class) {
            assertEquals(methods.last().id, (it.parameters as NativeAuthV2SelectAuthMethodCommandParameters).methodId)
        }
        val password = selection.nextState.selectAuthMethod(selection.authMethods.last())
            as NativeAuthResultV2.ExistingAccountPasswordRequired
        enqueueResult(NativeAuthV2CommandResult.MFARequired(
            correlationId, state, emptyList()), NativeAuthV2SubmitPasswordCommand::class)
        assertTrue(password.nextState.submitPassword("current-password".toCharArray()) is NativeAuthResultV2.MFARequired)
    }

    @Test
    fun existingAccountMethodSelection_rejectsUnknownIdsAndPreservesErrorsAndCancellation() = runTest {
        val state = NativeAuthV2ContinuationStateTestFactory.create(
            correlationId, scenario = NativeAuthV2FlowScenario.SIGN_IN)
        enqueueResult(NativeAuthV2CommandResult.AuthMethodSelectionRequired(correlationId, state,
            listOf(NativeAuthV2AuthMethod("email-id", "email", "***"), NativeAuthV2AuthMethod("password-id", "password", null))),
            NativeAuthV2SignUpStartCommand::class)
        val selection = (application.signUpV2(signUpParameters()) as NativeAuthResultV2.AuthMethodSelectionRequired).nextState
        val invalid = selection.selectAuthMethod(AuthMethod("not-offered", "oob", null, "email")) as NativeAuthErrorV2
        assertEquals(ErrorTypes.INVALID_STATE, invalid.errorType)
        verify(exactly = 0) {
            CommandDispatcher.submitSilentReturningFuture(match { it is NativeAuthV2SelectAuthMethodCommand })
        }
        enqueueResult(NativeAuthV2CommandResult.ExistingAccountPasswordRequired(correlationId, state),
            NativeAuthV2SelectAuthMethodCommand::class)
        assertEquals(ErrorTypes.INVALID_STATE,
            (selection.selectAuthMethod(selection.authMethods.first()) as NativeAuthErrorV2).errorType)
        enqueueResult(NativeAuthV2CommandResult.ExistingAccountCodeRequired(correlationId, state, 6, "***", "email"),
            NativeAuthV2SelectAuthMethodCommand::class)
        assertEquals(ErrorTypes.INVALID_STATE,
            (selection.selectAuthMethod(selection.authMethods.last()) as NativeAuthErrorV2).errorType)
        enqueueResult(INativeAuthCommandResult.APIError(
            "service_error", "retry", correlationId = correlationId, subError = "details", traceId = "trace"),
            NativeAuthV2SelectAuthMethodCommand::class)
        val error = selection.selectAuthMethod(selection.authMethods.first()) as NativeAuthErrorV2
        assertEquals("service_error", error.error)
        assertEquals("trace", error.traceId)
        assertEquals("details", error.subError)
        enqueueResult(INativeAuthCommandResult.Redirect(correlationId, "browser required"),
            NativeAuthV2SelectAuthMethodCommand::class)
        assertEquals(ErrorTypes.BROWSER_REQUIRED,
            (selection.selectAuthMethod(selection.authMethods.first()) as NativeAuthErrorV2).errorType)
        enqueueCancellation(NativeAuthV2SelectAuthMethodCommand::class)
        try {
            selection.selectAuthMethod(selection.authMethods.first())
            fail("Expected cancellation")
        } catch (_: CancellationException) {
        }
        enqueueResult(NativeAuthV2CommandResult.StrongAuthRegistrationRequired(correlationId, state, emptyList()),
            NativeAuthV2SelectAuthMethodCommand::class)
        val future = ResultFuture<NativeAuthResultV2>()
        selection.selectAuthMethod(selection.authMethods.first(),
            object : AuthMethodSelectionRequiredStateV2.SelectAuthMethodCallback {
                override fun onResult(result: NativeAuthResultV2) = future.setResult(result)
                override fun onError(exception: BaseException) = fail(exception.message)
            })
        val enrollment = future.get(30, TimeUnit.SECONDS) as NativeAuthResultV2.StrongAuthRegistrationRequired
        assertEquals(NativeAuthFlowScenarioV2.SIGN_IN, enrollment.scenario)
    }

    @Test
    fun existingAccountCode_isDistinctResendsAndContinuesSignIn() = runTest {
        val state = NativeAuthV2ContinuationStateTestFactory.create(
            correlationId, scenario = NativeAuthV2FlowScenario.SIGN_IN)
        enqueueResult(NativeAuthV2CommandResult.ExistingAccountCodeRequired(
            correlationId, state, 6, "***@contoso.com", "email"), NativeAuthV2SignUpStartCommand::class)
        val registration = application.signUpV2(signUpParameters())
            as NativeAuthResultV2.ExistingAccountCodeRequired
        assertEquals(NativeAuthFlowScenarioV2.SIGN_IN, registration.scenario)
        assertSame(state, registration.nextState.continuationState)
        assertEquals(6, registration.codeLength)
        assertEquals("***@contoso.com", registration.sentTo)
        enqueueResult(NativeAuthV2CommandResult.CodeRequired(
            correlationId, state, 8, "***@contoso.com", "email"), NativeAuthV2ResendCodeCommand::class)
        val resent = registration.nextState.resendCode() as NativeAuthResultV2.ExistingAccountCodeRequired
        assertEquals(8, resent.codeLength)
        enqueueResult(NativeAuthV2CommandResult.IncorrectCode(
            correlationId, "invalid_code", "Retry", "invalid_oob_value"), NativeAuthV2SignInSubmitCodeCommand::class)
        assertTrue((resent.nextState.submitCode("wrong") as NativeAuthErrorV2).isInvalidChallenge())
        enqueueResult(NativeAuthV2CommandResult.MFARequired(
            correlationId, state, emptyList()), NativeAuthV2SignInSubmitCodeCommand::class)
        val mfa = resent.nextState.submitCode("12345678") as NativeAuthResultV2.MFARequired
        assertEquals(NativeAuthFlowScenarioV2.SIGN_IN, mfa.scenario)
        assertSame(state, mfa.nextState.continuationState)
        verify(exactly = 0) {
            CommandDispatcher.submitSilentReturningFuture(match { it is NativeAuthV2SignUpSubmitCodeCommand })
        }
    }

    @Test
    fun existingAccountPassword_usesSignInCommandAndPreservesCallerBuffer() = runTest {
        val state = NativeAuthV2ContinuationStateTestFactory.create(
            correlationId, scenario = NativeAuthV2FlowScenario.SIGN_IN)
        enqueueResult(NativeAuthV2CommandResult.ExistingAccountPasswordRequired(
            correlationId, state), NativeAuthV2SignUpStartCommand::class)
        val existing = application.signUpV2(signUpParameters())
            as NativeAuthResultV2.ExistingAccountPasswordRequired
        assertEquals(NativeAuthFlowScenarioV2.SIGN_IN, existing.scenario)
        assertSame(state, existing.nextState.continuationState)
        val password = "Current-password!".toCharArray()
        enqueueResult(NativeAuthV2CommandResult.IncorrectPassword(
            correlationId, "invalid_credentials", "Retry", "invalid_password"), NativeAuthV2SubmitPasswordCommand::class)
        assertTrue(existing.nextState.submitPassword(password) is NativeAuthErrorV2)
        assertEquals("Current-password!", String(password))
        enqueueResult(NativeAuthV2CommandResult.MFARequired(
            correlationId, state, emptyList()), NativeAuthV2SubmitPasswordCommand::class)
        assertTrue(existing.nextState.submitPassword(password) is NativeAuthResultV2.MFARequired)
        verify(exactly = 0) {
            CommandDispatcher.submitSilentReturningFuture(match { it is NativeAuthV2SubmitAttributesCommand })
        }
    }

    @Test
    fun existingAccountSuccessorsMapFromEverySignUpState() = runTest {
        val signin = NativeAuthV2ContinuationStateTestFactory.create(
            correlationId, scenario = NativeAuthV2FlowScenario.SIGN_IN)
        for (result in listOf(
            NativeAuthV2CommandResult.AuthMethodSelectionRequired(correlationId, signin,
                listOf(NativeAuthV2AuthMethod("email", "email", null), NativeAuthV2AuthMethod("password", "password", null))),
            NativeAuthV2CommandResult.ExistingAccountCodeRequired(correlationId, signin, 6, "***", "email"),
            NativeAuthV2CommandResult.ExistingAccountPasswordRequired(correlationId, signin)
        )) {
            val expected = when (result) {
                is NativeAuthV2CommandResult.AuthMethodSelectionRequired -> NativeAuthResultV2.AuthMethodSelectionRequired::class.java
                is NativeAuthV2CommandResult.ExistingAccountCodeRequired -> NativeAuthResultV2.ExistingAccountCodeRequired::class.java
                else -> NativeAuthResultV2.ExistingAccountPasswordRequired::class.java
            }
            val attributes = attributesRequiredState()
            enqueueResult(result, NativeAuthV2SubmitAttributesCommand::class)
            assertTrue(expected.isInstance(attributes.submitAttributes(UserAttributes.Builder().build())))
            val password = signUpPasswordRequiredState()
            enqueueResult(result, NativeAuthV2SubmitAttributesCommand::class)
            assertTrue(expected.isInstance(password.submitPassword("password".toCharArray())))
            val code = codeRequiredState()
            enqueueResult(result, NativeAuthV2SignUpSubmitCodeCommand::class)
            assertTrue(expected.isInstance(code.submitCode("123456")))
        }
    }

    @Test
    fun enrollmentPreservesExistingAccountSignInFromEverySignUpStep() = runTest {
        for ((commonScenario, expectedScenario) in listOf(
            NativeAuthV2FlowScenario.SIGN_IN to NativeAuthFlowScenarioV2.SIGN_IN,
            NativeAuthV2FlowScenario.SIGN_UP to NativeAuthFlowScenarioV2.SIGN_UP
        )) {
            val continuation = NativeAuthV2ContinuationStateTestFactory.create(
                correlationId, scenario = commonScenario)
            val commandResult = NativeAuthV2CommandResult.StrongAuthRegistrationRequired(
                correlationId, continuation, listOf(NativeAuthV2AuthMethod("email-id", "email", null)))
            val results = mutableListOf<NativeAuthResultV2>()
            enqueueResult(commandResult, NativeAuthV2SignUpStartCommand::class)
            results += application.signUpV2(signUpParameters())
            val attributes = attributesRequiredState()
            enqueueResult(commandResult, NativeAuthV2SubmitAttributesCommand::class)
            results += attributes.submitAttributes(UserAttributes.Builder().build())
            val password = signUpPasswordRequiredState()
            enqueueResult(commandResult, NativeAuthV2SubmitAttributesCommand::class)
            results += password.submitPassword("password".toCharArray())
            val code = codeRequiredState()
            enqueueResult(commandResult, NativeAuthV2SignUpSubmitCodeCommand::class)
            results += code.submitCode("123456")

            for (result in results) {
                val registration = result as NativeAuthResultV2.StrongAuthRegistrationRequired
                assertEquals(expectedScenario, registration.scenario)
                assertEquals(expectedScenario, registration.nextState.scenario)
                assertSame(continuation, registration.nextState.continuationState)
                assertEquals("email-id", registration.authMethods.single().id)

                enqueueResult(NativeAuthV2CommandResult.StrongAuthVerificationRequired(
                    correlationId, continuation, 6, "***@contoso.com"), NativeAuthV2EnrollCommand::class)
                val verification = registration.nextState.selectAuthMethod(
                    registration.authMethods.single(), "user@contoso.com"
                ) as NativeAuthResultV2.StrongAuthVerificationRequired
                assertEquals(expectedScenario, verification.scenario)
                assertEquals(expectedScenario, verification.nextState.scenario)
                enqueueResult(NativeAuthV2CommandResult.MFARequired(
                    correlationId, continuation, emptyList()), NativeAuthV2ActivateCommand::class)
                val resumed = verification.nextState.submitChallenge("123456") as NativeAuthResultV2.MFARequired
                assertEquals(expectedScenario, resumed.scenario)
                assertEquals(expectedScenario, resumed.nextState.scenario)
            }
        }
    }

    @Test
    fun existingAccountStates_propagateCancellationAndCallbackResults() = runTest {
        val state = NativeAuthV2ContinuationStateTestFactory.create(
            correlationId, scenario = NativeAuthV2FlowScenario.SIGN_IN)
        enqueueResult(NativeAuthV2CommandResult.ExistingAccountPasswordRequired(
            correlationId, state), NativeAuthV2SignUpStartCommand::class)
        val password = (application.signUpV2(signUpParameters()) as
            NativeAuthResultV2.ExistingAccountPasswordRequired).nextState
        enqueueCancellation(NativeAuthV2SubmitPasswordCommand::class)
        try {
            password.submitPassword("password".toCharArray())
            fail("Expected cancellation")
        } catch (_: CancellationException) {
        }
        enqueueResult(NativeAuthV2CommandResult.MFARequired(
            correlationId, state, emptyList()), NativeAuthV2SubmitPasswordCommand::class)
        val future = ResultFuture<NativeAuthResultV2>()
        password.submitPassword("password".toCharArray(),
            object : ExistingAccountPasswordRequiredStateV2.SubmitPasswordCallback {
                override fun onResult(result: NativeAuthResultV2) = future.setResult(result)
                override fun onError(exception: BaseException) = fail(exception.message)
            })
        assertTrue(future.get(30, TimeUnit.SECONDS) is NativeAuthResultV2.MFARequired)
        enqueueResult(NativeAuthV2CommandResult.ExistingAccountCodeRequired(
            correlationId, state, 6, "***", "email"), NativeAuthV2SignUpStartCommand::class)
        val code = (application.signUpV2(signUpParameters()) as
            NativeAuthResultV2.ExistingAccountCodeRequired).nextState
        enqueueCancellation(NativeAuthV2SignInSubmitCodeCommand::class)
        try {
            code.submitCode("123456")
            fail("Expected cancellation")
        } catch (_: CancellationException) {
        }
        enqueueResult(NativeAuthV2CommandResult.MFARequired(
            correlationId, state, emptyList()), NativeAuthV2SignInSubmitCodeCommand::class)
        val codeFuture = ResultFuture<NativeAuthResultV2>()
        code.submitCode("123456", object : ExistingAccountCodeRequiredStateV2.SubmitCodeCallback {
            override fun onResult(result: NativeAuthResultV2) = codeFuture.setResult(result)
            override fun onError(exception: BaseException) = fail(exception.message)
        })
        assertTrue(codeFuture.get(30, TimeUnit.SECONDS) is NativeAuthResultV2.MFARequired)
    }

    private fun signUpParameters(
        username: String = this.username,
        attributes: UserAttributes? = null
    ): NativeAuthSignUpParametersV2 = NativeAuthSignUpParametersV2(username).also {
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

    private suspend fun signUpPasswordRequiredState(): PasswordRequiredStateV2 {
        enqueueResult(
            NativeAuthV2CommandResult.PasswordRequired(correlationId, createContinuationState()),
            NativeAuthV2SignUpStartCommand::class
        )
        return (application.signUpV2(signUpParameters()) as
            NativeAuthResultV2.PasswordRequired).nextState
    }

    private suspend fun signInAfterSignUpState(): SignInAfterSignUpStateV2 {
        enqueueResult(
            NativeAuthV2CommandResult.SignInAfterSignUpRequired(correlationId, createContinuationState()),
            NativeAuthV2SignUpSubmitCodeCommand::class
        )
        return (codeRequiredState().submitCode("123456") as
            NativeAuthResultV2.SignInAfterSignUpRequired).nextState
    }

    private suspend fun signUpAttributeSubmissionSteps():
        List<Pair<KClass<out BaseCommand<*>>, suspend () -> NativeAuthResultV2>> {
        val code = codeRequiredState()
        val required = attributesRequiredState()
        val password = signUpPasswordRequiredState()
        val collection = createContinuationState()
        enqueueResult(NativeAuthV2CommandResult.StrongAuthRegistrationRequired(
            correlationId, collection, listOf(NativeAuthV2AuthMethod("email-id", "email", null))),
            NativeAuthV2SignUpStartCommand::class)
        val registration = application.signUpV2(signUpParameters()) as NativeAuthResultV2.StrongAuthRegistrationRequired
        enqueueResult(NativeAuthV2CommandResult.StrongAuthVerificationRequired(
            correlationId, collection, 6, "***"), NativeAuthV2EnrollCommand::class)
        val verification = registration.nextState.selectAuthMethod(registration.authMethods.single(), "user@contoso.com")
            as NativeAuthResultV2.StrongAuthVerificationRequired
        return listOf(
            NativeAuthV2SignUpStartCommand::class to { application.signUpV2(signUpParameters()) },
            NativeAuthV2SignUpSubmitCodeCommand::class to { code.submitCode("123456") },
            NativeAuthV2SubmitAttributesCommand::class to { required.submitAttributes(UserAttributes.Builder().build()) },
            NativeAuthV2SubmitAttributesCommand::class to { password.submitPassword("Password123!".toCharArray()) },
            NativeAuthV2ActivateCommand::class to { verification.nextState.submitChallenge("123456") }
        )
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

    private fun createContinuationState(): NativeAuthV2ContinuationState =
        NativeAuthV2ContinuationStateTestFactory.create(
            correlationId = correlationId,
            entryRelation = NativeAuthV2LinkRelation.SIGN_UP,
            scenario = NativeAuthV2FlowScenario.SIGN_UP
        )

    private companion object {
        const val correlationId = "correlation-id"
        val errorCodes = listOf(1002027)
    }
}
