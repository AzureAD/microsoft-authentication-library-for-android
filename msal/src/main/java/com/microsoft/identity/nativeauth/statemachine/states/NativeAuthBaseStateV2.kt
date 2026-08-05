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
 */
abstract class NativeAuthBaseStateV2 internal constructor(
    override val continuationToken: String,
    override val correlationId: String,
    internal val scenario: NativeAuthFlowScenarioV2,
    internal val config: NativeAuthPublicClientApplicationConfiguration
) : BaseState(continuationToken = continuationToken, correlationId = correlationId), State, Parcelable {

    protected fun notImplemented(): NativeAuthResultV2 = NativeAuthErrorV2(
        errorType = NativeAuthV2ErrorTypes.NOT_IMPLEMENTED,
        errorMessage = "This is not implemented yet",
        correlationId = correlationId,
        scenario = scenario
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(continuationToken)
        parcel.writeString(correlationId)
        parcel.writeString(scenario.name)
        parcel.writeSerializable(config)
    }

    override fun describeContents(): Int = 0
}
