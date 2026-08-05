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
import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplication
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplicationConfiguration
import com.microsoft.identity.nativeauth.UserAttributes
import com.microsoft.identity.nativeauth.statemachine.errors.NativeAuthFlowScenarioV2
import com.microsoft.identity.nativeauth.statemachine.results.NativeAuthResultV2
import kotlinx.coroutines.launch

/**
 * State that requires the user to submit account attributes.
 */
class AttributesRequiredStateV2 internal constructor(
    continuationToken: String,
    correlationId: String,
    scenario: NativeAuthFlowScenarioV2,
    config: NativeAuthPublicClientApplicationConfiguration
) : NativeAuthBaseStateV2(continuationToken, correlationId, scenario, config) {
    private val TAG: String = AttributesRequiredStateV2::class.java.simpleName

    private constructor(parcel: Parcel) : this(
        continuationToken = parcel.readString() ?: "",
        correlationId = parcel.readString() ?: "UNSET",
        scenario = NativeAuthFlowScenarioV2.valueOf(parcel.readString() ?: NativeAuthFlowScenarioV2.UNKNOWN.name),
        // Also drains the continuationState field written by the base class, so the read order
        // stays symmetric with NativeAuthBaseStateV2.writeToParcel even though this state has none.
        config = parcel.readConfigAndSkipContinuationState()
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
        return notImplemented()
    }

    companion object CREATOR : Parcelable.Creator<AttributesRequiredStateV2> {
        override fun createFromParcel(parcel: Parcel): AttributesRequiredStateV2 = AttributesRequiredStateV2(parcel)

        override fun newArray(size: Int): Array<AttributesRequiredStateV2?> = arrayOfNulls(size)
    }
}
