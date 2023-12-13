// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.nativeauth.statemachine.results

import com.microsoft.identity.nativeauth.RequiredUserAttribute
import com.microsoft.identity.nativeauth.statemachine.states.SignInAfterSignUpState
import com.microsoft.identity.nativeauth.statemachine.states.SignUpAttributesRequiredState
import com.microsoft.identity.nativeauth.statemachine.states.SignUpCodeRequiredState
import com.microsoft.identity.nativeauth.statemachine.states.SignUpPasswordRequiredState

/**
 * Sign up result, produced by
 * [com.microsoft.identity.nativeauth.INativeAuthPublicClientApplication.signUp]
 */
interface SignUpResult : Result {
    /**
     * CompleteResult which indicates the sign up flow is complete,
     * i.e. the user account is created and can now be used to sign in with.
     *
     * @param nextState [com.microsoft.identity.nativeauth.statemachine.states.SignInAfterSignUpState] the current state of the flow with follow-on methods.
     */
    class Complete(
        override val nextState: SignInAfterSignUpState
    ) : Result.CompleteWithNextStateResult(resultValue = null, nextState = nextState),
        SignUpResult,
        SignUpSubmitCodeResult,
        SignUpSubmitPasswordResult,
        SignUpSubmitAttributesResult,
        SignUpUsingPasswordResult

    /**
     * CodeRequired Result, which indicates a verification code is required from the user to continue.
     *
     * @param nextState [com.microsoft.identity.nativeauth.statemachine.states.SignUpCodeRequiredState] the current state of the flow with follow-on methods.
     * @param codeLength the length of the code required by the server
     * @param sentTo the email/phone number the code was sent to
     * @param channel the channel(email/phone) the code was sent through
     */
    class CodeRequired(
        override val nextState: SignUpCodeRequiredState,
        val codeLength: Int,
        val sentTo: String,
        val channel: String
    ) : SignUpResult, Result.SuccessResult(nextState = nextState), SignUpUsingPasswordResult

    /**
     * AttributesRequired Result, which indicates the server requires one or more attributes to be sent, before the account can be created.
     *
     * @param nextState [com.microsoft.identity.nativeauth.statemachine.states.SignUpAttributesRequiredState] the current state of the flow with follow-on methods.
     * @param requiredAttributes a list of [com.microsoft.identity.common.java.providers.nativeauth.responses.RequiredUserAttributeApiResult.RequiredUserAttribute] objects with details about the account attributes required by the server.
     */
    class AttributesRequired(
        override val nextState: SignUpAttributesRequiredState,
        val requiredAttributes: List<RequiredUserAttribute>,
    ) : SignUpResult,
        Result.SuccessResult(nextState = nextState),
        SignUpUsingPasswordResult,
        SignUpSubmitCodeResult,
        SignUpSubmitAttributesResult,
        SignUpSubmitPasswordResult

    /**
     * PasswordRequired Result, which indicates that the valid password is required from the user to continue.
     *
     * @param nextState [com.microsoft.identity.nativeauth.statemachine.states.SignUpPasswordRequiredState] the current state of the flow with follow-on methods.
     */
    class PasswordRequired(
        override val nextState: SignUpPasswordRequiredState,
    ) : SignUpResult, Result.SuccessResult(nextState = nextState),
        SignUpSubmitCodeResult
}

/**
 * Sign up with password result, produced by [com.microsoft.identity.client.INativeAuthPublicClientApplication.signUpUsingPassword]
 */
interface SignUpUsingPasswordResult : Result

/**
 * Sign in submit code result, produced by
 * [com.microsoft.identity.nativeauth.statemachine.states.SignUpCodeRequiredState.submitCode]
 */
interface SignUpSubmitCodeResult : Result

/**
 * Sign in resend code result, produced by
 * [com.microsoft.identity.nativeauth.statemachine.states.SignUpCodeRequiredState.resendCode]
 */
interface SignUpResendCodeResult : Result {
    /**
     * Success Result,  which indicates a new verification code was successfully resent.
     *
     * @param codeLength the length of the code required by the server.
     * @param sentTo the email/phone number the code was sent to.
     * @param channel channel(email/phone) the code was sent through.
     */
    class Success(
        override val nextState: SignUpCodeRequiredState,
        val codeLength: Int,
        val sentTo: String,
        val channel: String
    ) : SignUpResendCodeResult, Result.SuccessResult(nextState = nextState)
}

/**
 * Sign in submit code result, produced by
 * [com.microsoft.identity.nativeauth.statemachine.states.SignUpPasswordRequiredState.submitPassword]
 */
interface SignUpSubmitPasswordResult :
    SignUpResult

/**
 * Sign in submit code result, produced by
 * [com.microsoft.identity.nativeauth.statemachine.states.SignUpAttributesRequiredState.submitAttributes]
 */
interface SignUpSubmitAttributesResult :
    SignUpResult
