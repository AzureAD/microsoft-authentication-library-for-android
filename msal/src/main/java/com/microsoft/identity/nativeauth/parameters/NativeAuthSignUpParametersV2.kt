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

package com.microsoft.identity.nativeauth.parameters

import com.microsoft.identity.client.claims.ClaimsRequest
import com.microsoft.identity.nativeauth.UserAttributes

/**
 * Parameters for Native Auth V2 sign-up.
 *
 * Use this type with `signUpV2`. Passwords are supplied
 * only through [com.microsoft.identity.nativeauth.statemachine.states.PasswordRequiredStateV2.submitPassword]
 * when the server requests one, not when starting sign-up.
 */
class NativeAuthSignUpParametersV2(
    /** Username sent at sign-up start, never as an `email` user attribute. */
    val username: String
) {
    /**
     * Profile values retained until attribute collection, including across verification and
     * enrollment. The names `email` and `password` are reserved and must not be included.
     */
    var attributes: UserAttributes? = null

    /** Requested scopes. Null or empty uses default OIDC scopes. */
    var scopes: List<String>? = null

    /** Claims sent at authorize-challenge entry. */
    var claimsRequest: ClaimsRequest? = null
}
