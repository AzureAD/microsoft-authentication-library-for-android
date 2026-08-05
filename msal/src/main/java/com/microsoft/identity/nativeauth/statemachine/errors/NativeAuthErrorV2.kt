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

package com.microsoft.identity.nativeauth.statemachine.errors

import com.microsoft.identity.nativeauth.statemachine.results.NativeAuthResultV2

internal class NativeAuthV2ErrorTypes {
    companion object {
        const val NOT_IMPLEMENTED = "not_implemented"
    }
}

/**
 * NativeAuthErrorV2 is the single unified error type for the Native Auth V2 surface.
 *
 * @param errorType the error type value of the error that occurred.
 * @param error the error returned by the authentication server.
 * @param errorMessage the error message returned by the authentication server.
 * @param correlationId a unique identifier for the request that can help in diagnostics.
 * @param scenario identifies which part of the Native Auth V2 surface produced this error.
 * @param errorCodes a list of specific error codes returned by the authentication server.
 * @param exception an internal unexpected exception that happened.
 */
open class NativeAuthErrorV2(
    override val errorType: String? = null,
    override val error: String? = null,
    override val errorMessage: String?,
    override val correlationId: String,
    override val scenario: NativeAuthFlowScenarioV2 = NativeAuthFlowScenarioV2.UNKNOWN,
    override val errorCodes: List<Int>? = null,
    override var exception: Exception? = null
) : NativeAuthResultV2,
    BrowserRequiredError,
    Error(
        errorType = errorType,
        error = error,
        errorMessage = errorMessage,
        correlationId = correlationId,
        errorCodes = errorCodes,
        exception = exception
    ) {

    fun isNotImplemented(): Boolean = this.errorType == NativeAuthV2ErrorTypes.NOT_IMPLEMENTED

    fun isUserNotFound(): Boolean = this.errorType == ErrorTypes.USER_NOT_FOUND

    fun isUserAlreadyExists(): Boolean = this.errorType == SignUpErrorTypes.USER_ALREADY_EXISTS

    fun isInvalidUsername(): Boolean = this.errorType == ErrorTypes.INVALID_USERNAME

    fun isInvalidCredentials(): Boolean = this.errorType == SignInErrorTypes.INVALID_CREDENTIALS

    fun isInvalidPassword(): Boolean = this.errorType == ErrorTypes.INVALID_PASSWORD

    fun isInvalidCode(): Boolean = this.errorType == ErrorTypes.INVALID_CODE

    fun isInvalidChallenge(): Boolean = this.errorType == ErrorTypes.INVALID_CHALLENGE

    fun isInvalidInput(): Boolean = this.errorType == ErrorTypes.INVALID_INPUT

    fun isInvalidAttributes(): Boolean = this.errorType == SignUpErrorTypes.INVALID_ATTRIBUTES

    fun isInvalidScopes(): Boolean = this.errorType == GetAccessTokenErrorTypes.INVALID_SCOPES

    fun isNoAccountFound(): Boolean = this.errorType == GetAccessTokenErrorTypes.NO_ACCOUNT_FOUND

    fun isPasswordResetFailed(): Boolean = this.errorType == ResetPasswordErrorTypes.PASSWORD_RESET_FAILED

    fun isAuthMethodBlocked(): Boolean = this.errorType == ErrorTypes.AUTH_METHOD_BLOCKED

    fun isAuthNotSupported(): Boolean = this.errorType == SignUpErrorTypes.AUTH_NOT_SUPPORTED

    fun isVerificationContactBlocked(): Boolean = this.errorType == ErrorTypes.VERIFICATION_CONTACT_BLOCKED
}
