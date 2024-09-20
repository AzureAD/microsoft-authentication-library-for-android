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

import android.os.Parcel
import android.os.Parcelable
import com.microsoft.identity.client.AuthenticationResultAdapter
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.client.internal.CommandParametersAdapter
import com.microsoft.identity.common.java.controllers.CommandDispatcher
import com.microsoft.identity.common.java.eststelemetry.PublicApiId
import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.nativeauth.controllers.results.GetAuthMethodsCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.INativeAuthCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.MFAChallengeCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.MFACommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.MFASubmitChallengeCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.SignInCommandResult
import com.microsoft.identity.common.java.nativeauth.util.checkAndWrapCommandResultType
import com.microsoft.identity.common.nativeauth.internal.commands.GetAuthMethodsCommand
import com.microsoft.identity.common.nativeauth.internal.commands.MFAChallengeCommand
import com.microsoft.identity.common.nativeauth.internal.commands.MFASubmitChallengeCommand
import com.microsoft.identity.common.nativeauth.internal.controllers.NativeAuthMsalController
import com.microsoft.identity.nativeauth.AuthMethod
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplication
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplicationConfiguration
import com.microsoft.identity.nativeauth.statemachine.errors.ErrorTypes
import com.microsoft.identity.nativeauth.statemachine.errors.MFAGetAuthMethodsError
import com.microsoft.identity.nativeauth.statemachine.errors.MFARequestChallengeError
import com.microsoft.identity.nativeauth.statemachine.errors.MFASubmitChallengeError
import com.microsoft.identity.nativeauth.statemachine.results.MFAGetAuthMethodsResult
import com.microsoft.identity.nativeauth.statemachine.results.MFARequiredResult
import com.microsoft.identity.nativeauth.statemachine.results.MFASubmitChallengeResult
import com.microsoft.identity.nativeauth.statemachine.results.SignInResult
import com.microsoft.identity.nativeauth.toListOfAuthMethods
import com.microsoft.identity.nativeauth.utils.serializable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AwaitingMFAState(
    override val continuationToken: String,
    override val correlationId: String,
    private val scopes: List<String>?,
    private val config: NativeAuthPublicClientApplicationConfiguration
) : BaseState(continuationToken = continuationToken, correlationId = correlationId), State, Parcelable {
    private val TAG: String = AwaitingMFAState::class.java.simpleName

    /**
     * RequestChallengeCallback receives the result for requestChallenge() in MFA flows in native authentication.
     */
    interface RequestChallengeCallback : Callback<MFARequiredResult>

    /**
     * Requests a challenge to be sent to the user's default authentication method; callback variant.
     *
     * <strong><u>Warning: this API is experimental. It may be changed in the future without notice. Do not use in production applications.</u></strong>
     * @param callback [com.microsoft.identity.nativeauth.statemachine.states.AwaitingMFAState.RequestChallengeCallback] to receive the result on.
     * @return The result of the request challenge action.
     */
    fun requestChallenge(callback: RequestChallengeCallback) {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.requestChallenge(callback: RequestChallengeCallback)"
        )
        NativeAuthPublicClientApplication.pcaScope.launch {
            try {
                val result = requestChallenge()
                callback.onResult(result)
            } catch (e: MsalException) {
                Logger.error(TAG, "Exception thrown in requestChallenge", e)
                callback.onError(e)
            }
        }
    }

    /**
     * Requests a challenge to be sent to the user's default authentication method; Kotlin coroutines variant.
     *
     * <strong><u>Warning: this API is experimental. It may be changed in the future without notice. Do not use in production applications.</u></strong>
     * @return The result of the request challenge action.
     */
    suspend fun requestChallenge(): MFARequiredResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.requestChallenge()"
        )

        Logger.warn(TAG, "Warning: this API is experimental. It may be changed in the future without notice. Do not use in production applications.")

        return withContext(Dispatchers.IO) {
            try {
                val params = CommandParametersAdapter.createMFADefaultChallengeCommandParameters(
                    config,
                    config.oAuth2TokenCache,
                    continuationToken,
                    correlationId,
                    scopes
                )
                val command = MFAChallengeCommand(
                    parameters = params,
                    controller = NativeAuthMsalController(),
                    publicApiId = PublicApiId.NATIVE_AUTH_MFA_DEFAULT_CHALLENGE
                )

                val rawCommandResult =
                    CommandDispatcher.submitSilentReturningFuture(command)
                        .get()

                return@withContext when (val result =
                    rawCommandResult.checkAndWrapCommandResultType<MFAChallengeCommandResult>()) {
                    is MFACommandResult.VerificationRequired -> {
                        MFARequiredResult.VerificationRequired(
                            nextState = MFARequiredState(
                                continuationToken = result.continuationToken,
                                correlationId = result.correlationId,
                                scopes = scopes,
                                config = config
                            ),
                            codeLength = result.codeLength,
                            sentTo = result.challengeTargetLabel,
                            channel = result.challengeChannel
                        )
                    }
                    is MFACommandResult.SelectionRequired -> {
                        MFARequiredResult.SelectionRequired(
                            nextState = MFARequiredState(
                                continuationToken = result.continuationToken,
                                correlationId = result.correlationId,
                                scopes = scopes,
                                config = config
                            ),
                            authMethods = result.authMethods.toListOfAuthMethods()
                        )
                    }
                    is INativeAuthCommandResult.APIError -> {
                        Logger.warnWithObject(
                            TAG,
                            result.correlationId,
                            "requestChallenge() received unexpected result: ",
                            result
                        )
                        MFARequestChallengeError(
                            errorMessage = result.errorDescription,
                            error = result.error,
                            correlationId = result.correlationId,
                            errorCodes = result.errorCodes,
                            exception = result.exception
                        )
                    }
                    is INativeAuthCommandResult.Redirect -> {
                        MFARequestChallengeError(
                            errorType = ErrorTypes.BROWSER_REQUIRED,
                            error = result.error,
                            errorMessage = result.errorDescription,
                            correlationId = result.correlationId
                        )
                    }
                }
            } catch (e: Exception) {
                MFARequestChallengeError(
                    errorType = ErrorTypes.CLIENT_EXCEPTION,
                    errorMessage = "MSAL client exception occurred in requestChallenge().",
                    exception = e,
                    correlationId = correlationId
                )
            }
        }
    }

    constructor(parcel: Parcel) : this(
        continuationToken = parcel.readString()  ?: "",
        correlationId = parcel.readString() ?: "UNSET",
        scopes = parcel.createStringArrayList(),
        config = parcel.serializable<NativeAuthPublicClientApplicationConfiguration>() as NativeAuthPublicClientApplicationConfiguration
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(continuationToken)
        parcel.writeString(correlationId)
        parcel.writeStringList(scopes)
        parcel.writeSerializable(config)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<AwaitingMFAState> {
        override fun createFromParcel(parcel: Parcel): AwaitingMFAState {
            return AwaitingMFAState(parcel)
        }

        override fun newArray(size: Int): Array<AwaitingMFAState?> {
            return arrayOfNulls(size)
        }
    }
}

class MFARequiredState(
    override val continuationToken: String,
    override val correlationId: String,
    private val scopes: List<String>?,
    private val config: NativeAuthPublicClientApplicationConfiguration
) : BaseState(continuationToken = continuationToken, correlationId = correlationId), State, Parcelable {
    private val TAG: String = MFARequiredState::class.java.simpleName

    /**
     * GetAuthMethodsCallback receives the result for getAuthMethods() in MFA flows in native authentication.
     */
    interface GetAuthMethodsCallback : Callback<MFAGetAuthMethodsResult>

    /**
     * Retrieves all authentication methods that can be used to complete the challenge flow; callback variant.
     *
     * <strong><u>Warning: this API is experimental. It may be changed in the future without notice. Do not use in production applications.</u></strong>
     * @param callback [com.microsoft.identity.nativeauth.statemachine.states.MFARequiredState.GetAuthMethodsCallback] to receive the result on.
     * @return The results of the get authentication methods action.
     */
    fun getAuthMethods(callback: GetAuthMethodsCallback) {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.getAuthMethods(callback: GetAuthMethodsCallback)"
        )
        NativeAuthPublicClientApplication.pcaScope.launch {
            try {
                val result = getAuthMethods()
                callback.onResult(result)
            } catch (e: MsalException) {
                Logger.error(TAG, "Exception thrown in getAuthMethods", e)
                callback.onError(e)
            }
        }
    }

    /**
     * Retrieves all authentication methods that can be used to complete the challenge flow; Kotlin coroutines variant.
     *
     * <strong><u>Warning: this API is experimental. It may be changed in the future without notice. Do not use in production applications.</u></strong>
     * @return The results of the get authentication methods action.
     */
    suspend fun getAuthMethods(): MFAGetAuthMethodsResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.getAuthMethods()"
        )

        Logger.warn(TAG, "Warning: this API is experimental. It may be changed in the future without notice. Do not use in production applications.")

        return withContext(Dispatchers.IO) {
            try {
                val params = CommandParametersAdapter.createGetAuthMethodsCommandParameters(
                    config,
                    config.oAuth2TokenCache,
                    continuationToken,
                    correlationId
                )
                val command = GetAuthMethodsCommand(
                    parameters = params,
                    controller = NativeAuthMsalController(),
                    publicApiId = PublicApiId.NATIVE_AUTH_GET_AUTH_METHODS
                )

                val rawCommandResult =
                    CommandDispatcher.submitSilentReturningFuture(command)
                        .get()

                return@withContext when (val result =
                    rawCommandResult.checkAndWrapCommandResultType<GetAuthMethodsCommandResult>()) {
                    is MFACommandResult.SelectionRequired -> {
                        MFARequiredResult.SelectionRequired(
                            nextState = MFARequiredState(
                                continuationToken = result.continuationToken,
                                correlationId = result.correlationId,
                                scopes = scopes,
                                config = config
                            ),
                            authMethods = result.authMethods.toListOfAuthMethods()
                        )
                    }
                    is INativeAuthCommandResult.APIError -> {
                        Logger.warnWithObject(
                            TAG,
                            result.correlationId,
                            "getAuthMethods() received unexpected result: ",
                            result
                        )
                        MFAGetAuthMethodsError(
                            errorMessage = result.errorDescription,
                            error = result.error,
                            correlationId = result.correlationId,
                            errorCodes = result.errorCodes,
                            exception = result.exception
                        )
                    }
                    is INativeAuthCommandResult.Redirect -> {
                        MFAGetAuthMethodsError(
                            errorType = ErrorTypes.BROWSER_REQUIRED,
                            error = result.error,
                            errorMessage = result.errorDescription,
                            correlationId = result.correlationId
                        )
                    }
                }
            } catch (e: Exception) {
                MFAGetAuthMethodsError(
                    errorType = ErrorTypes.CLIENT_EXCEPTION,
                    errorMessage = "MSAL client exception occurred in getAuthMethods().",
                    exception = e,
                    correlationId = correlationId
                )
            }
        }
    }

    /**
     * RequestChallengeCallback receives the result for requestChallenge() in MFA flows in native authentication.
     */
    interface RequestChallengeCallback : Callback<MFARequiredResult>

    /**
     * Requests a challenge to be sent to the user's default authentication method; callback variant.
     * If an authentication method ID was supplied, the server will send a challenge to the specified method. If no ID is supplied,
     * the server will attempt to send the challenge to the user's default auth method.
     *
     * <strong><u>Warning: this API is experimental. It may be changed in the future without notice. Do not use in production applications.</u></strong>
     * @param authMethod [com.microsoft.identity.nativeauth.AuthMethod] the authentication method used for the challenge operation.
     * @param callback [com.microsoft.identity.nativeauth.statemachine.states.MFARequiredState.RequestChallengeCallback] to receive the result on.
     * @return The result of the request challenge action.
     */
    fun requestChallenge(authMethod: AuthMethod? = null, callback: RequestChallengeCallback) {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.requestChallenge(callback: RequestChallengeCallback)"
        )
        NativeAuthPublicClientApplication.pcaScope.launch {
            try {
                val result = requestChallenge(authMethod)
                callback.onResult(result)
            } catch (e: MsalException) {
                Logger.error(TAG, "Exception thrown in requestChallenge", e)
                callback.onError(e)
            }
        }
    }

    /**
     * Requests a challenge to be sent to the user's default authentication method; Kotlin coroutines variant.
     * If an authentication method ID was supplied, the server will send a challenge to the specified method. If no ID is supplied,
     * the server will attempt to send the challenge to the user's default auth method.
     *
     * <strong><u>Warning: this API is experimental. It may be changed in the future without notice. Do not use in production applications.</u></strong>
     * @param authMethod [com.microsoft.identity.nativeauth.AuthMethod] the authentication method used for the challenge operation.
     * @return The result of the request challenge action.
     */
    suspend fun requestChallenge(authMethod: AuthMethod? = null): MFARequiredResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.requestChallenge(authMethod: AuthMethod)"
        )

        Logger.warn(TAG, "Warning: this API is experimental. It may be changed in the future without notice. Do not use in production applications.")

        return withContext(Dispatchers.IO) {
            try {
                val params = if (authMethod != null) {
                    CommandParametersAdapter.createMFASelectedChallengeCommandParameters(
                        config,
                        config.oAuth2TokenCache,
                        continuationToken,
                        correlationId,
                        authMethod
                    )
                } else {
                    CommandParametersAdapter.createMFADefaultChallengeCommandParameters(
                        config,
                        config.oAuth2TokenCache,
                        continuationToken,
                        correlationId,
                        scopes
                    )
                }

                val command = MFAChallengeCommand(
                    parameters = params,
                    controller = NativeAuthMsalController(),
                    publicApiId = PublicApiId.NATIVE_AUTH_MFA_SELECTED_CHALLENGE
                )

                val rawCommandResult =
                    CommandDispatcher.submitSilentReturningFuture(command)
                        .get()

                return@withContext when (val result =
                    rawCommandResult.checkAndWrapCommandResultType<MFAChallengeCommandResult>()) {
                    is MFACommandResult.VerificationRequired -> {
                        MFARequiredResult.VerificationRequired(
                            nextState = MFARequiredState(
                                continuationToken = result.continuationToken,
                                correlationId = result.correlationId,
                                scopes = scopes,
                                config = config
                            ),
                            codeLength = result.codeLength,
                            sentTo = result.challengeTargetLabel,
                            channel = result.challengeChannel
                        )
                    }
                    is MFACommandResult.SelectionRequired -> {
                        MFARequiredResult.SelectionRequired(
                            nextState = MFARequiredState(
                                continuationToken = result.continuationToken,
                                correlationId = result.correlationId,
                                scopes = scopes,
                                config = config
                            ),
                            authMethods = result.authMethods.toListOfAuthMethods()
                        )
                    }
                    is INativeAuthCommandResult.APIError -> {
                        Logger.warnWithObject(
                            TAG,
                            result.correlationId,
                            "requestChallenge(authMethod) received unexpected result: ",
                            result
                        )
                        MFARequestChallengeError(
                            errorMessage = result.errorDescription,
                            error = result.error,
                            correlationId = result.correlationId,
                            errorCodes = result.errorCodes,
                            exception = result.exception
                        )
                    }
                    is INativeAuthCommandResult.Redirect -> {
                        MFARequestChallengeError(
                            errorType = ErrorTypes.BROWSER_REQUIRED,
                            error = result.error,
                            errorMessage = result.errorDescription,
                            correlationId = result.correlationId
                        )
                    }
                }
            } catch (e: Exception) {
                MFARequestChallengeError(
                    errorType = ErrorTypes.CLIENT_EXCEPTION,
                    errorMessage = "MSAL client exception occurred in requestChallenge(authMethod).",
                    exception = e,
                    correlationId = correlationId
                )
            }
        }
    }

    /**
     * SubmitChallengeCallback receives the result for submitChallenge() in MFA flows in native authentication.
     */
    interface SubmitChallengeCallback : Callback<MFASubmitChallengeResult>

    /**
     * Submits the challenge value to the server; callback variant.
     *
     * <strong><u>Warning: this API is experimental. It may be changed in the future without notice. Do not use in production applications.</u></strong>
     * @param callback [com.microsoft.identity.nativeauth.statemachine.states.MFARequiredState.SubmitChallengeCallback] to receive the result on.
     * @return The result of the submit challenge action.
     */
    fun submitChallenge(challenge: String, callback: SubmitChallengeCallback) {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.submitChallenge(callback: SubmitChallengeCallback)"
        )
        NativeAuthPublicClientApplication.pcaScope.launch {
            try {
                val result = submitChallenge(challenge)
                callback.onResult(result)
            } catch (e: MsalException) {
                Logger.error(TAG, "Exception thrown in submitChallenge", e)
                callback.onError(e)
            }
        }
    }

    /**
     * Submits the challenge value to the server; Kotlin coroutines variant.
     *
     * <strong><u>Warning: this API is experimental. It may be changed in the future without notice. Do not use in production applications.</u></strong>
     * @return The result of the submit challenge action.
     */
    suspend fun submitChallenge(challenge: String): MFASubmitChallengeResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.submitChallenge(challenge: String)"
        )

        Logger.warn(TAG, "Warning: this API is experimental. It may be changed in the future without notice. Do not use in production applications.")

        return withContext(Dispatchers.IO) {
            try {
                val params = CommandParametersAdapter.createMFASubmitChallengeCommandParameters(
                    config,
                    config.oAuth2TokenCache,
                    challenge,
                    correlationId,
                    continuationToken,
                    scopes
                )

                val command = MFASubmitChallengeCommand(
                    parameters = params,
                    controller = NativeAuthMsalController(),
                    publicApiId = PublicApiId.NATIVE_AUTH_MFA_SUBMIT_CHALLENGE
                )

                val rawCommandResult =
                    CommandDispatcher.submitSilentReturningFuture(command)
                        .get()

                return@withContext when (val result =
                    rawCommandResult.checkAndWrapCommandResultType<MFASubmitChallengeCommandResult>()) {
                    is SignInCommandResult.Complete -> {
                        val authenticationResult =
                            AuthenticationResultAdapter.adapt(result.authenticationResult)

                        SignInResult.Complete(
                            resultValue = AccountState.createFromAuthenticationResult(
                                authenticationResult = authenticationResult,
                                correlationId = result.correlationId,
                                config = config
                            )
                        )
                    }
                    is SignInCommandResult.IncorrectCode -> {
                        MFASubmitChallengeError(
                            errorType = ErrorTypes.INVALID_CHALLENGE,
                            error = result.error,
                            errorMessage = result.errorDescription,
                            correlationId = result.correlationId,
                            errorCodes = result.errorCodes,
                            subError = result.subError
                        )

                    }
                    is INativeAuthCommandResult.APIError -> {
                        Logger.warnWithObject(
                            TAG,
                            result.correlationId,
                            "submitChallenge(challenge) received unexpected result: ",
                            result
                        )
                        MFASubmitChallengeError(
                            errorMessage = result.errorDescription,
                            error = result.error,
                            correlationId = result.correlationId,
                            errorCodes = result.errorCodes,
                            exception = result.exception
                        )
                    }
                }
            } catch (e: Exception) {
                MFASubmitChallengeError(
                    errorType = ErrorTypes.CLIENT_EXCEPTION,
                    errorMessage = "MSAL client exception occurred in submitChallenge(challenge)",
                    exception = e,
                    correlationId = correlationId
                )
            }
        }
    }

    constructor(parcel: Parcel) : this(
        continuationToken = parcel.readString()  ?: "",
        correlationId = parcel.readString() ?: "UNSET",
        scopes = parcel.createStringArrayList(),
        config = parcel.serializable<NativeAuthPublicClientApplicationConfiguration>() as NativeAuthPublicClientApplicationConfiguration
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(continuationToken)
        parcel.writeString(correlationId)
        parcel.writeStringList(scopes)
        parcel.writeSerializable(config)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MFARequiredState> {
        override fun createFromParcel(parcel: Parcel): MFARequiredState {
            return MFARequiredState(parcel)
        }

        override fun newArray(size: Int): Array<MFARequiredState?> {
            return arrayOfNulls(size)
        }
    }
}