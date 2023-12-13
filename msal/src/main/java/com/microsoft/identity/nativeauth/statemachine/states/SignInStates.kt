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

package com.microsoft.identity.nativeauth.statemachine.states

import com.microsoft.identity.client.AuthenticationResultAdapter
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplication
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplicationConfiguration
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.client.internal.CommandParametersAdapter
import com.microsoft.identity.nativeauth.statemachine.BrowserRequiredError
import com.microsoft.identity.nativeauth.statemachine.GeneralError
import com.microsoft.identity.nativeauth.statemachine.IncorrectCodeError
import com.microsoft.identity.nativeauth.statemachine.PasswordIncorrectError
import com.microsoft.identity.nativeauth.statemachine.results.SignInResendCodeResult
import com.microsoft.identity.nativeauth.statemachine.results.SignInResult
import com.microsoft.identity.nativeauth.statemachine.results.SignInSubmitCodeResult
import com.microsoft.identity.nativeauth.statemachine.results.SignInSubmitPasswordResult
import com.microsoft.identity.common.nativeauth.internal.commands.SignInResendCodeCommand
import com.microsoft.identity.common.nativeauth.internal.commands.SignInSubmitCodeCommand
import com.microsoft.identity.common.nativeauth.internal.commands.SignInSubmitPasswordCommand
import com.microsoft.identity.common.nativeauth.internal.commands.SignInWithSLTCommand
import com.microsoft.identity.common.nativeauth.internal.controllers.NativeAuthMsalController
import com.microsoft.identity.common.java.controllers.CommandDispatcher
import com.microsoft.identity.common.java.nativeauth.controllers.results.INativeAuthCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.SignInCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.SignInResendCodeCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.SignInSubmitCodeCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.SignInSubmitPasswordCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.SignInWithSLTCommandResult
import com.microsoft.identity.common.java.eststelemetry.PublicApiId
import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.util.StringUtil
import com.microsoft.identity.common.java.nativeauth.util.checkAndWrapCommandResultType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Serializable

/**
 * Native Auth uses a state machine to denote state of and transitions within a flow.
 * SignInCodeRequiredState class represents a state where the user has to provide a code to progress
 * in the signin flow.
 * @property flowToken: Flow token to be passed in the next request
 * @property scopes: List of scopes
 * @property config Configuration used by Native Auth
 */
class SignInCodeRequiredState internal constructor(
    override val flowToken: String,
    private val scopes: List<String>?,
    private val config: NativeAuthPublicClientApplicationConfiguration
) : BaseState(flowToken), State, Serializable {
    private val TAG: String = SignInCodeRequiredState::class.java.simpleName

    /**
     * SubmitCodeCallback receives the result for submit code for SignIn for Native Auth
     */
    interface SubmitCodeCallback : Callback<SignInSubmitCodeResult>

    /**
     * Submits the verification code received to the server; callback variant.
     *
     * @param code The code to submit.
     * @param callback [com.microsoft.identity.nativeauth.statemachine.states.SignInCodeRequiredState.SubmitCodeCallback] to receive the result on.
     * @return The results of the submit code action.
     */
    fun submitCode(code: String, callback: SubmitCodeCallback) {
        LogSession.logMethodCall(TAG, "${TAG}.submitCode")
        NativeAuthPublicClientApplication.pcaScope.launch {
            try {
                val result = submitCode(code)
                callback.onResult(result)
            } catch (e: MsalException) {
                Logger.error(TAG, "Exception thrown in submitCode", e)
                callback.onError(e)
            }
        }
    }

    /**
     * Submits the verification code received to the server; Kotlin coroutines variant.
     *
     * @param code The code to submit.
     * @return The results of the submit code action.
     */
    suspend fun submitCode(code: String): SignInSubmitCodeResult {
        LogSession.logMethodCall(TAG, "${TAG}.submitCode(code: String)")
        return withContext(Dispatchers.IO) {
            val params = CommandParametersAdapter.createSignInSubmitCodeCommandParameters(
                config,
                config.oAuth2TokenCache,
                code,
                flowToken,
                scopes
            )

            val signInSubmitCodeCommand = SignInSubmitCodeCommand(
                parameters = params,
                controller = NativeAuthMsalController(),
                publicApiId = PublicApiId.NATIVE_AUTH_SIGN_IN_SUBMIT_CODE
            )

            val rawCommandResult = CommandDispatcher.submitSilentReturningFuture(signInSubmitCodeCommand).get()

            return@withContext when (val result = rawCommandResult.checkAndWrapCommandResultType<SignInSubmitCodeCommandResult>()) {
                is SignInCommandResult.IncorrectCode -> {
                    SignInSubmitCodeResult.CodeIncorrect(
                        error = IncorrectCodeError(
                            error = result.error,
                            errorMessage = result.errorDescription,
                            correlationId = result.correlationId,
                            errorCodes = result.errorCodes
                        )
                    )
                }

                is SignInCommandResult.Complete -> {
                    val authenticationResult =
                        AuthenticationResultAdapter.adapt(result.authenticationResult)

                    SignInResult.Complete(
                        resultValue = AccountResult.createFromAuthenticationResult(
                            authenticationResult = authenticationResult,
                            config = config
                        )
                    )
                }

                is INativeAuthCommandResult.Redirect -> {
                    SignInResult.BrowserRequired(
                        error = BrowserRequiredError(
                            correlationId = result.correlationId
                        )
                    )
                }

                is INativeAuthCommandResult.UnknownError -> {
                    Logger.warn(
                        TAG,
                        "Unexpected result: $result"
                    )
                    SignInResult.UnexpectedError(
                        error = GeneralError(
                            errorMessage = result.errorDescription,
                            error = result.error,
                            correlationId = result.correlationId,
                            details = result.details,
                            errorCodes = result.errorCodes,
                            exception = result.exception
                        )
                    )
                }
            }
        }
    }

    /**
     * SubmitCodeCallback receives the result for resend code for SignIn for Native Auth
     */
    interface ResendCodeCallback : Callback<SignInResendCodeResult>

    /**
     * Resends a new verification code to the user; callback variant.
     *
     * @param callback [com.microsoft.identity.nativeauth.statemachine.states.SignInCodeRequiredState.ResendCodeCallback] to receive the result on.
     * @return The results of the resend code action.
     */
    fun resendCode(callback: ResendCodeCallback) {
        LogSession.logMethodCall(TAG, "${TAG}.resendCode")
        NativeAuthPublicClientApplication.pcaScope.launch {
            try {
                val result = resendCode()
                callback.onResult(result)
            } catch (e: MsalException) {
                Logger.error(TAG, "Exception thrown in resendCode", e)
                callback.onError(e)
            }
        }
    }

    /**
     * Resends a new verification code to the user; Kotlin coroutines variant.
     *
     * @return The results of the resend code action.
     */
    suspend fun resendCode(): SignInResendCodeResult {
        LogSession.logMethodCall(TAG, "${TAG}.resendCode()")
        return withContext(Dispatchers.IO) {
            val params = CommandParametersAdapter.createSignInResendCodeCommandParameters(
                config,
                config.oAuth2TokenCache,
                flowToken
            )

            val signInResendCodeCommand = SignInResendCodeCommand(
                parameters = params,
                controller = NativeAuthMsalController(),
                publicApiId = PublicApiId.NATIVE_AUTH_SIGN_IN_RESEND_CODE
            )

            val rawCommandResult = CommandDispatcher.submitSilentReturningFuture(signInResendCodeCommand).get()

            return@withContext when (val result = rawCommandResult.checkAndWrapCommandResultType<SignInResendCodeCommandResult>()) {
                is SignInCommandResult.CodeRequired -> {
                    SignInResendCodeResult.Success(
                        nextState = SignInCodeRequiredState(
                            flowToken = result.credentialToken,
                            scopes = scopes,
                            config = config
                        ),
                        codeLength = result.codeLength,
                        sentTo = result.challengeTargetLabel,
                        channel = result.challengeChannel
                    )
                }

                is INativeAuthCommandResult.Redirect -> {
                    SignInResult.BrowserRequired(
                        error = BrowserRequiredError(
                            correlationId = result.correlationId
                        )
                    )
                }

                is INativeAuthCommandResult.UnknownError -> {
                    Logger.warn(
                        TAG,
                        "Unexpected result: $result"
                    )
                    SignInResult.UnexpectedError(
                        error = GeneralError(
                            errorMessage = result.errorDescription,
                            error = result.error,
                            correlationId = result.correlationId,
                            details = result.details,
                            errorCodes = result.errorCodes,
                            exception = result.exception
                        )
                    )
                }
            }
        }
    }
}

/**
 * Native Auth uses a state machine to denote state of and transitions within a flow.
 * SignInPasswordRequiredState class represents a state where the user has to provide a password to progress
 * in the signin flow.
 * @property flowToken: Flow token to be passed in the next request
 * @property scopes: List of scopes
 * @property config Configuration used by Native Auth
 */
class SignInPasswordRequiredState(
    override val flowToken: String,
    private val scopes: List<String>?,
    private val config: NativeAuthPublicClientApplicationConfiguration
) : BaseState(flowToken), State {
    private val TAG: String = SignInPasswordRequiredState::class.java.simpleName

    /**
     * SubmitCodeCallback receives the result for submit password for SignIn for Native Auth
     */
    interface SubmitPasswordCallback : Callback<SignInSubmitPasswordResult>

    /**
     * Submits the password of the account to the server; callback variant.
     *
     * @param password the password to submit.
     * @param callback [com.microsoft.identity.nativeauth.statemachine.states.SignInPasswordRequiredState.SubmitPasswordCallback] to receive the result on.
     * @return The results of the submit password action.
     */
    fun submitPassword(password: CharArray, callback: SubmitPasswordCallback) {
        LogSession.logMethodCall(TAG, "${TAG}.submitPassword")
        NativeAuthPublicClientApplication.pcaScope.launch {
            try {
                val result = submitPassword(password)
                callback.onResult(result)
            } catch (e: MsalException) {
                Logger.error(TAG, "Exception thrown in submitPassword", e)
                callback.onError(e)
            }
        }
    }

    /**
     * Submits the password of the account to the server; Kotlin coroutines variant.
     *
     * @param password the password to submit.
     * @return The results of the submit password action.
     */
    suspend fun submitPassword(password: CharArray): SignInSubmitPasswordResult {
        LogSession.logMethodCall(TAG, "${TAG}.submitPassword(password: String)")
        return withContext(Dispatchers.IO) {
            val params = CommandParametersAdapter.createSignInSubmitPasswordCommandParameters(
                config,
                config.oAuth2TokenCache,
                flowToken,
                password,
                scopes
            )

            try
            {
                val signInSubmitPasswordCommand = SignInSubmitPasswordCommand(
                    parameters = params,
                    controller = NativeAuthMsalController(),
                    publicApiId = PublicApiId.NATIVE_AUTH_SIGN_IN_SUBMIT_PASSWORD
                )

                val rawCommandResult =
                    CommandDispatcher.submitSilentReturningFuture(signInSubmitPasswordCommand).get()

                return@withContext when (val result =
                    rawCommandResult.checkAndWrapCommandResultType<SignInSubmitPasswordCommandResult>()) {
                    is SignInCommandResult.InvalidCredentials -> {
                        SignInResult.InvalidCredentials(
                            error = PasswordIncorrectError(
                                error = result.error,
                                errorMessage = result.errorDescription,
                                correlationId = result.correlationId,
                                errorCodes = result.errorCodes
                            )
                        )
                    }
                    is SignInCommandResult.Complete -> {
                        val authenticationResult =
                            AuthenticationResultAdapter.adapt(result.authenticationResult)
                        SignInResult.Complete(
                            resultValue = AccountResult.createFromAuthenticationResult(
                                authenticationResult = authenticationResult,
                                config = config
                            )
                        )
                    }
                    is INativeAuthCommandResult.Redirect -> {
                        SignInResult.BrowserRequired(
                            error = BrowserRequiredError(
                                correlationId = result.correlationId
                            )
                        )
                    }
                    is INativeAuthCommandResult.UnknownError -> {
                        Logger.warn(
                            TAG,
                            "Unexpected result: $result"
                        )
                        SignInResult.UnexpectedError(
                            error = GeneralError(
                                errorMessage = result.errorDescription,
                                error = result.error,
                                correlationId = result.correlationId,
                                details = result.details,
                                errorCodes = result.errorCodes,
                                exception = result.exception
                            )
                        )
                    }
                }
            } finally {
                StringUtil.overwriteWithNull(params.password)
            }
        }
    }
}

/**
 * Native Auth uses a state machine to denote state of and transitions within a flow.
 * SignInAfterSignUpBaseState class is an abstract class to represent signin state after
 * successfull signup
 * in the signin flow.
 * @property signInVerificationCode: Short lived token from signup APIS
 * @property username: Username of the user
 * @property config Configuration used by Native Auth
 */
abstract class SignInAfterSignUpBaseState(
    internal open val signInVerificationCode: String?,
    internal open val username: String,
    private val config: NativeAuthPublicClientApplicationConfiguration
) : BaseState(signInVerificationCode), State, Serializable {
    private val TAG: String = SignInAfterSignUpBaseState::class.java.simpleName

    /**
     * SubmitCodeCallback receives the result for sign in after signup for Native Auth
     */
    interface SignInAfterSignUpCallback : Callback<SignInResult>

    /**
     * Submits the sign-in-after-sign-up verification code to the server; callback variant.
     *
     * @param scopes (Optional) The scopes to request.
     * @param callback [com.microsoft.identity.nativeauth.statemachine.states.SignInAfterSignUpBaseState.SignInAfterSignUpCallback] to receive the result on.
     * @return The results of the sign-in-after-sign-up action.
     */
    fun signInAfterSignUp(scopes: List<String>? = null, callback: SignInAfterSignUpCallback) {
        LogSession.logMethodCall(TAG, "${TAG}.signInAfterSignUp")

        NativeAuthPublicClientApplication.pcaScope.launch {
            try {
                val result = signInAfterSignUp(scopes)
                callback.onResult(result)
            } catch (e: MsalException) {
                Logger.error(TAG, "Exception thrown in signInAfterSignUp", e)
                callback.onError(e)
            }
        }
    }

    /**
     * Submits the sign-in-after-sign-up verification code to the server; Kotlin coroutines variant.
     *
     * @param scopes (Optional) The scopes to request.
     * @return The results of the sign-in-after-sign-up action.
     */
    suspend fun signInAfterSignUp(scopes: List<String>? = null): SignInResult {
        return withContext(Dispatchers.IO) {
            LogSession.logMethodCall(TAG, "${TAG}.signInAfterSignUp(scopes: List<String>)")

            // Check if verification code was passed. If not, return an UnknownError with instructions to call the other
            // sign in flows (code or password).
            if (signInVerificationCode.isNullOrEmpty()) {
                Logger.warn(
                    TAG,
                    "Unexpected result: signInSLT was null"
                )
                return@withContext SignInResult.UnexpectedError(
                    error = GeneralError(
                        errorMessage = "Sign In is not available through this state, please use the standalone sign in methods (signInWithCode or signInWithPassword).",
                        error = "invalid_state",
                        correlationId = "UNSET"
                    )
                )
            }

            val commandParameters = CommandParametersAdapter.createSignInWithSLTCommandParameters(
                config,
                config.oAuth2TokenCache,
                signInVerificationCode,
                username,
                scopes
            )

            val command = SignInWithSLTCommand(
                commandParameters,
                NativeAuthMsalController(),
                PublicApiId.NATIVE_AUTH_SIGN_IN_WITH_SLT
            )

            val rawCommandResult = CommandDispatcher.submitSilentReturningFuture(command).get()

            return@withContext when (val result = rawCommandResult.checkAndWrapCommandResultType<SignInWithSLTCommandResult>()) {
                is SignInCommandResult.CodeRequired -> {
                    SignInResult.CodeRequired(
                        nextState = SignInCodeRequiredState(
                            flowToken = result.credentialToken,
                            scopes = scopes,
                            config = config
                        ),
                        codeLength = result.codeLength,
                        sentTo = result.challengeTargetLabel,
                        channel = result.challengeChannel
                    )
                }
                is SignInCommandResult.PasswordRequired -> {
                    SignInResult.PasswordRequired(
                        nextState = SignInPasswordRequiredState(
                            flowToken = result.credentialToken,
                            scopes = scopes,
                            config = config
                        )
                    )
                }
                is SignInCommandResult.Complete -> {
                    val authenticationResult =
                        AuthenticationResultAdapter.adapt(result.authenticationResult)
                    SignInResult.Complete(
                        resultValue = AccountResult.createFromAuthenticationResult(
                            authenticationResult = authenticationResult,
                            config = config
                        )
                    )
                }
                is INativeAuthCommandResult.Redirect -> {
                    SignInResult.BrowserRequired(
                        error = BrowserRequiredError(
                            correlationId = result.correlationId
                        )
                    )
                }
                is INativeAuthCommandResult.UnknownError -> {
                    Logger.warn(
                        TAG,
                        "Unexpected result: $result"
                    )
                    SignInResult.UnexpectedError(
                        error = GeneralError(
                            errorMessage = result.errorDescription,
                            error = result.error,
                            correlationId = result.correlationId,
                            details = result.details,
                            errorCodes = result.errorCodes,
                            exception = result.exception
                        )
                    )
                }
            }
        }
    }
}
