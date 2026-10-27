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
import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2CommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.INativeAuthCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2StrongAuthCommandResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationState
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplicationConfiguration
import com.microsoft.identity.nativeauth.toListOfV2AuthMethods
import com.microsoft.identity.nativeauth.toListOfRequiredUserAttributeV2
import com.microsoft.identity.nativeauth.statemachine.errors.NativeAuthErrorV2
import com.microsoft.identity.nativeauth.statemachine.NativeAuthFlowScenarioV2
import com.microsoft.identity.nativeauth.statemachine.errors.ErrorTypes
import com.microsoft.identity.nativeauth.statemachine.errors.SignUpErrorTypes
import com.microsoft.identity.nativeauth.statemachine.errors.ResetPasswordErrorTypes
import com.microsoft.identity.nativeauth.statemachine.results.NativeAuthResultV2
import com.microsoft.identity.nativeauth.utils.serializable

/**
 * Base class for the Native Auth V2 states. Each concrete state exposes only the methods that are
 * valid at that point in the flow.
 *
 * @property correlationId Correlation ID taken from the previous API response and passed to the next request.
 * @property scenario Identifies which part of the Native Auth V2 surface this state belongs to.
 * @property config Configuration used by Native Auth.
 * @property continuationState Opaque DTO carrying mid-flow state; null in legacy/test paths and for states that don't drive a server call.
 */
abstract class NativeAuthBaseStateV2 internal constructor(
    internal val correlationId: String,
    internal val scenario: NativeAuthFlowScenarioV2,
    internal val config: NativeAuthPublicClientApplicationConfiguration,
    internal val continuationState: NativeAuthV2ContinuationState? = null
) : State, Parcelable {

    private val TAG: String = NativeAuthBaseStateV2::class.java.simpleName

    protected fun notImplemented(): NativeAuthResultV2 = NativeAuthErrorV2(
        errorType = ErrorTypes.NOT_IMPLEMENTED,
        errorMessage = "This is not implemented yet",
        correlationId = correlationId,
        scenario = scenario
    )

    protected fun invalidState(): NativeAuthResultV2 = NativeAuthErrorV2(
        errorType = ErrorTypes.INVALID_STATE,
        errorMessage = "The continuation state is unavailable. Restart the flow.",
        correlationId = correlationId,
        scenario = scenario
    )

    internal fun mapStrongAuthResult(
        result: NativeAuthV2StrongAuthCommandResult,
        selectedChannel: String? = null
    ): NativeAuthResultV2 = when (result) {
        is NativeAuthV2CommandResult.ExistingAccountCodeRequired -> result.toExistingAccountResult(config)
        is NativeAuthV2CommandResult.ExistingAccountPasswordRequired -> result.toExistingAccountResult(config)
        is NativeAuthV2CommandResult.StrongAuthRegistrationRequired ->
            result.toRegistrationResult(scenario, config)
        is NativeAuthV2CommandResult.StrongAuthVerificationRequired ->
            if (selectedChannel == null) invalidState() else NativeAuthResultV2.StrongAuthVerificationRequired(
                StrongAuthVerificationRequiredStateV2(
                    result.correlationId, scenario, config, result.continuationState, selectedChannel
                ), scenario, result.codeLength, result.challengeTargetLabel, selectedChannel
            )
        is NativeAuthV2CommandResult.Complete -> mapCompleteResult(result)
        is NativeAuthV2CommandResult.MFARequired -> NativeAuthResultV2.MFARequired(
            MFARequiredStateV2(result.continuationState, result.authMethods.toListOfV2AuthMethods(), scenario, config), scenario
        )
        is NativeAuthV2CommandResult.NewPasswordRequired -> NativeAuthResultV2.NewPasswordRequired(
            NewPasswordRequiredStateV2(result.continuationState, scenario, config), scenario
        )
        is NativeAuthV2CommandResult.SignInAfterResetPasswordRequired -> NativeAuthResultV2.SignInAfterResetPasswordRequired(
            SignInAfterResetPasswordStateV2(result.continuationState, scenario, config), scenario
        )
        is NativeAuthV2CommandResult.SignInAfterSignUpRequired -> NativeAuthResultV2.SignInAfterSignUpRequired(
            SignInAfterSignUpStateV2(result.continuationState, scenario, config), scenario
        )
        is NativeAuthV2CommandResult.PasswordRequired -> NativeAuthResultV2.PasswordRequired(
            PasswordRequiredStateV2(result.continuationState, scenario, config), scenario
        )
        is NativeAuthV2CommandResult.AttributesRequired -> NativeAuthResultV2.AttributesRequired(
            AttributesRequiredStateV2(result.continuationState, scenario, config), scenario,
            result.requiredAttributes.toListOfRequiredUserAttributeV2()
        )
        is NativeAuthV2CommandResult.AttributesInvalid -> NativeAuthResultV2.AttributesInvalid(
            AttributesInvalidStateV2(result.continuationState, scenario, config), scenario,
            result.invalidAttributes, NativeAuthErrorV2.fromAttributeError(result, scenario)
        )
        is NativeAuthV2CommandResult.CodeRequired -> NativeAuthResultV2.CodeRequired(
            CodeRequiredStateV2(result.continuationState, scenario, config), scenario,
            result.codeLength, result.challengeTargetLabel, result.challengeChannel
        )
        is NativeAuthV2CommandResult.IncorrectCode -> NativeAuthErrorV2(
            errorType = ErrorTypes.INVALID_CODE,
            error = result.error, errorMessage = result.errorDescription,
            correlationId = result.correlationId, scenario = scenario,
            errorCodes = result.errorCodes, subError = result.subError,
            traceId = result.traceId, timestamp = result.timestamp
        )
        is NativeAuthV2CommandResult.UserAlreadyExists -> NativeAuthErrorV2(
            errorType = SignUpErrorTypes.USER_ALREADY_EXISTS,
            error = result.error, errorMessage = result.errorDescription,
            correlationId = result.correlationId, scenario = scenario,
            errorCodes = result.errorCodes, subError = result.subError,
            attributeValidationDetails = result.attributeValidationDetails,
            traceId = result.traceId, timestamp = result.timestamp
        )
        is NativeAuthV2CommandResult.PasswordResetFailed -> NativeAuthErrorV2(
            errorType = ResetPasswordErrorTypes.PASSWORD_RESET_FAILED,
            error = result.error, errorMessage = result.errorDescription,
            correlationId = result.correlationId, scenario = scenario
        )
        is INativeAuthCommandResult.Redirect -> NativeAuthErrorV2(
            errorType = ErrorTypes.BROWSER_REQUIRED,
            error = result.error, errorMessage = result.errorDescription,
            correlationId = result.correlationId, scenario = scenario,
            errorCodes = result.errorCodes, traceId = result.traceId, timestamp = result.timestamp
        )
        is INativeAuthCommandResult.APIError -> NativeAuthErrorV2(
            error = result.error, errorMessage = result.errorDescription,
            correlationId = result.correlationId, scenario = scenario,
            errorCodes = result.errorCodes, subError = result.subError,
            exception = result.exception, attributeValidationDetails = result.attributeValidationDetails,
            traceId = result.traceId, timestamp = result.timestamp
        )
    }

    /**
     * Maps a completed V2 command result to a [NativeAuthResultV2.Complete] when an inline
     * sign-in result is present, otherwise to an API error carrying the flow [scenario].
     */
    protected fun mapCompleteResult(result: NativeAuthV2CommandResult.Complete): NativeAuthResultV2 {
        val localAuthResult = result.authenticationResult
        return if (localAuthResult != null) {
            NativeAuthResultV2.Complete(
                resultValue = AccountState.createFromAuthenticationResult(
                    authenticationResult = AuthenticationResultAdapter.adapt(localAuthResult),
                    correlationId = result.correlationId,
                    config = config
                ),
                scenario = scenario
            )
        } else {
            Logger.warn(TAG, result.correlationId, "V2 Complete result has no inline sign-in token; returning API error.")
            NativeAuthErrorV2(
                errorMessage = "Native Auth V2 flow completed but no authentication result was returned.",
                correlationId = result.correlationId,
                scenario = scenario
            )
        }
    }

    /**
     * Writes correlationId, scenario, config, then continuationState. Each subclass's `Parcel`
     * constructor must read the same order. Subclasses with additional fields must call this
     * implementation before writing them. Subclasses that never carry a continuation state
     * should read config via [readConfigAndSkipContinuationState].
     */
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(correlationId)
        parcel.writeString(scenario.name)
        parcel.writeSerializable(config)
        parcel.writeSerializable(continuationState)
    }

    override fun describeContents(): Int = 0
}

/** Maps an enrollment interruption without changing the originating public flow. */
internal fun NativeAuthV2CommandResult.StrongAuthRegistrationRequired.toRegistrationResult(
    scenario: NativeAuthFlowScenarioV2,
    config: NativeAuthPublicClientApplicationConfiguration
): NativeAuthResultV2.StrongAuthRegistrationRequired {
    val methods = authMethods.toListOfV2AuthMethods()
    val state = StrongAuthRegistrationRequiredStateV2(correlationId, scenario, config, continuationState, methods)
    return NativeAuthResultV2.StrongAuthRegistrationRequired(state, scenario, state.authMethods)
}

/** Exposes the existing-account transition independently of normal sign-up code collection. */
internal fun NativeAuthV2CommandResult.ExistingAccountCodeRequired.toExistingAccountResult(
    config: NativeAuthPublicClientApplicationConfiguration
): NativeAuthResultV2.ExistingAccountCodeRequired = NativeAuthResultV2.ExistingAccountCodeRequired(
    ExistingAccountCodeRequiredStateV2(continuationState, config),
    codeLength, challengeTargetLabel, challengeChannel
)

/** Exposes current-password sign-in independently of sign-up password creation. */
internal fun NativeAuthV2CommandResult.ExistingAccountPasswordRequired.toExistingAccountResult(
    config: NativeAuthPublicClientApplicationConfiguration
): NativeAuthResultV2.ExistingAccountPasswordRequired = NativeAuthResultV2.ExistingAccountPasswordRequired(
    ExistingAccountPasswordRequiredStateV2(continuationState, config)
)

/**
 * Used by states that never carry a continuation state, so
 * that the parcel read order stays symmetric with the write order.
 */
internal fun Parcel.readConfigAndSkipContinuationState(): NativeAuthPublicClientApplicationConfiguration {
    val config = serializable<NativeAuthPublicClientApplicationConfiguration>() as NativeAuthPublicClientApplicationConfiguration
    serializable<NativeAuthV2ContinuationState>()
    return config
}
