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

import com.microsoft.identity.nativeauth.statemachine.states.ResetPasswordCodeRequiredState
import com.microsoft.identity.nativeauth.statemachine.states.ResetPasswordPasswordRequiredState
import com.microsoft.identity.nativeauth.statemachine.states.SignInAfterSignUpState

/**
 * Self-service password reset.
 * Documentation: https://github.com/microsoft/entra-previews-internal/blob/main/PP4/docs/Native-Auth/Developer-guides/0-Android-Kotlin/3-additional-scenarios/4-self-service-password-reset.md
 */
interface ResetPasswordResult : Result {
    /**
     * Complete Result, which indicates the reset password flow completed successfully.
     * i.e. the password is successfully reset.
     *
     * @param nextState [com.microsoft.identity.nativeauth.statemachine.states.SignInAfterSignUpState] the current state of the flow with follow-on methods.
     */
    class Complete(
        override val nextState: SignInAfterSignUpState
    ) : Result.CompleteWithNextStateResult(resultValue = null, nextState = nextState),
        ResetPasswordResult,
        ResetPasswordSubmitPasswordResult
}

/**
 * SSPR start result, produced by
 * [com.microsoft.identity.nativeauth.INativeAuthPublicClientApplication.resetPassword]
 */
interface ResetPasswordStartResult : Result {
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
}

/**
 * SSPR submit code result, produced by
 * [com.microsoft.identity.nativeauth.statemachine.states.ResetPasswordCodeRequiredState.submitCode]
 */
interface ResetPasswordSubmitCodeResult : Result {
    /**
     * PasswordRequired Result, which indicates that a valid new password is required from the user to continue.
     *
     * @param nextState [com.microsoft.identity.nativeauth.statemachine.states.ResetPasswordPasswordRequiredState] the current state of the flow with follow-on methods.
     */
    class PasswordRequired(
        override val nextState: ResetPasswordPasswordRequiredState
    ) : ResetPasswordSubmitCodeResult, Result.SuccessResult(nextState = nextState)
}

/**
 * Sign in resend code result, produced by
 * [com.microsoft.identity.nativeauth.statemachine.states.ResetPasswordCodeRequiredState.resendCode]
 */
interface ResetPasswordResendCodeResult : Result {
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
interface ResetPasswordSubmitPasswordResult : Result

