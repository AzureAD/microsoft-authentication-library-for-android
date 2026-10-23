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
import com.microsoft.identity.common.java.nativeauth.commands.parameters.NativeAuthV2ActivateCommandParameters
import com.microsoft.identity.common.java.nativeauth.controllers.results.INativeAuthCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2CommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2ResendCodeCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2StrongAuthCommandResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationState
import com.microsoft.identity.common.java.nativeauth.util.checkAndWrapCommandResultType
import com.microsoft.identity.common.nativeauth.internal.commands.NativeAuthV2ActivateCommand
import com.microsoft.identity.common.nativeauth.internal.commands.NativeAuthV2ResendCodeCommand
import com.microsoft.identity.common.nativeauth.internal.controllers.v2.NativeAuthV2FlowController
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplication
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplicationConfiguration
import com.microsoft.identity.nativeauth.statemachine.NativeAuthFlowScenarioV2
import com.microsoft.identity.nativeauth.statemachine.results.NativeAuthResultV2
import com.microsoft.identity.nativeauth.statemachine.errors.ErrorTypes
import com.microsoft.identity.nativeauth.statemachine.errors.NativeAuthErrorV2
import com.microsoft.identity.nativeauth.utils.getCancellable
import com.microsoft.identity.nativeauth.utils.serializable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * State that requires a strong-auth activation code. Invalid codes are retryable on this state.
 * Activation resumes the original sign-in, sign-up, or password-reset flow.
 * [resendChallenge] returns a fresh verification state whose token and links must be used for
 * subsequent calls. Resend is available only when the service supplies a resend link.
 */
class StrongAuthVerificationRequiredStateV2 internal constructor(
    correlationId: String,
    scenario: NativeAuthFlowScenarioV2,
    config: NativeAuthPublicClientApplicationConfiguration,
    continuationState: NativeAuthV2ContinuationState? = null,
    internal val selectedChannel: String? = null
) : NativeAuthBaseStateV2(correlationId, scenario, config, continuationState) {
    private val TAG: String = StrongAuthVerificationRequiredStateV2::class.java.simpleName

    private constructor(parcel: Parcel) : this(
        correlationId = parcel.readString() ?: "UNSET",
        scenario = NativeAuthFlowScenarioV2.valueOf(parcel.readString() ?: NativeAuthFlowScenarioV2.UNKNOWN.name),
        config = parcel.serializable<NativeAuthPublicClientApplicationConfiguration>() as NativeAuthPublicClientApplicationConfiguration,
        continuationState = parcel.serializable<NativeAuthV2ContinuationState>(),
        selectedChannel = parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        super.writeToParcel(parcel, flags)
        parcel.writeString(selectedChannel)
    }

    interface SubmitChallengeCallback : Callback<NativeAuthResultV2>

    /** Receives a refreshed activation state or an error from [resendChallenge]. */
    interface ResendChallengeCallback : Callback<NativeAuthResultV2>

    /** Submits the activation code unchanged and delivers the resumed flow through [callback]. */
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

    /** Submits [challenge] without client-side OTP format validation; wrong codes are retryable. */
    suspend fun submitChallenge(challenge: String): NativeAuthResultV2 {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.submitChallenge(challenge: String)"
        )
        val state = continuationState ?: return invalidState()
        return withContext(Dispatchers.IO) {
            try {
                val carrier = CommandParametersAdapter.createNativeAuthV2SignInAfterSignUpCommandParameters(
                    config, config.oAuth2TokenCache, state, null, null
                )
                val command = NativeAuthV2ActivateCommand(
                    NativeAuthV2ActivateCommandParameters(carrier, state, challenge),
                    NativeAuthV2FlowController(),
                    PublicApiId.NATIVE_AUTH_JIT_SUBMIT_CHALLENGE
                )
                ensureActive()
                val rawResult = CommandDispatcher.submitSilentReturningFuture(command).getCancellable()
                ensureActive()
                mapStrongAuthResult(
                    rawResult.checkAndWrapCommandResultType<NativeAuthV2StrongAuthCommandResult>(),
                    selectedChannel
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                NativeAuthErrorV2(
                    errorType = ErrorTypes.CLIENT_EXCEPTION,
                    errorMessage = "MSAL client exception occurred during strong-auth activation.",
                    correlationId = correlationId, scenario = scenario, exception = e
                )
            }
        }
    }

    /**
     * Requests another activation code and delivers the refreshed state through [callback].
     *
     * @param callback receives the result or a callback-level exception.
     */
    fun resendChallenge(callback: ResendChallengeCallback) {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.resendChallenge(callback: ResendChallengeCallback)"
        )
        NativeAuthPublicClientApplication.pcaScope.launch {
            try {
                callback.onResult(resendChallenge())
            } catch (e: MsalException) {
                Logger.error(TAG, "Exception thrown in resendChallenge", e)
                callback.onError(e)
            }
        }
    }

    /**
     * Requests another activation code using the service's current resend link.
     *
     * @return a fresh [NativeAuthResultV2.StrongAuthVerificationRequired] on success. Its
     * code length is positive and its sent-to hint is empty when absent from the response.
     * Use its next state for subsequent submit/resend calls. If the service returns to method
     * selection, returns [NativeAuthResultV2.StrongAuthRegistrationRequired] with the current
     * methods and continuation state. A missing resend link produces
     * a [NativeAuthErrorV2] without making a network request.
     */
    suspend fun resendChallenge(): NativeAuthResultV2 {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.resendChallenge()"
        )
        val state = continuationState ?: return invalidState()
        return withContext(Dispatchers.IO) {
            try {
                val parameters = CommandParametersAdapter.createNativeAuthV2ResendCodeCommandParameters(
                    config, config.oAuth2TokenCache, state
                )
                val command = NativeAuthV2ResendCodeCommand(
                    parameters, NativeAuthV2FlowController(), PublicApiId.NATIVE_AUTH_JIT_RESEND_CHALLENGE
                )
                ensureActive()
                val rawResult = CommandDispatcher.submitSilentReturningFuture(command).getCancellable()
                ensureActive()
                when (val result = rawResult.checkAndWrapCommandResultType<NativeAuthV2ResendCodeCommandResult>()) {
                    is NativeAuthV2CommandResult.StrongAuthRegistrationRequired ->
                        result.toRegistrationResult(scenario, config)
                    is NativeAuthV2CommandResult.StrongAuthVerificationRequired ->
                        mapStrongAuthResult(result, selectedChannel)
                    is INativeAuthCommandResult.Redirect -> mapStrongAuthResult(result)
                    is INativeAuthCommandResult.APIError -> mapStrongAuthResult(result)
                    else -> {
                        Logger.warnWithObject(TAG, result.correlationId, "JIT resend received unsupported result: ", result)
                        NativeAuthErrorV2(
                            errorType = ErrorTypes.INVALID_STATE,
                            errorMessage = "Strong-auth resend returned an unsupported result.",
                            correlationId = result.correlationId, scenario = scenario
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.error(TAG, correlationId, "Exception thrown in resendChallenge", e)
                NativeAuthErrorV2(
                    errorType = ErrorTypes.CLIENT_EXCEPTION,
                    errorMessage = "MSAL client exception occurred during strong-auth resend.",
                    correlationId = correlationId, scenario = scenario, exception = e
                )
            }
        }
    }

    companion object CREATOR : Parcelable.Creator<StrongAuthVerificationRequiredStateV2> {
        override fun createFromParcel(parcel: Parcel): StrongAuthVerificationRequiredStateV2 = StrongAuthVerificationRequiredStateV2(parcel)

        override fun newArray(size: Int): Array<StrongAuthVerificationRequiredStateV2?> = arrayOfNulls(size)
    }
}
