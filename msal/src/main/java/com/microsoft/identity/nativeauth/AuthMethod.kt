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
package com.microsoft.identity.nativeauth

import android.os.Parcel
import android.os.Parcelable
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthConstants
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.AuthenticationMethodApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2AuthMethod
import com.microsoft.identity.common.java.nativeauth.util.ILoggable

/**
 * AuthMethod represents a user's authentication methods.
 */
data class AuthMethod(
    // Auth method ID
    val id: String,

    // Auth method challenge type (oob, etc.)
    val challengeType: String,

    // Auth method login hint (e.g. user@contoso.com)
    val loginHint: String?,

    // Auth method challenge channel (email, sms, etc.)
    val challengeChannel: String,
) : ILoggable, Parcelable {
    override fun toUnsanitizedString(): String = "AuthMethod(id=$id, " +
            "challengeType=$challengeType, loginHint=$loginHint, challengeChannel=$challengeChannel)"

    override fun toString(): String = "AuthMethod(id=$id, challengeType=$challengeType, challengeChannel=$challengeChannel)"

    constructor(parcel: Parcel) : this(
        id = parcel.readString()  ?: "",
        challengeType = parcel.readString() ?: "",
        loginHint = parcel.readString() ?: "",
        challengeChannel = parcel.readString() ?: ""
    )

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(challengeType)
        parcel.writeString(loginHint)
        parcel.writeString(challengeChannel)
    }

    companion object CREATOR : Parcelable.Creator<AuthMethod> {
        override fun createFromParcel(parcel: Parcel): AuthMethod {
            return AuthMethod(parcel)
        }

        override fun newArray(size: Int): Array<AuthMethod?> {
            return arrayOfNulls(size)
        }
    }
}

/**
 * Converts a list of auth method API response to a list of [AuthMethod] objects
 */
internal fun List<AuthenticationMethodApiResult>.toListOfAuthMethods(): List<AuthMethod> {
    return this.map { it.toAuthMethod() }
}

/**
 * Converts a list of Native Auth V2 server methods to a list of [AuthMethod] objects.
 */
@JvmName("toListOfAuthMethodsV2")
internal fun List<NativeAuthV2AuthMethod>.toListOfV2AuthMethods(): List<AuthMethod> {
    return this.map { it.toAuthMethod() }
}

/**
 * Converts a Native Auth V2 server method to an [AuthMethod] object.
 *
 * The V2 HAL model carries a single normalized method `type`, so it maps to
 * [AuthMethod.challengeChannel]. [AuthMethod.challengeType] reports `oob` for an email method,
 * which is always delivered as an out-of-band one-time code and matches the value V1 reports for
 * the same method; any other type is passed through unchanged rather than guessed at.
 */
internal fun NativeAuthV2AuthMethod.toAuthMethod(): AuthMethod {
    val isEmail = this.type.equals(NativeAuthConstants.ChallengeChannel.EMAIL, ignoreCase = true)
    return AuthMethod(
        id = this.id,
        challengeType = if (isEmail) NativeAuthConstants.ChallengeType.OOB else this.type,
        loginHint = this.hint,
        challengeChannel = this.type
    )
}

/**
 * Converts an [AuthenticationMethodApiResult] API response to an [AuthMethod] object
 */
internal fun AuthenticationMethodApiResult.toAuthMethod(): AuthMethod {
    return AuthMethod(
        id = this.id,
        challengeType = this.challengeType,
        loginHint = this.loginHint,
        challengeChannel = this.challengeChannel
    )
}
