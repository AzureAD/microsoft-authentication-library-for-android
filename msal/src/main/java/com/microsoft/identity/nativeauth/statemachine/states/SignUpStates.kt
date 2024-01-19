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
package com.microsoft.identity.nativeauth.statemachine.states

import android.os.Parcel
import android.os.Parcelable
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplication
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplicationConfiguration
import com.microsoft.identity.nativeauth.UserAttributes
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.client.internal.CommandParametersAdapter
import com.microsoft.identity.nativeauth.statemachine.results.SignInResult
import com.microsoft.identity.nativeauth.statemachine.results.SignUpResendCodeResult
import com.microsoft.identity.nativeauth.statemachine.results.SignUpResult
import com.microsoft.identity.nativeauth.statemachine.results.SignUpSubmitAttributesResult
import com.microsoft.identity.nativeauth.statemachine.results.SignUpSubmitCodeResult
import com.microsoft.identity.nativeauth.statemachine.results.SignUpSubmitPasswordResult
import com.microsoft.identity.nativeauth.toListOfRequiredUserAttribute
import com.microsoft.identity.nativeauth.toMap
import com.microsoft.identity.common.nativeauth.internal.commands.SignUpResendCodeCommand
import com.microsoft.identity.common.nativeauth.internal.commands.SignUpSubmitCodeCommand
import com.microsoft.identity.common.nativeauth.internal.commands.SignUpSubmitPasswordCommand
import com.microsoft.identity.common.nativeauth.internal.commands.SignUpSubmitUserAttributesCommand
import com.microsoft.identity.common.nativeauth.internal.controllers.NativeAuthMsalController
import com.microsoft.identity.common.java.controllers.CommandDispatcher
import com.microsoft.identity.common.java.nativeauth.controllers.results.INativeAuthCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.SignUpCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.SignUpResendCodeCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.SignUpSubmitCodeCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.SignUpSubmitPasswordCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.SignUpSubmitUserAttributesCommandResult
import com.microsoft.identity.common.java.eststelemetry.PublicApiId
import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.util.StringUtil
import com.microsoft.identity.common.java.nativeauth.util.checkAndWrapCommandResultType
import com.microsoft.identity.nativeauth.statemachine.errors.ErrorTypes
import com.microsoft.identity.nativeauth.statemachine.errors.ResendCodeError
import com.microsoft.identity.nativeauth.statemachine.errors.SignUpErrorTypes
import com.microsoft.identity.nativeauth.statemachine.errors.SignUpSubmitAttributesError
import com.microsoft.identity.nativeauth.statemachine.errors.SignUpSubmitPasswordError
import com.microsoft.identity.nativeauth.statemachine.errors.SubmitCodeError
import com.microsoft.identity.nativeauth.utils.serializable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Native Auth uses a state machine to denote state of and transitions within a flow.
 * SignUpCodeRequiredState class represents a state where the user has to provide a code to progress
 * in the signup flow.
 * @property continuationToken: Continuation token to be passed in the next request
 * @property correlationId: Correlation ID taken from the previous API response and passed to the next request
 * @property username: Email address of the user
 * @property config Configuration used by Native Auth
 */
class SignUpCodeRequiredState internal constructor(
    override val continuationToken: String,
    override val correlationId: String?,
    private val username: String,
    private val config: NativeAuthPublicClientApplicationConfiguration
) : BaseState(continuationToken = continuationToken, correlationId = correlationId), State, Parcelable {
    private val TAG: String = SignUpCodeRequiredState::class.java.simpleName

    constructor(parcel: Parcel) : this(
        continuationToken = parcel.readString() ?: "",
        correlationId = parcel.readString(),
        username = parcel.readString() ?: "",
        config = parcel.serializable<NativeAuthPublicClientApplicationConfiguration>() as NativeAuthPublicClientApplicationConfiguration
    )

    interface SubmitCodeCallback : Callback<SignUpSubmitCodeResult>

    /**
     * Submits the verification code received to the server; callback variant.
     *
     * @param code the code to submit.
     * @param callback [com.microsoft.identity.nativeauth.statemachine.states.SignUpCodeRequiredState.SubmitCodeCallback] to receive the result on.
     * @return The results of the submit code action.
     */
    fun submitCode(code: String, callback: SubmitCodeCallback) {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.submitCode"
        )

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
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.submitCode(code: String)"
        )
        return withContext(Dispatchers.IO) {
            val commandParameters =
                CommandParametersAdapter.createSignUpSubmitCodeCommandParameters(
                    config,
                    config.oAuth2TokenCache,
                    code,
                    continuationToken,
                    correlationId
                )

            val command = SignUpSubmitCodeCommand(
                commandParameters,
                NativeAuthMsalController(),
                PublicApiId.NATIVE_AUTH_SIGN_UP_SUBMIT_CODE
            )
            val rawCommandResult = CommandDispatcher.submitSilentReturningFuture(command).get()

            return@withContext when (val result = rawCommandResult.checkAndWrapCommandResultType<SignUpSubmitCodeCommandResult>()) {
                is SignUpCommandResult.PasswordRequired -> {
                    SignUpResult.PasswordRequired(
                        nextState = SignUpPasswordRequiredState(
                            continuationToken = result.continuationToken,
                            correlationId = result.correlationId,
                            username = username,
                            config = config
                        )
                    )
                }

                is SignUpCommandResult.AttributesRequired -> {
                    SignUpResult.AttributesRequired(
                        nextState = SignUpAttributesRequiredState(
                            continuationToken = result.continuationToken,
                            correlationId = result.correlationId,
                            username = username,
                            config = config
                        ),
                        requiredAttributes = result.requiredAttributes.toListOfRequiredUserAttribute()
                    )
                }

                is SignUpCommandResult.Complete -> {
                    SignUpResult.Complete(
                        nextState = SignInAfterSignUpState(
                            continuationToken = result.continuationToken,
                            correlationId = result.correlationId,
                            username = username,
                            config = config
                        )
                    )
                }

                is SignUpCommandResult.InvalidCode -> {
                    SubmitCodeError(
                        errorType = ErrorTypes.BROWSER_REQUIRED,
                        error = result.error,
                        errorMessage = result.errorDescription,
                        correlationId = result.correlationId,
                        subError = result.subError
                    )
                }


                is INativeAuthCommandResult.Redirect -> {
                    SubmitCodeError(
                        errorType = ErrorTypes.BROWSER_REQUIRED,
                        error = result.error,
                        errorMessage = result.errorDescription,
                        correlationId = result.correlationId
                    )
                }

                // This should be caught earlier in the flow, so throwing UnexpectedError
                is SignUpCommandResult.UsernameAlreadyExists -> {
                    Logger.warn(
                        TAG,
                        result.correlationId,
                        "Submit code received unexpected result: $result"
                    )
                    SubmitCodeError(
                        errorMessage = result.errorDescription,
                        error = result.error,
                        correlationId = result.correlationId
                    )
                }

                is INativeAuthCommandResult.UnknownError -> {
                    Logger.warn(
                        TAG,
                        result.correlationId,
                        "Submit code received unexpected result: $result"
                    )
                    SubmitCodeError(
                        errorMessage = result.errorDescription,
                        error = result.error,
                        correlationId = result.correlationId,
                        exception = result.exception
                    )
                }
            }
        }
    }

    interface SignUpWithResendCodeCallback : Callback<SignUpResendCodeResult>

    /**
     * Resends a new verification code to the user; callback variant.
     *
     * @param callback [com.microsoft.identity.nativeauth.statemachine.states.SignUpCodeRequiredState.SignUpWithResendCodeCallback] to receive the result on.
     * @return The results of the resend code action.
     */
    fun resendCode(
        callback: SignUpWithResendCodeCallback
    ) {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.resendCode"
        )
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
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.resendCode()"
        )
        return withContext(Dispatchers.IO) {
            val commandParameters =
                CommandParametersAdapter.createSignUpResendCodeCommandParameters(
                    config,
                    config.oAuth2TokenCache,
                    correlationId,
                    continuationToken
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
                            continuationToken = result.continuationToken,
                            correlationId = result.correlationId,
                            username = username,
                            config = config
                        ),
                        codeLength = result.codeLength,
                        sentTo = result.challengeTargetLabel,
                        channel = result.challengeChannel
                    )
                }

                is INativeAuthCommandResult.Redirect -> {
                    ResendCodeError(
                        errorType = ErrorTypes.BROWSER_REQUIRED,
                        error = result.error,
                        errorMessage = result.errorDescription,
                        correlationId = result.correlationId
                    )
                }

                is INativeAuthCommandResult.UnknownError -> {
                    Logger.warn(
                        TAG,
                        result.correlationId,
                        "Resend code received unexpected result: $result"
                    )
                    ResendCodeError(
                        errorMessage = result.errorDescription,
                        error = result.error,
                        correlationId = result.correlationId,
                        exception = result.exception
                    )
                }
            }
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(continuationToken)
        parcel.writeString(correlationId)
        parcel.writeString(username)
        parcel.writeSerializable(config)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SignUpCodeRequiredState> {
        override fun createFromParcel(parcel: Parcel): SignUpCodeRequiredState {
            return SignUpCodeRequiredState(parcel)
        }

        override fun newArray(size: Int): Array<SignUpCodeRequiredState?> {
            return arrayOfNulls(size)
        }
    }
}

/**
 * Native Auth uses a state machine to denote state of and transitions within a flow.
 * SignUpPasswordRequiredState class represents a state where the user has to provide a password
 * to progress in the signup flow.
 * @property continuationToken: Continuation token to be passed in the next request
 * @property correlationId: Correlation ID taken from the previous API response and passed to the next request
 * @property username: Email address of the user
 * @property config Configuration used by Native Auth
 */
class SignUpPasswordRequiredState internal constructor(
    override val continuationToken: String,
    override val correlationId: String?,
    private val username: String,
    private val config: NativeAuthPublicClientApplicationConfiguration
) : BaseState(continuationToken = continuationToken, correlationId = correlationId), State, Parcelable {
    private val TAG: String = SignUpPasswordRequiredState::class.java.simpleName

    constructor(parcel: Parcel) : this(
        continuationToken = parcel.readString() ?: "",
        correlationId = parcel.readString(),
        username = parcel.readString() ?: "",
        config = parcel.serializable<NativeAuthPublicClientApplicationConfiguration>() as NativeAuthPublicClientApplicationConfiguration
    )

    interface SignUpSubmitPasswordCallback : Callback<SignUpSubmitPasswordResult>

    /**
     * Submits a password for the account to the server; callback variant.
     *
     * @param password the password to submit.
     * @param callback [com.microsoft.identity.nativeauth.statemachine.states.SignUpPasswordRequiredState.SignUpSubmitPasswordCallback] to receive the result on.
     * @return The results of the submit password action.
     */
    fun submitPassword(
        password: CharArray,
        callback: SignUpSubmitPasswordCallback
    ) {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.submitPassword"
        )
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
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.submitPassword(password: String)"
        )
        return withContext(Dispatchers.IO) {
            val commandParameters =
                CommandParametersAdapter.createSignUpSubmitPasswordCommandParameters(
                    config,
                    config.oAuth2TokenCache,
                    continuationToken,
                    correlationId,
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
                    is SignUpCommandResult.Complete -> {
                        SignUpResult.Complete(
                            nextState = SignInAfterSignUpState(
                                continuationToken = result.continuationToken,
                                correlationId = result.correlationId,
                                username = username,
                                config = config
                            )
                        )
                    }

                    is SignUpCommandResult.AttributesRequired -> {
                        SignUpResult.AttributesRequired(
                            nextState = SignUpAttributesRequiredState(
                                continuationToken = result.continuationToken,
                                correlationId = result.correlationId,
                                username = username,
                                config = config
                            ),
                            requiredAttributes = result.requiredAttributes.toListOfRequiredUserAttribute()
                        )
                    }

                    is SignUpCommandResult.InvalidPassword -> {
                        SignUpSubmitPasswordError(
                            errorType = ErrorTypes.INVALID_PASSWORD,
                            error = result.error,
                            errorMessage = result.errorDescription,
                            correlationId = result.correlationId,
                            subError = result.subError
                        )
                    }

                    is INativeAuthCommandResult.Redirect -> {
                        SignUpSubmitPasswordError(
                            errorType = ErrorTypes.BROWSER_REQUIRED,
                            error = result.error,
                            errorMessage = result.errorDescription,
                            correlationId = result.correlationId
                        )
                    }

                    // This should be caught earlier in the flow, so throwing UnexpectedError
                    is SignUpCommandResult.UsernameAlreadyExists -> {
                        Logger.warn(
                            TAG,
                            result.correlationId,
                            "Submit password received unexpected result: $result"
                        )
                        SignUpSubmitPasswordError(
                            error = result.error,
                            errorMessage = result.errorDescription,
                            correlationId = result.correlationId
                        )
                    }

                    // This should be caught earlier in the flow, so throwing UnexpectedError
                    is SignUpCommandResult.InvalidEmail -> {
                        Logger.warn(
                            TAG,
                            result.correlationId,
                            "Submit password received unexpected result: $result"
                        )
                        SignUpSubmitPasswordError(
                            error = result.error,
                            errorMessage = result.errorDescription,
                            correlationId = result.correlationId
                        )
                    }

                    is INativeAuthCommandResult.UnknownError -> {
                        Logger.warn(
                            TAG,
                            result.correlationId,
                            "Submit password received unexpected result: $result"
                        )
                        SignUpSubmitPasswordError(
                            errorMessage = result.errorDescription,
                            error = result.error,
                            correlationId = result.correlationId,
                            exception = result.exception
                        )
                    }
                }
            } finally {
                StringUtil.overwriteWithNull(commandParameters.password)
            }
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(continuationToken)
        parcel.writeString(correlationId)
        parcel.writeString(username)
        parcel.writeSerializable(config)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SignUpPasswordRequiredState> {
        override fun createFromParcel(parcel: Parcel): SignUpPasswordRequiredState {
            return SignUpPasswordRequiredState(parcel)
        }

        override fun newArray(size: Int): Array<SignUpPasswordRequiredState?> {
            return arrayOfNulls(size)
        }
    }
}

/**
 * Native Auth uses a state machine to denote state of and transitions within a flow.
 * SignUpAttributesRequiredState class represents a state where the user has to provide signup
 * attributes to progress in the signup flow.
 * @property continuationToken: Continuation token to be passed in the next request
 * @property correlationId: Correlation ID taken from the previous API response and passed to the next request
 * @property username: Email address of the user
 * @property config Configuration used by Native Auth
 */
class SignUpAttributesRequiredState internal constructor(
    override val continuationToken: String,
    override val correlationId: String?,
    private val username: String,
    private val config: NativeAuthPublicClientApplicationConfiguration
) : BaseState(continuationToken = continuationToken, correlationId = correlationId), State, Parcelable {
    private val TAG: String = SignUpAttributesRequiredState::class.java.simpleName

    constructor(parcel: Parcel) : this(
        continuationToken = parcel.readString() ?: "",
        correlationId = parcel.readString(),
        username = parcel.readString() ?: "",
        config = parcel.serializable<NativeAuthPublicClientApplicationConfiguration>() as NativeAuthPublicClientApplicationConfiguration
    )

    interface SignUpSubmitUserAttributesCallback : Callback<SignUpSubmitAttributesResult>

    /**
     * Submits the user attributes required to the server; callback variant.
     *
     * @param attributes mandatory attributes set in the tenant configuration. Should use [com.microsoft.identity.nativeauth.UserAttributes] to convert to a map.
     * @param callback [com.microsoft.identity.nativeauth.statemachine.states.SignUpAttributesRequiredState.SignUpSubmitUserAttributesCallback] to receive the result on.
     * @return The results of the submit user attributes action.
     */
    fun submitAttributes(
        attributes: UserAttributes,
        callback: SignUpSubmitUserAttributesCallback
    ) {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.submitAttributes"
        )
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
     * @param attributes mandatory attributes set in the tenant configuration. Should use [com.microsoft.identity.nativeauth.UserAttributes] to convert to a map.
     * @return The results of the submit user attributes action.
     */
    suspend fun submitAttributes(attributes: UserAttributes): SignUpSubmitAttributesResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.submitAttributes(attributes: UserAttributes)"
        )
        return withContext(Dispatchers.IO) {
            val commandParameters =
                CommandParametersAdapter.createSignUpStarSubmitUserAttributesCommandParameters(
                    config,
                    config.oAuth2TokenCache,
                    continuationToken,
                    correlationId,
                    attributes.toMap()
                )

            val command = SignUpSubmitUserAttributesCommand(
                commandParameters,
                NativeAuthMsalController(),
                PublicApiId.NATIVE_AUTH_SIGN_UP_SUBMIT_ATTRIBUTES
            )

            val rawCommandResult = CommandDispatcher.submitSilentReturningFuture(command).get()

            return@withContext when (val result = rawCommandResult.checkAndWrapCommandResultType<SignUpSubmitUserAttributesCommandResult>()) {
                is SignUpCommandResult.AttributesRequired -> {
                    SignUpResult.AttributesRequired(
                        nextState = SignUpAttributesRequiredState(
                            continuationToken = result.continuationToken,
                            correlationId = result.correlationId,
                            username = username,
                            config = config
                        ),
                        requiredAttributes = result.requiredAttributes.toListOfRequiredUserAttribute()
                    )
                }
                is SignUpCommandResult.Complete -> {
                    SignUpResult.Complete(
                        nextState = SignInAfterSignUpState(
                            continuationToken = result.continuationToken,
                            correlationId = result.correlationId,
                            username = username,
                            config = config
                        )
                    )
                }
                is SignUpCommandResult.InvalidAttributes -> {
                    SignUpSubmitAttributesError(
                        errorType = SignUpErrorTypes.INVALID_ATTRIBUTES,
                        error = result.error,
                        errorMessage = result.errorDescription,
                        correlationId = result.correlationId
                    )
                }
                is INativeAuthCommandResult.Redirect -> {
                    SignUpSubmitAttributesError(
                        errorType = ErrorTypes.BROWSER_REQUIRED,
                        error = result.error,
                        errorMessage = result.errorDescription,
                        correlationId = result.correlationId
                    )
                }
                // This should be caught earlier in the flow, so throwing UnexpectedError
                is SignUpCommandResult.UsernameAlreadyExists -> {
                    Logger.warn(
                        TAG,
                        result.correlationId,
                        "Submit attributes received unexpected result: $result"
                    )
                    SignUpSubmitAttributesError(
                        errorMessage = result.errorDescription,
                        error = result.error,
                        correlationId = result.correlationId
                    )
                }
                is INativeAuthCommandResult.UnknownError -> {
                    Logger.warn(
                        TAG,
                        result.correlationId,
                        "Submit attributes received unexpected result: $result"
                    )
                    SignUpSubmitAttributesError(
                        errorMessage = result.errorDescription,
                        error = result.error,
                        correlationId = result.correlationId,
                        exception = result.exception
                    )
                }
            }
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(continuationToken)
        parcel.writeString(correlationId)
        parcel.writeString(username)
        parcel.writeSerializable(config)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SignUpAttributesRequiredState> {
        override fun createFromParcel(parcel: Parcel): SignUpAttributesRequiredState {
            return SignUpAttributesRequiredState(parcel)
        }

        override fun newArray(size: Int): Array<SignUpAttributesRequiredState?> {
            return arrayOfNulls(size)
        }
    }
}

/**
 * Native Auth uses a state machine to denote state of and transitions within a flow.
 * SignInAfterSignUpState class represents a state where the user must signin after successful
 * signup flow.
 * @property continuationToken: Token to be passed in the next request
 * @property username: Email address of the user
 * @property config Configuration used by Native Auth
 */
class SignInAfterSignUpState internal constructor(
    override val continuationToken: String?,
    override val correlationId: String?,
    override val username: String,
    private val config: NativeAuthPublicClientApplicationConfiguration
) : SignInAfterSignUpBaseState(
    continuationToken = continuationToken,
    correlationId = correlationId,
    username = username,
    config = config
) {
    private val TAG: String = SignInAfterSignUpState::class.java.simpleName

    constructor(parcel: Parcel) : this(
        continuationToken = parcel.readString() ?: "",
        correlationId = parcel.readString(),
        username = parcel.readString() ?: "",
        config = parcel.serializable<NativeAuthPublicClientApplicationConfiguration>() as NativeAuthPublicClientApplicationConfiguration
    )

    interface SignInAfterSignUpCallback : SignInAfterSignUpBaseState.SignInAfterSignUpCallback

    /**
     * Signs in with the sign-in-after-sign-up verification code; callback variant.
     *
     * @param scopes (Optional) the scopes to request.
     * @param callback [com.microsoft.identity.nativeauth.statemachine.states.SignInAfterSignUpState.SignInAfterSignUpCallback] to receive the result on.
     * @return The results of the sign-in-after-sign-up action.
     */
    fun signIn(scopes: List<String>? = null, callback: SignInAfterSignUpCallback) {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.signIn"
        )
        return signInAfterSignUp(scopes = scopes, callback = callback)
    }

    /**
     * Signs in with the sign-in-after-sign-up verification code; Kotlin coroutines variant.
     *
     * @param scopes (Optional) the scopes to request.
     * @return The results of the sign-in-after-sign-up action.
     */
    suspend fun signIn(scopes: List<String>? = null): SignInResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.signIn(scopes: List<String>)"
        )
        return signInAfterSignUp(scopes = scopes)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(continuationToken)
        parcel.writeString(correlationId)
        parcel.writeString(username)
        parcel.writeSerializable(config)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SignInAfterSignUpState> {
        override fun createFromParcel(parcel: Parcel): SignInAfterSignUpState {
            return SignInAfterSignUpState(parcel)
        }

        override fun newArray(size: Int): Array<SignInAfterSignUpState?> {
            return arrayOfNulls(size)
        }
    }
}
