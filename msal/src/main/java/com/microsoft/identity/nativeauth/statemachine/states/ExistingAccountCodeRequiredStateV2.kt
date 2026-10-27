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
import com.microsoft.identity.common.java.exception.BaseException
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationState
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplicationConfiguration
import com.microsoft.identity.nativeauth.statemachine.NativeAuthFlowScenarioV2
import com.microsoft.identity.nativeauth.statemachine.results.NativeAuthResultV2

/**
 * Sign-up found an existing account. Submitting its email code continues sign-in, not registration.
 * The existing sign-in implementation owns verification, retries, MFA/JIT and token acquisition.
 */
class ExistingAccountCodeRequiredStateV2 private constructor(
    private val signInState: CodeRequiredStateV2
) : NativeAuthBaseStateV2(
    signInState.correlationId, NativeAuthFlowScenarioV2.SIGN_IN,
    signInState.config, signInState.continuationState
) {
    internal constructor(
        continuationState: NativeAuthV2ContinuationState,
        config: NativeAuthPublicClientApplicationConfiguration
    ) : this(CodeRequiredStateV2(continuationState, NativeAuthFlowScenarioV2.SIGN_IN, config))

    /** Receives the existing-account sign-in outcome. */
    interface SubmitCodeCallback : CodeRequiredStateV2.SubmitCodeCallback

    /** Verifies [code] for the existing account and delivers the sign-in outcome to [callback]. */
    fun submitCode(code: String, callback: SubmitCodeCallback) = signInState.submitCode(code, callback)

    /** Verifies [code] for the existing account; an invalid code can be retried on this state. */
    suspend fun submitCode(code: String): NativeAuthResultV2 = signInState.submitCode(code)

    /** Receives a fresh existing-account code state or a sign-in error. */
    interface ResendCodeCallback : Callback<NativeAuthResultV2>

    /** Requests a fresh email code and delivers the successor through [callback]. */
    fun resendCode(callback: ResendCodeCallback) {
        signInState.resendCode(object : CodeRequiredStateV2.ResendCodeCallback {
            override fun onResult(result: NativeAuthResultV2) {
                callback.onResult(asExistingAccountResult(result))
            }

            override fun onError(exception: BaseException) = callback.onError(exception)
        })
    }

    /** Requests a fresh email code while retaining the existing-account distinction. */
    suspend fun resendCode(): NativeAuthResultV2 = asExistingAccountResult(signInState.resendCode())

    private fun asExistingAccountResult(result: NativeAuthResultV2): NativeAuthResultV2 =
        if (result is NativeAuthResultV2.CodeRequired) {
            NativeAuthResultV2.ExistingAccountCodeRequired(
                ExistingAccountCodeRequiredStateV2(result.nextState),
                result.codeLength, result.sentTo, result.channel
            )
        } else result

    /** Restores the same opaque sign-in continuation used by the underlying code state. */
    companion object CREATOR : Parcelable.Creator<ExistingAccountCodeRequiredStateV2> {
        override fun createFromParcel(parcel: Parcel): ExistingAccountCodeRequiredStateV2 =
            ExistingAccountCodeRequiredStateV2(CodeRequiredStateV2.CREATOR.createFromParcel(parcel))

        override fun newArray(size: Int): Array<ExistingAccountCodeRequiredStateV2?> = arrayOfNulls(size)
    }
}
