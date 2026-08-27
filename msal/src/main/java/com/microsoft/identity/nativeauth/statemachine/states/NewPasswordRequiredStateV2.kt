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
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2SubmitNewPasswordCommandResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationState
import com.microsoft.identity.common.java.nativeauth.util.checkAndWrapCommandResultType
import com.microsoft.identity.common.java.util.StringUtil
import com.microsoft.identity.common.nativeauth.internal.commands.NativeAuthV2SubmitNewPasswordCommand
import com.microsoft.identity.common.nativeauth.internal.controllers.v2.NativeAuthV2FlowController
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplication
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplicationConfiguration
import com.microsoft.identity.nativeauth.statemachine.errors.ErrorTypes
import com.microsoft.identity.nativeauth.statemachine.errors.NativeAuthErrorV2
import com.microsoft.identity.nativeauth.statemachine.errors.ResetPasswordErrorTypes
import com.microsoft.identity.nativeauth.statemachine.errors.SubmitNewPasswordErrorV2
import com.microsoft.identity.nativeauth.statemachine.NativeAuthFlowScenarioV2
import com.microsoft.identity.nativeauth.statemachine.results.NativeAuthResultV2
import com.microsoft.identity.nativeauth.utils.getCancellable
import com.microsoft.identity.nativeauth.utils.serializable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * State that requires the user to submit a new password.
 */
class NewPasswordRequiredStateV2 internal constructor(
    continuationToken: String?,
    correlationId: String,
    scenario: NativeAuthFlowScenarioV2,
    config: NativeAuthPublicClientApplicationConfiguration,
    continuationState: NativeAuthV2ContinuationState? = null
) : NativeAuthBaseStateV2(continuationToken, correlationId, scenario, config, continuationState) {
    private val TAG: String = NewPasswordRequiredStateV2::class.java.simpleName

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

    interface SubmitNewPasswordCallback : Callback<NativeAuthResultV2>

    /**
     * Submits a new password to the server; callback variant.
     *
     * @param password The new password to submit.
     * @param callback [com.microsoft.identity.nativeauth.statemachine.states.NewPasswordRequiredStateV2.SubmitNewPasswordCallback] to receive the result on.
     */
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

    /**
     * Submits a new password to the server; Kotlin coroutines variant.
     *
     * @param password The new password to submit.
     * @return The results of the submit new password action.
     */
    suspend fun submitNewPassword(password: CharArray): NativeAuthResultV2 {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.submitNewPassword(password: CharArray)"
        )
        try {
            return submitNewPasswordInternal(password)
        } finally {
            StringUtil.overwriteWithNull(password)
        }
    }

    private suspend fun submitNewPasswordInternal(password: CharArray): NativeAuthResultV2 {
        val state = continuationState ?: return notImplemented()
        if (password.isEmpty()) {
            return SubmitNewPasswordErrorV2(
                errorType = ErrorTypes.INVALID_PASSWORD,
                errorMessage = "Password cannot be empty.",
                correlationId = correlationId,
                scenario = scenario
            )
        }
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
                val rawCommandResult = CommandDispatcher.submitSilentReturningFuture(command).getCancellable()
                ensureActive()
                when (val result = rawCommandResult.checkAndWrapCommandResultType<NativeAuthV2SubmitNewPasswordCommandResult>()) {
                    is NativeAuthV2CommandResult.Complete -> {
                        mapCompleteResult(result)
                    }
                    is NativeAuthV2CommandResult.SignInAfterResetPasswordRequired -> {
                        NativeAuthResultV2.SignInAfterResetPasswordRequired(
                            nextState = SignInAfterResetPasswordStateV2(result.continuationState, scenario, config),
                            scenario = scenario
                        )
                    }
                    is NativeAuthV2CommandResult.PasswordNotAccepted -> {
                        SubmitNewPasswordErrorV2(
                            errorType = ErrorTypes.INVALID_PASSWORD,
                            error = result.error,
                            errorMessage = result.errorDescription,
                            correlationId = result.correlationId,
                            scenario = scenario,
                            errorCodes = result.errorCodes,
                            subError = result.subError
                        )
                    }
                    is NativeAuthV2CommandResult.PasswordResetFailed -> {
                        SubmitNewPasswordErrorV2(
                            errorType = ResetPasswordErrorTypes.PASSWORD_RESET_FAILED,
                            error = result.error,
                            errorMessage = result.errorDescription,
                            correlationId = result.correlationId,
                            scenario = scenario
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
                        SubmitNewPasswordErrorV2(
                            errorType = ErrorTypes.BROWSER_REQUIRED,
                            error = result.error,
                            errorMessage = result.errorDescription,
                            correlationId = result.correlationId,
                            scenario = scenario
                        )
                    }
                    is INativeAuthCommandResult.APIError -> {
                        SubmitNewPasswordErrorV2(
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
                Logger.error(TAG, correlationId, "Exception thrown in submitNewPassword", e)
                SubmitNewPasswordErrorV2(
                    errorType = ErrorTypes.CLIENT_EXCEPTION,
                    errorMessage = "MSAL client exception occurred in submitNewPassword.",
                    correlationId = correlationId,
                    scenario = scenario,
                    exception = e
                )
            }
        }
    }

    companion object CREATOR : Parcelable.Creator<NewPasswordRequiredStateV2> {
        override fun createFromParcel(parcel: Parcel): NewPasswordRequiredStateV2 = NewPasswordRequiredStateV2(parcel)

        override fun newArray(size: Int): Array<NewPasswordRequiredStateV2?> = arrayOfNulls(size)
    }
}
