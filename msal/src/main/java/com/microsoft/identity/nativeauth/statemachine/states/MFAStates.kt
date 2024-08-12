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
import com.microsoft.identity.nativeauth.statemachine.results.MFARequiredResult
import com.microsoft.identity.nativeauth.statemachine.results.MFAGetAuthMethodsResult
import com.microsoft.identity.nativeauth.statemachine.results.MFASubmitChallengeResult
import com.microsoft.identity.nativeauth.statemachine.results.SignInResult
import com.microsoft.identity.nativeauth.utils.serializable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SignInAwaitingMFAState(
    override val continuationToken: String,
    override val correlationId: String,
    private val scopes: List<String>?,
    private val config: NativeAuthPublicClientApplicationConfiguration
) : BaseState(continuationToken = continuationToken, correlationId = correlationId), State, Parcelable {

    // Challenge default auth method
    suspend fun sendChallenge(): MFARequiredResult {
        return withContext(Dispatchers.IO) {
            // if /challenge returns HTTP 200
            if (true) {
                MFARequiredResult.VerificationRequired(
                    nextState = MFARequiredState(
                        continuationToken = continuationToken,
                        correlationId = correlationId,
                        scopes = scopes,
                        config = config
                    ),
                    codeLength = 6,
                    sentTo = "user@contoso.com",
                    channel = "email"
                )
            } else {
                // /challenge returns introspect_required
                // call /introspect and return authMethods
                MFARequiredResult.SelectionRequired(
                    nextState = MFARequiredState(
                        continuationToken = continuationToken,
                        correlationId = correlationId,
                        scopes = scopes,
                        config = config
                    ),
                    authMethods = listOf(1, 2, 3)
                )
            }
        }
    }

    constructor(parcel: Parcel) : this(
        continuationToken = parcel.readString()  ?: "",
        correlationId = parcel.readString() ?: "UNSET",
        scopes = parcel.createStringArrayList(),
        config = parcel.serializable<NativeAuthPublicClientApplicationConfiguration>() as NativeAuthPublicClientApplicationConfiguration
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(continuationToken)
        parcel.writeString(correlationId)
        parcel.writeStringList(scopes)
        parcel.writeSerializable(config)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SignInAwaitingMFAState> {
        override fun createFromParcel(parcel: Parcel): SignInAwaitingMFAState {
            return SignInAwaitingMFAState(parcel)
        }

        override fun newArray(size: Int): Array<SignInAwaitingMFAState?> {
            return arrayOfNulls(size)
        }
    }
}

class MFARequiredState(
    override val continuationToken: String,
    override val correlationId: String,
    private val scopes: List<String>?,
    private val config: NativeAuthPublicClientApplicationConfiguration
) : BaseState(continuationToken = continuationToken, correlationId = correlationId), State, Parcelable {

    // Challenge default auth method
    suspend fun sendChallenge(): MFARequiredResult {
        return withContext(Dispatchers.IO) {
            // if /challenge returns HTTP 200
            if (true) {
                MFARequiredResult.VerificationRequired(
                    nextState = MFARequiredState(
                        continuationToken = continuationToken,
                        correlationId = correlationId,
                        scopes = scopes,
                        config = config
                    ),
                    codeLength = 6,
                    sentTo = "user@contoso.com",
                    channel = "email"
                )
            } else {
                // /challenge returns introspect_required
                // call /introspect and return authMethods
                MFARequiredResult.SelectionRequired(
                    nextState = MFARequiredState(
                        continuationToken = continuationToken,
                        correlationId = correlationId,
                        scopes = scopes,
                        config = config
                    ),
                    authMethods = listOf(1, 2, 3)
                )
            }
        }
    }

    // Challenge specified auth methods
    suspend fun sendChallenge(authMethod: Int): MFARequiredResult {
        return withContext(Dispatchers.IO) {
            // if /challenge returns HTTP 200
            if (true) {
                MFARequiredResult.VerificationRequired(
                    nextState = MFARequiredState(
                        continuationToken = continuationToken,
                        correlationId = correlationId,
                        scopes = scopes,
                        config = config
                    ),
                    codeLength = 6,
                    sentTo = "user@contoso.com",
                    channel = "email"
                )
            } else {
                // /challenge returns introspect_required
                // call /introspect and return authMethods
                MFARequiredResult.SelectionRequired(
                    nextState = MFARequiredState(
                        continuationToken = continuationToken,
                        correlationId = correlationId,
                        scopes = scopes,
                        config = config
                    ),
                    authMethods = listOf(1, 2, 3)
                )
            }
        }
    }

    // Call /introspect
    suspend fun getAuthMethods(): MFAGetAuthMethodsResult {
        return MFAGetAuthMethodsResult(
            authMethods = listOf(1, 2, 3)
        )
    }

    // Call /token
    suspend fun submitChallenge(code: Int): MFASubmitChallengeResult {
        // If /token returns HTTP 200
        return if (true) {
            SignInResult.DummyComplete()
        } else {
            // If /token returns another mfa_required error
            SignInResult.MFARequired(
               nextState = SignInAwaitingMFAState(
                   continuationToken = continuationToken,
                   correlationId = correlationId,
                   scopes = scopes,
                   config = config
               )
            )
        }
    }

    constructor(parcel: Parcel) : this(
        continuationToken = parcel.readString()  ?: "",
        correlationId = parcel.readString() ?: "UNSET",
        scopes = parcel.createStringArrayList(),
        config = parcel.serializable<NativeAuthPublicClientApplicationConfiguration>() as NativeAuthPublicClientApplicationConfiguration
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(continuationToken)
        parcel.writeString(correlationId)
        parcel.writeStringList(scopes)
        parcel.writeSerializable(config)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MFARequiredState> {
        override fun createFromParcel(parcel: Parcel): MFARequiredState {
            return MFARequiredState(parcel)
        }

        override fun newArray(size: Int): Array<MFARequiredState?> {
            return arrayOfNulls(size)
        }
    }
}