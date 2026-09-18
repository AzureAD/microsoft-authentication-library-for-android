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
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2SelectResetPasswordMethodCommandResult
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthConstants
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationState
import com.microsoft.identity.common.java.nativeauth.util.checkAndWrapCommandResultType
import com.microsoft.identity.common.nativeauth.internal.commands.NativeAuthV2SelectResetPasswordMethodCommand
import com.microsoft.identity.common.nativeauth.internal.controllers.v2.NativeAuthV2FlowController
import com.microsoft.identity.nativeauth.AuthMethod
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplication
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplicationConfiguration
import com.microsoft.identity.nativeauth.statemachine.NativeAuthFlowScenarioV2
import com.microsoft.identity.nativeauth.statemachine.errors.ErrorTypes
import com.microsoft.identity.nativeauth.statemachine.errors.NativeAuthErrorV2
import com.microsoft.identity.nativeauth.statemachine.errors.ResetPasswordErrorV2
import com.microsoft.identity.nativeauth.statemachine.results.NativeAuthResultV2
import com.microsoft.identity.nativeauth.utils.getCancellable
import com.microsoft.identity.nativeauth.utils.serializable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections

/**
 * State that requires the user to select an email or SMS verification method for password reset.
 *
 * No challenge is sent until the app explicitly selects one of [authMethods].
 */
class ResetPasswordMethodRequiredStateV2 internal constructor(
    continuationToken: String?,
    correlationId: String,
    scenario: NativeAuthFlowScenarioV2,
    config: NativeAuthPublicClientApplicationConfiguration,
    continuationState: NativeAuthV2ContinuationState? = null,
    authMethods: List<AuthMethod> = emptyList()
) : NativeAuthBaseStateV2(continuationToken, correlationId, scenario, config, continuationState) {
    private val TAG: String = ResetPasswordMethodRequiredStateV2::class.java.simpleName
    val authMethods: List<AuthMethod> = Collections.unmodifiableList(ArrayList(authMethods))

    internal constructor(
        continuationState: NativeAuthV2ContinuationState,
        authMethods: List<AuthMethod>,
        config: NativeAuthPublicClientApplicationConfiguration
    ) : this(
        continuationToken = null,
        correlationId = continuationState.correlationId,
        scenario = NativeAuthFlowScenarioV2.RESET_PASSWORD,
        config = config,
        continuationState = continuationState,
        authMethods = authMethods
    )

    private constructor(parcel: Parcel) : this(
        continuationToken = parcel.readString(),
        correlationId = parcel.readString() ?: "UNSET",
        scenario = NativeAuthFlowScenarioV2.valueOf(
            parcel.readString() ?: NativeAuthFlowScenarioV2.UNKNOWN.name
        ),
        config = parcel.serializable<NativeAuthPublicClientApplicationConfiguration>()
            as NativeAuthPublicClientApplicationConfiguration,
        continuationState = parcel.serializable<NativeAuthV2ContinuationState>(),
        authMethods = parcel.createTypedArrayList(AuthMethod.CREATOR) ?: emptyList()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        super.writeToParcel(parcel, flags)
        parcel.writeTypedList(authMethods)
    }

    interface SelectAuthMethodCallback : Callback<NativeAuthResultV2>

    /**
     * Sends a verification code using the selected method; callback variant.
     */
    fun selectAuthMethod(method: AuthMethod, callback: SelectAuthMethodCallback) {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.selectAuthMethod(method: AuthMethod, callback: SelectAuthMethodCallback)"
        )
        NativeAuthPublicClientApplication.pcaScope.launch {
            try {
                callback.onResult(selectAuthMethod(method))
            } catch (e: MsalException) {
                Logger.error(TAG, "Exception thrown in selectAuthMethod", e)
                callback.onError(e)
            }
        }
    }

    /**
     * Sends a verification code using the selected method; Kotlin coroutines variant.
     */
    suspend fun selectAuthMethod(method: AuthMethod): NativeAuthResultV2 {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.selectAuthMethod(method: AuthMethod)"
        )
        val state = continuationState ?: return invalidState()
        val offeredMethod = authMethods.firstOrNull { it.id == method.id }
            ?: return ResetPasswordErrorV2(
                errorType = ErrorTypes.INVALID_STATE,
                errorMessage = "The selected authentication method is not one of the methods the server offered.",
                correlationId = correlationId,
                scenario = scenario
            )

        if (!offeredMethod.challengeChannel.equals(
                NativeAuthConstants.ChallengeChannel.EMAIL,
                ignoreCase = true
            ) &&
            !offeredMethod.challengeChannel.equals(
                NativeAuthConstants.ChallengeChannel.SMS,
                ignoreCase = true
            )
        ) {
            return NativeAuthErrorV2(
                errorType = ErrorTypes.NOT_IMPLEMENTED,
                errorMessage = "Only email and SMS authentication methods are supported for password reset.",
                correlationId = correlationId,
                scenario = scenario
            )
        }

        return withContext(Dispatchers.IO) {
            try {
                val parameters =
                    CommandParametersAdapter.createNativeAuthV2SelectResetPasswordMethodCommandParameters(
                        config,
                        config.oAuth2TokenCache,
                        offeredMethod.id,
                        state
                    )
                val command = NativeAuthV2SelectResetPasswordMethodCommand(
                    parameters,
                    NativeAuthV2FlowController(),
                    PublicApiId.NATIVE_AUTH_V2_RESET_PASSWORD_SELECT_METHOD
                )
                ensureActive()
                val rawCommandResult =
                    CommandDispatcher.submitSilentReturningFuture(command).getCancellable()
                ensureActive()
                when (val result =
                    rawCommandResult.checkAndWrapCommandResultType<NativeAuthV2SelectResetPasswordMethodCommandResult>()) {
                    is NativeAuthV2CommandResult.CodeRequired -> NativeAuthResultV2.CodeRequired(
                        nextState = CodeRequiredStateV2(
                            continuationState = result.continuationState,
                            scenario = scenario,
                            config = config
                        ),
                        scenario = scenario,
                        codeLength = result.codeLength,
                        sentTo = result.challengeTargetLabel,
                        channel = result.challengeChannel
                    )
                    is NativeAuthV2CommandResult.NotImplemented -> NativeAuthErrorV2(
                        errorType = ErrorTypes.NOT_IMPLEMENTED,
                        error = result.error,
                        errorMessage = result.errorDescription,
                        correlationId = result.correlationId,
                        scenario = scenario
                    )
                    is INativeAuthCommandResult.Redirect -> ResetPasswordErrorV2(
                        errorType = ErrorTypes.BROWSER_REQUIRED,
                        error = result.error,
                        errorMessage = result.errorDescription,
                        correlationId = result.correlationId,
                        scenario = scenario
                    )
                    is INativeAuthCommandResult.APIError -> ResetPasswordErrorV2(
                        error = result.error,
                        errorMessage = result.errorDescription,
                        correlationId = result.correlationId,
                        scenario = scenario,
                        errorCodes = result.errorCodes,
                        exception = result.exception
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.error(TAG, correlationId, "Exception thrown in selectAuthMethod", e)
                ResetPasswordErrorV2(
                    errorType = ErrorTypes.CLIENT_EXCEPTION,
                    errorMessage = "MSAL client exception occurred in selectAuthMethod.",
                    correlationId = correlationId,
                    scenario = scenario,
                    exception = e
                )
            }
        }
    }

    companion object CREATOR : Parcelable.Creator<ResetPasswordMethodRequiredStateV2> {
        override fun createFromParcel(parcel: Parcel): ResetPasswordMethodRequiredStateV2 =
            ResetPasswordMethodRequiredStateV2(parcel)

        override fun newArray(size: Int): Array<ResetPasswordMethodRequiredStateV2?> =
            arrayOfNulls(size)
    }
}
