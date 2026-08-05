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
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2SignInAfterResetPasswordCommandResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationState
import com.microsoft.identity.common.java.nativeauth.util.checkAndWrapCommandResultType
import com.microsoft.identity.common.nativeauth.internal.commands.NativeAuthV2SignInAfterResetPasswordCommand
import com.microsoft.identity.common.nativeauth.internal.controllers.v2.NativeAuthV2FlowController
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplication
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplicationConfiguration
import com.microsoft.identity.nativeauth.statemachine.errors.ErrorTypes
import com.microsoft.identity.nativeauth.statemachine.errors.NativeAuthErrorV2
import com.microsoft.identity.nativeauth.statemachine.errors.NativeAuthFlowScenarioV2
import com.microsoft.identity.nativeauth.statemachine.results.NativeAuthResultV2
import com.microsoft.identity.nativeauth.utils.serializable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * State reached once a password reset has completed server-side. The token exchange and cache
 * persistence are deferred until the app explicitly invokes [signIn].
 */
class SignInAfterResetPasswordStateV2 internal constructor(
    continuationToken: String,
    correlationId: String,
    scenario: NativeAuthFlowScenarioV2,
    config: NativeAuthPublicClientApplicationConfiguration,
    continuationState: NativeAuthV2ContinuationState? = null
) : NativeAuthBaseStateV2(continuationToken, correlationId, scenario, config, continuationState) {
    private val TAG: String = SignInAfterResetPasswordStateV2::class.java.simpleName

    internal constructor(
        continuationState: NativeAuthV2ContinuationState,
        scenario: NativeAuthFlowScenarioV2,
        config: NativeAuthPublicClientApplicationConfiguration
    ) : this(
        continuationToken = continuationState.correlationId,
        correlationId = continuationState.correlationId,
        scenario = scenario,
        config = config,
        continuationState = continuationState
    )

    private constructor(parcel: Parcel) : this(
        continuationToken = parcel.readString() ?: "",
        correlationId = parcel.readString() ?: "UNSET",
        scenario = NativeAuthFlowScenarioV2.valueOf(parcel.readString() ?: NativeAuthFlowScenarioV2.UNKNOWN.name),
        config = parcel.serializable<NativeAuthPublicClientApplicationConfiguration>() as NativeAuthPublicClientApplicationConfiguration,
        continuationState = parcel.serializable<NativeAuthV2ContinuationState>()
    )

    interface SignInCallback : Callback<NativeAuthResultV2>

    /**
     * Explicit app-invoked sign-in step following a completed password reset flow. This is the only
     * method that triggers the token exchange and cache persistence for the reset flow.
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
     * Explicit app-invoked sign-in step following a completed password reset flow. This is the only
     * method that triggers the token exchange and cache persistence for the reset flow.
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
                Logger.error(TAG, correlationId, "Exception thrown in signIn", e)
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

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        super.writeToParcel(parcel, flags)
        parcel.writeSerializable(continuationState)
    }

    companion object CREATOR : Parcelable.Creator<SignInAfterResetPasswordStateV2> {
        override fun createFromParcel(parcel: Parcel): SignInAfterResetPasswordStateV2 = SignInAfterResetPasswordStateV2(parcel)

        override fun newArray(size: Int): Array<SignInAfterResetPasswordStateV2?> = arrayOfNulls(size)
    }
}
