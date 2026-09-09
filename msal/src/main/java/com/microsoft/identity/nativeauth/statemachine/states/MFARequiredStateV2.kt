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
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2SelectMFAMethodCommandResult
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthConstants
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationState
import com.microsoft.identity.common.java.nativeauth.util.checkAndWrapCommandResultType
import com.microsoft.identity.common.nativeauth.internal.commands.NativeAuthV2SelectMFAMethodCommand
import com.microsoft.identity.common.nativeauth.internal.controllers.v2.NativeAuthV2FlowController
import com.microsoft.identity.nativeauth.AuthMethod
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplication
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplicationConfiguration
import com.microsoft.identity.nativeauth.statemachine.NativeAuthFlowScenarioV2
import com.microsoft.identity.nativeauth.statemachine.errors.ErrorTypes
import com.microsoft.identity.nativeauth.statemachine.errors.MFARequestChallengeErrorV2
import com.microsoft.identity.nativeauth.statemachine.errors.NativeAuthErrorV2
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
 * State that requires the user to select an authentication method for multi-factor authentication.
 *
 * No challenge is sent until the app selects a method explicitly. [authMethods] is exactly the set
 * the server offered for this step; selecting anything else fails without issuing a request. This
 * increment supports email one-time codes only, so any other channel returns a not-implemented
 * error ([com.microsoft.identity.nativeauth.statemachine.errors.NativeAuthErrorV2.isNotImplemented])
 * rather than following the wrong link.
 */
class MFARequiredStateV2 internal constructor(
    continuationToken: String?,
    correlationId: String,
    scenario: NativeAuthFlowScenarioV2,
    config: NativeAuthPublicClientApplicationConfiguration,
    continuationState: NativeAuthV2ContinuationState? = null,

    /**
     * The authentication methods the server offered for this multi-factor step.
     */
    authMethods: List<AuthMethod> = emptyList()
) : NativeAuthBaseStateV2(continuationToken, correlationId, scenario, config, continuationState) {
    private val TAG: String = MFARequiredStateV2::class.java.simpleName
    val authMethods: List<AuthMethod> = Collections.unmodifiableList(ArrayList(authMethods))

    internal constructor(
        continuationState: NativeAuthV2ContinuationState,
        authMethods: List<AuthMethod>,
        scenario: NativeAuthFlowScenarioV2,
        config: NativeAuthPublicClientApplicationConfiguration
    ) : this(
        continuationToken = null,
        correlationId = continuationState.correlationId,
        scenario = scenario,
        config = config,
        continuationState = continuationState,
        authMethods = authMethods
    )

    private constructor(parcel: Parcel) : this(
        continuationToken = parcel.readString(),
        correlationId = parcel.readString() ?: "UNSET",
        scenario = NativeAuthFlowScenarioV2.valueOf(parcel.readString() ?: NativeAuthFlowScenarioV2.UNKNOWN.name),
        config = parcel.serializable<NativeAuthPublicClientApplicationConfiguration>() as NativeAuthPublicClientApplicationConfiguration,
        continuationState = parcel.serializable<NativeAuthV2ContinuationState>(),
        authMethods = parcel.createTypedArrayList(AuthMethod.CREATOR) ?: emptyList()
    )

    /**
     * Writes the base fields first, then this state's own [authMethods], so the read order in the
     * `Parcel` constructor above stays symmetric. The base fields themselves are still written
     * only by the base implementation.
     */
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        super.writeToParcel(parcel, flags)
        parcel.writeTypedList(authMethods)
    }

    interface SelectAuthMethodCallback : Callback<NativeAuthResultV2>

    /**
     * Requests a challenge on the selected authentication method; callback variant.
     *
     * @param method one of the methods listed in [authMethods].
     * @param verificationContact unused by this flow; the server already knows the contact bound to
     * [method]. Retained for signature compatibility with the wider Native Auth V2 surface.
     * @param callback [SelectAuthMethodCallback] to receive the result.
     */
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

    /**
     * Requests a challenge on the selected authentication method; Kotlin coroutines variant.
     *
     * @param method one of the methods listed in [authMethods].
     * @param verificationContact unused by this flow; see the callback variant.
     * @return [NativeAuthResultV2] see detailed possible return state under the object.
     */
    suspend fun selectAuthMethod(method: AuthMethod, verificationContact: String? = null): NativeAuthResultV2 {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.selectAuthMethod(method: AuthMethod, verificationContact: String?)"
        )
        val state = continuationState ?: return invalidState()

        val offeredMethod = authMethods.firstOrNull { it.id == method.id }
            ?: return MFARequestChallengeErrorV2(
                errorType = ErrorTypes.INVALID_STATE,
                errorMessage = "The selected authentication method is not one of the methods the server offered.",
                correlationId = correlationId,
                scenario = scenario
            )

        if (!offeredMethod.challengeChannel.equals(NativeAuthConstants.ChallengeChannel.EMAIL, ignoreCase = true)) {
            // Reported as not-implemented rather than a bare error so the app can tell "this SDK
            // increment only supports email one-time codes" apart from an unspecified server error,
            // which is what an untyped error would be indistinguishable from.
            return MFARequestChallengeErrorV2(
                errorType = ErrorTypes.NOT_IMPLEMENTED,
                errorMessage = "Only email authentication methods are supported for multi-factor authentication.",
                correlationId = correlationId,
                scenario = scenario
            )
        }

        return withContext(Dispatchers.IO) {
            try {
                val parameters = CommandParametersAdapter.createNativeAuthV2SelectMFAMethodCommandParameters(
                    config,
                    config.oAuth2TokenCache,
                    offeredMethod.id,
                    state
                )
                val command = NativeAuthV2SelectMFAMethodCommand(
                    parameters,
                    NativeAuthV2FlowController(),
                    PublicApiId.NATIVE_AUTH_V2_SIGN_IN_SELECT_MFA_METHOD
                )
                ensureActive()
                val rawCommandResult = CommandDispatcher.submitSilentReturningFuture(command).getCancellable()
                ensureActive()
                when (val result = rawCommandResult.checkAndWrapCommandResultType<NativeAuthV2SelectMFAMethodCommandResult>()) {
                    is NativeAuthV2CommandResult.MFAVerificationRequired -> {
                        NativeAuthResultV2.MFAVerificationRequired(
                            nextState = MFAVerificationRequiredStateV2(
                                continuationState = result.continuationState,
                                scenario = scenario,
                                config = config
                            ),
                            scenario = scenario,
                            codeLength = result.codeLength,
                            sentTo = result.challengeTargetLabel,
                            channel = result.challengeChannel
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
                        MFARequestChallengeErrorV2(
                            errorType = ErrorTypes.BROWSER_REQUIRED,
                            error = result.error,
                            errorMessage = result.errorDescription,
                            correlationId = result.correlationId,
                            scenario = scenario
                        )
                    }
                    is INativeAuthCommandResult.APIError -> {
                        MFARequestChallengeErrorV2(
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
                Logger.error(TAG, correlationId, "Exception thrown in selectAuthMethod", e)
                MFARequestChallengeErrorV2(
                    errorType = ErrorTypes.CLIENT_EXCEPTION,
                    errorMessage = "MSAL client exception occurred in selectAuthMethod.",
                    correlationId = correlationId,
                    scenario = scenario,
                    exception = e
                )
            }
        }
    }

    companion object CREATOR : Parcelable.Creator<MFARequiredStateV2> {
        override fun createFromParcel(parcel: Parcel): MFARequiredStateV2 = MFARequiredStateV2(parcel)

        override fun newArray(size: Int): Array<MFARequiredStateV2?> = arrayOfNulls(size)
    }
}
