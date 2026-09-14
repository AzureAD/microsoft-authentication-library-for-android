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
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.client.internal.CommandParametersAdapter
import com.microsoft.identity.common.java.controllers.CommandDispatcher
import com.microsoft.identity.common.java.eststelemetry.PublicApiId
import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.nativeauth.controllers.results.INativeAuthCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2CommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2SubmitPasswordCommandResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationState
import com.microsoft.identity.common.java.nativeauth.util.checkAndWrapCommandResultType
import com.microsoft.identity.common.java.util.StringUtil
import com.microsoft.identity.common.nativeauth.internal.commands.NativeAuthV2SubmitPasswordCommand
import com.microsoft.identity.common.nativeauth.internal.controllers.v2.NativeAuthV2FlowController
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplication
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplicationConfiguration
import com.microsoft.identity.nativeauth.statemachine.NativeAuthFlowScenarioV2
import com.microsoft.identity.nativeauth.statemachine.errors.ErrorTypes
import com.microsoft.identity.nativeauth.statemachine.errors.NativeAuthErrorV2
import com.microsoft.identity.nativeauth.statemachine.errors.SubmitPasswordErrorV2
import com.microsoft.identity.nativeauth.statemachine.results.NativeAuthResultV2
import com.microsoft.identity.nativeauth.toListOfV2AuthMethods
import com.microsoft.identity.nativeauth.utils.getCancellable
import com.microsoft.identity.nativeauth.utils.launchOwningPasswordSnapshot
import com.microsoft.identity.nativeauth.utils.serializable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * State that requires the user to submit a password.
 *
 * A password rejected here is a [SubmitPasswordErrorV2] with
 * [SubmitPasswordErrorV2.isInvalidPassword], not an invalid-credentials error: the account itself
 * was already accepted at the sign-in entry point and only the deferred password was wrong. The
 * caller retries on this same state instance; no error result carries a next state.
 */
class PasswordRequiredStateV2 internal constructor(
    continuationToken: String?,
    correlationId: String,
    scenario: NativeAuthFlowScenarioV2,
    config: NativeAuthPublicClientApplicationConfiguration,
    continuationState: NativeAuthV2ContinuationState? = null
) : NativeAuthBaseStateV2(continuationToken, correlationId, scenario, config, continuationState) {
    private val TAG: String = PasswordRequiredStateV2::class.java.simpleName

    internal constructor(
        continuationState: NativeAuthV2ContinuationState,
        scenario: NativeAuthFlowScenarioV2,
        config: NativeAuthPublicClientApplicationConfiguration
    ) : this(
        continuationToken = null,
        correlationId = continuationState.correlationId,
        scenario = scenario,
        config = config,
        continuationState = continuationState
    )

    private constructor(parcel: Parcel) : this(
        continuationToken = parcel.readString(),
        correlationId = parcel.readString() ?: "UNSET",
        scenario = NativeAuthFlowScenarioV2.valueOf(parcel.readString() ?: NativeAuthFlowScenarioV2.UNKNOWN.name),
        config = parcel.serializable<NativeAuthPublicClientApplicationConfiguration>() as NativeAuthPublicClientApplicationConfiguration,
        continuationState = parcel.serializable<NativeAuthV2ContinuationState>()
    )

    interface SubmitPasswordCallback : Callback<NativeAuthResultV2>

    /**
     * Submits the password; callback variant.
     *
     * @param password the password to submit. The caller's buffer is copied, and only the copy is
     * cleared once the request completes.
     * @param callback [SubmitPasswordCallback] to receive the result.
     */
    fun submitPassword(password: CharArray, callback: SubmitPasswordCallback) {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.submitPassword(password: CharArray, callback: SubmitPasswordCallback)"
        )
        val passwordSnapshot = password.copyOf()
        NativeAuthPublicClientApplication.pcaScope.launchOwningPasswordSnapshot(passwordSnapshot) {
            try {
                callback.onResult(submitPassword(passwordSnapshot))
            } catch (e: MsalException) {
                Logger.error(TAG, "Exception thrown in submitPassword", e)
                callback.onError(e)
            }
        }
    }

    /**
     * Submits the password; Kotlin coroutines variant.
     *
     * @param password the password to submit. The caller's buffer is copied, and only the copy is
     * cleared once the request completes.
     * @return [NativeAuthResultV2] see detailed possible return state under the object.
     */
    suspend fun submitPassword(password: CharArray): NativeAuthResultV2 {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.submitPassword(password: CharArray)"
        )
        val state = continuationState ?: return invalidState()
        if (password.isEmpty()) {
            return SubmitPasswordErrorV2(
                errorType = ErrorTypes.INVALID_PASSWORD,
                errorMessage = "Password cannot be empty.",
                correlationId = correlationId,
                scenario = scenario
            )
        }
        return withContext(Dispatchers.IO) {
            // Copied so the caller keeps ownership of its own buffer, while this copy is cleared
            // below on every exit path, including cancellation.
            val passwordCopy = password.copyOf()
            try {
                val parameters = CommandParametersAdapter.createNativeAuthV2SubmitPasswordCommandParameters(
                    config,
                    config.oAuth2TokenCache,
                    passwordCopy,
                    state
                )
                val command = NativeAuthV2SubmitPasswordCommand(
                    parameters,
                    NativeAuthV2FlowController(),
                    PublicApiId.NATIVE_AUTH_V2_SIGN_IN_SUBMIT_PASSWORD
                )
                ensureActive()
                val rawCommandResult = CommandDispatcher.submitSilentReturningFuture(command).getCancellable()
                ensureActive()
                when (val result = rawCommandResult.checkAndWrapCommandResultType<NativeAuthV2SubmitPasswordCommandResult>()) {
                    is NativeAuthV2CommandResult.Complete -> {
                        mapCompleteResult(result)
                    }
                    is NativeAuthV2CommandResult.MFARequired -> {
                        val authMethods = result.authMethods.toListOfV2AuthMethods()
                        NativeAuthResultV2.MFARequired(
                            nextState = MFARequiredStateV2(
                                continuationState = result.continuationState,
                                authMethods = authMethods,
                                scenario = scenario,
                                config = config
                            ),
                            scenario = scenario
                        )
                    }
                    is NativeAuthV2CommandResult.IncorrectPassword -> {
                        SubmitPasswordErrorV2(
                            errorType = ErrorTypes.INVALID_PASSWORD,
                            error = result.error,
                            errorMessage = result.errorDescription,
                            correlationId = result.correlationId,
                            scenario = scenario,
                            errorCodes = result.errorCodes
                        )
                    }
                    is NativeAuthV2CommandResult.NotImplemented -> {
                        NativeAuthErrorV2(
                            errorType = ErrorTypes.NOT_IMPLEMENTED,
                            error = result.error,
                            errorMessage = result.errorDescription,
                            correlationId = result.correlationId,
                            scenario = scenario
                        )
                    }
                    is INativeAuthCommandResult.Redirect -> {
                        SubmitPasswordErrorV2(
                            errorType = ErrorTypes.BROWSER_REQUIRED,
                            error = result.error,
                            errorMessage = result.errorDescription,
                            correlationId = result.correlationId,
                            scenario = scenario
                        )
                    }
                    is INativeAuthCommandResult.APIError -> {
                        SubmitPasswordErrorV2(
                            error = result.error,
                            errorMessage = result.errorDescription,
                            correlationId = result.correlationId,
                            scenario = scenario,
                            errorCodes = result.errorCodes,
                            exception = result.exception
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.error(TAG, correlationId, "Exception thrown in submitPassword", e)
                SubmitPasswordErrorV2(
                    errorType = ErrorTypes.CLIENT_EXCEPTION,
                    errorMessage = "MSAL client exception occurred in submitPassword.",
                    correlationId = correlationId,
                    scenario = scenario,
                    exception = e
                )
            } finally {
                StringUtil.overwriteWithNull(passwordCopy)
            }
        }
    }

    companion object CREATOR : Parcelable.Creator<PasswordRequiredStateV2> {
        override fun createFromParcel(parcel: Parcel): PasswordRequiredStateV2 = PasswordRequiredStateV2(parcel)

        override fun newArray(size: Int): Array<PasswordRequiredStateV2?> = arrayOfNulls(size)
    }
}
