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

package com.microsoft.identity.client.statemachine.results

import com.microsoft.identity.client.statemachine.BrowserRequiredError
import com.microsoft.identity.client.statemachine.Error
import com.microsoft.identity.client.statemachine.IncorrectCodeError
import com.microsoft.identity.client.statemachine.PasswordIncorrectError
import com.microsoft.identity.client.statemachine.UserNotFoundError
import com.microsoft.identity.client.statemachine.states.AccountResult
import com.microsoft.identity.client.statemachine.states.SignInCodeRequiredState
import com.microsoft.identity.client.statemachine.states.SignInPasswordRequiredState

/**
 * Sign in result, produced by
 * [com.microsoft.identity.client.INativeAuthPublicClientApplication.signIn]
 */
sealed interface SignInResult : Result {

    /**
     * Complete Result, which indicates the sign in flow is complete,
     * i.e. tokens are retrieved for the provided scopes (if any).
     *
     * @param resultValue an [com.microsoft.identity.client.statemachine.states.AccountResult] object containing account information and account related methods.
     */
    class Complete(override val resultValue: AccountResult) :
        Result.CompleteResult(resultValue = resultValue),
        SignInResult,
        SignInSubmitCodeResult,
        SignInUsingPasswordResult,
        SignInSubmitPasswordResult

    /**
     * BrowserRequired ErrorResult, which indicates that the server requires more/different authentication mechanisms than the client is configured to provide.
     * The flow should be restarted with a browser, by calling [com.microsoft.identity.client.IPublicClientApplication.acquireToken]
     *
     * @param error [com.microsoft.identity.client.statemachine.BrowserRequiredError].
     */
    class BrowserRequired(override val error: BrowserRequiredError) :
        Result.ErrorResult(error = error),
        SignInResult,
        SignInUsingPasswordResult,
        SignInSubmitCodeResult,
        SignInResendCodeResult,
        SignInSubmitPasswordResult

    /**
     * UnexpectedError ErrorResult is a general error wrapper which indicates an unexpected error occurred during the flow.
     * If this occurs, the flow should be restarted.
     *
     * @param error [com.microsoft.identity.client.statemachine.Error].
     */
    class UnexpectedError(override val error: Error) :
        Result.ErrorResult(error = error),
        SignInResult,
        SignInUsingPasswordResult,
        SignInResendCodeResult,
        SignInSubmitCodeResult,
        SignInSubmitPasswordResult

    /**
     * CodeRequired Result, which indicates a verification code is required from the user to continue.
     *
     * @param nextState [com.microsoft.identity.client.statemachine.states.SignInCodeRequiredState] the current state of the flow with follow-on methods.
     * @param codeLength the length of the code required by the server.
     * @param sentTo the email/phone number the code was sent to.
     * @param channel the channel(email/phone) the code was sent through.
     */
    class CodeRequired(
        override val nextState: SignInCodeRequiredState,
        val codeLength: Int,
        val sentTo: String,
        val channel: String,
    ) : Result.SuccessResult(nextState = nextState), SignInResult, SignInUsingPasswordResult, SignInSubmitPasswordResult

    /**
     * InvalidCredentials ErrorResult, which indicates credentials provided by the users are not acceptable to the server.
     * The flow should be restarted or the password should be re-submitted, as appropriate.
     *
     * @param error [com.microsoft.identity.client.statemachine.PasswordIncorrectError].
     */
    class InvalidCredentials(
        override val error: PasswordIncorrectError
    ) : Result.ErrorResult(error = error), SignInUsingPasswordResult, SignInSubmitPasswordResult

    /**
     * UserNotFound ErrorResult, which indicates there was no account found with the provided email.
     * The flow should be restarted.
     *
     * @param error [com.microsoft.identity.client.statemachine.UserNotFoundError].
     */
    class UserNotFound(
        override val error: UserNotFoundError
    ) : SignInResult, SignInUsingPasswordResult, Result.ErrorResult(error = error)

    /**
     * PasswordRequired Result, which indicates that the valid password is required from the user to continue.
     *
     * @param nextState [com.microsoft.identity.client.statemachine.states.SignInPasswordRequiredState] the current state of the flow with follow-on methods.
     */
    class PasswordRequired(
        override val nextState: SignInPasswordRequiredState
    ) : SignInResult, Result.SuccessResult(nextState = nextState)
}

/**
 * Sign in with password result, produced by
 * [com.microsoft.identity.client.INativeAuthPublicClientApplication.signInUsingPassword]
 */
sealed interface SignInUsingPasswordResult : Result

/**
 * Sign in submit code result, produced by
 * [com.microsoft.identity.client.statemachine.states.SignInCodeRequiredState.submitCode]
 */
sealed interface SignInSubmitCodeResult : Result {
    /**
     * CodeIncorrect ErrorResult, which indicates the verification code provided by user is incorrect.
     * The code should be re-submitted.
     *
     * @param error [com.microsoft.identity.client.statemachine.IncorrectCodeError].
     */
    class CodeIncorrect(
        override val error: IncorrectCodeError
    ) : SignInSubmitCodeResult, Result.ErrorResult(error = error)
}

/**
 * Sign in submit password result, produced by
 * [com.microsoft.identity.client.statemachine.states.SignInPasswordRequiredState.submitPassword]
 */
sealed interface SignInSubmitPasswordResult : Result

/**
 * Sign in resend code result, produced by
 * [com.microsoft.identity.client.statemachine.states.SignInCodeRequiredState.resendCode]
 */
sealed interface SignInResendCodeResult : Result {
    /**
     * Success Result, which indicates a new verification code was successfully resent.
     *
     * @param codeLength the length of the code required by the server.
     * @param sentTo the email/phone number the code was sent to.
     * @param channel channel(email/phone) the code was sent through.
     */
    class Success(
        override val nextState: SignInCodeRequiredState,
        val codeLength: Int,
        val sentTo: String,
        val channel: String,
    ) : SignInResendCodeResult, Result.SuccessResult(nextState = nextState)
}
