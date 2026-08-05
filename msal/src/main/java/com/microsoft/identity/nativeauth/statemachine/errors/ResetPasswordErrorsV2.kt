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

/**
 * Reset password error for the Native Auth V2 surface. Use the utility methods of this class to
 * identify and handle the error. This error is produced by
 * [com.microsoft.identity.nativeauth.INativeAuthPublicClientApplication.resetPasswordV2].
 *
 * @param errorType the error type value of the error that occurred.
 * @param error the error returned by the authentication server.
 * @param errorMessage the error message returned by the authentication server.
 * @param correlationId a unique identifier for the request that can help in diagnostics.
 * @param scenario identifies which part of the Native Auth V2 surface produced this error.
 * @param errorCodes a list of specific error codes returned by the authentication server.
 * @param exception an internal unexpected exception that happened.
 */
class ResetPasswordErrorV2(
    errorType: String? = null,
    error: String? = null,
    errorMessage: String?,
    correlationId: String,
    scenario: NativeAuthFlowScenarioV2 = NativeAuthFlowScenarioV2.RESET_PASSWORD,
    errorCodes: List<Int>? = null,
    exception: Exception? = null
) : NativeAuthErrorV2(errorType, error, errorMessage, correlationId, scenario, errorCodes, exception) {

    fun isUserNotFound(): Boolean = this.errorType == ErrorTypes.USER_NOT_FOUND

    fun isInvalidUsername(): Boolean = this.errorType == ErrorTypes.INVALID_USERNAME
}

/**
 * Submit new password error for the Native Auth V2 surface. Use the utility methods of this class
 * to identify and handle the error. This error is produced by
 * [com.microsoft.identity.nativeauth.statemachine.states.NewPasswordRequiredStateV2.submitNewPassword].
 *
 * @param errorType the error type value of the error that occurred.
 * @param error the error returned by the authentication server.
 * @param errorMessage the error message returned by the authentication server.
 * @param correlationId a unique identifier for the request that can help in diagnostics.
 * @param scenario identifies which part of the Native Auth V2 surface produced this error.
 * @param errorCodes a list of specific error codes returned by the authentication server.
 * @param subError the sub error returned by the authentication server.
 * @param exception an internal unexpected exception that happened.
 */
class SubmitNewPasswordErrorV2(
    errorType: String? = null,
    error: String? = null,
    errorMessage: String?,
    correlationId: String,
    scenario: NativeAuthFlowScenarioV2 = NativeAuthFlowScenarioV2.RESET_PASSWORD,
    errorCodes: List<Int>? = null,
    val subError: String? = null,
    exception: Exception? = null
) : NativeAuthErrorV2(errorType, error, errorMessage, correlationId, scenario, errorCodes, exception) {

    fun isInvalidPassword(): Boolean = this.errorType == ErrorTypes.INVALID_PASSWORD

    fun isPasswordResetFailed(): Boolean = this.errorType == ResetPasswordErrorTypes.PASSWORD_RESET_FAILED
}
