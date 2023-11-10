// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.client.statemachine.states

import com.microsoft.identity.client.NativeAuthPublicClientApplication
import com.microsoft.identity.client.NativeAuthPublicClientApplicationConfiguration
import com.microsoft.identity.client.UserAttributes
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.client.internal.CommandParametersAdapter
import com.microsoft.identity.client.statemachine.BrowserRequiredError
import com.microsoft.identity.client.statemachine.GeneralError
import com.microsoft.identity.client.statemachine.IncorrectCodeError
import com.microsoft.identity.client.statemachine.InvalidAttributesError
import com.microsoft.identity.client.statemachine.InvalidPasswordError
import com.microsoft.identity.client.statemachine.results.SignInResult
import com.microsoft.identity.client.statemachine.results.SignUpResendCodeResult
import com.microsoft.identity.client.statemachine.results.SignUpResult
import com.microsoft.identity.client.statemachine.results.SignUpSubmitAttributesResult
import com.microsoft.identity.client.statemachine.results.SignUpSubmitCodeResult
import com.microsoft.identity.client.statemachine.results.SignUpSubmitPasswordResult
import com.microsoft.identity.client.toListOfRequiredUserAttribute
import com.microsoft.identity.client.toMap
import com.microsoft.identity.common.internal.commands.SignUpResendCodeCommand
import com.microsoft.identity.common.internal.commands.SignUpSubmitCodeCommand
import com.microsoft.identity.common.internal.commands.SignUpSubmitPasswordCommand
import com.microsoft.identity.common.internal.commands.SignUpSubmitUserAttributesCommand
import com.microsoft.identity.common.internal.controllers.NativeAuthMsalController
import com.microsoft.identity.common.java.controllers.CommandDispatcher
import com.microsoft.identity.common.java.controllers.results.INativeAuthCommandResult
import com.microsoft.identity.common.java.controllers.results.SignUpCommandResult
import com.microsoft.identity.common.java.controllers.results.SignUpResendCodeCommandResult
import com.microsoft.identity.common.java.controllers.results.SignUpSubmitCodeCommandResult
import com.microsoft.identity.common.java.controllers.results.SignUpSubmitPasswordCommandResult
import com.microsoft.identity.common.java.controllers.results.SignUpSubmitUserAttributesCommandResult
import com.microsoft.identity.common.java.eststelemetry.PublicApiId
import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.util.StringUtil
import com.microsoft.identity.common.java.util.checkAndWrapCommandResultType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Serializable

class SignUpCodeRequiredState internal constructor(
    override val flowToken: String,
    private val username: String,
    private val config: NativeAuthPublicClientApplicationConfiguration
) : BaseState(flowToken), State, Serializable {
    private val TAG: String = SignUpCodeRequiredState::class.java.simpleName

    interface SubmitCodeCallback : Callback<SignUpSubmitCodeResult>

    /**
     * Submits the verification code received to the server; callback variant.
     *
     * @param code the code to submit.
     * @param callback [com.microsoft.identity.client.statemachine.states.SignUpCodeRequiredState.SubmitCodeCallback] to receive the result on.
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
     * @param code the code to submit.
     * @return The results of the submit code action.
     */
    suspend fun submitCode(
        code: String,
    ): SignUpSubmitCodeResult {
        LogSession.logMethodCall(TAG, "${TAG}.submitCode(code: String)")
        return withContext(Dispatchers.IO) {
            val commandParameters =
                CommandParametersAdapter.createSignUpSubmitCodeCommandParameters(
                    config,
                    config.oAuth2TokenCache,
                    code,
                    flowToken
                )

            val command = SignUpSubmitCodeCommand(
                commandParameters,
                NativeAuthMsalController(),
                PublicApiId.NATIVE_AUTH_SIGN_UP_SUBMIT_CODE
            )
            val rawCommandResult = CommandDispatcher.submitSilentReturningFuture(command).get()

            return@withContext when (val result = rawCommandResult.checkAndWrapCommandResultType<SignUpSubmitCodeCommandResult>()) {
                is SignUpCommandResult.InvalidCode -> {
                    SignUpSubmitCodeResult.CodeIncorrect(
                        IncorrectCodeError(
                            error = result.error,
                            errorMessage = result.errorDescription,
                            correlationId = result.correlationId
                        )
                    )
                }

                is SignUpCommandResult.PasswordRequired -> {
                    SignUpResult.PasswordRequired(
                        nextState = SignUpPasswordRequiredState(
                            flowToken = result.signupToken,
                            username = username,
                            config = config
                        )
                    )
                }

                is SignUpCommandResult.AttributesRequired -> {
                    SignUpResult.AttributesRequired(
                        nextState = SignUpAttributesRequiredState(
                            flowToken = result.signupToken,
                            username = username,
                            config = config
                        ),
                        requiredAttributes = result.requiredAttributes.toListOfRequiredUserAttribute()
                    )
                }

                is SignUpCommandResult.Complete -> {
                    SignUpResult.Complete(
                        nextState = SignInAfterSignUpState(
                            signInVerificationCode = result.signInSLT,
                            username = username,
                            config = config
                        )
                    )
                }

                is INativeAuthCommandResult.Redirect -> {
                    SignUpResult.BrowserRequired(
                        error = BrowserRequiredError(
                            correlationId = result.correlationId
                        )
                    )
                }

                // This should be caught earlier in the flow, so throwing UnexpectedError
                is SignUpCommandResult.UsernameAlreadyExists -> {
                    Logger.warn(
                        TAG,
                        "Unexpected result: $result"
                    )
                    SignUpResult.UnexpectedError(
                        error = GeneralError(
                            errorMessage = result.errorDescription,
                            error = result.error,
                            correlationId = result.correlationId
                        )
                    )
                }

                is INativeAuthCommandResult.UnknownError -> {
                    Logger.warn(
                        TAG,
                        "Unexpected result: $result"
                    )
                    SignUpResult.UnexpectedError(
                        error = GeneralError(
                            errorMessage = result.errorDescription,
                            error = result.error,
                            correlationId = result.correlationId,
                            details = result.details,
                            exception = result.exception
                        )
                    )
                }
            }
        }
    }

    interface SignUpWithResendCodeCallback : Callback<SignUpResendCodeResult>

    /**
     * Resends a new verification code to the user; callback variant.
     *
     * @param callback [com.microsoft.identity.client.statemachine.states.SignUpCodeRequiredState.SignUpWithResendCodeCallback] to receive the result on.
     * @return The results of the resend code action.
     */
    fun resendCode(
        callback: SignUpWithResendCodeCallback
    ) {
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
    suspend fun resendCode(): SignUpResendCodeResult {
        LogSession.logMethodCall(TAG, "${TAG}.resendCode()")
        return withContext(Dispatchers.IO) {
            val commandParameters =
                CommandParametersAdapter.createSignUpResendCodeCommandParameters(
                    config,
                    config.oAuth2TokenCache,
                    flowToken
                )
            val command = SignUpResendCodeCommand(
                commandParameters,
                NativeAuthMsalController(),
                PublicApiId.NATIVE_AUTH_SIGN_UP_RESEND_CODE
            )
            val rawCommandResult = CommandDispatcher.submitSilentReturningFuture(command).get()

            return@withContext when (val result = rawCommandResult.checkAndWrapCommandResultType<SignUpResendCodeCommandResult>()) {
                is SignUpCommandResult.CodeRequired -> {
                    SignUpResendCodeResult.Success(
                        nextState = SignUpCodeRequiredState(
                            flowToken = result.signupToken,
                            username = username,
                            config = config
                        ),
                        codeLength = result.codeLength,
                        sentTo = result.challengeTargetLabel,
                        channel = result.challengeChannel
                    )
                }

                is INativeAuthCommandResult.Redirect -> {
                    SignUpResult.BrowserRequired(
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
                    SignUpResult.UnexpectedError(
                        error = GeneralError(
                            errorMessage = result.errorDescription,
                            error = result.error,
                            correlationId = result.correlationId,
                            details = result.details,
                            exception = result.exception
                        )
                    )
                }
            }
        }
    }
}

class SignUpPasswordRequiredState internal constructor(
    override val flowToken: String,
    private val username: String,
    private val config: NativeAuthPublicClientApplicationConfiguration
) : BaseState(flowToken), State, Serializable {
    private val TAG: String = SignUpPasswordRequiredState::class.java.simpleName

    interface SignUpSubmitPasswordCallback : Callback<SignUpSubmitPasswordResult>

    /**
     * Submits a password for the account to the server; callback variant.
     *
     * @param password the password to submit.
     * @param callback [com.microsoft.identity.client.statemachine.states.SignUpPasswordRequiredState.SignUpSubmitPasswordCallback] to receive the result on.
     * @return The results of the submit password action.
     */
    fun submitPassword(
        password: CharArray,
        callback: SignUpSubmitPasswordCallback
    ) {
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
     * Submits a password for the account to the server; Kotlin coroutines variant.
     *
     * @param password the password to submit.
     * @return The results of the submit password action.
     */
    suspend fun submitPassword(password: CharArray): SignUpSubmitPasswordResult {
        LogSession.logMethodCall(TAG, "${TAG}.submitPassword(password: String)")
        return withContext(Dispatchers.IO) {
            val commandParameters =
                CommandParametersAdapter.createSignUpSubmitPasswordCommandParameters(
                    config,
                    config.oAuth2TokenCache,
                    flowToken,
                    password
                )
            val command = SignUpSubmitPasswordCommand(
                commandParameters,
                NativeAuthMsalController(),
                PublicApiId.NATIVE_AUTH_SIGN_UP_SUBMIT_PASSWORD
            )

            try {
                val rawCommandResult = CommandDispatcher.submitSilentReturningFuture(command).get()

                return@withContext when (val result =
                    rawCommandResult.checkAndWrapCommandResultType<SignUpSubmitPasswordCommandResult>()) {
                    is SignUpCommandResult.InvalidPassword -> {
                        SignUpResult.InvalidPassword(
                            InvalidPasswordError(
                                error = result.error,
                                errorMessage = result.errorDescription,
                                correlationId = result.correlationId
                            )
                        )
                    }

                    is SignUpCommandResult.AttributesRequired -> {
                        SignUpResult.AttributesRequired(
                            nextState = SignUpAttributesRequiredState(
                                flowToken = result.signupToken,
                                username = username,
                                config = config
                            ),
                            requiredAttributes = result.requiredAttributes.toListOfRequiredUserAttribute()
                        )
                    }

                    is SignUpCommandResult.Complete -> {
                        SignUpResult.Complete(
                            nextState = SignInAfterSignUpState(
                                signInVerificationCode = result.signInSLT,
                                username = username,
                                config = config
                            )
                        )
                    }

                    is INativeAuthCommandResult.Redirect -> {
                        SignUpResult.BrowserRequired(
                            error = BrowserRequiredError(
                                correlationId = result.correlationId
                            )
                        )
                    }

                    // This should be caught earlier in the flow, so throwing UnexpectedError
                    is SignUpCommandResult.UsernameAlreadyExists -> {
                        Logger.warn(
                            TAG,
                            "Unexpected result: $result"
                        )
                        SignUpResult.UnexpectedError(
                            error = GeneralError(
                                errorMessage = result.errorDescription,
                                error = result.error,
                                correlationId = result.correlationId
                            )
                        )
                    }

                    // This should be caught earlier in the flow, so throwing UnexpectedError
                    is SignUpCommandResult.InvalidEmail -> {
                        Logger.warn(
                            TAG,
                            "Unexpected result: $result"
                        )
                        SignUpResult.UnexpectedError(
                            error = GeneralError(
                                errorMessage = result.errorDescription,
                                error = result.error,
                                correlationId = result.correlationId
                            )
                        )
                    }

                    is INativeAuthCommandResult.UnknownError -> {
                        Logger.warn(
                            TAG,
                            "Unexpected result: $result"
                        )
                        SignUpResult.UnexpectedError(
                            error = GeneralError(
                                errorMessage = result.errorDescription,
                                error = result.error,
                                correlationId = result.correlationId,
                                details = result.details,
                                exception = result.exception
                            )
                        )
                    }
                }
            } finally {
                StringUtil.overwriteWithNull(commandParameters.password)
            }
        }
    }
}

class SignUpAttributesRequiredState internal constructor(
    override val flowToken: String,
    private val username: String,
    private val config: NativeAuthPublicClientApplicationConfiguration
) : BaseState(flowToken), State, Serializable {
    private val TAG: String = SignUpAttributesRequiredState::class.java.simpleName

    interface SignUpSubmitUserAttributesCallback : Callback<SignUpSubmitAttributesResult>

    /**
     * Submits the user attributes required to the server; callback variant.
     *
     * @param attributes mandatory attributes set in the tenant configuration. Should use [com.microsoft.identity.client.UserAttributes] to convert to a map.
     * @param callback [com.microsoft.identity.client.statemachine.states.SignUpAttributesRequiredState.SignUpSubmitUserAttributesCallback] to receive the result on.
     * @return The results of the submit user attributes action.
     */
    fun submitAttributes(
        attributes: UserAttributes,
        callback: SignUpSubmitUserAttributesCallback
    ) {
        LogSession.logMethodCall(TAG, "${TAG}.submitAttributes")
        NativeAuthPublicClientApplication.pcaScope.launch {
            try {
                val result = submitAttributes(attributes)
                callback.onResult(result)
            } catch (e: MsalException) {
                Logger.error(TAG, "Exception thrown in submitAttributes", e)
                callback.onError(e)
            }
        }
    }

    /**
     * Submits the user attributes required to the server; Kotlin coroutines variant.
     *
     * @param attributes mandatory attributes set in the tenant configuration. Should use [com.microsoft.identity.client.UserAttributes] to convert to a map.
     * @return The results of the submit user attributes action.
     */
    suspend fun submitAttributes(attributes: UserAttributes): SignUpSubmitAttributesResult {
        LogSession.logMethodCall(TAG, "${TAG}.submitAttributes(attributes: UserAttributes)")
        return withContext(Dispatchers.IO) {

            val commandParameters =
                CommandParametersAdapter.createSignUpStarSubmitUserAttributesCommandParameters(
                    config,
                    config.oAuth2TokenCache,
                    flowToken,
                    attributes.toMap()
                )

            val command = SignUpSubmitUserAttributesCommand(
                commandParameters,
                NativeAuthMsalController(),
                PublicApiId.NATIVE_AUTH_SIGN_UP_SUBMIT_ATTRIBUTES
            )

            val rawCommandResult = CommandDispatcher.submitSilentReturningFuture(command).get()

            return@withContext when (val result = rawCommandResult.checkAndWrapCommandResultType<SignUpSubmitUserAttributesCommandResult>()) {
                is SignUpCommandResult.InvalidAttributes -> {
                    SignUpResult.InvalidAttributes(
                        error = InvalidAttributesError(
                            errorMessage = result.errorDescription,
                            error = result.error,
                            correlationId = result.correlationId
                        ),
                        invalidAttributes = result.invalidAttributes
                    )
                }
                is SignUpCommandResult.AttributesRequired -> {
                    SignUpResult.AttributesRequired(
                        nextState = SignUpAttributesRequiredState(
                            flowToken = result.signupToken,
                            username = username,
                            config = config
                        ),
                        requiredAttributes = result.requiredAttributes.toListOfRequiredUserAttribute()
                    )
                }
                is SignUpCommandResult.Complete -> {
                    SignUpResult.Complete(
                        nextState = SignInAfterSignUpState(
                            signInVerificationCode = result.signInSLT,
                            username = username,
                            config = config
                        )
                    )
                }
                is INativeAuthCommandResult.Redirect -> {
                    SignUpResult.BrowserRequired(
                        error = BrowserRequiredError(
                            correlationId = result.correlationId
                        )
                    )
                }
                // This should be caught earlier in the flow, so throwing UnexpectedError
                is SignUpCommandResult.UsernameAlreadyExists -> {
                    Logger.warn(
                        TAG,
                        "Unexpected result: $result"
                    )
                    SignUpResult.UnexpectedError(
                        error = GeneralError(
                            errorMessage = result.errorDescription,
                            error = result.error,
                            correlationId = result.correlationId
                        )
                    )
                }
                is INativeAuthCommandResult.UnknownError -> {
                    Logger.warn(
                        TAG,
                        "Unexpected result: $result"
                    )
                    SignUpResult.UnexpectedError(
                        error = GeneralError(
                            errorMessage = result.errorDescription,
                            error = result.error,
                            correlationId = result.correlationId,
                            details = result.details,
                            exception = result.exception
                        )
                    )
                }
            }
        }
    }
}

class SignInAfterSignUpState internal constructor(
    override val signInVerificationCode: String?,
    override val username: String,
    private val config: NativeAuthPublicClientApplicationConfiguration
) : SignInAfterSignUpBaseState(signInVerificationCode, username, config) {
    private val TAG: String = SignInAfterSignUpState::class.java.simpleName
    interface SignInAfterSignUpCallback : SignInAfterSignUpBaseState.SignInAfterSignUpCallback

    /**
     * Signs in with the sign-in-after-sign-up verification code; callback variant.
     *
     * @param scopes (Optional) the scopes to request.
     * @param callback [com.microsoft.identity.client.statemachine.states.SignInAfterSignUpState.SignInAfterSignUpCallback] to receive the result on.
     * @return The results of the sign-in-after-sign-up action.
     */
    fun signIn(scopes: List<String>? = null, callback: SignInAfterSignUpCallback) {
        LogSession.logMethodCall(TAG, "${TAG}.signIn")
        return signInAfterSignUp(scopes = scopes, callback = callback)
    }

    /**
     * Signs in with the sign-in-after-sign-up verification code; Kotlin coroutines variant.
     *
     * @param scopes (Optional) the scopes to request.
     * @return The results of the sign-in-after-sign-up action.
     */
    suspend fun signIn(scopes: List<String>? = null): SignInResult {
        LogSession.logMethodCall(TAG, "${TAG}.signIn(scopes: List<String>)")
        return signInAfterSignUp(scopes = scopes)
    }
}
