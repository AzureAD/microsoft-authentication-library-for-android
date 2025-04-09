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

import com.microsoft.identity.nativeauth.parameters.NativeAuthRegisterStrongAuthVerificationRequiredResultParameter

interface RegisterStrongAuthChallengeResult: Result {

    /**
     * Verification required result, which indicates that a challenge was sent to the user's auth method,
     * and the server expects the challenge to be verified.
     *
     * @param result [com.microsoft.identity.nativeauth.parameters.NativeAuthRegisterStrongAuthVerificationRequiredResultParameter] a parameter object containing the result of the action.
     */
    class VerificationRequired(
        val result: NativeAuthRegisterStrongAuthVerificationRequiredResultParameter
    ) : RegisterStrongAuthChallengeResult, Result.SuccessResult(nextState = result.nextState)

}

/**
 * Results related to submit challenge operation, produced by
 * [com.microsoft.identity.nativeauth.statemachine.states.RegisterStrongAuthVerificationquiredState.submitChallenge]
 */
interface RegisterStrongAuthSubmitChallengeResult : Result