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

import com.microsoft.identity.nativeauth.statemachine.results.SignUpResult
import com.microsoft.identity.nativeauth.statemachine.results.SignUpSubmitAttributesResult
import com.microsoft.identity.nativeauth.statemachine.results.SignUpSubmitPasswordResult

/**
 * SignUpErrorTypes class holds the specific error type values that can be returned
 * only by the signup flow.
 */
internal class SignUpErrorTypes {
    companion object {
        /* The USER_ALREADY_EXISTS value indicates there is already an account with the same email.
         * If this occurs, the flow should be restarted.
         */
        const val USER_ALREADY_EXISTS = "user_already_exists"

        /* The INVALID_USERNAME value indicates the username provided by the user is not acceptable to the server.
         * If this occurs, the flow should be restarted.
         */
        const val INVALID_USERNAME = "invalid_username"

        /*
         * The INVALID_ATTRIBUTES value indicates one or more attributes that were sent failed input validation.
         * The attributes should be resubmitted.
         */
        const val INVALID_ATTRIBUTES = "invalid_attributes"

        /*
         * The AUTH_NOT_SUPPORTED value indicates the server does not support password authentication.
         * The challenge type configuration in the client is incompatible with the user flow configuration in the tenant.
         * The flow should be restarted.
         */
        const val AUTH_NOT_SUPPORTED = "auth_not_supported"
    }
}

/**
 * Sign up start error. The user should use the utility methods of this class
 * to identify and handle the error. This error is produced by
 * [com.microsoft.identity.nativeauth.INativeAuthPublicClientApplication.signUp]
 * @param errorType the error type value of the error that occurred
 * @param error the error returned by the authentication server.
 * @param errorMessage the error message returned by the authentication server.
 * @param correlationId a unique identifier for the request that can help in diagnostics.
 * @param errorCodes a list of specific error codes returned by the authentication server.
 * @param exception an internal unexpected exception that happened.
 */
open class SignUpError (
    override val errorType: String? = null,
    override val error: String? = null,
    override val errorMessage: String?,
    override val correlationId: String,
    override val errorCodes: List<Int>? = null,
    override var exception: Exception? = null
): SignUpResult, BrowserRequiredError, Error(errorType = errorType, error = error, errorMessage= errorMessage, correlationId = correlationId, errorCodes = errorCodes, exception = exception) {

    fun isUserAlreadyExists(): Boolean = this.errorType == SignUpErrorTypes.USER_ALREADY_EXISTS

    fun isInvalidUsername(): Boolean = this.errorType == SignUpErrorTypes.INVALID_USERNAME

    fun isInvalidAttributes(): Boolean = this.errorType == SignUpErrorTypes.INVALID_ATTRIBUTES

    fun isInvalidPassword(): Boolean = this.errorType == ErrorTypes.INVALID_PASSWORD

    fun isAuthNotSupported(): Boolean = this.errorType == SignUpErrorTypes.AUTH_NOT_SUPPORTED
}

/**
 * Sign in submit password error. The user should use the utility methods of this class
 * to identify and handle the error. This error is produced by
 * [com.microsoft.identity.nativeauth.statemachine.states.SignUpPasswordRequiredState.submitPassword]
 * @param errorType the error type value of the error that occurred
 * @param error the error returned by the authentication server.
 * @param errorMessage the error message returned by the authentication server.
 * @param correlationId a unique identifier for the request that can help in diagnostics.
 * @param errorCodes a list of specific error codes returned by the authentication server.
 * @param subError the sub error returned by the authentication server.
 * @param exception an internal unexpected exception that happened.
 */
class SignUpSubmitPasswordError (
    override val errorType: String? = null,
    override val error: String? = null,
    override val errorMessage: String?,
    override val correlationId: String,
    override val errorCodes: List<Int>? = null,
    val subError: String? = null,
    override var exception: Exception? = null
): SignUpSubmitPasswordResult, BrowserRequiredError, Error(errorType = errorType, error = error, errorMessage= errorMessage, correlationId = correlationId, errorCodes = errorCodes, exception = exception) {

    fun isInvalidPassword(): Boolean = this.errorType == ErrorTypes.INVALID_PASSWORD
}

/**
 * Sign in submit attributes error. The user should use the utility methods of this class
 * to identify and handle the error. This error is produced by
 * [com.microsoft.identity.nativeauth.statemachine.states.SignUpAttributesRequiredState.submitAttributes]
 * @param errorType the error type value of the error that occurred
 * @param error the error returned by the authentication server.
 * @param errorMessage the error message returned by the authentication server.
 * @param correlationId a unique identifier for the request that can help in diagnostics.
 * @param errorCodes a list of specific error codes returned by the authentication server.
 * @param exception an internal unexpected exception that happened.
 */
class SignUpSubmitAttributesError (
    override val errorType: String? = null,
    override val error: String? = null,
    override val errorMessage: String?,
    override val correlationId: String,
    override val errorCodes: List<Int>? = null,
    override var exception: Exception? = null
): SignUpSubmitAttributesResult, BrowserRequiredError, Error(errorType = errorType, error = error, errorMessage= errorMessage, correlationId = correlationId, errorCodes = errorCodes, exception = exception) {

    fun isInvalidAttributes(): Boolean = this.errorType == SignUpErrorTypes.INVALID_ATTRIBUTES
}
