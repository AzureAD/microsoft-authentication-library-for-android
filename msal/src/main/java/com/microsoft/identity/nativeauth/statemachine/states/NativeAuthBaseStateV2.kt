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
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationState
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplicationConfiguration
import com.microsoft.identity.nativeauth.statemachine.errors.NativeAuthErrorV2
import com.microsoft.identity.nativeauth.statemachine.errors.NativeAuthFlowScenarioV2
import com.microsoft.identity.nativeauth.statemachine.errors.NativeAuthV2ErrorTypes
import com.microsoft.identity.nativeauth.statemachine.results.NativeAuthResultV2

/**
 * Base class for the Native Auth V2 states. Each concrete state exposes only the methods that are
 * valid at that point in the flow.
 *
 * @property continuationToken Continuation token passed in the next request.
 * @property correlationId Correlation ID taken from the previous API response and passed to the next request.
 * @property scenario Identifies which part of the Native Auth V2 surface this state belongs to.
 * @property config Configuration used by Native Auth.
 * @property continuationState Opaque DTO carrying mid-flow state; null in legacy/test paths and for states that don't drive a server call.
 */
abstract class NativeAuthBaseStateV2 internal constructor(
    override val continuationToken: String,
    override val correlationId: String,
    internal val scenario: NativeAuthFlowScenarioV2,
    internal val config: NativeAuthPublicClientApplicationConfiguration,
    internal val continuationState: NativeAuthV2ContinuationState? = null
) : BaseState(continuationToken = continuationToken, correlationId = correlationId), State, Parcelable {

    private val TAG: String = NativeAuthBaseStateV2::class.java.simpleName

    protected fun notImplemented(): NativeAuthResultV2 = NativeAuthErrorV2(
        errorType = NativeAuthV2ErrorTypes.NOT_IMPLEMENTED,
        errorMessage = "This is not implemented yet",
        correlationId = correlationId,
        scenario = scenario
    )

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
                errorMessage = "Password reset completed but no sign-in result was returned.",
                correlationId = result.correlationId,
                scenario = scenario
            )
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(continuationToken)
        parcel.writeString(correlationId)
        parcel.writeString(scenario.name)
        parcel.writeSerializable(config)
    }

    override fun describeContents(): Int = 0
}
