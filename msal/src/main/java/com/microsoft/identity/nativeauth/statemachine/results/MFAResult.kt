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

package com.microsoft.identity.nativeauth.statemachine.results

import com.microsoft.identity.nativeauth.AuthMethod
import com.microsoft.identity.nativeauth.statemachine.states.MFARequiredState

/**
 * Results related to various MFA operations.
 */
interface MFARequiredResult: Result {

    /**
     * Verification required result, which indicates that a challenge was sent to the user's auth method,
     * and the server expects the challenge to be verified.
     *
     * @param nextState [com.microsoft.identity.nativeauth.statemachine.states.MFARequiredState] the current state of the flow with follow-on methods.
     * @param codeLength the length of the challenge required by the server.
     * @param sentTo the email/phone number the challenge was sent to.
     * @param channel the channel(email/phone) the challenge was sent through.
     */
    class VerificationRequired(
        override val nextState: MFARequiredState,
        val codeLength: Int,
        val sentTo: String,
        val channel: String,
    ) : MFARequiredResult, Result.SuccessResult(nextState = nextState)

    /**
     * Selection required result, which indicates that a specific authentication method must be selected, which
     * the server will send the challenge to (once sendChallenge() is called).
     *
     * @param nextState [com.microsoft.identity.nativeauth.statemachine.states.MFARequiredState] the current state of the flow with follow-on methods.
     * @param authMethods the authentication methods that can be used to complete the challenge flow.
     */
    class SelectionRequired(
        override val nextState: MFARequiredState,
        val authMethods: List<AuthMethod>
    ) : MFARequiredResult, MFAGetAuthMethodsResult, Result.SuccessResult(nextState = nextState)
}

/**
 * Results related to get authentication methods operation, produced by
 * [com.microsoft.identity.nativeauth.statemachine.states.MFARequiredState.getAuthMethods]
 */
interface MFAGetAuthMethodsResult : Result

/**
 * Results related to MFA submit challenge operation, produced by
 *  * [com.microsoft.identity.nativeauth.statemachine.states.MFARequiredState.submitChallenge]
 */
interface MFASubmitChallengeResult : Result
