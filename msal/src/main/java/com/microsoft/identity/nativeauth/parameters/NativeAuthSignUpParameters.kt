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

import com.microsoft.identity.nativeauth.UserAttributes
import com.microsoft.identity.client.claims.ClaimsRequest

/**
 * Encapsulates the parameters passed to the signUp methods of NativeAuthPublicClientApplication
 */
class NativeAuthSignUpParameters(
    /**
     * username of the account to sign up.
     * V2 sends this at sign-up start, never as an `email` user attribute.
     */
    val username: String
) {

    /**
     * password of the account to sign up.
     *
     * V2 retains an in-memory copy until attribute collection, including across verification
     * and code resend. The copy is erased after submission or sign-up completion and is never
     * serialized. Restoring a saved flow may require the app to supply the password again.
     */
    var password: CharArray? = null

    /**
     * User attributes to be used during account creation.
     * V2 retains supplied profile values until attribute collection, even when verification
     * occurs first.
     *
     * The attribute names `email` and `password` are reserved and must not be included.
     * Supply them through the dedicated [username] and [password] properties instead.
     */
    var attributes: UserAttributes? = null

    /** V2-only requested scopes. Null or empty uses default OIDC scopes; ignored by V1. */
    var scopes: List<String>? = null

    /** V2-only claims sent at authorize-challenge entry; ignored by V1. */
    var claimsRequest: ClaimsRequest? = null
}