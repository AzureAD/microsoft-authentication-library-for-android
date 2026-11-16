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
import com.microsoft.identity.common.java.nativeauth.commands.parameters.NativeAuthV2EnrollCommandParameters
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2StrongAuthCommandResult
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthConstants
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationState
import com.microsoft.identity.common.java.nativeauth.util.checkAndWrapCommandResultType
import com.microsoft.identity.common.nativeauth.internal.commands.NativeAuthV2EnrollCommand
import com.microsoft.identity.common.nativeauth.internal.controllers.v2.NativeAuthV2FlowController
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplication
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplicationConfiguration
import com.microsoft.identity.nativeauth.AuthMethod
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
import java.util.Collections

/**
 * State that requires an offered email or SMS method and an explicit contact to enroll.
 * Missing continuation state is an invalid-state error; invalid selection or blank contact is
 * invalid input. The SDK does not infer a contact from the account username.
 */
class StrongAuthRegistrationRequiredStateV2 internal constructor(
    correlationId: String,
    scenario: NativeAuthFlowScenarioV2,
    config: NativeAuthPublicClientApplicationConfiguration,
    continuationState: NativeAuthV2ContinuationState? = null,
    authMethods: List<AuthMethod> = emptyList()
) : NativeAuthBaseStateV2(correlationId, scenario, config, continuationState) {
    private val TAG: String = StrongAuthRegistrationRequiredStateV2::class.java.simpleName

    /** The immutable server-offered methods available for enrollment. */
    val authMethods: List<AuthMethod> = Collections.unmodifiableList(ArrayList(authMethods))

    private constructor(parcel: Parcel) : this(
        correlationId = parcel.readString() ?: "UNSET",
        scenario = NativeAuthFlowScenarioV2.valueOf(parcel.readString() ?: NativeAuthFlowScenarioV2.UNKNOWN.name),
        config = parcel.serializable<NativeAuthPublicClientApplicationConfiguration>() as NativeAuthPublicClientApplicationConfiguration,
        continuationState = parcel.serializable<NativeAuthV2ContinuationState>(),
        authMethods = parcel.createTypedArrayList(AuthMethod.CREATOR) ?: emptyList()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        super.writeToParcel(parcel, flags)
        parcel.writeTypedList(authMethods)
    }

    interface SelectAuthMethodCallback : Callback<NativeAuthResultV2>

    /** Enrolls [method] using [verificationContact], delivering the result through [callback]. */
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

    /** Enrolls an offered method with an explicit contact; no contact or OTP format is inferred. */
    suspend fun selectAuthMethod(method: AuthMethod, verificationContact: String? = null): NativeAuthResultV2 {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.selectAuthMethod(method: AuthMethod, verificationContact: String?)"
        )
        val state = continuationState ?: return invalidState()
        val offered = authMethods.firstOrNull { it.id == method.id }
        if (offered == null || verificationContact.isNullOrBlank()) {
            return NativeAuthErrorV2(
                errorType = ErrorTypes.INVALID_INPUT,
                errorMessage = "Select an offered authentication method and provide a nonblank verification contact.",
                correlationId = correlationId,
                scenario = scenario
            )
        }
        if (!offered.challengeChannel.equals(NativeAuthConstants.ChallengeChannel.EMAIL, true) &&
            !offered.challengeChannel.equals(NativeAuthConstants.ChallengeChannel.SMS, true)) {
            return NativeAuthErrorV2(
                errorType = ErrorTypes.NOT_IMPLEMENTED,
                errorMessage = "Only email and SMS enrollment are supported.",
                correlationId = correlationId,
                scenario = scenario
            )
        }
        return withContext(Dispatchers.IO) {
            try {
                val carrier = CommandParametersAdapter.createNativeAuthV2SignInAfterSignUpCommandParameters(
                    config, config.oAuth2TokenCache, state, null, null
                )
                val command = NativeAuthV2EnrollCommand(
                    NativeAuthV2EnrollCommandParameters(carrier, state, offered.id, verificationContact),
                    NativeAuthV2FlowController(),
                    PublicApiId.NATIVE_AUTH_JIT_CHALLENGE_AUTH_METHOD
                )
                ensureActive()
                val rawResult = CommandDispatcher.submitSilentReturningFuture(command).getCancellable()
                ensureActive()
                mapStrongAuthResult(
                    rawResult.checkAndWrapCommandResultType<NativeAuthV2StrongAuthCommandResult>(),
                    offered.challengeChannel
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                NativeAuthErrorV2(
                    errorType = ErrorTypes.CLIENT_EXCEPTION,
                    errorMessage = "MSAL client exception occurred during strong-auth enrollment.",
                    correlationId = correlationId, scenario = scenario, exception = e
                )
            }
        }
    }

    companion object CREATOR : Parcelable.Creator<StrongAuthRegistrationRequiredStateV2> {
        override fun createFromParcel(parcel: Parcel): StrongAuthRegistrationRequiredStateV2 = StrongAuthRegistrationRequiredStateV2(parcel)

        override fun newArray(size: Int): Array<StrongAuthRegistrationRequiredStateV2?> = arrayOfNulls(size)
    }
}
