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

import com.microsoft.identity.nativeauth.statemachine.BrowserRequiredError
import com.microsoft.identity.nativeauth.statemachine.GeneralError
import com.microsoft.identity.nativeauth.statemachine.IncorrectCodeError
import com.microsoft.identity.nativeauth.statemachine.InvalidPasswordError
import com.microsoft.identity.nativeauth.statemachine.UserNotFoundError
import com.microsoft.identity.nativeauth.statemachine.states.ResetPasswordCodeRequiredState
import com.microsoft.identity.nativeauth.statemachine.states.ResetPasswordPasswordRequiredState

/**
 * Self-service password reset.
 */
sealed interface ResetPasswordResult : Result {
    /**
     * Complete Result, which indicates the reset password flow completed successfully.
     * i.e. the password is successfully reset.
     *
     * @param resultValue null
     */
    object Complete :
        Result.CompleteResult(resultValue = null),
        ResetPasswordResult,
        ResetPasswordSubmitPasswordResult

    /**
     * BrowserRequired ErrorResult, which indicates that the server requires more/different authentication mechanisms than the client is configured or able to provide.
     * The flow should be restarted with a browser, by calling [com.microsoft.identity.client.IPublicClientApplication.acquireToken]
     *
     * @param error [com.microsoft.identity.nativeauth.statemachine.BrowserRequiredError]
     */
    class BrowserRequired(
        override val error: BrowserRequiredError
    ) : Result.ErrorResult(error = error),
        ResetPasswordResult,
        ResetPasswordStartResult,
        ResetPasswordSubmitCodeResult,
        ResetPasswordSubmitPasswordResult,
        ResetPasswordResendCodeResult

    /**
     * UnexpectedError ErrorResult is a general error wrapper which indicates an unexpected error occurred during the flow.
     * If this occurs, the flow should be restarted.
     *
     * @param error [com.microsoft.identity.nativeauth.statemachine.Error]
     */
    class UnexpectedError(override val error: com.microsoft.identity.nativeauth.statemachine.Error) :
        Result.ErrorResult(error = error),
        ResetPasswordResult,
        ResetPasswordStartResult,
        ResetPasswordSubmitCodeResult,
        ResetPasswordSubmitPasswordResult,
        ResetPasswordResendCodeResult
}

/**
 * SSPR start result, produced by
 * [com.microsoft.identity.client.INativeAuthPublicClientApplication.resetPassword]
 */
sealed interface ResetPasswordStartResult : Result {
    /**
     * CodeRequired Result, which indicates a verification code is required from the user to continue.
     *
     * @param nextState [com.microsoft.identity.nativeauth.statemachine.states.ResetPasswordCodeRequiredState] the current state of the flow with follow-on methods.
     * @param codeLength the length of the code required by the server.
     * @param sentTo the email/phone number the code was sent to.
     * @param channel the channel(email/phone) the code was sent through.
     */
    class CodeRequired(
        override val nextState: ResetPasswordCodeRequiredState,
        val codeLength: Int,
        val sentTo: String,
        val channel: String,
    ) : ResetPasswordStartResult, Result.SuccessResult(nextState = nextState)

    /**
     * UserNotFound ErrorResult, which indicates there was no account found with the provided email.
     * The flow should be restarted.
     *
     * @param error [com.microsoft.identity.nativeauth.statemachine.UserNotFoundError] the current state of the flow with follow-on methods.
     */
    class UserNotFound(
        override val error: UserNotFoundError
    ) : ResetPasswordStartResult, Result.ErrorResult(error = error)
}

/**
 * SSPR submit code result, produced by
 * [com.microsoft.identity.nativeauth.statemachine.states.ResetPasswordCodeRequiredState.submitCode]
 */
sealed interface ResetPasswordSubmitCodeResult : Result {
    /**
     * PasswordRequired Result, which indicates that a valid new password is required from the user to continue.
     *
     * @param nextState [com.microsoft.identity.nativeauth.statemachine.states.ResetPasswordPasswordRequiredState] the current state of the flow with follow-on methods.
     */
    class PasswordRequired(
        override val nextState: ResetPasswordPasswordRequiredState
    ) : ResetPasswordSubmitCodeResult, Result.SuccessResult(nextState = nextState)

    /**
     * CodeIncorrect ErrorResult, which indicates the verification code provided by user is incorrect.
     * The code should be re-submitted.
     *
     * @param error [com.microsoft.identity.nativeauth.statemachine.IncorrectCodeError]
     */
    class CodeIncorrect(
        override val error: IncorrectCodeError
    ) : ResetPasswordSubmitCodeResult, Result.ErrorResult(error = error)
}

/**
 * Sign in resend code result, produced by
 * [com.microsoft.identity.nativeauth.statemachine.states.ResetPasswordCodeRequiredState.resendCode]
 */
sealed interface ResetPasswordResendCodeResult : Result {
    /**
     * Success Result, which indicates a new verification code was successfully resent.
     *
     * @param nextState [com.microsoft.identity.nativeauth.statemachine.states.ResetPasswordCodeRequiredState] the current state of the flow with follow-on methods.
     * @param codeLength the length of the code required by the server.
     * @param sentTo the email/phone number the code was sent to.
     * @param channel channel(email/phone) the code was sent through.
     */
    class Success(
        override val nextState: ResetPasswordCodeRequiredState,
        val codeLength: Int,
        val sentTo: String,
        val channel: String,
    ) : ResetPasswordResendCodeResult, Result.SuccessResult(nextState = nextState)
}

/**
 * SSPR submit password result, produced by
 * [com.microsoft.identity.nativeauth.statemachine.states.ResetPasswordPasswordRequiredState.submitPassword]
 */
sealed interface ResetPasswordSubmitPasswordResult : Result {
    /**
     * InvalidPassword ErrorResult, which indicates the new password provided by the user is not acceptable to the server.
     * The password should be re-submitted.
     *
     * @param error [com.microsoft.identity.nativeauth.statemachine.InvalidPasswordError]
     */
    class InvalidPassword(
        override val error: InvalidPasswordError
    ) : ResetPasswordSubmitPasswordResult, Result.ErrorResult(error = error)
    /**
     * PasswordResetFailed ErrorResult, which indicates the password reset flow failed.
     *
     * @param error [com.microsoft.identity.nativeauth.statemachine.GeneralError]
     */
    class PasswordResetFailed(
        override val error: GeneralError
    ) : ResetPasswordSubmitPasswordResult, Result.ErrorResult(error = error)
}
