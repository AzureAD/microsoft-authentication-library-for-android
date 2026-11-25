// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.nativeauth

import android.util.Base64
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
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.nativeauth.commands.parameters.BaseSignInTokenCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.MFASubmitChallengeCommandParameters
import com.microsoft.identity.common.java.nativeauth.controllers.results.INativeAuthCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.JITCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.MFACommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.SignInCommandResult
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthConstants
import com.microsoft.identity.common.java.providers.oauth2.AgeAssuranceTargetValidator
import com.microsoft.identity.common.java.result.FinalizableResultFuture
import com.microsoft.identity.common.nativeauth.internal.commands.JITChallengeAuthMethodCommand
import com.microsoft.identity.common.nativeauth.internal.commands.JITSubmitChallengeCommand
import com.microsoft.identity.common.nativeauth.internal.commands.MFAChallengeCommand
import com.microsoft.identity.common.nativeauth.internal.commands.MFASubmitChallengeCommand
import com.microsoft.identity.common.nativeauth.internal.commands.SignInStartCommand
import com.microsoft.identity.common.nativeauth.internal.commands.SignInResendCodeCommand
import com.microsoft.identity.common.nativeauth.internal.commands.SignInSubmitCodeCommand
import com.microsoft.identity.common.nativeauth.internal.commands.SignInSubmitPasswordCommand
import com.microsoft.identity.common.nativeauth.internal.commands.SignInWithContinuationTokenCommand
import com.microsoft.identity.common.nativeauth.internal.util.CommandUtil
import com.microsoft.identity.nativeauth.parameters.NativeAuthChallengeAuthMethodParameters
import com.microsoft.identity.nativeauth.parameters.NativeAuthSignInContinuationParameters
import com.microsoft.identity.nativeauth.parameters.NativeAuthSignInParameters
import com.microsoft.identity.nativeauth.statemachine.errors.Error
import com.microsoft.identity.nativeauth.statemachine.errors.MFASubmitChallengeError
import com.microsoft.identity.nativeauth.statemachine.errors.RegisterStrongAuthChallengeError
import com.microsoft.identity.nativeauth.statemachine.errors.RegisterStrongAuthSubmitChallengeError
import com.microsoft.identity.nativeauth.statemachine.results.MFARequiredResult
import com.microsoft.identity.nativeauth.statemachine.results.RegisterStrongAuthChallengeResult
import com.microsoft.identity.nativeauth.statemachine.results.SignInResult
import com.microsoft.identity.nativeauth.statemachine.results.SignInResendCodeResult
import com.microsoft.identity.nativeauth.statemachine.states.AwaitingMFAState
import com.microsoft.identity.nativeauth.statemachine.states.MFARequiredState
import com.microsoft.identity.nativeauth.statemachine.states.RegisterStrongAuthState
import com.microsoft.identity.nativeauth.statemachine.states.RegisterStrongAuthVerificationRequiredState
import com.microsoft.identity.nativeauth.statemachine.states.SignInContinuationState
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.json.JSONObject
import java.io.File
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowAndroidSdkStorageEncryptionManager::class])
class NativeAuthV1TargetBindingTest : PublicClientApplicationAbstractTest() {
    private lateinit var application: INativeAuthPublicClientApplication
    private val correlation = "test-correlation"
    private val username = "user@example.com"
    private val claimsJson = """{"id_token":{"xms_agvs":{"additionalProperties":["target-mc"]}}}"""
    private val method = AuthMethod("email", "oob", null, "email")
    private val routes = listOf("start", "code", "resend", "password", "continuation")

    override fun getConfigFilePath() = "src/test/res/raw/native_auth_native_only_test_config.json"

    @Before
    override fun setup() {
        application = PublicClientApplication.createNativeAuthPublicClientApplication(
            ApplicationProvider.getApplicationContext(), File(getConfigFilePath()))
        CommandDispatcher.getSilentExecutorPoolSize()
        mockkStatic(CommandDispatcher::class)
        every { CommandDispatcher.submitSilentReturningFuture(any()) } throws
            AssertionError("Unexpected command dispatch")
    }

    @After
    fun tearDown() {
        unmockkStatic(CommandDispatcher::class)
    }

    @Test
    fun allSignInHandoffsRetainNamedAndEmptyContextThroughMfaRetries() =
        runTest(timeout = 60.seconds) {
            for (claims in listOf(claimsJson, null)) {
                for (route in routes) {
                    val awaiting = (enterInterruption(route,
                        SignInCommandResult.MFARequired(correlation, "mfa", emptyList()),
                        claims) as SignInResult.MFARequired).nextState
                    enqueue(MFACommandResult.VerificationRequired(
                        correlation, "challenge", "***", "email", 6), MFAChallengeCommand::class)
                    val verification = awaiting.requestChallenge(method) as MFARequiredResult.VerificationRequired
                    val rechallenged = verification.nextState.requestChallenge(method) as MFARequiredResult.VerificationRequired
                    enqueue(SignInCommandResult.IncorrectCode(
                        correlation, "invalid_grant", "Incorrect code", emptyList(), ""),
                        MFASubmitChallengeCommand::class) { assertClaims(claims, it) }
                    repeat(2) {
                        assertTrue(rechallenged.nextState.submitChallenge("123456") is MFASubmitChallengeError)
                    }
                }
            }
            verify(exactly = routes.size * 2 * 2) {
                CommandDispatcher.submitSilentReturningFuture(match { it is MFASubmitChallengeCommand })
            }
        }

    @Test
    fun allSignInHandoffsRetainNamedAndEmptyContextThroughJitRetries() =
        runTest(timeout = 60.seconds) {
            for (claims in listOf(claimsJson, null)) {
                for (route in routes) {
                    val registration = (enterInterruption(route,
                        SignInCommandResult.StrongAuthMethodRegistrationRequired(correlation, "jit", emptyList()),
                        claims) as SignInResult.StrongAuthMethodRegistrationRequired).nextState
                    enqueue(JITCommandResult.VerificationRequired(correlation, "challenge", "***", "email", 6),
                        JITChallengeAuthMethodCommand::class) { assertClaims(claims, it) }
                    val parameters = NativeAuthChallengeAuthMethodParameters(method, username)
                    val verification = registration.challengeAuthMethod(parameters) as RegisterStrongAuthChallengeResult.VerificationRequired
                    val rechallenged = verification.result.nextState.challengeAuthMethod(parameters) as RegisterStrongAuthChallengeResult.VerificationRequired
                    enqueue(JITCommandResult.IncorrectChallenge(
                        correlation, "invalid_grant", "Incorrect code", emptyList(), ""),
                        JITSubmitChallengeCommand::class) { assertClaims(claims, it) }
                    repeat(2) {
                        assertTrue(rechallenged.result.nextState.submitChallenge("123456") is RegisterStrongAuthSubmitChallengeError)
                    }
                }
            }
            verify(exactly = routes.size * 2 * 2) {
                CommandDispatcher.submitSilentReturningFuture(match { it is JITChallengeAuthMethodCommand })
            }
            verify(exactly = routes.size * 2 * 2) {
                CommandDispatcher.submitSilentReturningFuture(match { it is JITSubmitChallengeCommand })
            }
        }

    @Test
    fun mutatingClaimsAfterEnteringMfaDoesNotChangeTheCompletionContext() = runTest(timeout = 60.seconds) {
        val parameters = parameters(claimsJson)
        enqueue(SignInCommandResult.MFARequired(correlation, "mfa", emptyList()), SignInStartCommand::class)
        val awaiting = (application.signIn(parameters) as SignInResult.MFARequired).nextState
        parameters.claimsRequest!!.idTokenClaimsRequested.clear()
        parameters.claimsRequest = null
        enqueue(MFACommandResult.VerificationRequired(correlation, "challenge", "***", "email", 6),
            MFAChallengeCommand::class)
        val verification = awaiting.requestChallenge(method) as MFARequiredResult.VerificationRequired
        enqueue(SignInCommandResult.IncorrectCode(correlation, "invalid_grant", "Incorrect code", emptyList(), ""),
            MFASubmitChallengeCommand::class) { assertClaims(claimsJson, it) }
        assertTrue(verification.nextState.submitChallenge("123456") is MFASubmitChallengeError)
    }

    @Test
    fun mfaAndBothJitCompletionRoutesPreserveTerminalMismatchDiagnostics() = runTest(timeout = 60.seconds) {
        val mismatch = ClientException(ClientException.AGE_ASSURANCE_TARGET_MISMATCH, "Target mismatch")
        val failure = INativeAuthCommandResult.APIError(
            error = mismatch.errorCode,
            errorDescription = "Target mismatch",
            correlationId = correlation,
            exception = mismatch
        )
        val awaiting = (enterInterruption("start",
            SignInCommandResult.MFARequired(correlation, "mfa", emptyList()),
            claimsJson) as SignInResult.MFARequired).nextState
        enqueue(MFACommandResult.VerificationRequired(correlation, "challenge", "***", "email", 6),
            MFAChallengeCommand::class)
        val mfa = (awaiting.requestChallenge(method) as MFARequiredResult.VerificationRequired).nextState
        enqueue(failure, MFASubmitChallengeCommand::class) { assertClaims(claimsJson, it) }
        assertSame(mismatch, (mfa.submitChallenge("123456") as MFASubmitChallengeError).exception)

        val registration = (enterInterruption("start",
            SignInCommandResult.StrongAuthMethodRegistrationRequired(correlation, "jit", emptyList()),
            claimsJson) as SignInResult.StrongAuthMethodRegistrationRequired).nextState
        val parameters = NativeAuthChallengeAuthMethodParameters(method, username)
        enqueue(failure, JITChallengeAuthMethodCommand::class) { assertClaims(claimsJson, it) }
        assertSame(mismatch, (registration.challengeAuthMethod(parameters) as RegisterStrongAuthChallengeError).exception)
        enqueue(JITCommandResult.VerificationRequired(correlation, "challenge", "***", "email", 6),
            JITChallengeAuthMethodCommand::class)
        val verification = registration.challengeAuthMethod(parameters) as RegisterStrongAuthChallengeResult.VerificationRequired
        enqueue(failure, JITSubmitChallengeCommand::class) { assertClaims(claimsJson, it) }
        assertSame(mismatch, (verification.result.nextState.submitChallenge("123456") as RegisterStrongAuthSubmitChallengeError).exception)
        verify(exactly = 1) {
            CommandDispatcher.submitSilentReturningFuture(match { it is MFASubmitChallengeCommand })
        }
        verify(exactly = 2) {
            CommandDispatcher.submitSilentReturningFuture(match { it is JITChallengeAuthMethodCommand })
        }
        verify(exactly = 1) {
            CommandDispatcher.submitSilentReturningFuture(match { it is JITSubmitChallengeCommand })
        }
    }

    @Test
    fun manuallyConstructedUnknownStatesRejectEveryOperationWithoutDispatch() = runTest(timeout = 60.seconds) {
        val config = configuration()
        val awaiting = AwaitingMFAState("mfa", correlation, null, config)
        val mfa = MFARequiredState("mfa", correlation, null, config)
        val registration = RegisterStrongAuthState("jit", correlation, config)
        val verification = RegisterStrongAuthVerificationRequiredState("jit", correlation, config)
        val parameters = NativeAuthChallengeAuthMethodParameters(method, username)
        listOf(
            awaiting.requestChallenge(method), mfa.requestChallenge(method), mfa.submitChallenge("123456"),
            registration.challengeAuthMethod(parameters), verification.challengeAuthMethod(parameters),
            verification.submitChallenge("123456"),
            registration.internalChallengeAuthMethod(parameters, "test"),
            registration.challengeAuthMethod(NativeAuthChallengeAuthMethodParameters(method, "")),
            verification.submitChallenge("")
        ).forEach { assertRestart(it as Error) }
        verify(exactly = 0) { CommandDispatcher.submitSilentReturningFuture(any()) }
    }

    @Test
    fun unknownStateCallbackReceivesOneExplicitRestartResultWithoutDispatch() = runTest(timeout = 60.seconds) {
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val applicationScope = NativeAuthPublicClientApplication.pcaScope
        mockkObject(applicationScope)
        try {
            every { applicationScope.coroutineContext } returns scope.coroutineContext
            val completed = CompletableDeferred<MFARequiredResult>()
            var callbacks = 0
            AwaitingMFAState("mfa", correlation, null, configuration()).requestChallenge(
                method, object : AwaitingMFAState.RequestChallengeCallback {
                    override fun onResult(result: MFARequiredResult) {
                        callbacks++
                        completed.complete(result)
                    }
                    override fun onError(exception: BaseException) {
                        completed.completeExceptionally(exception)
                    }
                })
            assertRestart(completed.await() as Error)
            assertEquals(1, callbacks)
            verify(exactly = 0) { CommandDispatcher.submitSilentReturningFuture(any()) }
        } finally {
            scope.cancel()
            unmockkObject(applicationScope)
        }
    }

    private suspend fun enterInterruption(
        route: String,
        interruption: INativeAuthCommandResult,
        claims: String?
    ): Any = when (route) {
        "start" -> {
            enqueue(interruption, SignInStartCommand::class) { assertClaims(claims, it) }
            application.signIn(parameters(claims))
        }
        "code", "resend" -> {
            enqueue(SignInCommandResult.CodeRequired(correlation, "code", "***", "email", 6), SignInStartCommand::class)
            val state = (application.signIn(parameters(claims)) as SignInResult.CodeRequired).nextState
            val submittedState = if (route == "resend") {
                enqueue(SignInCommandResult.CodeRequired(correlation, "new-code", "***", "email", 6),
                    SignInResendCodeCommand::class)
                (state.resendCode() as SignInResendCodeResult.Success).nextState
            } else state
            enqueue(interruption, SignInSubmitCodeCommand::class) { assertClaims(claims, it) }
            submittedState.submitCode("123456")
        }
        "password" -> {
            enqueue(SignInCommandResult.PasswordRequired(correlation, "password"), SignInStartCommand::class)
            val state = (application.signIn(parameters(claims)) as SignInResult.PasswordRequired).nextState
            enqueue(interruption, SignInSubmitPasswordCommand::class) { assertClaims(claims, it) }
            state.submitPassword("password".toCharArray())
        }
        else -> {
            enqueue(interruption, SignInWithContinuationTokenCommand::class) { assertClaims(claims, it) }
            SignInContinuationState("continuation", correlation, username, configuration()).signIn(
                NativeAuthSignInContinuationParameters().apply {
                    claimsRequest = claims?.let { ClaimsRequest.getClaimsRequestFromJsonString(it) }
                })
        }
    }

    private fun parameters(claims: String?) = NativeAuthSignInParameters(username).apply {
        claimsRequest = claims?.let { ClaimsRequest.getClaimsRequestFromJsonString(it) }
    }

    private fun assertClaims(claims: String?, command: BaseCommand<*>) {
        val expected = ClaimsRequest.getJsonStringFromClaimsRequest(parameters(claims).claimsRequest)
        val parameters = command.parameters as BaseSignInTokenCommandParameters
        assertEquals(expected, parameters.claimsRequestJson)
        // These are the real Common conversions after the wrapper's command boundary.
        val completionParameters = when (command) {
            is MFASubmitChallengeCommand -> CommandUtil.createSignInSubmitCodeCommandParameters(
                parameters as MFASubmitChallengeCommandParameters)
            is JITChallengeAuthMethodCommand -> CommandUtil.createSignInWithContinuationTokenCommandParameters(
                CommandUtil.createJITContinueCommandParameters(parameters, correlation,
                    NativeAuthConstants.GrantType.CONTINUATION_TOKEN, "jit-continuation"),
                correlation, "sign-in-continuation")
            is JITSubmitChallengeCommand -> CommandUtil.createSignInWithContinuationTokenCommandParameters(
                parameters, correlation, "sign-in-continuation")
            else -> parameters
        }
        assertEquals(expected, completionParameters.claimsRequestJson)
        assertReceiptContract(completionParameters.claimsRequestJson, claims != null)
    }

    private fun assertReceiptContract(claims: String?, named: Boolean) {
        val accepted = if (named) listOf("+mc") else listOf(null, "", "+all")
        for (receipt in accepted) AgeAssuranceTargetValidator.validate(claims, token(receipt))
        val rejected = if (named) listOf(null, "", "+all", "+other") else listOf("+mc")
        for (receipt in rejected) {
            val error = assertThrows(ClientException::class.java) {
                AgeAssuranceTargetValidator.validate(claims, token(receipt))
            }
            assertEquals(ClientException.AGE_ASSURANCE_TARGET_MISMATCH, error.errorCode)
        }
        if (!named) AgeAssuranceTargetValidator.validate(claims, null)
    }

    private fun token(receipt: String?): String {
        val payload = JSONObject().put("xms_agvs", 0).apply {
            if (receipt != null) put("xms_agvt", receipt)
        }
        val flags = Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
        val header = Base64.encodeToString("""{"alg":"none"}""".toByteArray(Charsets.UTF_8), flags)
        val body = Base64.encodeToString(payload.toString().toByteArray(Charsets.UTF_8), flags)
        return "$header.$body."
    }

    private fun assertRestart(error: Error) {
        assertEquals("invalid_state", error.error)
        assertTrue(error.errorMessage!!.contains("restart sign-in"))
        assertEquals(correlation, error.correlationId)
    }

    private fun configuration(): NativeAuthPublicClientApplicationConfiguration =
        NativeAuthPublicClientApplication::class.java.getDeclaredField("nativeAuthConfig").let {
            it.isAccessible = true
            it.get(application) as NativeAuthPublicClientApplicationConfiguration
        }

    private fun enqueue(
        result: INativeAuthCommandResult,
        commandClass: KClass<out BaseCommand<*>>,
        onCommand: (BaseCommand<*>) -> Unit = {}
    ) {
        val future = FinalizableResultFuture<CommandResult<Any>>()
        future.setResult(CommandResult(ICommandResult.ResultStatus.COMPLETED, result as Any, correlation))
        every {
            CommandDispatcher.submitSilentReturningFuture(match { commandClass.java.isInstance(it) })
        } answers {
            onCommand(firstArg())
            future
        }
    }
}
