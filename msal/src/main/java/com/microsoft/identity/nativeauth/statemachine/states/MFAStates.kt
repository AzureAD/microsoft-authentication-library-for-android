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
import com.microsoft.identity.nativeauth.AuthMethod
import com.microsoft.identity.client.AuthenticationResultAdapter
import com.microsoft.identity.client.internal.CommandParametersAdapter
import com.microsoft.identity.common.java.controllers.CommandDispatcher
import com.microsoft.identity.common.java.eststelemetry.PublicApiId
import com.microsoft.identity.common.java.logging.LogSession
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
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplicationConfiguration
import com.microsoft.identity.nativeauth.statemachine.errors.ErrorTypes
import com.microsoft.identity.nativeauth.statemachine.errors.MFAError
import com.microsoft.identity.nativeauth.statemachine.results.MFAGetAuthMethodsResult
import com.microsoft.identity.nativeauth.statemachine.results.MFARequiredResult
import com.microsoft.identity.nativeauth.statemachine.results.MFASubmitChallengeResult
import com.microsoft.identity.nativeauth.statemachine.results.SignInResult
import com.microsoft.identity.nativeauth.toListOfAuthMethods
import com.microsoft.identity.nativeauth.utils.serializable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AwaitingMFAState(
    override val continuationToken: String,
    override val correlationId: String,
    private val scopes: List<String>?,
    private val config: NativeAuthPublicClientApplicationConfiguration
) : BaseState(continuationToken = continuationToken, correlationId = correlationId), State, Parcelable {
    private val TAG: String = AwaitingMFAState::class.java.simpleName

    // Challenge default auth method
    suspend fun sendChallenge(): MFARequiredResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.sendChallenge()"
        )
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
                    is INativeAuthCommandResult.APIError -> TODO()
                }
            } catch (e: Exception) {
                MFAError(
                    errorType = ErrorTypes.CLIENT_EXCEPTION,
                    errorMessage = "MSAL client exception occurred in sendChallenge().",
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

    // Call /introspect
    suspend fun getAuthMethods(): MFAGetAuthMethodsResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.getAuthMethods()"
        )
        return withContext(Dispatchers.IO) {
            try {
                val params = CommandParametersAdapter.createGetAuthMethodsCommandParameters(
                    config,
                    config.oAuth2TokenCache,
                    continuationToken,
                    correlationId,
                    scopes
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

                    is INativeAuthCommandResult.APIError -> TODO()
                }
            } catch (e: Exception) {
                MFAError(
                    errorType = ErrorTypes.CLIENT_EXCEPTION,
                    errorMessage = "MSAL client exception occurred in getAuthMethods().",
                    exception = e,
                    correlationId = correlationId
                )
            }
        }
    }

    suspend fun sendChallenge(authMethodId: String): MFARequiredResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.sendChallenge(authMethodId: String)"
        )

        return withContext(Dispatchers.IO) {
            try {
                val params = CommandParametersAdapter.createMFASelectedChallengeCommandParameters(
                    config,
                    config.oAuth2TokenCache,
                    continuationToken,
                    correlationId,
                    authMethodId,
                    scopes
                )

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
                        TODO()
                    }

                    is INativeAuthCommandResult.APIError -> TODO()
                }
            } catch (e: Exception) {
                MFAError(
                    errorType = ErrorTypes.CLIENT_EXCEPTION,
                    errorMessage = "MSAL client exception occurred in sendChallenge(authMethodId).",
                    exception = e,
                    correlationId = correlationId
                )
            }
        }
    }

    // Call /token
    suspend fun submitChallenge(challenge: String): MFASubmitChallengeResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.submitChallenge(challenge: String)"
        )

        return withContext(Dispatchers.IO) {
            try {
                val params = CommandParametersAdapter.createMFASubmitChallengeCommandParameters(
                    config,
                    config.oAuth2TokenCache,
                    continuationToken,
                    correlationId,
                    challenge,
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

                    is INativeAuthCommandResult.APIError -> TODO()
                }
            } catch (e: Exception) {
                MFAError(
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