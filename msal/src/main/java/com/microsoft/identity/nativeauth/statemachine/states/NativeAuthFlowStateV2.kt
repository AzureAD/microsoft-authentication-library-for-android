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
import com.microsoft.identity.nativeauth.AuthMethod
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplication
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplicationConfiguration
import com.microsoft.identity.nativeauth.UserAttributes
import com.microsoft.identity.nativeauth.statemachine.errors.NativeAuthErrorV2
import com.microsoft.identity.nativeauth.statemachine.errors.NativeAuthFlowScenarioV2
import com.microsoft.identity.nativeauth.statemachine.errors.NativeAuthV2ErrorTypes
import com.microsoft.identity.nativeauth.statemachine.results.NativeAuthResultV2
import com.microsoft.identity.nativeauth.utils.serializable
import kotlinx.coroutines.launch

/**
 * NativeAuthFlowStateV2 is the single unified state for the Native Auth V2 surface.
 *
 * @property continuationToken Continuation token passed in the next request.
 * @property correlationId Correlation ID taken from the previous API response and passed to the next request.
 * @property scenario Identifies which part of the Native Auth V2 surface this state belongs to.
 * @property config Configuration used by Native Auth.
 */
class NativeAuthFlowStateV2 internal constructor(
    override val continuationToken: String,
    override val correlationId: String,
    internal val scenario: NativeAuthFlowScenarioV2,
    private val config: NativeAuthPublicClientApplicationConfiguration
) : BaseState(continuationToken = continuationToken, correlationId = correlationId), State, Parcelable {
    private val TAG: String = NativeAuthFlowStateV2::class.java.simpleName

    constructor(parcel: Parcel) : this(
        continuationToken = parcel.readString() ?: "",
        correlationId = parcel.readString() ?: "UNSET",
        scenario = NativeAuthFlowScenarioV2.valueOf(parcel.readString() ?: NativeAuthFlowScenarioV2.UNKNOWN.name),
        config = parcel.serializable<NativeAuthPublicClientApplicationConfiguration>() as NativeAuthPublicClientApplicationConfiguration
    )

    private fun notImplemented(): NativeAuthResultV2 = NativeAuthErrorV2(
        errorType = NativeAuthV2ErrorTypes.NOT_IMPLEMENTED,
        errorMessage = "This is not implemented yet",
        correlationId = correlationId,
        scenario = scenario
    )

    interface SubmitCodeCallback : Callback<NativeAuthResultV2>

    fun submitCode(code: String, callback: SubmitCodeCallback) {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.submitCode(code: String, callback: SubmitCodeCallback)"
        )
        NativeAuthPublicClientApplication.pcaScope.launch {
            try {
                callback.onResult(submitCode(code))
            } catch (e: MsalException) {
                Logger.error(TAG, "Exception thrown in submitCode", e)
                callback.onError(e)
            }
        }
    }

    suspend fun submitCode(code: String): NativeAuthResultV2 {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.submitCode(code: String)"
        )
        return notImplemented()
    }

    interface SubmitPasswordCallback : Callback<NativeAuthResultV2>

    fun submitPassword(password: CharArray, callback: SubmitPasswordCallback) {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.submitPassword(password: CharArray, callback: SubmitPasswordCallback)"
        )
        NativeAuthPublicClientApplication.pcaScope.launch {
            try {
                callback.onResult(submitPassword(password))
            } catch (e: MsalException) {
                Logger.error(TAG, "Exception thrown in submitPassword", e)
                callback.onError(e)
            }
        }
    }

    suspend fun submitPassword(password: CharArray): NativeAuthResultV2 {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.submitPassword(password: CharArray)"
        )
        return notImplemented()
    }

    interface SubmitNewPasswordCallback : Callback<NativeAuthResultV2>

    fun submitNewPassword(password: CharArray, callback: SubmitNewPasswordCallback) {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.submitNewPassword(password: CharArray, callback: SubmitNewPasswordCallback)"
        )
        NativeAuthPublicClientApplication.pcaScope.launch {
            try {
                callback.onResult(submitNewPassword(password))
            } catch (e: MsalException) {
                Logger.error(TAG, "Exception thrown in submitNewPassword", e)
                callback.onError(e)
            }
        }
    }

    suspend fun submitNewPassword(password: CharArray): NativeAuthResultV2 {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.submitNewPassword(password: CharArray)"
        )
        return notImplemented()
    }

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

    interface SelectAuthMethodCallback : Callback<NativeAuthResultV2>

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

    suspend fun selectAuthMethod(method: AuthMethod, verificationContact: String? = null): NativeAuthResultV2 {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.selectAuthMethod(method: AuthMethod, verificationContact: String?)"
        )
        return notImplemented()
    }

    interface SubmitChallengeCallback : Callback<NativeAuthResultV2>

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

    suspend fun submitChallenge(challenge: String): NativeAuthResultV2 {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.submitChallenge(challenge: String)"
        )
        return notImplemented()
    }

    interface ResendCodeCallback : Callback<NativeAuthResultV2>

    fun resendCode(callback: ResendCodeCallback) {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.resendCode(callback: ResendCodeCallback)"
        )
        NativeAuthPublicClientApplication.pcaScope.launch {
            try {
                callback.onResult(resendCode())
            } catch (e: MsalException) {
                Logger.error(TAG, "Exception thrown in resendCode", e)
                callback.onError(e)
            }
        }
    }

    suspend fun resendCode(): NativeAuthResultV2 {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.resendCode()"
        )
        return notImplemented()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(continuationToken)
        parcel.writeString(correlationId)
        parcel.writeString(scenario.name)
        parcel.writeSerializable(config)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<NativeAuthFlowStateV2> {
        override fun createFromParcel(parcel: Parcel): NativeAuthFlowStateV2 {
            return NativeAuthFlowStateV2(parcel)
        }

        override fun newArray(size: Int): Array<NativeAuthFlowStateV2?> {
            return arrayOfNulls(size)
        }
    }
}
