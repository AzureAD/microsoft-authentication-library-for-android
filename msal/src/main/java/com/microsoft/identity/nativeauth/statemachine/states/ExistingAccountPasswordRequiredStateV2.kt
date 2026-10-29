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
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationState
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplicationConfiguration
import com.microsoft.identity.nativeauth.statemachine.NativeAuthFlowScenarioV2
import com.microsoft.identity.nativeauth.statemachine.results.NativeAuthResultV2

/**
 * Sign-up found an existing account. The app must collect its current password, not a new password.
 * Submission reuses sign-in password handling, including buffer ownership, retries and MFA/JIT.
 */
class ExistingAccountPasswordRequiredStateV2 private constructor(
    private val signInState: PasswordRequiredStateV2
) : NativeAuthBaseStateV2(
    signInState.correlationId, NativeAuthFlowScenarioV2.SIGN_IN,
    signInState.config, signInState.continuationState
) {
    internal constructor(
        continuationState: NativeAuthV2ContinuationState,
        config: NativeAuthPublicClientApplicationConfiguration
    ) : this(PasswordRequiredStateV2(continuationState, NativeAuthFlowScenarioV2.SIGN_IN, config))

    /** Receives the existing-account sign-in outcome. */
    interface SubmitPasswordCallback : PasswordRequiredStateV2.SubmitPasswordCallback

    /**
     * Submits the existing account's [password] and delivers the outcome through [callback].
     * The caller retains its buffer; sign-in owns and clears a copy.
     */
    fun submitPassword(password: CharArray, callback: SubmitPasswordCallback) =
        signInState.submitPassword(password, callback)

    /**
     * Submits the existing account's [password]. The caller's buffer is copied, not cleared.
     * An incorrect password can be retried on this state.
     */
    suspend fun submitPassword(password: CharArray): NativeAuthResultV2 = signInState.submitPassword(password)

    /** Restores the same opaque sign-in continuation used by the underlying password state. */
    companion object CREATOR : Parcelable.Creator<ExistingAccountPasswordRequiredStateV2> {
        override fun createFromParcel(parcel: Parcel): ExistingAccountPasswordRequiredStateV2 =
            ExistingAccountPasswordRequiredStateV2(PasswordRequiredStateV2.CREATOR.createFromParcel(parcel))

        override fun newArray(size: Int): Array<ExistingAccountPasswordRequiredStateV2?> = arrayOfNulls(size)
    }
}
