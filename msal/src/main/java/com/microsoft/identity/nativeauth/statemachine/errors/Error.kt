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

import com.microsoft.identity.nativeauth.statemachine.results.ResetPasswordResendCodeResult
import com.microsoft.identity.nativeauth.statemachine.results.ResetPasswordResult
import com.microsoft.identity.nativeauth.statemachine.results.ResetPasswordStartResult
import com.microsoft.identity.nativeauth.statemachine.results.ResetPasswordSubmitCodeResult
import com.microsoft.identity.nativeauth.statemachine.results.SignInResendCodeResult
import com.microsoft.identity.nativeauth.statemachine.results.SignInResult
import com.microsoft.identity.nativeauth.statemachine.results.SignInSubmitCodeResult
import com.microsoft.identity.nativeauth.statemachine.results.SignUpResendCodeResult
import com.microsoft.identity.nativeauth.statemachine.results.SignUpResult
import com.microsoft.identity.nativeauth.statemachine.results.SignUpSubmitAttributesResult
import com.microsoft.identity.nativeauth.statemachine.results.SignUpSubmitCodeResult
import com.microsoft.identity.nativeauth.statemachine.results.SignUpSubmitPasswordResult

/**
 * ErrorTypes class holds the possible error type values that are shared between the errors
 * returned from each flow.
 */
internal class ErrorTypes {
    companion object {
        /*
         * The BROWSER_REQUIRED value indicates that the server requires more/different authentication mechanisms than the client is configured to provide.
         * The flow should be restarted with a browser, by calling [com.microsoft.identity.client.IPublicClientApplication.acquireToken]
         */
        const val BROWSER_REQUIRED = "browser_required"

        /*
         * The INVALID_CODE value indicates the verification code provided by user is incorrect.
         * The code should be re-submitted.
         */
        const val INVALID_CODE = "invalid_code"

        /*
         * The USER_NOT_FOUND value indicates there was no account found with the provided email.
         * The flow should be restarted.
         */
        const val USER_NOT_FOUND = "user_not_found"

        /*
         * The INVALID_PASSWORD value indicates the new password provided by the user was not accepted by the server.
         * The password should be re-submitted.
         */
        const val INVALID_PASSWORD = "invalid_password"

        /*
         * The INVALID_USERNAME value indicates the username provided by the user is not acceptable to the server.
         * If this occurs, the flow should be restarted.
         */
        const val INVALID_USERNAME = "invalid_username"

        /*
         * The INVALID_STATE value indicates a misconfigured or expired state, or an internal error
         * in state transitions. If this occurs, the flow should be restarted.
         */
        const val INVALID_STATE = "invalid_state"
    }
}

/**
 * Error is a base class for all errors present in the Native Auth.
 * @param errorType the error type value of the error that occurred
 * @param error the error returned by the authentication server.
 * @param errorMessage the error message returned by the authentication server.
 * @param correlationId a unique identifier for the request that can help in diagnostics.
 * @param errorCodes a list of specific error codes returned by the authentication server.
 * @param exception an internal unexpected exception that happened.
 */
open class Error(
    internal open val errorType: String? = null,
    open val error: String? = null,
    open val errorMessage: String?,
    open val correlationId: String,
    open var exception: Exception? = null,
    open val errorCodes: List<Int>? = null
) {
}

/**
 * BrowserRequiredError error is an interface for all errors that could require a browser redirection in Native Auth.
 * All error classes that can potentially return a browser redirection must implement this interface.
 */
interface BrowserRequiredError {
    fun isBrowserRequired(): Boolean = (this as Error).errorType == ErrorTypes.BROWSER_REQUIRED
}

/**
 * SubmitCodeError Error, which indicates that an error occurred when the verification code
 * was submitted by the user. The user should use the utility methods of this class
 * to identify and handle the error. This error is produced by
 * [com.microsoft.identity.nativeauth.statemachine.states.SignInCodeRequiredState.submitCode],
 * [com.microsoft.identity.nativeauth.statemachine.states.SignUpCodeRequiredState.submitCode],
 * [com.microsoft.identity.nativeauth.statemachine.states.ResetPasswordCodeRequiredState.submitCode]
 * @param errorType the error type value of the error that occurred
 * @param error the error returned by the authentication server.
 * @param errorMessage the error message returned by the authentication server.
 * @param correlationId a unique identifier for the request that can help in diagnostics.
 * @param errorCodes a list of specific error codes returned by the authentication server.
 * @param subError the sub error returned by the authentication server.
 * @param exception an internal unexpected exception that happened.
 */
class SubmitCodeError(
    override val errorType: String? = null,
    override val error: String? = null,
    override val errorMessage: String?,
    override val correlationId: String,
    override val errorCodes: List<Int>? = null,
    val subError: String? = null,
    override var exception: Exception? = null
): BrowserRequiredError, SignInSubmitCodeResult, SignUpSubmitCodeResult, ResetPasswordSubmitCodeResult, Error(errorType = errorType, error = error, errorMessage= errorMessage, correlationId = correlationId, errorCodes = errorCodes, exception = exception)
{
    fun isInvalidCode(): Boolean = this.errorType == ErrorTypes.INVALID_CODE
}

/**
 * ResendCodeError Error, which indicates that an error occurred when a new verification code
 * was requested by the user. The user should use the utility methods of this class
 * to identify and handle the error. This error is produced by
 * [com.microsoft.identity.nativeauth.statemachine.states.SignInCodeRequiredState.resendCode],
 * [com.microsoft.identity.nativeauth.statemachine.states.SignUpCodeRequiredState.resendCode],
 * [com.microsoft.identity.nativeauth.statemachine.states.ResetPasswordCodeRequiredState.resendCode]
 * @param errorType the error type value of the error that occurred
 * @param error the error returned by the authentication server.
 * @param errorMessage the error message returned by the authentication server.
 * @param correlationId a unique identifier for the request that can help in diagnostics.
 * @param errorCodes a list of specific error codes returned by the authentication server.
 * @param exception an internal unexpected exception that happened.
 */
class ResendCodeError(
    override val errorType: String? = null,
    override val error: String? = null,
    override val errorMessage: String?,
    override val correlationId: String,
    override val errorCodes: List<Int>? = null,
    override var exception: Exception? = null
): SignInResendCodeResult, SignUpResendCodeResult, ResetPasswordResendCodeResult, Error(errorType = errorType, error = error, errorMessage= errorMessage, correlationId = correlationId, errorCodes = errorCodes, exception = exception)
