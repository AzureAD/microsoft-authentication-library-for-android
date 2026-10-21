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

import com.microsoft.identity.nativeauth.statemachine.NativeAuthFlowScenarioV2
import com.microsoft.identity.nativeauth.statemachine.results.NativeAuthResultV2

/**
 * The shared error type for every Native Auth V2 operation. All V2 errors are a
 * [NativeAuthResultV2], so any V2 state method or entry point that returns a result can also
 * return an error.
 *
 * Use the classifiers to handle known failures. Raw service diagnostics are retained, including
 * unknown errors. Migrate operation-specific V2 error checks to this type; V1 is unchanged.
 *
 * @param errorType the error type value of the error that occurred.
 * @param error the error returned by the authentication server.
 * @param errorMessage the error message returned by the authentication server.
 * @param correlationId a unique identifier for the request that can help in diagnostics.
 * @param scenario identifies which part of the Native Auth V2 surface produced this error.
 * @param errorCodes a list of specific error codes returned by the authentication server.
 * @param exception an internal unexpected exception that happened.
 * @param subError the unmodified service suberror.
 * @param attributeValidationDetails service validation details, runtime-checked by classifiers.
 * @param traceId the unmodified service trace identifier, when available.
 * @param timestamp the unmodified service timestamp, when available.
 */
class NativeAuthErrorV2 @JvmOverloads constructor(
    override val errorType: String? = null,
    override val error: String? = null,
    override val errorMessage: String?,
    override val correlationId: String,
    override val scenario: NativeAuthFlowScenarioV2,
    override val errorCodes: List<Int>? = null,
    override var exception: Exception? = null,
    val subError: String? = null,
    val attributeValidationDetails: Any? = null,
    val traceId: String? = null,
    val timestamp: String? = null
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

    /**
     * Returns true if the requested Native Auth V2 flow or step is not implemented yet.
     */
    fun isNotImplemented(): Boolean = this.errorType == ErrorTypes.NOT_IMPLEMENTED

    /** Whether the service rejected the username. */
    fun isInvalidUsername(): Boolean = errorType == ErrorTypes.INVALID_USERNAME ||
        (error == "invalidRequest" && errorMessage?.contains("AADSTS90100") == true &&
            errorMessage.contains("username", ignoreCase = true))

    /** Whether the account does not exist. */
    fun isUserNotFound(): Boolean = errorType == ErrorTypes.USER_NOT_FOUND ||
        (error == "invalidRequest" && errorMessage?.contains("AADSTS50034") == true)

    /** Whether sign-in credentials were rejected, not a new-password policy violation. */
    fun isInvalidCredentials(): Boolean = errorType == SignInErrorTypes.INVALID_CREDENTIALS ||
        (error == "invalidGrant" && subError == "invalidUserNameOrPassword")

    /** Whether a first-factor or MFA one-time code was rejected. */
    fun isInvalidCode(): Boolean = errorType == ErrorTypes.INVALID_CODE ||
        (error == "invalidGrant" && subError == "invalidOneTimeCode")

    /** Whether password-specific errors or validation details indicate a new-password policy violation. */
    fun isInvalidPassword(): Boolean = errorType == ErrorTypes.INVALID_PASSWORD ||
        (error == "invalidRequest" && subError in PASSWORD_POLICY_SUBERRORS) ||
        validationDetails().any { detail ->
            detail["code"] == "passwordPolicyViolation" &&
                (detail["attributeIds"] as? List<*>)?.any {
                    it is String && it.equals("password", ignoreCase = true)
                } == true
        }

    /** Whether the identifier already belongs to an account. */
    fun isUserAlreadyExists(): Boolean = errorType == SignUpErrorTypes.USER_ALREADY_EXISTS ||
        subError == "userAlreadyExists" ||
        validationDetails().any { it["code"] == "userAlreadyExists" }

    /** Whether the request omitted required attributes. */
    fun isMissingRequiredAttributes(): Boolean =
        validationDetails().any { it["code"] == "attributeRequired" }

    /** Whether submitted attributes failed service validation. */
    fun isInvalidAttributes(): Boolean = errorType == SignUpErrorTypes.INVALID_ATTRIBUTES ||
        subError == "attributeValidationError" || isMissingRequiredAttributes()

    /** Whether the service rejected a verification challenge. */
    fun isInvalidChallenge(): Boolean = isInvalidCode() || errorType == ErrorTypes.INVALID_CHALLENGE

    /** Whether the service blocked the selected authentication method. */
    fun isAuthMethodBlocked(): Boolean = errorType == ErrorTypes.AUTH_METHOD_BLOCKED

    /** Whether the service blocked the supplied verification contact. */
    fun isVerificationContactBlocked(): Boolean = errorType == ErrorTypes.VERIFICATION_CONTACT_BLOCKED

    /** Whether caller input is invalid. */
    fun isInvalidInput(): Boolean = errorType == ErrorTypes.INVALID_INPUT ||
        (errorType == ErrorTypes.CLIENT_EXCEPTION && exception is IllegalArgumentException)

    /** Whether the requested authentication mechanism is unsupported. */
    fun isAuthNotSupported(): Boolean = errorType == SignUpErrorTypes.AUTH_NOT_SUPPORTED

    /** Whether password reset failed after credential validation. */
    fun isPasswordResetFailed(): Boolean = errorType == ResetPasswordErrorTypes.PASSWORD_RESET_FAILED

    /** Whether no stable classifier describes this error; raw diagnostics remain available. */
    fun isGeneralError(): Boolean = !(isInvalidUsername() || isUserNotFound() ||
        isInvalidCredentials() || isInvalidCode() || isInvalidPassword() ||
        isUserAlreadyExists() || isMissingRequiredAttributes() || isInvalidAttributes() ||
        isInvalidChallenge() || isAuthMethodBlocked() || isVerificationContactBlocked() ||
        isInvalidInput() || isAuthNotSupported() || isPasswordResetFailed() ||
        isBrowserRequired() || isNotImplemented())

    private fun validationDetails(): List<Map<*, *>> =
        (attributeValidationDetails as? List<*>)?.mapNotNull { it as? Map<*, *> }.orEmpty()

    companion object {
        internal fun fromAttributeError(
            result: com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2CommandResult.AttributesInvalid,
            scenario: NativeAuthFlowScenarioV2
        ): NativeAuthErrorV2 = NativeAuthErrorV2(
            errorType = SignUpErrorTypes.INVALID_ATTRIBUTES,
            error = result.error,
            errorMessage = result.errorDescription,
            correlationId = result.correlationId,
            scenario = scenario,
            traceId = result.traceId,
            timestamp = result.timestamp,
            errorCodes = result.errorCodes,
            subError = result.subError,
            attributeValidationDetails = result.attributeValidationDetails
        )

        private val PASSWORD_POLICY_SUBERRORS = setOf("passwordTooWeak", "passwordTooShort",
            "passwordTooLong", "passwordIsInvalid", "passwordRecentlyUsed", "passwordBanned")
    }
}
