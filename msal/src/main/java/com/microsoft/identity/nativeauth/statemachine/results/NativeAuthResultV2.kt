// ktlint-disable filename

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
import com.microsoft.identity.nativeauth.RequiredUserAttribute
import com.microsoft.identity.nativeauth.statemachine.NativeAuthFlowScenarioV2
import com.microsoft.identity.nativeauth.statemachine.states.AccountState
import com.microsoft.identity.nativeauth.statemachine.states.AttributesInvalidStateV2
import com.microsoft.identity.nativeauth.statemachine.states.AttributesRequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.CodeRequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.MFARequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.MFAVerificationRequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.NewPasswordRequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.PasswordRequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.SignInAfterResetPasswordStateV2
import com.microsoft.identity.nativeauth.statemachine.states.StrongAuthRegistrationRequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.StrongAuthVerificationRequiredStateV2
import java.util.Collections

/**
 * NativeAuthResultV2 is the single unified result for the Native Auth V2 surface.
 */
interface NativeAuthResultV2 : Result {

    /**
     * Identifies which part of the Native Auth V2 surface produced this result.
     */
    val scenario: NativeAuthFlowScenarioV2

    /**
     * Complete Result, which indicates tokens have been acquired.
     *
     * @param resultValue an [AccountState] object containing account information and related methods.
     * @param scenario identifies which part of the Native Auth V2 surface produced this result.
     */
    class Complete(
        override val resultValue: AccountState,
        override val scenario: NativeAuthFlowScenarioV2
    ) : Result.CompleteResult(resultValue = resultValue),
        NativeAuthResultV2

    /**
     * CodeRequired Result, which indicates a verification code is required from the user to continue.
     *
     * @param nextState the current state with follow-on methods.
     * @param codeLength the length of the code required by the server.
     * @param sentTo the email/phone number the code was sent to.
     * @param channel the channel (email/phone) the code was sent through.
     */
    class CodeRequired(
        override val nextState: CodeRequiredStateV2,
        override val scenario: NativeAuthFlowScenarioV2,
        val codeLength: Int,
        val sentTo: String,
        val channel: String
    ) : Result.SuccessResult(nextState = nextState), NativeAuthResultV2

    /**
     * PasswordRequired Result, which indicates a password is required from the user to continue.
     *
     * @param nextState the current state with follow-on methods.
     */
    class PasswordRequired(
        override val nextState: PasswordRequiredStateV2,
        override val scenario: NativeAuthFlowScenarioV2
    ) : Result.SuccessResult(nextState = nextState), NativeAuthResultV2

    /**
     * NewPasswordRequired Result, which indicates a new password is required from the user to continue.
     *
     * @param nextState the current state with follow-on methods.
     */
    class NewPasswordRequired(
        override val nextState: NewPasswordRequiredStateV2,
        override val scenario: NativeAuthFlowScenarioV2
    ) : Result.SuccessResult(nextState = nextState), NativeAuthResultV2

    /**
     * SignInAfterResetPasswordRequired Result, which indicates the password reset flow has
     * completed server-side. Token exchange and cache persistence are deferred until the app
     * explicitly invokes [SignInAfterResetPasswordStateV2.signIn] on [nextState].
     *
     * @param nextState the current state with the follow-on signIn() method.
     * @param scenario identifies which part of the Native Auth V2 surface produced this result.
     */
    class SignInAfterResetPasswordRequired(
        override val nextState: SignInAfterResetPasswordStateV2,
        override val scenario: NativeAuthFlowScenarioV2
    ) : Result.SuccessResult(nextState = nextState), NativeAuthResultV2

    /**
     * AttributesRequired Result, which indicates user attributes are required to continue.
     *
     * @param nextState the current state with follow-on methods.
     * @param requiredAttributes the attributes required by the server.
     */
    class AttributesRequired(
        override val nextState: AttributesRequiredStateV2,
        override val scenario: NativeAuthFlowScenarioV2,
        val requiredAttributes: List<RequiredUserAttribute>
    ) : Result.SuccessResult(nextState = nextState), NativeAuthResultV2

    /**
     * AttributesInvalid Result, which indicates the submitted attributes were rejected and must be corrected.
     *
     * @param nextState the current state with follow-on methods.
     * @param invalidAttributes the names of the attributes that were rejected by the server.
     */
    class AttributesInvalid(
        override val nextState: AttributesInvalidStateV2,
        override val scenario: NativeAuthFlowScenarioV2,
        val invalidAttributes: List<String>
    ) : Result.SuccessResult(nextState = nextState), NativeAuthResultV2

    /**
     * MFARequired Result, which indicates multi-factor authentication is required and the user must
     * select an authentication method.
     *
     * @param nextState the current state with follow-on methods.
     * @param authMethods the authentication methods available to the user.
     */
    class MFARequired(
        override val nextState: MFARequiredStateV2,
        override val scenario: NativeAuthFlowScenarioV2,
        authMethods: List<AuthMethod>
    ) : Result.SuccessResult(nextState = nextState), NativeAuthResultV2 {
        val authMethods: List<AuthMethod> = Collections.unmodifiableList(ArrayList(authMethods))
    }

    /**
     * MFAVerificationRequired Result, which indicates an MFA challenge was sent and the user must
     * enter the verification code.
     *
     * @param nextState the current state with follow-on methods.
     * @param codeLength the length of the code required by the server.
     * @param sentTo the email/phone number the code was sent to.
     * @param channel the channel (email/phone) the code was sent through.
     */
    class MFAVerificationRequired(
        override val nextState: MFAVerificationRequiredStateV2,
        override val scenario: NativeAuthFlowScenarioV2,
        val codeLength: Int,
        val sentTo: String,
        val channel: String
    ) : Result.SuccessResult(nextState = nextState), NativeAuthResultV2

    /**
     * StrongAuthRegistrationRequired Result, which indicates a strong-authentication method
     * registration is required and the user must select a method.
     *
     * @param nextState the current state with follow-on methods.
     * @param authMethods the authentication methods available for registration.
     */
    class StrongAuthRegistrationRequired(
        override val nextState: StrongAuthRegistrationRequiredStateV2,
        override val scenario: NativeAuthFlowScenarioV2,
        val authMethods: List<AuthMethod>
    ) : Result.SuccessResult(nextState = nextState), NativeAuthResultV2

    /**
     * StrongAuthVerificationRequired Result, which indicates a strong-authentication challenge
     * was sent and the user must enter the verification code.
     *
     * @param nextState the current state with follow-on methods.
     * @param codeLength the length of the code required by the server.
     * @param sentTo the email/phone number the code was sent to.
     * @param channel the channel (email/phone) the code was sent through.
     */
    class StrongAuthVerificationRequired(
        override val nextState: StrongAuthVerificationRequiredStateV2,
        override val scenario: NativeAuthFlowScenarioV2,
        val codeLength: Int,
        val sentTo: String,
        val channel: String
    ) : Result.SuccessResult(nextState = nextState), NativeAuthResultV2
}
