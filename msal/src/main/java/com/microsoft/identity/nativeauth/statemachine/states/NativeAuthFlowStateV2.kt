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
import com.microsoft.identity.common.java.nativeauth.controllers.results.INativeAuthCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2CommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2ResendCodeCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2SignInAfterResetPasswordCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2SubmitCodeCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2SubmitNewPasswordCommandResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationState
import com.microsoft.identity.common.java.nativeauth.util.checkAndWrapCommandResultType
import com.microsoft.identity.common.nativeauth.internal.commands.NativeAuthV2ResendCodeCommand
import com.microsoft.identity.common.nativeauth.internal.commands.NativeAuthV2SignInAfterResetPasswordCommand
import com.microsoft.identity.common.nativeauth.internal.commands.NativeAuthV2SubmitCodeCommand
import com.microsoft.identity.common.nativeauth.internal.commands.NativeAuthV2SubmitNewPasswordCommand
import com.microsoft.identity.common.nativeauth.internal.controllers.v2.NativeAuthV2FlowController
import com.microsoft.identity.nativeauth.AuthMethod
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplication
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplicationConfiguration
import com.microsoft.identity.nativeauth.UserAttributes
import com.microsoft.identity.nativeauth.statemachine.errors.ErrorTypes
import com.microsoft.identity.nativeauth.statemachine.errors.NativeAuthErrorV2
import com.microsoft.identity.nativeauth.statemachine.errors.NativeAuthFlowScenarioV2
import com.microsoft.identity.nativeauth.statemachine.errors.NativeAuthV2ErrorTypes
import com.microsoft.identity.nativeauth.statemachine.results.NativeAuthResultV2
import com.microsoft.identity.nativeauth.utils.serializable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * NativeAuthFlowStateV2 is the single unified state for the Native Auth V2 surface.
 *
 * When created from a command result the opaque [continuationState] DTO is the single source of
 * truth for the continuation token, correlation ID, scopes, and scenario; those fields are
 * derived from it and are not stored redundantly. When no DTO is present (legacy/test
 * construction) the fields are stored directly and [continuationState] is null.
 *
 * @property continuationToken Continuation token (derived from [continuationState] when present).
 * @property correlationId Correlation ID (derived from [continuationState] when present).
 * @property scenario Identifies which part of the Native Auth V2 surface this state belongs to.
 * @property config Configuration used by Native Auth.
 * @property continuationState Opaque DTO carrying mid-flow state; null only in legacy/test paths.
 */
class NativeAuthFlowStateV2 internal constructor(
    override val continuationToken: String,
    override val correlationId: String,
    internal val scenario: NativeAuthFlowScenarioV2,
    private val config: NativeAuthPublicClientApplicationConfiguration,
    internal val continuationState: NativeAuthV2ContinuationState? = null
) : BaseState(continuationToken = continuationToken, correlationId = correlationId), State, Parcelable {
    private val TAG: String = NativeAuthFlowStateV2::class.java.simpleName

    /**
     * Constructs a V2 flow state from the opaque continuation DTO returned by a command result.
     * The base fields are derived from [continuationState.correlationId]; the raw continuation
     * token stays inside the opaque DTO and is never exposed to MSAL-layer code.
     */
    internal constructor(
        continuationState: NativeAuthV2ContinuationState,
        config: NativeAuthPublicClientApplicationConfiguration
    ) : this(
        // continuationState.continuationToken is internal to common4j; use correlationId as the
        // BaseState placeholder. V2 commands receive the full opaque DTO, never this field.
        continuationToken = continuationState.correlationId,
        correlationId = continuationState.correlationId,
        scenario = NativeAuthFlowScenarioV2.RESET_PASSWORD,
        config = config,
        continuationState = continuationState
    )

    private fun notImplemented(): NativeAuthResultV2 = NativeAuthErrorV2(
        errorType = NativeAuthV2ErrorTypes.NOT_IMPLEMENTED,
        errorMessage = "This is not implemented yet",
        correlationId = correlationId,
        scenario = scenario
    )

    private fun NativeAuthV2ContinuationState.toFlowState(): NativeAuthFlowStateV2 =
        NativeAuthFlowStateV2(continuationState = this, config = config)

    interface SubmitCodeCallback : Callback<NativeAuthResultV2>

    fun submitCode(code: String, callback: SubmitCodeCallback) {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.submitCode(code: String, callback: SubmitCodeCallback)"
        )
        NativeAuthPublicClientApplication.pcaScope.launch {
            try {
                callback.onResult(submitCode(code))
            } catch (e: MsalException) {
                Logger.error(TAG, "Exception thrown in submitCode", e)
                callback.onError(e)
            }
        }
    }

    suspend fun submitCode(code: String): NativeAuthResultV2 {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.submitCode(code: String)"
        )
        val state = continuationState ?: return notImplemented()
        return withContext(Dispatchers.IO) {
            try {
                val parameters = CommandParametersAdapter.createNativeAuthV2SubmitCodeCommandParameters(
                    config,
                    config.oAuth2TokenCache,
                    code,
                    state
                )
                val command = NativeAuthV2SubmitCodeCommand(
                    parameters,
                    NativeAuthV2FlowController(),
                    PublicApiId.NATIVE_AUTH_V2_RESET_PASSWORD_SUBMIT_CODE
                )
                ensureActive()
                val rawCommandResult = CommandDispatcher.submitSilentReturningFuture(command).get()
                ensureActive()
                when (val result = rawCommandResult.checkAndWrapCommandResultType<NativeAuthV2SubmitCodeCommandResult>()) {
                    is NativeAuthV2CommandResult.NewPasswordRequired -> {
                        NativeAuthResultV2.NewPasswordRequired(
                            nextState = result.continuationState.toFlowState()
                        )
                    }
                    is NativeAuthV2CommandResult.SignInAfterResetPasswordRequired -> {
                        NativeAuthResultV2.SignInAfterResetPasswordRequired(
                            nextState = result.continuationState.toFlowState()
                        )
                    }
                    is NativeAuthV2CommandResult.Complete -> {
                        mapCompleteResult(result)
                    }
                    is NativeAuthV2CommandResult.IncorrectCode -> {
                        NativeAuthErrorV2(
                            errorType = ErrorTypes.INVALID_CODE,
                            error = result.error,
                            errorMessage = result.errorDescription,
                            correlationId = result.correlationId,
                            scenario = scenario,
                            nextState = result.retryState.toFlowState()
                        )
                    }
                    is NativeAuthV2CommandResult.NotImplemented -> {
                        NativeAuthErrorV2(
                            errorType = NativeAuthV2ErrorTypes.NOT_IMPLEMENTED,
                            error = result.error,
                            errorMessage = result.errorDescription,
                            correlationId = result.correlationId,
                            scenario = scenario
                        )
                    }
                    is INativeAuthCommandResult.Redirect -> {
                        NativeAuthErrorV2(
                            errorType = ErrorTypes.BROWSER_REQUIRED,
                            error = result.error,
                            errorMessage = result.errorDescription,
                            correlationId = result.correlationId,
                            scenario = scenario
                        )
                    }
                    is INativeAuthCommandResult.APIError -> {
                        NativeAuthErrorV2(
                            error = result.error,
                            errorMessage = result.errorDescription,
                            correlationId = result.correlationId,
                            scenario = scenario,
                            exception = result.exception
                        )
                    }
                }
            } catch (e: Exception) {
                NativeAuthErrorV2(
                    errorType = ErrorTypes.CLIENT_EXCEPTION,
                    errorMessage = "MSAL client exception occurred in submitCode.",
                    correlationId = correlationId,
                    scenario = scenario,
                    exception = e
                )
            }
        }
    }

    interface SubmitPasswordCallback : Callback<NativeAuthResultV2>

    fun submitPassword(password: CharArray, callback: SubmitPasswordCallback) {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.submitPassword(password: CharArray, callback: SubmitPasswordCallback)"
        )
        NativeAuthPublicClientApplication.pcaScope.launch {
            try {
                callback.onResult(submitPassword(password))
            } catch (e: MsalException) {
                Logger.error(TAG, "Exception thrown in submitPassword", e)
                callback.onError(e)
            }
        }
    }

    suspend fun submitPassword(password: CharArray): NativeAuthResultV2 {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.submitPassword(password: CharArray)"
        )
        return notImplemented()
    }

    interface SubmitNewPasswordCallback : Callback<NativeAuthResultV2>

    fun submitNewPassword(password: CharArray, callback: SubmitNewPasswordCallback) {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.submitNewPassword(password: CharArray, callback: SubmitNewPasswordCallback)"
        )
        NativeAuthPublicClientApplication.pcaScope.launch {
            try {
                callback.onResult(submitNewPassword(password))
            } catch (e: MsalException) {
                Logger.error(TAG, "Exception thrown in submitNewPassword", e)
                callback.onError(e)
            }
        }
    }

    suspend fun submitNewPassword(password: CharArray): NativeAuthResultV2 {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.submitNewPassword(password: CharArray)"
        )
        val state = continuationState ?: return notImplemented()
        return withContext(Dispatchers.IO) {
            try {
                val parameters = CommandParametersAdapter.createNativeAuthV2SubmitNewPasswordCommandParameters(
                    config,
                    config.oAuth2TokenCache,
                    password,
                    state
                )
                val command = NativeAuthV2SubmitNewPasswordCommand(
                    parameters,
                    NativeAuthV2FlowController(),
                    PublicApiId.NATIVE_AUTH_V2_RESET_PASSWORD_SUBMIT_NEW_PASSWORD
                )
                ensureActive()
                val rawCommandResult = CommandDispatcher.submitSilentReturningFuture(command).get()
                ensureActive()
                when (val result = rawCommandResult.checkAndWrapCommandResultType<NativeAuthV2SubmitNewPasswordCommandResult>()) {
                    is NativeAuthV2CommandResult.Complete -> {
                        mapCompleteResult(result)
                    }
                    is NativeAuthV2CommandResult.SignInAfterResetPasswordRequired -> {
                        NativeAuthResultV2.SignInAfterResetPasswordRequired(
                            nextState = result.continuationState.toFlowState()
                        )
                    }
                    is NativeAuthV2CommandResult.PasswordNotAccepted -> {
                        NativeAuthErrorV2(
                            errorType = ErrorTypes.INVALID_PASSWORD,
                            error = result.error,
                            errorMessage = result.errorDescription,
                            correlationId = result.correlationId,
                            scenario = scenario,
                            nextState = result.retryState.toFlowState()
                        )
                    }
                    is NativeAuthV2CommandResult.PasswordResetFailed -> {
                        NativeAuthErrorV2(
                            error = result.error,
                            errorMessage = result.errorDescription,
                            correlationId = result.correlationId,
                            scenario = scenario
                        )
                    }
                    is NativeAuthV2CommandResult.NotImplemented -> {
                        NativeAuthErrorV2(
                            errorType = NativeAuthV2ErrorTypes.NOT_IMPLEMENTED,
                            error = result.error,
                            errorMessage = result.errorDescription,
                            correlationId = result.correlationId,
                            scenario = scenario
                        )
                    }
                    is INativeAuthCommandResult.Redirect -> {
                        NativeAuthErrorV2(
                            errorType = ErrorTypes.BROWSER_REQUIRED,
                            error = result.error,
                            errorMessage = result.errorDescription,
                            correlationId = result.correlationId,
                            scenario = scenario
                        )
                    }
                    is INativeAuthCommandResult.APIError -> {
                        NativeAuthErrorV2(
                            error = result.error,
                            errorMessage = result.errorDescription,
                            correlationId = result.correlationId,
                            scenario = scenario,
                            exception = result.exception
                        )
                    }
                }
            } catch (e: Exception) {
                NativeAuthErrorV2(
                    errorType = ErrorTypes.CLIENT_EXCEPTION,
                    errorMessage = "MSAL client exception occurred in submitNewPassword.",
                    correlationId = correlationId,
                    scenario = scenario,
                    exception = e
                )
            }
        }
    }

    interface SubmitAttributesCallback : Callback<NativeAuthResultV2>

    fun submitAttributes(attributes: UserAttributes, callback: SubmitAttributesCallback) {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.submitAttributes(attributes: UserAttributes, callback: SubmitAttributesCallback)"
        )
        NativeAuthPublicClientApplication.pcaScope.launch {
            try {
                callback.onResult(submitAttributes(attributes))
            } catch (e: MsalException) {
                Logger.error(TAG, "Exception thrown in submitAttributes", e)
                callback.onError(e)
            }
        }
    }

    suspend fun submitAttributes(attributes: UserAttributes): NativeAuthResultV2 {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.submitAttributes(attributes: UserAttributes)"
        )
        return notImplemented()
    }

    interface SelectAuthMethodCallback : Callback<NativeAuthResultV2>

    fun selectAuthMethod(method: AuthMethod, verificationContact: String? = null, callback: SelectAuthMethodCallback) {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.selectAuthMethod(method: AuthMethod, verificationContact: String?, callback: SelectAuthMethodCallback)"
        )
        NativeAuthPublicClientApplication.pcaScope.launch {
            try {
                callback.onResult(selectAuthMethod(method, verificationContact))
            } catch (e: MsalException) {
                Logger.error(TAG, "Exception thrown in selectAuthMethod", e)
                callback.onError(e)
            }
        }
    }

    suspend fun selectAuthMethod(method: AuthMethod, verificationContact: String? = null): NativeAuthResultV2 {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.selectAuthMethod(method: AuthMethod, verificationContact: String?)"
        )
        return notImplemented()
    }

    interface SubmitChallengeCallback : Callback<NativeAuthResultV2>

    fun submitChallenge(challenge: String, callback: SubmitChallengeCallback) {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.submitChallenge(challenge: String, callback: SubmitChallengeCallback)"
        )
        NativeAuthPublicClientApplication.pcaScope.launch {
            try {
                callback.onResult(submitChallenge(challenge))
            } catch (e: MsalException) {
                Logger.error(TAG, "Exception thrown in submitChallenge", e)
                callback.onError(e)
            }
        }
    }

    suspend fun submitChallenge(challenge: String): NativeAuthResultV2 {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.submitChallenge(challenge: String)"
        )
        return notImplemented()
    }

    interface ResendCodeCallback : Callback<NativeAuthResultV2>

    fun resendCode(callback: ResendCodeCallback) {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.resendCode(callback: ResendCodeCallback)"
        )
        NativeAuthPublicClientApplication.pcaScope.launch {
            try {
                callback.onResult(resendCode())
            } catch (e: MsalException) {
                Logger.error(TAG, "Exception thrown in resendCode", e)
                callback.onError(e)
            }
        }
    }

    suspend fun resendCode(): NativeAuthResultV2 {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.resendCode()"
        )
        val state = continuationState ?: return notImplemented()
        return withContext(Dispatchers.IO) {
            try {
                val parameters = CommandParametersAdapter.createNativeAuthV2ResendCodeCommandParameters(
                    config,
                    config.oAuth2TokenCache,
                    state
                )
                val command = NativeAuthV2ResendCodeCommand(
                    parameters,
                    NativeAuthV2FlowController(),
                    PublicApiId.NATIVE_AUTH_V2_RESET_PASSWORD_RESEND_CODE
                )
                ensureActive()
                val rawCommandResult = CommandDispatcher.submitSilentReturningFuture(command).get()
                ensureActive()
                when (val result = rawCommandResult.checkAndWrapCommandResultType<NativeAuthV2ResendCodeCommandResult>()) {
                    is NativeAuthV2CommandResult.CodeRequired -> {
                        NativeAuthResultV2.CodeRequired(
                            nextState = result.continuationState.toFlowState(),
                            codeLength = result.codeLength,
                            sentTo = result.challengeTargetLabel,
                            channel = result.challengeChannel
                        )
                    }
                    is NativeAuthV2CommandResult.Complete -> {
                        mapCompleteResult(result)
                    }
                    is NativeAuthV2CommandResult.NotImplemented -> {
                        NativeAuthErrorV2(
                            errorType = NativeAuthV2ErrorTypes.NOT_IMPLEMENTED,
                            error = result.error,
                            errorMessage = result.errorDescription,
                            correlationId = result.correlationId,
                            scenario = scenario
                        )
                    }
                    is INativeAuthCommandResult.Redirect -> {
                        NativeAuthErrorV2(
                            errorType = ErrorTypes.BROWSER_REQUIRED,
                            error = result.error,
                            errorMessage = result.errorDescription,
                            correlationId = result.correlationId,
                            scenario = scenario
                        )
                    }
                    is INativeAuthCommandResult.APIError -> {
                        NativeAuthErrorV2(
                            error = result.error,
                            errorMessage = result.errorDescription,
                            correlationId = result.correlationId,
                            scenario = scenario,
                            exception = result.exception
                        )
                    }
                }
            } catch (e: Exception) {
                NativeAuthErrorV2(
                    errorType = ErrorTypes.CLIENT_EXCEPTION,
                    errorMessage = "MSAL client exception occurred in resendCode.",
                    correlationId = correlationId,
                    scenario = scenario,
                    exception = e
                )
            }
        }
    }

    interface SignInCallback : Callback<NativeAuthResultV2>

    /**
     * Explicit app-invoked sign-in step following a completed password reset flow. This is the
     * only method that triggers the token exchange and cache persistence for the reset flow; the
     * reset-password steps above never invoke it automatically.
     */
    fun signIn(callback: SignInCallback) {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.signIn(callback: SignInCallback)"
        )
        NativeAuthPublicClientApplication.pcaScope.launch {
            try {
                callback.onResult(signIn())
            } catch (e: MsalException) {
                Logger.error(TAG, "Exception thrown in signIn", e)
                callback.onError(e)
            }
        }
    }

    /**
     * Explicit app-invoked sign-in step following a completed password reset flow. This is the
     * only method that triggers the token exchange and cache persistence for the reset flow; the
     * reset-password steps above never invoke it automatically.
     */
    suspend fun signIn(): NativeAuthResultV2 {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.signIn()"
        )
        val state = continuationState ?: return notImplemented()
        return withContext(Dispatchers.IO) {
            try {
                val parameters = CommandParametersAdapter.createNativeAuthV2SignInAfterResetPasswordCommandParameters(
                    config,
                    config.oAuth2TokenCache,
                    state
                )
                val command = NativeAuthV2SignInAfterResetPasswordCommand(
                    parameters,
                    NativeAuthV2FlowController(),
                    PublicApiId.NATIVE_AUTH_V2_SIGN_IN_AFTER_RESET_PASSWORD
                )
                ensureActive()
                val rawCommandResult = CommandDispatcher.submitSilentReturningFuture(command).get()
                ensureActive()
                when (val result = rawCommandResult.checkAndWrapCommandResultType<NativeAuthV2SignInAfterResetPasswordCommandResult>()) {
                    is NativeAuthV2CommandResult.Complete -> {
                        mapCompleteResult(result)
                    }
                    is INativeAuthCommandResult.Redirect -> {
                        NativeAuthErrorV2(
                            errorType = ErrorTypes.BROWSER_REQUIRED,
                            error = result.error,
                            errorMessage = result.errorDescription,
                            correlationId = result.correlationId,
                            scenario = scenario
                        )
                    }
                    is INativeAuthCommandResult.APIError -> {
                        NativeAuthErrorV2(
                            error = result.error,
                            errorMessage = result.errorDescription,
                            correlationId = result.correlationId,
                            scenario = scenario,
                            exception = result.exception
                        )
                    }
                }
            } catch (e: Exception) {
                NativeAuthErrorV2(
                    errorType = ErrorTypes.CLIENT_EXCEPTION,
                    errorMessage = "MSAL client exception occurred in signIn.",
                    correlationId = correlationId,
                    scenario = scenario,
                    exception = e
                )
            }
        }
    }

    private fun mapCompleteResult(result: NativeAuthV2CommandResult.Complete): NativeAuthResultV2 {
        val localAuthResult = result.authenticationResult
        return if (localAuthResult != null) {
            val authenticationResult = AuthenticationResultAdapter.adapt(localAuthResult)
            NativeAuthResultV2.Complete(
                resultValue = AccountState.createFromAuthenticationResult(
                    authenticationResult = authenticationResult,
                    correlationId = result.correlationId,
                    config = config
                )
            )
        } else {
            Logger.warn(TAG, result.correlationId, "V2 Complete result has no inline sign-in token; returning API error.")
            NativeAuthErrorV2(
                errorMessage = "Password reset completed but no sign-in result was returned.",
                correlationId = result.correlationId,
                scenario = scenario
            )
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        if (continuationState != null) {
            parcel.writeByte(1)
            parcel.writeSerializable(continuationState)
            parcel.writeSerializable(config)
        } else {
            parcel.writeByte(0)
            parcel.writeString(continuationToken)
            parcel.writeString(correlationId)
            parcel.writeString(scenario.name)
            parcel.writeSerializable(config)
        }
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<NativeAuthFlowStateV2> {
        override fun createFromParcel(parcel: Parcel): NativeAuthFlowStateV2 {
            return if (parcel.readByte().toInt() == 1) {
                val continuationState = parcel.serializable<NativeAuthV2ContinuationState>()!!
                val config = parcel.serializable<NativeAuthPublicClientApplicationConfiguration>()!!
                NativeAuthFlowStateV2(continuationState = continuationState, config = config)
            } else {
                NativeAuthFlowStateV2(
                    continuationToken = parcel.readString() ?: "",
                    correlationId = parcel.readString() ?: "UNSET",
                    scenario = NativeAuthFlowScenarioV2.valueOf(
                        parcel.readString() ?: NativeAuthFlowScenarioV2.UNKNOWN.name
                    ),
                    config = parcel.serializable()!!
                )
            }
        }

        override fun newArray(size: Int): Array<NativeAuthFlowStateV2?> = arrayOfNulls(size)
    }
}
