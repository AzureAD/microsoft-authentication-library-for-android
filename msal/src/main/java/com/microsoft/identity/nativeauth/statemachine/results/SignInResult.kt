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
import com.microsoft.identity.nativeauth.statemachine.states.AccountState
import com.microsoft.identity.nativeauth.statemachine.states.AwaitingMFAState
import com.microsoft.identity.nativeauth.statemachine.states.MFARequiredState
import com.microsoft.identity.nativeauth.statemachine.states.SignInCodeRequiredState
import com.microsoft.identity.nativeauth.statemachine.states.SignInPasswordRequiredState

/**
 * Sign in result, produced by
 * [com.microsoft.identity.nativeauth.INativeAuthPublicClientApplication.signIn]
 */
interface SignInResult : Result {

    /**
     * Complete Result, which indicates the sign in flow is complete,
     * i.e. tokens are retrieved for the provided scopes (if any).
     *
     * @param resultValue an [com.microsoft.identity.nativeauth.statemachine.states.AccountState] object containing account information and account related methods.
     */
    class Complete(override val resultValue: AccountState) :
        Result.CompleteResult(resultValue = resultValue),
        SignInResult,
        SignInSubmitCodeResult,
        SignInSubmitPasswordResult,
        MFASubmitChallengeResult

    /**
     * CodeRequired Result, which indicates a verification code is required from the user to continue.
     *
     * @param nextState [com.microsoft.identity.nativeauth.statemachine.states.SignInCodeRequiredState] the current state of the flow with follow-on methods.
     * @param codeLength the length of the code required by the server.
     * @param sentTo the email/phone number the code was sent to.
     * @param channel the channel(email/phone) the code was sent through.
     */
    class CodeRequired(
        override val nextState: SignInCodeRequiredState,
        val codeLength: Int,
        val sentTo: String,
        val channel: String,
    ) : Result.SuccessResult(nextState = nextState), SignInResult, SignInSubmitPasswordResult

    /**
     * PasswordRequired Result, which indicates that the valid password is required from the user to continue.
     *
     * @param nextState [com.microsoft.identity.nativeauth.statemachine.states.SignInPasswordRequiredState] the current state of the flow with follow-on methods.
     */
    class PasswordRequired(
        override val nextState: SignInPasswordRequiredState
    ) : SignInResult, Result.SuccessResult(nextState = nextState)


    class MFARequired(
        override val nextState: AwaitingMFAState
    ) : SignInResult, Result.SuccessResult(nextState = nextState), SignInSubmitPasswordResult, MFASubmitChallengeResult
}

/**
 * Sign in submit code result, produced by
 * [com.microsoft.identity.nativeauth.statemachine.states.SignInCodeRequiredState.submitCode]
 */
interface SignInSubmitCodeResult : Result

/**
 * Sign in submit password result, produced by
 * [com.microsoft.identity.nativeauth.statemachine.states.SignInPasswordRequiredState.submitPassword]
 */
interface SignInSubmitPasswordResult : Result

/**
 * Sign in resend code result, produced by
 * [com.microsoft.identity.nativeauth.statemachine.states.SignInCodeRequiredState.resendCode]
 */
interface SignInResendCodeResult : Result {
    /**
     * Success Result, which indicates a new verification code was successfully resent.
     *
     * @param codeLength the length of the code required by the server.
     * @param sentTo the email/phone number the code was sent to.
     * @param channel channel(email/phone) the code was sent    through.
     */
    class Success(
        override val nextState: SignInCodeRequiredState,
        val codeLength: Int,
        val sentTo: String,
        val channel: String,
    ) : SignInResendCodeResult, Result.SuccessResult(nextState = nextState)
}

interface MFARequiredResult: Result {
    class VerificationRequired(
        override val nextState: MFARequiredState,
        val codeLength: Int,
        val sentTo: String,
        val channel: String,
    ) : MFARequiredResult, Result.SuccessResult(nextState = nextState)

    class SelectionRequired(
        override val nextState: MFARequiredState,
        val authMethods: List<AuthMethod>
    ) : MFARequiredResult, MFAGetAuthMethodsResult, Result.SuccessResult(nextState = nextState)
}

interface MFASubmitChallengeResult : Result

interface MFAGetAuthMethodsResult : Result