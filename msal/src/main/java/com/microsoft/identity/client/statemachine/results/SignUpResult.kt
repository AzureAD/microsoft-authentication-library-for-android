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
package com.microsoft.identity.client.statemachine.results

import com.microsoft.identity.client.RequiredUserAttribute
import com.microsoft.identity.client.statemachine.BrowserRequiredError
import com.microsoft.identity.client.statemachine.Error
import com.microsoft.identity.client.statemachine.GeneralError
import com.microsoft.identity.client.statemachine.IncorrectCodeError
import com.microsoft.identity.client.statemachine.InvalidAttributesError
import com.microsoft.identity.client.statemachine.InvalidEmailError
import com.microsoft.identity.client.statemachine.InvalidPasswordError
import com.microsoft.identity.client.statemachine.UserAlreadyExistsError
import com.microsoft.identity.client.statemachine.states.SignInAfterSignUpState
import com.microsoft.identity.client.statemachine.states.SignUpAttributesRequiredState
import com.microsoft.identity.client.statemachine.states.SignUpCodeRequiredState
import com.microsoft.identity.client.statemachine.states.SignUpPasswordRequiredState

/**
 * Sign up result, produced by
 * [com.microsoft.identity.client.INativeAuthPublicClientApplication.signUp]
 */
sealed interface SignUpResult : Result {
    /**
     * CompleteResult which indicates the sign up flow is complete,
     * i.e. the user account is created and can now be used to sign in with.
     *
     * @param nextState [com.microsoft.identity.client.statemachine.states.SignInAfterSignUpState] the current state of the flow with follow-on methods.
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
     * BrowserRequired ErrorResult, which indicates that the server requires more/different authentication mechanisms than the client is configured to provide.
     * The flow should be restarted with a browser, by calling [com.microsoft.identity.client.IPublicClientApplication.acquireToken]
     *
     * @param error  error [com.microsoft.identity.client.statemachine.BrowserRequiredError].
     */
    class BrowserRequired(override val error: BrowserRequiredError) :
        Result.ErrorResult(error = error),
        SignUpResult,
        SignUpSubmitCodeResult,
        SignUpSubmitPasswordResult,
        SignUpSubmitAttributesResult,
        SignUpResendCodeResult,
        SignUpUsingPasswordResult

    /**
     * UnexpectedError ErrorResult is a general error wrapper which indicates an unexpected error occurred during the flow.
     * If this occurs, the flow should be restarted.
     *
     * @param error [com.microsoft.identity.client.statemachine.GeneralError].
     */
    class UnexpectedError(override val error: Error) :
        Result.ErrorResult(error = error),
        SignUpResult,
        SignUpSubmitCodeResult,
        SignUpSubmitPasswordResult,
        SignUpSubmitAttributesResult,
        SignUpResendCodeResult,
        SignUpUsingPasswordResult

    /**
     * CodeRequired Result, which indicates a verification code is required from the user to continue.
     *
     * @param nextState [com.microsoft.identity.client.statemachine.states.SignUpCodeRequiredState] the current state of the flow with follow-on methods.
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
     * @param nextState [com.microsoft.identity.client.statemachine.states.SignUpAttributesRequiredState] the current state of the flow with follow-on methods.
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
     * UserAlreadyExists ErrorResult, which indicates there is already an account with the same email.
     * If this occurs, the flow should be restarted.
     *
     * @param error [com.microsoft.identity.client.statemachine.UserAlreadyExistsError]
     */
    class UserAlreadyExists(
        override val error: UserAlreadyExistsError
    ) : SignUpResult, Result.ErrorResult(error = error), SignUpUsingPasswordResult

    /**
     * InvalidEmail ErrorResult, which indicates the email provided by the user is not acceptable to the server.
     * If this occurs, the flow should be restarted.
     */
    class InvalidEmail(
        override val error: InvalidEmailError
    ) : SignUpResult, Result.ErrorResult(error = error), SignUpUsingPasswordResult

    /**
     * InvalidAttributes ErrorResult, which indicates one or more attributes that were sent failed input validation.
     * The attributes should be resubmitted.
     *
     * @param invalidAttributes a list of attributes that failed input validation
     */
    class InvalidAttributes(
        override val error: InvalidAttributesError,
        val invalidAttributes: List<String>,
    ) : SignUpResult, Result.ErrorResult(error = error), SignUpUsingPasswordResult, SignUpSubmitAttributesResult

    /**
     * PasswordRequired Result, which indicates that the valid password is required from the user to continue.
     *
     * @param nextState [com.microsoft.identity.client.statemachine.states.SignUpPasswordRequiredState] the current state of the flow with follow-on methods.
     */
    class PasswordRequired(
        override val nextState: SignUpPasswordRequiredState,
    ) : SignUpResult, Result.SuccessResult(nextState = nextState), SignUpSubmitCodeResult

    /**
     * InvalidPassword ErrorResult, which indicates the new password provided by the user was not accepted by the server.
     * The password should be re-submitted.
     *
     * @param error [com.microsoft.identity.client.statemachine.InvalidPasswordError].
     */
    class InvalidPassword(
        override val error: InvalidPasswordError
    ) : SignUpSubmitPasswordResult, SignUpUsingPasswordResult, Result.ErrorResult(error = error)
}

/**
 * Sign up with password result, produced by [com.microsoft.identity.client.INativeAuthPublicClientApplication.signUpUsingPassword]
 */
sealed interface SignUpUsingPasswordResult : Result {
    /**
     * AuthNotSupported ErrorResult, which indicates the server does not support password authentication.
     * Check your challenge type configuration in the client with the user flow configuration in the tenant.
     * The flow should be restarted.
     *
     * @param error [com.microsoft.identity.client.statemachine.GeneralError].
     */
    class AuthNotSupported(
        override val error: GeneralError
    ) : SignUpUsingPasswordResult, Result.ErrorResult(error = error)
}

/**
 * Sign in submit code result, produced by
 * [com.microsoft.identity.client.statemachine.states.SignUpCodeRequiredState.submitCode]
 */
sealed interface SignUpSubmitCodeResult : Result {
    /**
     * CodeIncorrect ErrorResult, which indicates the verification code provided by user is incorrect.
     * The code should be re-submitted.
     *
     * @param error [com.microsoft.identity.client.statemachine.IncorrectCodeError].
     */
    class CodeIncorrect(
        override val error: IncorrectCodeError
    ) : SignUpSubmitCodeResult, Result.ErrorResult(error = error)
}

/**
 * Sign in resend code result, produced by
 * [com.microsoft.identity.client.statemachine.states.SignUpCodeRequiredState.resendCode]
 */
sealed interface SignUpResendCodeResult : Result {
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
 * [com.microsoft.identity.client.statemachine.states.SignUpPasswordRequiredState.submitPassword]
 */
sealed interface SignUpSubmitPasswordResult :
    SignUpResult

/**
 * Sign in submit code result, produced by
 * [com.microsoft.identity.client.statemachine.states.SignUpAttributesRequiredState.submitAttributes]
 */
sealed interface SignUpSubmitAttributesResult :
    SignUpResult
