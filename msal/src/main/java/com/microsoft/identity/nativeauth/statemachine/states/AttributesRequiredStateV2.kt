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
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2SubmitAttributesCommandResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationState
import com.microsoft.identity.common.java.nativeauth.util.checkAndWrapCommandResultType
import com.microsoft.identity.common.nativeauth.internal.commands.NativeAuthV2SubmitAttributesCommand
import com.microsoft.identity.common.nativeauth.internal.controllers.v2.NativeAuthV2FlowController
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplication
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplicationConfiguration
import com.microsoft.identity.nativeauth.UserAttributes
import com.microsoft.identity.nativeauth.statemachine.NativeAuthFlowScenarioV2
import com.microsoft.identity.nativeauth.statemachine.errors.ErrorTypes
import com.microsoft.identity.nativeauth.statemachine.errors.NativeAuthErrorV2
import com.microsoft.identity.nativeauth.statemachine.errors.SignUpErrorTypes
import com.microsoft.identity.nativeauth.statemachine.errors.SignUpErrorV2
import com.microsoft.identity.nativeauth.statemachine.errors.SubmitAttributesErrorV2
import com.microsoft.identity.nativeauth.statemachine.results.NativeAuthResultV2
import com.microsoft.identity.nativeauth.toListOfRequiredUserAttributeV2
import com.microsoft.identity.nativeauth.toMap
import com.microsoft.identity.nativeauth.utils.getCancellable
import com.microsoft.identity.nativeauth.utils.serializable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * State that requires the user to submit account attributes.
 */
class AttributesRequiredStateV2 internal constructor(
    continuationToken: String?,
    correlationId: String,
    scenario: NativeAuthFlowScenarioV2,
    config: NativeAuthPublicClientApplicationConfiguration,
    continuationState: NativeAuthV2ContinuationState? = null
) : NativeAuthBaseStateV2(continuationToken, correlationId, scenario, config, continuationState) {
    private val TAG: String = AttributesRequiredStateV2::class.java.simpleName

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
        return submitAttributesInternal(scenario, config, continuationState, correlationId, attributes)
    }

    companion object CREATOR : Parcelable.Creator<AttributesRequiredStateV2> {
        override fun createFromParcel(parcel: Parcel): AttributesRequiredStateV2 = AttributesRequiredStateV2(parcel)

        override fun newArray(size: Int): Array<AttributesRequiredStateV2?> = arrayOfNulls(size)
    }
}

/**
 * Shared implementation of the submit-attributes step used by [AttributesRequiredStateV2] and
 * [AttributesInvalidStateV2]. Submits the app-collected attributes against [continuationState] and
 * maps the server response to the corresponding [NativeAuthResultV2].
 */
internal suspend fun submitAttributesInternal(
    scenario: NativeAuthFlowScenarioV2,
    config: NativeAuthPublicClientApplicationConfiguration,
    continuationState: NativeAuthV2ContinuationState?,
    correlationId: String,
    attributes: UserAttributes
): NativeAuthResultV2 {
    val TAG = "AttributesStateV2.submitAttributes"
    val state = continuationState ?: return NativeAuthErrorV2(
        errorType = ErrorTypes.INVALID_STATE,
        errorMessage = "The continuation state is unavailable. Restart the flow.",
        correlationId = correlationId,
        scenario = scenario
    )
    return withContext(Dispatchers.IO) {
        try {
            val parameters = CommandParametersAdapter.createNativeAuthV2SubmitAttributesCommandParameters(
                config,
                config.oAuth2TokenCache,
                attributes.toMap(),
                state
            )
            val command = NativeAuthV2SubmitAttributesCommand(
                parameters,
                NativeAuthV2FlowController(),
                PublicApiId.NATIVE_AUTH_V2_SIGN_UP_SUBMIT_ATTRIBUTES
            )
            ensureActive()
            val rawCommandResult = CommandDispatcher.submitSilentReturningFuture(command).getCancellable()
            ensureActive()
            when (val result = rawCommandResult.checkAndWrapCommandResultType<NativeAuthV2SubmitAttributesCommandResult>()) {
                is NativeAuthV2CommandResult.CodeRequired -> {
                    NativeAuthResultV2.CodeRequired(
                        nextState = CodeRequiredStateV2(result.continuationState, scenario, config),
                        scenario = scenario,
                        codeLength = result.codeLength,
                        sentTo = result.challengeTargetLabel,
                        channel = result.challengeChannel
                    )
                }
                is NativeAuthV2CommandResult.PasswordRequired -> {
                    NativeAuthResultV2.PasswordRequired(
                        nextState = PasswordRequiredStateV2(result.continuationState, scenario, config),
                        scenario = scenario
                    )
                }
                is NativeAuthV2CommandResult.AttributesRequired -> {
                    NativeAuthResultV2.AttributesRequired(
                        nextState = AttributesRequiredStateV2(result.continuationState, scenario, config),
                        scenario = scenario,
                        requiredAttributes = result.requiredAttributes.toListOfRequiredUserAttributeV2()
                    )
                }
                is NativeAuthV2CommandResult.AttributesInvalid -> {
                    NativeAuthResultV2.AttributesInvalid(
                        nextState = AttributesInvalidStateV2(result.continuationState, scenario, config),
                        scenario = scenario,
                        invalidAttributes = result.invalidAttributes
                    )
                }
                is NativeAuthV2CommandResult.SignInAfterSignUpRequired -> {
                    NativeAuthResultV2.SignInAfterSignUpRequired(
                        nextState = SignInAfterSignUpStateV2(result.continuationState, scenario, config),
                        scenario = scenario
                    )
                }
                is NativeAuthV2CommandResult.UserAlreadyExists -> {
                    SignUpErrorV2(
                        errorType = SignUpErrorTypes.USER_ALREADY_EXISTS,
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
                    SubmitAttributesErrorV2(
                        errorType = ErrorTypes.BROWSER_REQUIRED,
                        error = result.error,
                        errorMessage = result.errorDescription,
                        correlationId = result.correlationId,
                        scenario = scenario
                    )
                }
                is INativeAuthCommandResult.APIError -> {
                    SubmitAttributesErrorV2(
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
            Logger.error(TAG, correlationId, "Exception thrown in submitAttributes", e)
            SubmitAttributesErrorV2(
                errorType = ErrorTypes.CLIENT_EXCEPTION,
                errorMessage = "MSAL client exception occurred in submitAttributes.",
                correlationId = correlationId,
                scenario = scenario,
                exception = e
            )
        }
    }
}
