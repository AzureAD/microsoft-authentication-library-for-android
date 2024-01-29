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

import com.microsoft.identity.nativeauth.statemachine.results.ResetPasswordResult
import com.microsoft.identity.nativeauth.statemachine.results.ResetPasswordStartResult
import com.microsoft.identity.nativeauth.statemachine.results.ResetPasswordSubmitPasswordResult

/**
 * ResetPasswordErrorTypes class holds the specific error type values that can be returned
 * only by the reset password flow.
 */
internal class ResetPasswordErrorTypes {
    companion object {
        /*
         * The PASSWORD_RESET_FAILED value indicates that the password reset flow failed.
         */
        const val PASSWORD_RESET_FAILED = "password_reset_failed"
    }
}

/**
 * Reset password error. The user should use the utility methods of this class
 * to identify and handle the error. This error is produced by
 * [com.microsoft.identity.nativeauth.INativeAuthPublicClientApplication.resetPassword]
 * @param errorType the error type value of the error that occurred.
 * @param error the error returned by the authentication server.
 * @param errorMessage the error message returned by the authentication server.
 * @param correlationId a unique identifier for the request that can help in diagnostics.
 * @param errorCodes a list of specific error codes returned by the authentication server.
 * @param exception an internal unexpected exception that happened.
 */
class ResetPasswordError(
    override val errorType: String? = null,
    override val error: String? = null,
    override val errorMessage: String?,
    override val correlationId: String,
    override val errorCodes: List<Int>? = null,
    override var exception: Exception? = null
): ResetPasswordResult, ResetPasswordStartResult, BrowserRequiredError, Error(errorType = errorType, error = error, errorMessage= errorMessage, correlationId = correlationId, errorCodes = errorCodes, exception = exception) {
    fun isUserNotFound() : Boolean = this.errorType == ErrorTypes.USER_NOT_FOUND

    fun isInvalidUsername(): Boolean = this.errorType == ErrorTypes.INVALID_USERNAME
}

/**
 * SSPR submit password error. The user should use the utility methods of this class
 * to identify and handle the error. This error is produced by
 * [com.microsoft.identity.nativeauth.statemachine.states.ResetPasswordPasswordRequiredState.submitPassword]
 * @param errorType the error type value of the error that occurred
 * @param error the error returned by the authentication server.
 * @param errorMessage the error message returned by the authentication server.
 * @param correlationId a unique identifier for the request that can help in diagnostics.
 * @param errorCodes a list of specific error codes returned by the authentication server.
 * @param subError the sub error returned by the authentication server.
 * @param exception an internal unexpected exception that happened.
 */
class ResetPasswordSubmitPasswordError(
    override val errorType: String? = null,
    override val error: String? = null,
    override val errorMessage: String?,
    override val correlationId: String,
    override val errorCodes: List<Int>? = null,
    val subError: String? = null,
    override var exception: Exception? = null
): ResetPasswordSubmitPasswordResult, Error(errorType = errorType, error = error, errorMessage= errorMessage, correlationId = correlationId, errorCodes = errorCodes, exception = exception) {

    fun isInvalidPassword() : Boolean = this.errorType == ErrorTypes.INVALID_PASSWORD

    fun isPasswordResetFailed() : Boolean = this.errorType == ResetPasswordErrorTypes.PASSWORD_RESET_FAILED

    fun isInvalidUsername() : Boolean = this.errorType == ErrorTypes.INVALID_USERNAME
}
